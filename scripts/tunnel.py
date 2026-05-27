#!/usr/bin/env python3
"""
tunnel.py — SSH port-forwarding tool for mtg-bro services.
Usage: python3 scripts/tunnel.py [ssh-host]

Requires: pip install rich questionary
"""

import argparse
import os
import re
import signal
import subprocess
import sys
from pathlib import Path

try:
    import questionary
    from questionary import Style
    from rich.console import Console
    from rich.panel import Panel
    from rich.table import Table
    from rich.text import Text
    from rich import box
except ImportError:
    print("Missing dependencies. Run: pip install rich questionary")
    sys.exit(1)

console = Console()

STYLE = Style([
    ("qmark",        "fg:#a78bfa bold"),
    ("question",     "bold"),
    ("answer",       "fg:#34d399 bold"),
    ("pointer",      "fg:#a78bfa bold"),
    ("highlighted",  "fg:#a78bfa bold"),
    ("selected",     "fg:#34d399"),
    ("separator",    "fg:#6b7280"),
    ("instruction",  "fg:#6b7280 italic"),
])

# Services: (label, local_port, remote_host, remote_port)
# remote_host is resolved from the SSH server's perspective.
# Containers without host ports are reached via Docker DNS (container name).
# postgres is bound to 127.0.0.1:5433 on the server host itself.
# qdrant exposes HTTP on 6333 and gRPC on 6334 on the server host.
SERVICES = [
    ("postgres",               5433, "127.0.0.1", 5433),
    ("qdrant-http",            6333, "127.0.0.1", 6333),
    ("qdrant-grpc",            6334, "127.0.0.1", 6334),
    ("collection-manager",     8080, "127.0.0.1", 8080),
    ("draftsim-parser",        8081, "127.0.0.1", 8081),
    ("wizard-stat-aggregator", 8082, "127.0.0.1", 8082),
    ("auth-service",           8083, "127.0.0.1", 8083),
    ("mcp-server",             3000, "127.0.0.1", 3000),
]


# ── Banner ─────────────────────────────────────────────────────────────────────

def print_banner() -> None:
    art = Text(justify="center")
    art.append("  SSH Tunnel Tool\n", style="bold #a78bfa")
    art.append("  mtg-bro", style="#6b7280")
    console.print(Panel(art, border_style="#7c3aed", padding=(0, 4)))
    console.print()


# ── SSH config helpers ─────────────────────────────────────────────────────────

def read_ssh_hosts() -> list[str]:
    """Parse Host entries from ~/.ssh/config."""
    config = Path.home() / ".ssh" / "config"
    if not config.exists():
        return []
    hosts = []
    for line in config.read_text().splitlines():
        m = re.match(r"^\s*Host\s+(.+)", line, re.IGNORECASE)
        if m:
            # skip wildcards
            for h in m.group(1).split():
                if "*" not in h and "?" not in h:
                    hosts.append(h)
    return hosts


# ── Summary ────────────────────────────────────────────────────────────────────

def print_tunnel_table(host: str, selected: list[tuple]) -> None:
    table = Table(
        box=box.ROUNDED,
        border_style="#4b5563",
        header_style="bold #a78bfa",
        padding=(0, 1),
    )
    table.add_column("Service", style="bold white")
    table.add_column("localhost:port", style="#34d399 bold")
    table.add_column("→ via SSH →", style="#6b7280", justify="center")
    table.add_column("remote", style="#f59e0b")

    for label, local_port, remote_host, remote_port in selected:
        table.add_row(
            label,
            f"localhost:{local_port}",
            f"[dim]{host}[/]",
            f"{remote_host}:{remote_port}",
        )

    console.print(table)
    console.print()


# ── Entry point ────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description="SSH tunnel tool for mtg-bro services.",
        formatter_class=argparse.RawTextHelpFormatter,
    )
    parser.add_argument("--host", metavar="HOST", help="SSH host or alias from ~/.ssh/config")
    parser.add_argument(
        "--services",
        metavar="NAME,...",
        help="Comma-separated service names to tunnel.\nAvailable: " + ", ".join(s[0] for s in SERVICES),
    )
    # positional for backwards compat (legacy: python3 tunnel.py <host>)
    parser.add_argument("host_pos", nargs="?", metavar="HOST_POS", help=argparse.SUPPRESS)
    args = parser.parse_args()

    ssh_host = args.host or args.host_pos

    # Resolve SSH host interactively if not provided
    if not ssh_host:
        print_banner()
        ssh_hosts = read_ssh_hosts()
        if ssh_hosts:
            ssh_host = questionary.autocomplete(
                "SSH host (or alias from ~/.ssh/config):",
                choices=ssh_hosts,
                style=STYLE,
            ).ask()
        else:
            ssh_host = questionary.text("SSH host:", style=STYLE).ask()
        if not ssh_host:
            console.print("[dim]Aborted.[/]")
            sys.exit(0)
        console.print()

    # Resolve services
    if args.services:
        names = {s.strip() for s in args.services.split(",")}
        service_map = {s[0]: s for s in SERVICES}
        unknown = names - service_map.keys()
        if unknown:
            print(f"ERROR: unknown service(s): {', '.join(sorted(unknown))}", file=sys.stderr)
            print(f"Available: {', '.join(s[0] for s in SERVICES)}", file=sys.stderr)
            sys.exit(1)
        selected = [service_map[n] for n in (s[0] for s in SERVICES) if s[0] in names]
    else:
        if not args.host and not args.host_pos:
            pass  # already printed banner above
        else:
            print_banner()
            console.print(f"[dim]Host:[/] [bold]{ssh_host}[/]\n")
        choices = [
            questionary.Choice(
                f"{label:<28} localhost:{local_port}  →  {remote_host}:{remote_port}",
                value=(label, local_port, remote_host, remote_port),
            )
            for label, local_port, remote_host, remote_port in SERVICES
        ]
        selected = questionary.checkbox(
            "Select services to tunnel:",
            choices=choices,
            style=STYLE,
            instruction="(↑↓ navigate, SPACE toggle, ENTER confirm)",
        ).ask()
        if not selected:
            console.print("[dim]Nothing selected. Exiting.[/]")
            sys.exit(0)
        console.print()

    print_tunnel_table(ssh_host, selected)

    # Build ssh command
    forwards = []
    for _, local_port, remote_host, remote_port in selected:
        forwards += ["-L", f"{local_port}:{remote_host}:{remote_port}"]

    cmd = ["ssh", "-N", *forwards, ssh_host]
    console.print(f"[dim]$ {' '.join(cmd)}[/]")
    console.print()
    console.print("[bold #34d399]Tunnels open.[/] Press [bold]Ctrl-C[/] to close.\n")

    proc = subprocess.Popen(cmd)
    try:
        proc.wait()
    except KeyboardInterrupt:
        proc.send_signal(signal.SIGINT)
        proc.wait()
        console.print("\n[dim]Tunnels closed.[/]")


if __name__ == "__main__":
    main()
