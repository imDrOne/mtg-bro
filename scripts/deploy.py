#!/usr/bin/env python3
"""
deploy.py — interactive deploy tool for mtg-bro.
Usage: python3 scripts/deploy.py

Requires: pip install rich questionary
"""

import argparse
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
    from rich.live import Live
    from rich.spinner import Spinner
    from rich.columns import Columns
except ImportError:
    print("Missing dependencies. Run: pip install rich questionary")
    sys.exit(1)

REPO_ROOT = Path(__file__).parent.parent


def get_github_repo() -> str | None:
    """Return 'owner/repo' parsed from git remote origin, or None."""
    result = subprocess.run(
        ["git", "remote", "get-url", "origin"],
        capture_output=True, text=True, cwd=REPO_ROOT,
    )
    url = result.stdout.strip()
    if not url:
        return None
    # SSH: git@github.com:owner/repo.git
    if url.startswith("git@github.com:"):
        path = url.removeprefix("git@github.com:")
    # HTTPS: https://github.com/owner/repo.git
    elif "github.com/" in url:
        path = url.split("github.com/", 1)[1]
    else:
        return None
    return path.removesuffix(".git")


GITHUB_REPO: str | None = get_github_repo()

MODULES = [
    "collection-manager",
    "draftsim-parser",
    "mcp-server",
    "wizard-stat-aggregator",
    "auth-service",
    "caddy",
]

BUMP_TYPES = ["PATCH", "MINOR", "MAJOR"]

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
    ("text",         ""),
])


# ── Banner ─────────────────────────────────────────────────────────────────────

def print_banner() -> None:
    art = Text(justify="center")
    art.append("  ███╗   ███╗████████╗ ██████╗      ██████╗ ██████╗  ██████╗ \n", style="bold #7c3aed")
    art.append("  ████╗ ████║╚══██╔══╝██╔════╝      ██╔══██╗██╔══██╗██╔═══██╗\n", style="bold #8b5cf6")
    art.append("  ██╔████╔██║   ██║   ██║  ███╗     ██████╔╝██████╔╝██║   ██║\n", style="bold #a78bfa")
    art.append("  ██║╚██╔╝██║   ██║   ██║   ██║     ██╔══██╗██╔══██╗██║   ██║\n", style="bold #c4b5fd")
    art.append("  ██║ ╚═╝ ██║   ██║   ╚██████╔╝     ██████╔╝██║  ██║╚██████╔╝\n", style="bold #ddd6fe")
    art.append("  ╚═╝     ╚═╝   ╚═╝    ╚═════╝      ╚═════╝ ╚═╝  ╚═╝ ╚═════╝ \n", style="#ede9fe")
    art.append("\n                    deploy tool  🚀", style="bold #a78bfa")
    console.print(Panel(art, border_style="#7c3aed", padding=(0, 2)))
    console.print()


# ── Git helpers ────────────────────────────────────────────────────────────────

def get_latest_tag(module: str) -> tuple[int, int, int] | None:
    result = subprocess.run(
        ["git", "tag", "--list", f"{module}/v*", "--sort=-version:refname"],
        capture_output=True, text=True, cwd=REPO_ROOT,
    )
    tags = [t.strip() for t in result.stdout.strip().splitlines() if t.strip()]
    if not tags:
        return None
    version_str = tags[0].split("/v", 1)[1]
    parts = version_str.split(".")
    if len(parts) != 3:
        return None
    try:
        return int(parts[0]), int(parts[1]), int(parts[2])
    except ValueError:
        return None


def bump_version(tag: tuple[int, int, int] | None, bump: str) -> str:
    if tag is None:
        base = (0, 0, 0)
    else:
        base = tag
    major, minor, patch = base
    if bump == "MAJOR":
        return f"v{major + 1}.0.0"
    elif bump == "MINOR":
        return f"v{major}.{minor + 1}.0"
    else:  # PATCH
        return f"v{major}.{minor}.{patch + 1}"


def current_version_str(module: str) -> str:
    tag = get_latest_tag(module)
    if tag is None:
        return "(none)"
    return "v{}.{}.{}".format(*tag)


# ── Preflight ──────────────────────────────────────────────────────────────────

def preflight_check() -> None:
    result = subprocess.run(
        ["git", "status", "--porcelain"],
        capture_output=True, text=True, cwd=REPO_ROOT,
    )
    if result.stdout.strip():
        console.print(Panel(
            "[bold yellow]⚠  Working tree has uncommitted changes.[/]\n"
            "[dim]Tags point to HEAD commit — dirty tree won't affect the tag.[/]",
            border_style="yellow", padding=(0, 2),
        ))
        confirmed = questionary.confirm(
            "Continue anyway?", default=False, style=STYLE,
        ).ask()
        if not confirmed:
            console.print("[dim]Aborted.[/]")
            sys.exit(0)
        console.print()


# ── Summary table ──────────────────────────────────────────────────────────────

def print_summary_table(plan: list[dict]) -> None:
    table = Table(
        box=box.ROUNDED,
        border_style="#4b5563",
        header_style="bold #a78bfa",
        show_lines=False,
        padding=(0, 1),
    )
    table.add_column("Module", style="bold white")
    table.add_column("Current", style="#6b7280")
    table.add_column("Bump", style="#f59e0b bold", justify="center")
    table.add_column("New tag", style="bold #34d399")

    bump_colors = {"MAJOR": "#f87171", "MINOR": "#fb923c", "PATCH": "#34d399"}
    for p in plan:
        color = bump_colors.get(p["bump"], "#34d399")
        table.add_row(
            p["module"],
            p["current"],
            f"[{color}]{p['bump']}[/]",
            p["next"],
        )

    console.print(table)
    console.print()


# ── Deploy ─────────────────────────────────────────────────────────────────────

def pipeline_url(module: str) -> str | None:
    if GITHUB_REPO:
        return f"https://github.com/{GITHUB_REPO}/actions/workflows/deploy-{module}.yml"
    return None


def run_deploy(module: str, version: str) -> bool:
    tag = f"{module}/{version}"
    for cmd in [
        ["git", "tag", tag],
        ["git", "push", "origin", tag],
    ]:
        result = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True)
        if result.returncode != 0:
            err = result.stderr.strip() or result.stdout.strip()
            console.print(f"  [bold red]✗[/] [red]{' '.join(cmd)}[/]\n    [dim]{err}[/]")
            return False
    url = pipeline_url(module)
    link = f"  [dim]→ {url}[/]" if url else ""
    console.print(f"  [bold #34d399]✓[/] pushed [bold]{tag}[/]{link}")
    return True


# ── Entry point ────────────────────────────────────────────────────────────────

def main() -> None:
    parser = argparse.ArgumentParser(
        description="Deploy tool for mtg-bro modules.",
        formatter_class=argparse.RawTextHelpFormatter,
    )
    parser.add_argument(
        "--modules", "-m",
        metavar="NAME,...",
        help="Comma-separated modules to deploy.\nAvailable: " + ", ".join(MODULES),
    )
    parser.add_argument(
        "--bump", "-b",
        choices=["PATCH", "MINOR", "MAJOR"],
        metavar="TYPE",
        help="Version bump type: PATCH, MINOR, or MAJOR",
    )
    parser.add_argument(
        "--yes", "-y",
        action="store_true",
        help="Skip confirmation prompt",
    )
    args = parser.parse_args()

    # --- Non-interactive path ---
    if args.modules and args.bump:
        selected = [m.strip() for m in args.modules.split(",")]
        unknown = set(selected) - set(MODULES)
        if unknown:
            console.print(f"[bold red]ERROR:[/] unknown module(s): {', '.join(sorted(unknown))}")
            console.print(f"Available: {', '.join(MODULES)}")
            sys.exit(1)
        bump = args.bump

        plan = []
        for m in selected:
            tag = get_latest_tag(m)
            plan.append({
                "module":  m,
                "current": current_version_str(m),
                "bump":    bump,
                "next":    bump_version(tag, bump),
            })

        print_summary_table(plan)

        if not args.yes:
            confirmed = questionary.confirm("Deploy now?", default=True, style=STYLE).ask()
            if not confirmed:
                console.print("[dim]Aborted.[/]")
                sys.exit(0)
            console.print()

        ok, fail = 0, 0
        for p in plan:
            success = run_deploy(p["module"], p["next"])
            if success:
                ok += 1
            else:
                fail += 1

        console.print()
        if fail == 0:
            lines = [f"[bold #34d399]✓ All {ok} module(s) deployed — pipelines running:[/]"]
            for p in plan:
                url = pipeline_url(p["module"])
                if url:
                    lines.append(f"  [dim]{p['module']}[/]  [link={url}]{url}[/link]")
            console.print(Panel("\n".join(lines), border_style="#34d399", padding=(0, 2)))
        else:
            lines = [f"[bold #34d399]✓ {ok} succeeded[/]  [bold red]✗ {fail} failed[/]"]
            for p in plan:
                url = pipeline_url(p["module"])
                if url:
                    lines.append(f"  [dim]{p['module']}[/]  [link={url}]{url}[/link]")
            console.print(Panel("\n".join(lines), border_style="red", padding=(0, 2)))
            sys.exit(1)
        return

    # --- Interactive path (original) ---
    print_banner()
    preflight_check()

    selected = questionary.checkbox(
        "Select modules to deploy:",
        choices=MODULES,
        style=STYLE,
        instruction="(↑↓ navigate, SPACE toggle, ENTER confirm)",
    ).ask()

    if not selected:
        console.print("[dim]Nothing selected. Exiting.[/]")
        sys.exit(0)

    console.print()

    bump = questionary.select(
        "Version bump type:",
        choices=[
            questionary.Choice("PATCH  — bug fixes, e.g. 1.2.3 → 1.2.4", value="PATCH"),
            questionary.Choice("MINOR  — new features, e.g. 1.2.3 → 1.3.0", value="MINOR"),
            questionary.Choice("MAJOR  — breaking changes, e.g. 1.2.3 → 2.0.0", value="MAJOR"),
        ],
        style=STYLE,
    ).ask()

    if bump is None:
        console.print("[dim]Aborted.[/]")
        sys.exit(0)

    console.print()

    plan = []
    for m in selected:
        tag = get_latest_tag(m)
        plan.append({
            "module":  m,
            "current": current_version_str(m),
            "bump":    bump,
            "next":    bump_version(tag, bump),
        })

    print_summary_table(plan)

    confirmed = questionary.confirm("Deploy now?", default=True, style=STYLE).ask()

    if not confirmed:
        console.print("[dim]Aborted.[/]")
        sys.exit(0)

    console.print()

    ok, fail = 0, 0
    for p in plan:
        success = run_deploy(p["module"], p["next"])
        if success:
            ok += 1
        else:
            fail += 1

    console.print()
    if fail == 0:
        lines = [f"[bold #34d399]✓ All {ok} module(s) deployed — pipelines running:[/]"]
        for p in plan:
            url = pipeline_url(p["module"])
            if url:
                lines.append(f"  [dim]{p['module']}[/]  [link={url}]{url}[/link]")
        console.print(Panel("\n".join(lines), border_style="#34d399", padding=(0, 2)))
    else:
        lines = [f"[bold #34d399]✓ {ok} succeeded[/]  [bold red]✗ {fail} failed[/]"]
        for p in plan:
            url = pipeline_url(p["module"])
            if url:
                lines.append(f"  [dim]{p['module']}[/]  [link={url}]{url}[/link]")
        console.print(Panel("\n".join(lines), border_style="red", padding=(0, 2)))
        sys.exit(1)


if __name__ == "__main__":
    main()
