#!/usr/bin/env python3
"""
deploy.py — interactive deploy tool for mtg-bro.
Usage: python3 scripts/deploy.py
"""

import curses
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).parent.parent

MODULES = [
    "collection-manager",
    "draftsim-parser",
    "mcp-server",
    "wizard-stat-aggregator",
]


# ── Banner ─────────────────────────────────────────────────────────────────────

def print_banner() -> None:
    print("""
  _ __ ___ | |_ __ _       | |__ _ __ ___
 | '_ ` _ \\| __/ _` |      | '_ \\| '__/ _ \\
 | | | | | | || (_| |  _   | |_) | | | (_) |
 |_| |_| |_|\\__\\__, | (_)  |_.__/|_|  \\___/
                |___/
        deploy tool
""")


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


def next_version(module: str) -> str:
    tag = get_latest_tag(module)
    if tag is None:
        return "v0.0.1"
    major, minor, patch = tag
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
        print("WARNING: working tree has uncommitted changes.")
        print("Tags will point to the current HEAD commit regardless.")
        answer = input("Continue? [y/N]: ").strip().lower()
        if answer != "y":
            sys.exit(0)
        print()


# ── Curses multiselect ─────────────────────────────────────────────────────────

class CheckboxMenu:
    HEADER = "Select modules to deploy   [SPACE=toggle  ENTER=confirm  q=quit]"

    def __init__(self, items: list[str]) -> None:
        self.items = items
        self.cursor = 0
        self.selected: set[int] = set()
        self._warn = False

    def run(self) -> list[str]:
        try:
            return curses.wrapper(self._main)
        except curses.error:
            return self._fallback()

    def _main(self, stdscr) -> list[str]:
        curses.curs_set(0)
        curses.start_color()
        curses.use_default_colors()
        while True:
            self._draw(stdscr)
            key = stdscr.getch()
            if key in (curses.KEY_UP, ord("k")):
                self.cursor = max(0, self.cursor - 1)
                self._warn = False
            elif key in (curses.KEY_DOWN, ord("j")):
                self.cursor = min(len(self.items) - 1, self.cursor + 1)
                self._warn = False
            elif key == ord(" "):
                if self.cursor in self.selected:
                    self.selected.discard(self.cursor)
                else:
                    self.selected.add(self.cursor)
                self._warn = False
            elif key in (curses.KEY_ENTER, 10, 13):
                if not self.selected:
                    self._warn = True
                    continue
                return [self.items[i] for i in sorted(self.selected)]
            elif key in (ord("q"), ord("Q"), 27):  # ESC
                sys.exit(0)

    def _draw(self, stdscr) -> None:
        stdscr.clear()
        h, w = stdscr.getmaxyx()
        header = self.HEADER[:w - 1]
        stdscr.addstr(0, 0, header, curses.A_BOLD)
        stdscr.addstr(1, 0, "─" * min(len(self.HEADER), w - 1))
        for idx, item in enumerate(self.items):
            row = idx + 2
            if row >= h - 1:
                break
            mark = "x" if idx in self.selected else " "
            line = f"  [{mark}] {item}"[:w - 1]
            attr = curses.A_REVERSE if idx == self.cursor else curses.A_NORMAL
            stdscr.addstr(row, 0, line, attr)
        if self._warn:
            warn = "  Nothing selected — use SPACE to toggle"[:w - 1]
            stdscr.addstr(len(self.items) + 3, 0, warn, curses.A_BOLD)
        stdscr.refresh()

    def _fallback(self) -> list[str]:
        """Plain text fallback when curses is unavailable."""
        print("Select modules (space-separated numbers, e.g. 1 3):")
        for i, m in enumerate(self.items, 1):
            print(f"  {i}. {m}")
        raw = input("> ").strip()
        indices = []
        for part in raw.split():
            try:
                n = int(part) - 1
                if 0 <= n < len(self.items):
                    indices.append(n)
            except ValueError:
                pass
        return [self.items[i] for i in sorted(set(indices))]


# ── Summary ────────────────────────────────────────────────────────────────────

def print_summary_table(plan: list[dict]) -> None:
    col1 = max(len(p["module"]) for p in plan)
    col2 = max(len(p["current"]) for p in plan)
    col3 = max(len(p["next"]) for p in plan)
    col1 = max(col1, len("Module"))
    col2 = max(col2, len("Current"))
    col3 = max(col3, len("New tag"))
    fmt = f"  {{:<{col1}}}  {{:<{col2}}}  {{:<{col3}}}"
    sep = "  " + "─" * (col1 + col2 + col3 + 4)
    print()
    print(fmt.format("Module", "Current", "New tag"))
    print(sep)
    for p in plan:
        print(fmt.format(p["module"], p["current"], p["next"]))
    print()


# ── Deploy ─────────────────────────────────────────────────────────────────────

def run_deploy(module: str, version: str) -> bool:
    tag = f"{module}/{version}"
    for cmd in [
        ["git", "tag", tag],
        ["git", "push", "origin", tag],
    ]:
        result = subprocess.run(cmd, cwd=REPO_ROOT)
        if result.returncode != 0:
            print(f"  ERROR: {' '.join(cmd)}")
            return False
    print(f"  pushed {tag}")
    return True


# ── Entry point ────────────────────────────────────────────────────────────────

def main() -> None:
    print_banner()
    preflight_check()

    selected = CheckboxMenu(MODULES).run()
    if not selected:
        print("Nothing selected.")
        sys.exit(0)

    plan = [
        {"module": m, "current": current_version_str(m), "next": next_version(m)}
        for m in selected
    ]

    print_summary_table(plan)

    try:
        input("Press ENTER to deploy, Ctrl-C to abort...")
    except KeyboardInterrupt:
        print("\nAborted.")
        sys.exit(0)

    print()
    ok, fail = 0, 0
    for p in plan:
        success = run_deploy(p["module"], p["next"])
        if success:
            ok += 1
        else:
            fail += 1

    print()
    print(f"Done: {ok} succeeded, {fail} failed.")
    if fail:
        sys.exit(1)


if __name__ == "__main__":
    main()
