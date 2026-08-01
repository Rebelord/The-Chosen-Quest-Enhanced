#!/usr/bin/env python3
"""Create the shareable private beta invitation and quick-reference assets."""

from pathlib import Path
import re
import textwrap

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont
from reportlab.graphics import renderPDF
from reportlab.graphics.barcode import qr
from reportlab.graphics.shapes import Drawing
from reportlab.lib.colors import Color, HexColor, white
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import inch
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen.canvas import Canvas
from reportlab.platypus import Paragraph


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "private-tester-kit"
ASSETS = OUT / "assets"
DOCS = OUT / "docs"
BACKGROUND = ASSETS / "private-beta-invite-background.png"
COMMUNITY_BANNER = ROOT / "assets" / "community" / "The-Chosen-Quest-Banner.png"
TESTER_GUIDE = ROOT / "output" / "pdf" / "The-Chosen-Quest-Enhanced-Tester-Guide.pdf"
FONT_DISPLAY = ROOT / "assets" / "fonts" / "Cinzel.ttf"
FONT_BODY = ROOT / "assets" / "fonts" / "CormorantGaramond.ttf"
APP_ICON = ROOT / "assets" / "app-icon.png"
PANEL_FRAME = ROOT / "assets" / "ui" / "borders" / "panel-frame-brass-v1.png"

VERSION_SOURCE = (
    ROOT / "src" / "thechosenquest" / "desktop" / "AppVersion.java"
).read_text(encoding="utf-8")
VERSION_MATCH = re.search(r'VERSION\s*=\s*"([^"]+)"', VERSION_SOURCE)
if not VERSION_MATCH:
    raise RuntimeError("Unable to read AppVersion.VERSION")
VERSION = VERSION_MATCH.group(1)
RELEASE_URL = (
    "https://github.com/Rebelord/The-Chosen-Quest-Enhanced/"
    f"releases/tag/v{VERSION}"
)
DOWNLOAD_URL = (
    "https://github.com/Rebelord/The-Chosen-Quest-Enhanced/releases/download/"
    f"v{VERSION}/The-Chosen-Quest-Enhanced-{VERSION}.zip"
)
FEEDBACK_URL = "https://forms.gle/GJLCPtzP7LN9PeBeA"
BUG_URL = "https://forms.gle/F8LtxPn5sKbwPAxX9"

GOLD = "#D9B56D"
PALE_GOLD = "#F3D89A"
PARCHMENT = "#E9D6AA"
INK = "#17120C"
DEEP = "#0D0B09"
CRIMSON = "#8B3028"
CLASS_COLORS = ("#B84C3F", "#3D93DA", "#9150BE", "#49A066")


def fit_font(draw, text, path, max_size, min_size, width, stroke_width=0):
    for size in range(max_size, min_size - 1, -1):
        font = ImageFont.truetype(str(path), size)
        box = draw.textbbox((0, 0), text, font=font, stroke_width=stroke_width)
        if box[2] - box[0] <= width:
            return font
    return ImageFont.truetype(str(path), min_size)


def cover_crop(image, size, focus_x=0.5, focus_y=0.5):
    target_w, target_h = size
    scale = max(target_w / image.width, target_h / image.height)
    resized = image.resize(
        (round(image.width * scale), round(image.height * scale)),
        Image.Resampling.LANCZOS,
    )
    left = round((resized.width - target_w) * focus_x)
    top = round((resized.height - target_h) * focus_y)
    left = max(0, min(left, resized.width - target_w))
    top = max(0, min(top, resized.height - target_h))
    return resized.crop((left, top, left + target_w, top + target_h))


def nine_slice(image, size, source_cap=175, destination_cap=58):
    """Resize authored border rails without stretching painterly corners."""
    target_w, target_h = size
    result = Image.new("RGBA", size, (0, 0, 0, 0))
    source_w, source_h = image.size
    destination_cap = min(
        destination_cap, target_w // 2, target_h // 2
    )
    sx = (0, source_cap, source_w - source_cap, source_w)
    sy = (0, source_cap, source_h - source_cap, source_h)
    dx = (0, destination_cap, target_w - destination_cap, target_w)
    dy = (0, destination_cap, target_h - destination_cap, target_h)
    for row in range(3):
        for column in range(3):
            crop = image.crop((sx[column], sy[row], sx[column + 1], sy[row + 1]))
            width = dx[column + 1] - dx[column]
            height = dy[row + 1] - dy[row]
            if width <= 0 or height <= 0:
                continue
            crop = crop.resize((width, height), Image.Resampling.LANCZOS)
            result.alpha_composite(crop, (dx[column], dy[row]))
    return result


def draw_diamond(draw, center, radius, fill, outline=GOLD):
    x, y = center
    points = ((x, y - radius), (x + radius, y),
              (x, y + radius), (x - radius, y))
    draw.polygon(points, fill=fill, outline=outline)


def add_invite_typography(image, layout):
    draw = ImageDraw.Draw(image, "RGBA")
    width, height = image.size

    if layout == "landscape":
        panel = (95, 92, 1215, 676)
        title_width = 1010
        kicker_size = 42
        title_max = 106
        title_min = 68
        sub_size = 46
        meta_size = 31
        line_height = 108
    else:
        panel = (58, 62, 1022, 674)
        title_width = 850
        kicker_size = 34
        title_max = 79
        title_min = 58
        sub_size = 37
        meta_size = 27
        line_height = 88

    px0, py0, px1, py1 = panel
    panel_layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    panel_draw = ImageDraw.Draw(panel_layer, "RGBA")
    cut = 26 if layout == "landscape" else 22
    panel_points = (
        (px0 + cut, py0), (px1 - cut, py0), (px1, py0 + cut),
        (px1, py1 - cut), (px1 - cut, py1), (px0 + cut, py1),
        (px0, py1 - cut), (px0, py0 + cut),
    )
    panel_draw.polygon(panel_points, fill=(10, 8, 6, 205))
    panel_draw.line(panel_points + (panel_points[0],),
                    fill=(217, 181, 109, 115), width=2, joint="curve")
    image.alpha_composite(panel_layer)
    authored_frame = nine_slice(
        Image.open(PANEL_FRAME).convert("RGBA"),
        (px1 - px0, py1 - py0),
        destination_cap=52 if layout == "landscape" else 46,
    )
    image.alpha_composite(authored_frame, (px0, py0))
    draw = ImageDraw.Draw(image, "RGBA")

    display_small = ImageFont.truetype(str(FONT_DISPLAY), kicker_size)
    body = ImageFont.truetype(str(FONT_BODY), sub_size)
    meta = ImageFont.truetype(str(FONT_BODY), meta_size)
    title_font = fit_font(
        draw,
        "THE CHOSEN QUEST",
        FONT_DISPLAY,
        title_max,
        title_min,
        title_width,
        stroke_width=2,
    )

    x = px0 + 52
    y = py0 + 44
    draw.text(
        (x, y),
        "PRIVATE BETA INVITATION",
        font=display_small,
        fill=GOLD,
        stroke_width=1,
        stroke_fill=(37, 23, 12, 255),
    )
    y += kicker_size + 30
    draw.text(
        (x, y),
        "THE CHOSEN QUEST",
        font=title_font,
        fill=PALE_GOLD,
        stroke_width=2,
        stroke_fill=(24, 15, 8, 255),
    )
    y += line_height
    draw.text(
        (x, y),
        "ENHANCED EDITION",
        font=display_small,
        fill=PARCHMENT,
        stroke_width=1,
        stroke_fill=(20, 13, 8, 255),
    )
    y += kicker_size + 34
    draw.text(
        (x, y),
        "Your quest: explore, survive, and help shape the adventure.",
        font=body,
        fill=(244, 231, 199, 255),
    )
    y += sub_size + 34
    bar_right = px1 - 52
    bar_cut = 12
    bar_points = (
        (x + bar_cut, y), (bar_right - bar_cut, y),
        (bar_right, y + bar_cut), (bar_right, y + 58 - bar_cut),
        (bar_right - bar_cut, y + 58), (x + bar_cut, y + 58),
        (x, y + 58 - bar_cut), (x, y + bar_cut),
    )
    draw.polygon(bar_points, fill=(53, 28, 22, 232))
    draw.line(bar_points + (bar_points[0],),
              fill=(242, 207, 135, 190), width=2)
    draw.text(
        (x + 20, y + 9),
        f"PRIVATE PLAYTEST BUILD  |  v{VERSION}",
        font=meta,
        fill=(255, 238, 198, 255),
    )
    diamond_spacing = 25 if layout == "landscape" else 22
    diamond_start = bar_right - diamond_spacing * 4 - 12
    for index, color in enumerate(CLASS_COLORS):
        draw_diamond(
            draw,
            (diamond_start + index * diamond_spacing, y + 29),
            7 if layout == "landscape" else 6,
            color,
        )

    footer_font = ImageFont.truetype(str(FONT_BODY), 25 if layout == "landscape" else 23)
    footer = "Java 8+  |  Windows - macOS - Linux - Chromebook Linux"
    fb = draw.textbbox((0, 0), footer, font=footer_font)
    draw.text(
        ((width - (fb[2] - fb[0])) / 2, height - 48),
        footer,
        font=footer_font,
        fill=(239, 218, 173, 255),
        stroke_width=1,
        stroke_fill=(12, 8, 4, 220),
    )
    draw.text(
        ((width - (fb[2] - fb[0])) / 2, height - 48),
        footer,
        font=footer_font,
        fill=(239, 218, 173, 255),
        stroke_width=1,
        stroke_fill=(12, 8, 4, 220),
    )


def create_invite_art():
    source = Image.open(BACKGROUND).convert("RGBA")

    landscape = cover_crop(source, (1920, 1080), 0.5, 0.5)
    landscape = ImageEnhance.Contrast(landscape).enhance(1.05)
    add_invite_typography(landscape, "landscape")
    landscape.convert("RGB").save(
        ASSETS / "The-Chosen-Quest-Private-Beta-Invite-1920x1080.jpg",
        quality=94,
        optimize=True,
    )

    square = cover_crop(source, (1080, 1080), 0.36, 0.5)
    square = ImageEnhance.Contrast(square).enhance(1.05)
    add_invite_typography(square, "square")
    square.convert("RGB").save(
        ASSETS / "The-Chosen-Quest-Private-Beta-Invite-1080x1080.jpg",
        quality=94,
        optimize=True,
    )

    banner = cover_crop(Image.open(COMMUNITY_BANNER).convert("RGB"), (1600, 640), 0.5, 0.5)
    banner.save(
        ASSETS / "The-Chosen-Quest-Community-Banner-1600x640.jpg",
        quality=94,
        optimize=True,
    )


def draw_qr(canvas, url, x, y, size):
    widget = qr.QrCodeWidget(url)
    bounds = widget.getBounds()
    drawing = Drawing(
        size,
        size,
        transform=[
            size / (bounds[2] - bounds[0]),
            0,
            0,
            size / (bounds[3] - bounds[1]),
            0,
            0,
        ],
    )
    drawing.add(widget)
    renderPDF.draw(drawing, canvas, x, y)


def paragraph(canvas, text, x, y, width, style):
    p = Paragraph(text, style)
    _, height = p.wrap(width, 1000)
    p.drawOn(canvas, x, y - height)
    return y - height


def section_title(canvas, text, x, y, width):
    canvas.setFillColor(HexColor(CRIMSON))
    canvas.roundRect(x, y - 19, width, 25, 5, stroke=0, fill=1)
    canvas.setFillColor(white)
    canvas.setFont("Cinzel", 9.4)
    canvas.drawString(x + 9, y - 11.5, text.upper())
    return y - 29


def create_quick_reference_pdf():
    pdfmetrics.registerFont(TTFont("Cinzel", str(FONT_DISPLAY)))
    pdfmetrics.registerFont(TTFont("Cormorant", str(FONT_BODY)))

    output = DOCS / "The-Chosen-Quest-Private-Tester-Quick-Reference.pdf"
    canvas = Canvas(str(output), pagesize=letter)
    canvas.setTitle("The Chosen Quest Enhanced - Private Tester Quick Reference")
    canvas.setAuthor("The Chosen Quest Enhanced")
    canvas.setSubject(f"Private beta playtest quick reference for v{VERSION}")
    page_w, page_h = letter
    margin = 0.43 * inch

    # Use the clean no-copy master so the document title remains editable and
    # never competes with a baked wordmark.
    header = Image.open(BACKGROUND).convert("RGB")
    header = cover_crop(header, (1700, 430), 0.5, 0.39)
    icon = Image.open(APP_ICON).convert("RGB").resize(
        (190, 190), Image.Resampling.LANCZOS
    )
    header.paste(icon, (105, 115))
    header_path = ROOT / "tmp" / "pdfs" / "private-tester-kit" / "header.jpg"
    header.save(header_path, quality=92)
    canvas.drawImage(str(header_path), 0, page_h - 2.05 * inch, page_w, 2.05 * inch)

    canvas.setFillColor(Color(0.03, 0.025, 0.02, 0.76))
    canvas.rect(0, page_h - 2.05 * inch, page_w, 2.05 * inch, stroke=0, fill=1)
    canvas.setFillColor(HexColor(PALE_GOLD))
    canvas.setFont("Cinzel", 25)
    canvas.drawCentredString(page_w / 2, page_h - 0.69 * inch, "PRIVATE BETA PLAYTEST")
    canvas.setFillColor(HexColor(PARCHMENT))
    canvas.setFont("Cormorant", 15.5)
    canvas.drawCentredString(
        page_w / 2,
        page_h - 1.08 * inch,
        "A quick reference for invited testers",
    )
    canvas.setFillColor(HexColor(GOLD))
    canvas.setFont("Cinzel", 9)
    canvas.drawCentredString(
        page_w / 2,
        page_h - 1.42 * inch,
        f"THE CHOSEN QUEST ENHANCED  |  v{VERSION}",
    )

    body = ParagraphStyle(
        "body",
        fontName="Cormorant",
        fontSize=11.2,
        leading=13.1,
        textColor=HexColor(INK),
        alignment=TA_LEFT,
        spaceAfter=2,
    )
    body_small = ParagraphStyle(
        "small",
        parent=body,
        fontSize=9.2,
        leading=10.5,
    )
    link_style = ParagraphStyle(
        "link",
        parent=body_small,
        textColor=HexColor("#4D2B14"),
        leading=10.2,
    )

    top = page_h - 2.23 * inch
    gap = 0.17 * inch
    left_w = 3.35 * inch
    right_x = margin + left_w + gap
    right_w = page_w - margin - right_x

    canvas.setFillColor(HexColor("#F6EDDB"))
    canvas.roundRect(
        margin - 5,
        0.42 * inch,
        page_w - 2 * margin + 10,
        top - 0.31 * inch,
        8,
        stroke=0,
        fill=1,
    )

    y = top
    y = section_title(canvas, "1. Download and start", margin, y, left_w)
    y = paragraph(
        canvas,
        "<b>Download:</b> Use the release page or scan the QR code on the right. "
        "Extract the ZIP before launching.",
        margin,
        y,
        left_w,
        body,
    )
    y -= 5
    y = paragraph(
        canvas,
        "<b>Windows:</b> Double-click <i>Launch The Chosen Quest.bat</i>.<br/>"
        "<b>macOS:</b> Double-click <i>Launch The Chosen Quest.command</i>. "
        "If blocked, Control-click, choose Open, then confirm.<br/>"
        "<b>Linux / Chromebook Linux:</b> Make the .sh launcher executable, then run it.",
        margin,
        y,
        left_w,
        body,
    )
    y -= 8
    y = section_title(canvas, "2. Minimum requirements", margin, y, left_w)
    y = paragraph(
        canvas,
        "<b>Java 8 or newer</b> - install from adoptium.net if needed<br/>"
        "About <b>250 MB</b> free disk space<br/>"
        "<b>1280 x 720</b> minimum; 1440 x 900 recommended<br/>"
        "Desktop Windows, macOS, Linux, or Chromebook Linux environment",
        margin,
        y,
        left_w,
        body,
    )
    y -= 8
    y = section_title(canvas, "3. Essential controls", margin, y, left_w)
    y = paragraph(
        canvas,
        "<b>WASD</b> move  |  <b>Arrow keys</b> alternate movement<br/>"
        "<b>1-7</b> combat actions and abilities  |  <b>M</b> world map<br/>"
        "<b>- / +</b> map zoom  |  <b>Escape</b> close overlay<br/>"
        "<b>Gear icon</b> settings and manual <b>Check Updates</b> action",
        margin,
        y,
        left_w,
        body,
    )
    y -= 8
    y = section_title(canvas, "4. What to test", margin, y, left_w)
    paragraph(
        canvas,
        "Try different race, class, and starting weapon combinations. Explore the "
        "fog-aware map, follow rumors, visit vendors, clear the Spider Nest, identify "
        "relics, and challenge elite and boss encounters. Note anything confusing, "
        "too easy, too punishing, slow, visually broken, or unexpectedly fun.",
        margin,
        y,
        left_w,
        body,
    )
    y -= 88
    y = section_title(canvas, "5. Suggested 30-minute route", margin, y, left_w)
    paragraph(
        canvas,
        "Create a new hero and note the starting build. Explore until you reach a "
        "vendor and one standard fight. Test inventory sorting and equipment "
        "comparison. Follow a rumor or map clue. Open the full world map with M. "
        "Attempt one elite objective or the Spider Nest. Finish by submitting the "
        "feedback form, even if nothing broke.",
        margin,
        y,
        left_w,
        body,
    )

    y2 = top
    y2 = section_title(canvas, "Release and reporting links", right_x, y2, right_w)
    qr_size = 0.9 * inch
    qr_x = right_x
    draw_qr(canvas, RELEASE_URL, qr_x, y2 - qr_size + 2, qr_size)
    canvas.setFillColor(HexColor(INK))
    canvas.setFont("Cinzel", 9)
    canvas.drawString(qr_x + qr_size + 8, y2 - 11, "DOWNLOAD THE BETA")
    paragraph(
        canvas,
        f'<link href="{RELEASE_URL}">github.com/Rebelord/The-Chosen-Quest-Enhanced/releases</link>',
        qr_x + qr_size + 8,
        y2 - 20,
        right_w - qr_size - 8,
        link_style,
    )
    y2 -= qr_size + 15

    draw_qr(canvas, FEEDBACK_URL, qr_x, y2 - qr_size + 2, qr_size)
    canvas.setFillColor(HexColor(INK))
    canvas.setFont("Cinzel", 9)
    canvas.drawString(qr_x + qr_size + 8, y2 - 11, "GENERAL FEEDBACK")
    paragraph(
        canvas,
        f'<link href="{FEEDBACK_URL}">{FEEDBACK_URL}</link>',
        qr_x + qr_size + 8,
        y2 - 20,
        right_w - qr_size - 8,
        link_style,
    )
    y2 -= qr_size + 15

    draw_qr(canvas, BUG_URL, qr_x, y2 - qr_size + 2, qr_size)
    canvas.setFillColor(HexColor(INK))
    canvas.setFont("Cinzel", 9)
    canvas.drawString(qr_x + qr_size + 8, y2 - 11, "REPORT A BUG")
    paragraph(
        canvas,
        f'<link href="{BUG_URL}">{BUG_URL}</link>',
        qr_x + qr_size + 8,
        y2 - 20,
        right_w - qr_size - 8,
        link_style,
    )
    y2 -= qr_size + 14

    y2 = section_title(canvas, "A useful report includes", right_x, y2, right_w)
    y2 = paragraph(
        canvas,
        "Operating system and Java version; race, class, level, and loadout; exact "
        "steps before the problem; what you expected; what happened; whether a reload "
        "reproduces it; and a game-window screenshot for visual issues.",
        right_x,
        y2,
        right_w,
        body,
    )
    y2 -= 8
    y2 = section_title(canvas, "Update feature baseline", right_x, y2, right_w)
    y2 = paragraph(
        canvas,
        "In Settings, choose <b>Check Updates</b>. Before the next patch exists, this "
        f"build should report <b>v{VERSION}</b> as current. Keep this copy installed. "
        "After the next beta is published, relaunch it and verify the update notice, "
        "View Changes, Remind Me Later, and official Download handoff.",
        right_x,
        y2,
        right_w,
        body,
    )
    y2 -= 8
    y2 = section_title(canvas, "Beta expectations", right_x, y2, right_w)
    y2 = paragraph(
        canvas,
        "This is an unfinished private beta. Saves may change. Do not include "
        "passwords, private messages, or sensitive information in reports. Share "
        "the invite only with people the project owner has invited.",
        right_x,
        y2,
        right_w,
        body,
    )

    canvas.setStrokeColor(HexColor(GOLD))
    canvas.line(margin, 0.29 * inch, page_w - margin, 0.29 * inch)
    canvas.setFillColor(HexColor("#5B4631"))
    canvas.setFont("Cormorant", 8.2)
    canvas.drawCentredString(
        page_w / 2,
        0.16 * inch,
        "Thank you for helping shape The Chosen Quest Enhanced.",
    )
    canvas.save()


def create_text_files():
    invite = f"""THE CHOSEN QUEST ENHANCED - PRIVATE BETA INVITATION

You are invited to help test The Chosen Quest Enhanced, a modernized version of
the original turn-based fantasy RPG.

Build: v{VERSION}
Download: {RELEASE_URL}

What you will need:
- Java 8 or newer
- About 250 MB of free disk space
- Windows, macOS, Linux, or a Chromebook Linux environment
- A display resolution of at least 1280 x 720

The download includes simple launchers and a Tester Guide. Please extract the ZIP
before launching the game.

UPDATE TEST
- Open Settings and choose Check Updates. This build should initially report
  v{VERSION} as current.
- Keep this exact folder installed. When the next beta is announced, launch this
  older copy again and test the in-game update notice and official download handoff.

After playing:
General feedback: {FEEDBACK_URL}
Bug reports: {BUG_URL}

Please keep this private beta invitation within the invited testing group. Thank
you for helping us improve the adventure!
"""
    (OUT / "SHARE-ME-FIRST.txt").write_text(invite, encoding="utf-8")

    messages = f"""COPY-READY PRIVATE BETA INVITATIONS

SHORT MESSAGE / DISCORD

You have been invited to privately test The Chosen Quest Enhanced v{VERSION}.
Download the beta and Tester Guide here:
{RELEASE_URL}

Java 8 or newer is required. After playing, please share general feedback at
{FEEDBACK_URL} or report reproducible bugs at {BUG_URL}.

Please keep this exact build installed for the update test. It should initially
report v{VERSION} as current under Settings > Check Updates. After the next beta
is announced, relaunch this older build and verify that its update notice opens
the official release download.

Please keep this build within the invited testing group. Thank you for helping
shape the adventure!


EMAIL

Subject: Your invitation to test The Chosen Quest Enhanced

Hello,

I would love your help testing The Chosen Quest Enhanced, a modernized version
of our original turn-based fantasy RPG. This private beta focuses on combat
balance, exploration, character builds, vendors, relic progression, audio, and
interface clarity.

Download v{VERSION}:
{RELEASE_URL}

You will need Java 8 or newer, about 250 MB of free space, and a Windows, macOS,
Linux, or Chromebook Linux desktop environment. Extract the ZIP before using the
included launcher. A one-page Quick Reference and full Tester Guide are included.

Please keep this exact build folder after testing. In Settings, choose Check Updates
and confirm v{VERSION} is current. When the next beta is announced, launch this
older build again and test its update notice, View Changes, Remind Me Later, and
official Download handoff.

When you finish:
- General feedback: {FEEDBACK_URL}
- Bug reports: {BUG_URL}

Please keep the build and invitation within the private testing group. Honest
feedback is valuable - especially when something feels confusing, unbalanced,
slow, visually broken, or unexpectedly fun.

Thank you for helping shape The Chosen Quest Enhanced.
"""
    (DOCS / "Copy-Ready-Invite-Messages.txt").write_text(
        messages,
        encoding="utf-8",
    )

    checklist = f"""PRIVATE TESTER CHECKLIST - v{VERSION}

BEFORE PLAYING
[ ] Download the build from: {RELEASE_URL}
[ ] Extract the ZIP to a normal folder.
[ ] Confirm Java 8 or newer is installed.
[ ] Read the included Quick Reference or full Tester Guide.
[ ] Open Settings > Check Updates and confirm v{VERSION} is reported as current.
[ ] Keep this exact build folder for the next-beta update test.

DURING PLAY
[ ] Note your race, class, starting loadout, and current level.
[ ] Try exploration, vendors, inventory, rumors, combat, and the world map.
[ ] Record clear steps when anything unexpected happens.
[ ] Capture only the game window if a screenshot would help.

AFTER THE NEXT BETA IS ANNOUNCED
[ ] Launch this older v{VERSION} copy while online.
[ ] Confirm a styled update notice appears without delaying game startup.
[ ] Confirm View Changes opens release information.
[ ] Confirm Remind Me Later closes the notice without changing game files.
[ ] Check again, choose Download, and confirm the official GitHub release opens.
[ ] Disconnect from the internet once and confirm the game still launches normally.

AFTER PLAY
[ ] Submit general impressions: {FEEDBACK_URL}
[ ] Submit reproducible problems: {BUG_URL}
[ ] Include operating system, Java version, and reproduction steps.

Please do not include passwords, private messages, or sensitive personal
information in feedback or screenshots.
"""
    (DOCS / "Private-Tester-Checklist.txt").write_text(checklist, encoding="utf-8")


def copy_existing_guide():
    destination = DOCS / "The-Chosen-Quest-Enhanced-Full-Tester-Guide.pdf"
    destination.write_bytes(TESTER_GUIDE.read_bytes())


def main():
    ASSETS.mkdir(parents=True, exist_ok=True)
    DOCS.mkdir(parents=True, exist_ok=True)
    (ROOT / "tmp" / "pdfs" / "private-tester-kit").mkdir(parents=True, exist_ok=True)
    create_invite_art()
    create_quick_reference_pdf()
    create_text_files()
    copy_existing_guide()
    print(f"Created private tester kit at {OUT}")


if __name__ == "__main__":
    main()
