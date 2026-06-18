#!/usr/bin/env python3
"""Render the KAI OS evidence-proof launch image."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


WIDTH = 1600
HEIGHT = 900

BG = (11, 15, 19)
PANEL = (17, 24, 31)
PANEL_2 = (21, 30, 38)
INNER = (14, 20, 26)
LINE = (49, 64, 79)
TEXT = (244, 247, 251)
MUTED = (159, 174, 190)
SOFT = (201, 211, 221)
MINT = (132, 236, 182)
CYAN = (104, 209, 247)
AMBER = (241, 184, 88)
RED = (244, 120, 120)
GREEN_BG = (21, 54, 42)
AMBER_BG = (62, 44, 20)
CYAN_BG = (18, 46, 58)
RED_BG = (62, 28, 30)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/SFNS.ttf",
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf" if bold else "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for candidate in candidates:
        if candidate and Path(candidate).exists():
            return ImageFont.truetype(candidate, size=size)
    return ImageFont.load_default()


def mono(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/Menlo.ttc",
        "/Library/Fonts/Menlo.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    ]
    for candidate in candidates:
        path = Path(candidate)
        if path.exists():
            return ImageFont.truetype(str(path), size=size, index=1 if bold and path.suffix == ".ttc" else 0)
    return ImageFont.load_default()


def pill(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], text: str, fill: tuple[int, int, int], fg: tuple[int, int, int]) -> None:
    draw.rounded_rectangle(box, radius=16, fill=fill, outline=LINE, width=1)
    x1, y1, x2, y2 = box
    bbox = draw.textbbox((0, 0), text, font=mono(22, True))
    draw.text((x1 + (x2 - x1 - bbox[2]) / 2, y1 + (y2 - y1 - bbox[3]) / 2 - 2), text, font=mono(22, True), fill=fg)


def draw_panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str, subtitle: str | None = None) -> None:
    draw.rounded_rectangle(box, radius=18, fill=PANEL, outline=LINE, width=2)
    x1, y1, _, _ = box
    draw.text((x1 + 28, y1 + 24), title, font=font(30, True), fill=TEXT)
    if subtitle:
        draw.text((x1 + 28, y1 + 62), subtitle, font=mono(18), fill=MUTED)


def draw_process_table(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    headers = ["PID", "AGENT", "STATE", "TOK", "MEM", "SYSCALLS", "COST"]
    rows = [
        ["101", "planner", "SUCCEEDED", "412", "9 KB", "2", "$0.000"],
        ["102", "executor", "SUCCEEDED", "731", "18 KB", "5", "$0.000"],
        ["103", "validator", "RECOVERED", "286", "7 KB", "1", "$0.000"],
    ]
    widths = [70, 150, 150, 76, 90, 122, 104]
    height = 48
    draw.rounded_rectangle((x, y, x + sum(widths), y + height * 4), radius=12, fill=INNER, outline=LINE, width=1)
    cx = x
    for i, header in enumerate(headers):
        draw.text((cx + 16, y + 16), header, font=mono(18, True), fill=MUTED)
        cx += widths[i]
    for r, row in enumerate(rows):
        ry = y + height * (r + 1)
        draw.line((x, ry, x + sum(widths), ry), fill=LINE, width=1)
        cx = x
        for i, value in enumerate(row):
            color = TEXT
            if value == "SUCCEEDED":
                color = MINT
            elif value == "RECOVERED":
                color = AMBER
            draw.text((cx + 16, ry + 15), value, font=mono(18), fill=color)
            cx += widths[i]


def draw_syscall_ledger(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    rows = [
        ("ALLOW", "file.read", "workspace diff", "18ms"),
        ("ALLOW", "model.mock", "deterministic", "42ms"),
        ("DENY", "http.post", "outside grant", "0ms"),
        ("ALLOW", "capsule.write", "portable replay", "7ms"),
    ]
    for i, (status, tool, scope, ms) in enumerate(rows):
        yy = y + i * 68
        bg = GREEN_BG if status == "ALLOW" else RED_BG
        fg = MINT if status == "ALLOW" else RED
        draw.rounded_rectangle((x, yy, x + 552, yy + 52), radius=12, fill=INNER, outline=LINE, width=1)
        pill(draw, (x + 14, yy + 10, x + 104, yy + 42), status, bg, fg)
        draw.text((x + 124, yy + 14), tool, font=mono(20, True), fill=TEXT)
        draw.text((x + 310, yy + 14), scope, font=mono(18), fill=MUTED)
        draw.text((x + 492, yy + 14), ms, font=mono(18), fill=CYAN)


def draw_capsule(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    capsule = [
        "{",
        '  "schema": "kaios.review/v1",',
        '  "runId": "run_demo_001",',
        '  "replay": "offline",',
        '  "baselineDiff": "stable",',
        '  "verdict": "PASS"',
        "}",
    ]
    draw.rounded_rectangle((x, y, x + 552, y + 158), radius=14, fill=INNER, outline=LINE, width=1)
    for i, line in enumerate(capsule):
        color = MINT if "PASS" in line or "stable" in line else (CYAN if "kaios.review" in line or "offline" in line else SOFT)
        draw.text((x + 22, y + 16 + i * 20), line, font=mono(16), fill=color)


def render(out: Path) -> None:
    image = Image.new("RGB", (WIDTH, HEIGHT), BG)
    draw = ImageDraw.Draw(image)

    draw.rounded_rectangle((44, 42, WIDTH - 44, HEIGHT - 42), radius=34, fill=(13, 18, 23), outline=LINE, width=2)
    draw.text((88, 78), "KAI OS", font=font(56, True), fill=TEXT)
    draw.text((88, 142), "Every agent run becomes evidence you can inspect, replay, and gate.", font=font(34, True), fill=TEXT)
    draw.text((90, 190), "Agent = Process    Workflow = Scheduler    Tool = Syscall    Run = Evidence", font=mono(22), fill=MUTED)

    pill(draw, (1250, 84, 1456, 126), "NO API KEY", GREEN_BG, MINT)
    pill(draw, (1250, 138, 1456, 180), "LOCAL FIRST", CYAN_BG, CYAN)

    draw_panel(draw, (88, 252, 900, 520), "Process Table", "PID, lifecycle, memory, syscalls, cost")
    draw_process_table(draw, 116, 342)

    draw_panel(draw, (936, 252, 1512, 590), "Syscall Ledger", "Every tool call is audited")
    draw_syscall_ledger(draw, 964, 342)

    draw_panel(draw, (88, 560, 704, 806), "Replay Capsule", "Portable offline proof")
    draw_capsule(draw, 116, 638)

    draw_panel(draw, (740, 626, 1512, 806), "CI Evidence Gate", "Compare runtime behavior, fail on regressions")
    pill(draw, (774, 710, 924, 758), "PASS", GREEN_BG, MINT)
    draw.text((952, 720), "kaios evidence --baseline --check", font=mono(27, True), fill=TEXT)
    draw.text((88, 830), "github.com/morning-verlu/KAI", font=mono(24), fill=MUTED)
    draw.text((924, 830), "Evidence Viewer: morning-verlu.github.io/KAI", font=mono(21), fill=CYAN)

    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    print(f"wrote {out} ({out.stat().st_size} bytes)")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", default="docs/assets/kaios-evidence-proof.png")
    args = parser.parse_args()
    render(Path(args.out))


if __name__ == "__main__":
    main()
