#!/usr/bin/env python3
"""Generate the release-owned private beta tester guide PDF."""

from pathlib import Path
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    Image,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "The-Chosen-Quest-Enhanced-Tester-Guide.pdf"
BANNER = ROOT / "assets" / "community" / "The-Chosen-Quest-Banner.png"
TITLE_FONT = ROOT / "assets" / "fonts" / "Cinzel.ttf"
BODY_FONT = ROOT / "assets" / "fonts" / "CormorantGaramond.ttf"
VERSION_SOURCE = (
    ROOT / "src" / "thechosenquest" / "desktop" / "AppVersion.java"
).read_text(encoding="utf-8")
VERSION_MATCH = re.search(r'VERSION\s*=\s*"([^"]+)"', VERSION_SOURCE)
if not VERSION_MATCH:
    raise RuntimeError("Unable to read AppVersion.VERSION")
VERSION = VERSION_MATCH.group(1)
PREVIOUS_VERSION = "0.7.0-beta.2"

PARCHMENT = colors.HexColor("#F3E7CA")
INK = colors.HexColor("#241A14")
MUTED = colors.HexColor("#685849")
GOLD = colors.HexColor("#9A6B24")
GOLD_LIGHT = colors.HexColor("#D9B86B")
SURFACE = colors.HexColor("#38291F")
DEEP = colors.HexColor("#1B130F")
GREEN = colors.HexColor("#365E3A")
RED = colors.HexColor("#8A342E")


def register_fonts():
    pdfmetrics.registerFont(TTFont("TCQTitle", str(TITLE_FONT)))
    pdfmetrics.registerFont(TTFont("TCQBody", str(BODY_FONT)))


def page_background(canvas, document):
    canvas.saveState()
    canvas.setFillColor(PARCHMENT)
    canvas.rect(0, 0, letter[0], letter[1], stroke=0, fill=1)
    canvas.setStrokeColor(GOLD)
    canvas.setLineWidth(1)
    canvas.rect(0.42 * inch, 0.42 * inch,
                letter[0] - 0.84 * inch, letter[1] - 0.84 * inch,
                stroke=1, fill=0)
    canvas.setFont("TCQBody", 9)
    canvas.setFillColor(MUTED)
    canvas.drawString(0.62 * inch, 0.52 * inch,
                      f"The Chosen Quest Enhanced - Private Beta v{VERSION}")
    canvas.drawRightString(letter[0] - 0.62 * inch, 0.52 * inch,
                           "Page %d" % document.page)
    canvas.restoreState()


def callout(title, body, accent, styles):
    content = [
        Paragraph(title, styles["CardTitle"]),
        Spacer(1, 4),
        Paragraph(body, styles["CardBody"]),
    ]
    table = Table([[content]], colWidths=[7.0 * inch])
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#E8D7B4")),
        ("BOX", (0, 0), (-1, -1), 1.2, accent),
        ("LINEBEFORE", (0, 0), (0, -1), 5, accent),
        ("LEFTPADDING", (0, 0), (-1, -1), 14),
        ("RIGHTPADDING", (0, 0), (-1, -1), 14),
        ("TOPPADDING", (0, 0), (-1, -1), 10),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 10),
    ]))
    return table


def route_table(styles):
    rows = [
        [
            Paragraph("<b>Route</b>", styles["TableHead"]),
            Paragraph("<b>What to exercise</b>", styles["TableHead"]),
            Paragraph("<b>Watch for</b>", styles["TableHead"]),
        ],
        [
            Paragraph("Fighter", styles["TableBody"]),
            Paragraph("Knight/Berserker, Rage pacing, Shield Counter, Heavy Strike",
                      styles["TableBody"]),
            Paragraph("Path mismatch, unlimited recovery, or abilities appearing too early",
                      styles["TableBody"]),
        ],
        [
            Paragraph("Rogue", styles["TableBody"]),
            Paragraph("Assassin/Skirmisher, Momentum, stealth, combos, fleeing",
                      styles["TableBody"]),
            Paragraph("Style mismatch, Momentum farming, unclear stealth, unsafe fleeing",
                      styles["TableBody"]),
        ],
        [
            Paragraph("Hunter", styles["TableBody"]),
            Paragraph("Ranger/Marksman, Focus, opening shot, Mark, Volley",
                      styles["TableBody"]),
            Paragraph("Style mismatch, Volley without Mark, or unclear Focus gains",
                      styles["TableBody"]),
        ],
        [
            Paragraph("Mage", styles["TableBody"]),
            Paragraph("Channeler/Arcanist, Mana, direct spells, Channel Ward",
                      styles["TableBody"]),
            Paragraph("Style mismatch, confusing spell readiness, or bad mana pressure",
                      styles["TableBody"]),
        ],
        [
            Paragraph("World", styles["TableBody"]),
            Paragraph("Fog, rumors, map zoom, full map (M), Ashweb Nest",
                      styles["TableBody"]),
            Paragraph("Clutter, hidden objectives, farming, stuck movement, or misplaced markers",
                      styles["TableBody"]),
        ],
        [
            Paragraph("Stability", styles["TableBody"]),
            Paragraph("Two fights in sequence, save/load, rapid choices and key presses",
                      styles["TableBody"]),
            Paragraph("Stale buttons, missing art, duplicate turns, rewards, or location actions",
                      styles["TableBody"]),
        ],
        [
            Paragraph("Updates", styles["TableBody"]),
            Paragraph("Manual check, next-beta notice, release notes, reminder, download, offline launch",
                      styles["TableBody"]),
            Paragraph("Blocked startup, repeated notices, wrong version, unsafe handoff, or offline errors",
                      styles["TableBody"]),
        ],
    ]
    table = Table(rows, colWidths=[0.85 * inch, 3.05 * inch, 3.1 * inch],
                  repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), SURFACE),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("BACKGROUND", (0, 1), (-1, -1), colors.HexColor("#F8EFD9")),
        ("GRID", (0, 0), (-1, -1), 0.6, colors.HexColor("#B89C6C")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    return table


def build_pdf():
    register_fonts()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(
        name="GuideTitle", fontName="TCQTitle", fontSize=22, leading=26,
        textColor=GOLD_LIGHT, alignment=TA_CENTER, spaceAfter=5,
    ))
    styles.add(ParagraphStyle(
        name="GuideSubtitle", fontName="TCQBody", fontSize=13, leading=16,
        textColor=colors.white, alignment=TA_CENTER,
    ))
    styles.add(ParagraphStyle(
        name="Section", fontName="TCQTitle", fontSize=15, leading=19,
        textColor=GOLD, spaceBefore=3, spaceAfter=7,
    ))
    styles.add(ParagraphStyle(
        name="BodyTCQ", fontName="TCQBody", fontSize=11.2, leading=14.2,
        textColor=INK, spaceAfter=6,
    ))
    styles.add(ParagraphStyle(
        name="BulletTCQ", fontName="TCQBody", fontSize=10.7, leading=13.2,
        leftIndent=15, firstLineIndent=-9, bulletIndent=4, textColor=INK,
        spaceAfter=3,
    ))
    styles.add(ParagraphStyle(
        name="CardTitle", fontName="TCQTitle", fontSize=12.5, leading=15,
        textColor=INK,
    ))
    styles.add(ParagraphStyle(
        name="CardBody", fontName="TCQBody", fontSize=10.8, leading=13.4,
        textColor=INK,
    ))
    styles.add(ParagraphStyle(
        name="TableHead", fontName="TCQTitle", fontSize=8.8, leading=11,
        textColor=colors.white,
    ))
    styles.add(ParagraphStyle(
        name="TableBody", fontName="TCQBody", fontSize=8.9, leading=11,
        textColor=INK,
    ))
    styles.add(ParagraphStyle(
        name="Small", fontName="TCQBody", fontSize=9.5, leading=12,
        textColor=MUTED,
    ))
    document = BaseDocTemplate(
        str(OUTPUT),
        pagesize=letter,
        leftMargin=0.7 * inch,
        rightMargin=0.7 * inch,
        topMargin=0.62 * inch,
        bottomMargin=0.72 * inch,
        title="The Chosen Quest Enhanced - Private Beta Tester Guide",
        author="The Chosen Quest Enhanced",
        subject=f"v{VERSION} private beta instructions and feedback guide",
    )
    frame = Frame(
        document.leftMargin, document.bottomMargin,
        document.width, document.height,
        leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0,
    )
    document.addPageTemplates([
        PageTemplate(id="guide", frames=[frame], onPage=page_background)
    ])

    story = []
    banner = Image(str(BANNER), width=6.5 * inch, height=2.6 * inch)
    banner.hAlign = "CENTER"
    story.append(banner)
    story.append(Spacer(1, 8))

    title_box = Table([[
        [
            Paragraph("PRIVATE BETA TESTER GUIDE", styles["GuideTitle"]),
            Paragraph(f"Version {VERSION}", styles["GuideSubtitle"]),
        ]
    ]], colWidths=[7.1 * inch])
    title_box.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), DEEP),
        ("BOX", (0, 0), (-1, -1), 1.2, GOLD),
        ("TOPPADDING", (0, 0), (-1, -1), 9),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 9),
    ]))
    story.append(title_box)
    story.append(Spacer(1, 4))
    story.append(Paragraph(
        "Thank you for helping test The Chosen Quest Enhanced. This build is being "
        "shared specifically to collect real playthrough feedback. You do not need to "
        "finish every route or understand the underlying systems - honest observations "
        "are more valuable than trying to give the \"right\" answer.",
        styles["BodyTCQ"],
    ))
    story.append(Spacer(1, 5))
    story.append(Paragraph("QUICK START", styles["Section"]))
    for text in [
        "<b>1.</b> Extract the downloaded ZIP before launching the game.",
        "<b>2.</b> Install Java 8 or newer if the launcher cannot find Java.",
        "<b>3.</b> Use the launcher for your operating system, or run "
        "<font name='Courier'>java -jar TheChosenQuest-Enhanced.jar</font>.",
        "<b>4.</b> Start a hero, note the race, class, and Combat Path, then play naturally.",
        "<b>5.</b> Submit general impressions after the session and a separate bug report "
        "for reproducible defects.",
    ]:
        story.append(Paragraph(text, styles["BulletTCQ"], bulletText="-"))
    story.append(Spacer(1, 7))
    story.append(KeepTogether([
        Paragraph("REQUIREMENTS & CONTROLS", styles["Section"]),
        Table([
            [
                Paragraph("<b>Requirements</b><br/>Java 8+<br/>400 MB free space<br/>"
                          "1280 x 720 minimum", styles["BodyTCQ"]),
                Paragraph("<b>Movement</b><br/>WASD or arrow keys<br/>M: full map<br/>"
                          "Escape: close overlay", styles["BodyTCQ"]),
                Paragraph("<b>Combat</b><br/>1-7: actions<br/>Mouse: inspect help<br/>"
                          "Gear: settings and updates", styles["BodyTCQ"]),
            ]
        ], colWidths=[2.35 * inch, 2.35 * inch, 2.4 * inch],
            style=TableStyle([
                ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F8EFD9")),
                ("BOX", (0, 0), (-1, -1), 0.8, GOLD),
                ("INNERGRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#B89C6C")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 10),
                ("RIGHTPADDING", (0, 0), (-1, -1), 10),
                ("TOPPADDING", (0, 0), (-1, -1), 9),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ]))
    ]))
    story.append(Spacer(1, 12))
    story.append(callout(
        "GENERAL FEEDBACK",
        "After a play session: <link href='https://forms.gle/GJLCPtzP7LN9PeBeA' "
        "color='#365E3A'><u>https://forms.gle/GJLCPtzP7LN9PeBeA</u></link>",
        GREEN, styles,
    ))
    story.append(Spacer(1, 8))
    story.append(callout(
        "BUG REPORT",
        "For reproducible defects: <link href='https://forms.gle/F8LtxPn5sKbwPAxX9' "
        "color='#8A342E'><u>https://forms.gle/F8LtxPn5sKbwPAxX9</u></link>",
        RED, styles,
    ))

    story.append(PageBreak())
    story.append(Paragraph("RECOMMENDED TEST ROUTES", styles["Section"]))
    story.append(Paragraph(
        "Choose whatever sounds interesting. We especially need to know whether each "
        "class feels distinct, understandable, and fair through standard, elite, and "
        "boss encounters.",
        styles["BodyTCQ"],
    ))
    story.append(route_table(styles))
    story.append(Spacer(1, 4))
    story.append(Paragraph("WHAT MAKES A USEFUL REPORT?", styles["Section"]))
    for text in [
        "Game version, operating system, and Java version.",
        "Hero race, class, level, Origin Path, Current Style, and important equipment.",
        "What you expected, what happened, and whether it happened again.",
        "The exact action or location immediately before the problem.",
        "A screenshot, combat-log excerpt, or saved game when practical.",
        "For balance feedback, describe whether you played cautiously, explored widely, "
        "or rushed directly toward enemies.",
    ]:
        story.append(Paragraph(text, styles["BulletTCQ"], bulletText="-"))
    story.append(Spacer(1, 4))
    story.append(KeepTogether([
        Paragraph("CURRENT BETA FOCUS", styles["Section"]),
        callout(
            "PLEASE WATCH THESE AREAS",
            "Combat Path clarity; Origin Path and Current Style persistence; equipment-driven "
            "style changes; relic Equip Now choices; finite vendor gear; boss preparation; "
            "portrait framing; text clipping; reduced motion; and save/load behavior. Also "
            "watch for actions that repeat healing, gold, experience, charges, or rewards.",
            GOLD, styles,
        ),
    ]))
    story.append(Spacer(1, 8))
    story.append(Paragraph("PRIVACY, CREDITS & SPOILERS", styles["Section"]))
    story.append(Paragraph(
        "Your name and contact information are optional. Do not include passwords, account "
        "details, private messages, or other sensitive information in reports. Public beta "
        "tester credits require separate permission; you may request a real name, gamer tag, "
        "anonymous group acknowledgment, or no listing. Please mark major story or boss "
        "details as spoilers when discussing them with other testers.",
        styles["BodyTCQ"],
    ))
    story.append(PageBreak())
    story.append(KeepTogether([
        Paragraph("UPDATE & SAVE MIGRATION TEST", styles["Section"]),
        callout(
            f"START WITH v{PREVIOUS_VERSION}",
            f"If you retained v{PREVIOUS_VERSION}, launch it online and verify that it "
            f"discovers v{VERSION}. Test View Changes, Remind Me Later, and the official "
            "Download handoff. Extract the new build separately, load the older save, and "
            "confirm equipment, Origin Path, and Current Style. The updater must never "
            "overwrite game files or save data.",
            GREEN, styles,
        ),
    ]))
    story.append(Spacer(1, 8))
    story.append(Paragraph("TWO-BUILD TEST SEQUENCE", styles["Section"]))
    for text in [
        f"<b>Discover:</b> Launch v{PREVIOUS_VERSION} online and confirm the non-blocking "
        f"notice identifies v{VERSION}.",
        "<b>Inspect:</b> Verify View Changes, Remind Me Later, and a later manual "
        "recheck before choosing Download.",
        "<b>Handoff:</b> Download must open the exact official GitHub release; it must "
        "not silently replace the game or touch save data.",
        f"<b>Migrate:</b> Extract v{VERSION} separately and load a v{PREVIOUS_VERSION} "
        "save; equipment and hero identity must remain coherent.",
        f"<b>Current:</b> In v{VERSION}, Check Updates must report this version as current. "
        "Keep the folder for the next update test.",
        "<b>Offline:</b> Launch once without internet access. Startup and gameplay "
        "must remain normal with no system error dialog.",
    ]:
        story.append(Paragraph(text, styles["BulletTCQ"], bulletText="-"))
    story.append(Spacer(1, 5))
    story.append(callout(
        "WHAT TO RECORD",
        "Installed and discovered versions, operating system, Java version, whether the "
        "notice appeared, which actions worked, the release page opened, save-migration "
        "result, Origin Path, Current Style, and repeated or missing notifications.",
        GOLD, styles,
    ))
    story.append(Spacer(1, 8))
    story.append(Paragraph("RESULT CHECKLIST", styles["Section"]))
    result_rows = [
        [Paragraph("<b>Check</b>", styles["TableHead"]),
         Paragraph("<b>Expected result</b>", styles["TableHead"])],
        [Paragraph("Older build discovery", styles["TableBody"]),
         Paragraph(f"v{PREVIOUS_VERSION} offers v{VERSION} without blocking startup", styles["TableBody"])],
        [Paragraph("Available version", styles["TableBody"]),
         Paragraph("Notice identifies the exact newer beta", styles["TableBody"])],
        [Paragraph("View / Remind", styles["TableBody"]),
         Paragraph("Release details open; reminder closes without file changes", styles["TableBody"])],
        [Paragraph("Download", styles["TableBody"]),
         Paragraph("Official GitHub release and matching ZIP open", styles["TableBody"])],
        [Paragraph("Save migration", styles["TableBody"]),
         Paragraph("Equipment and hero path/style identity remain coherent", styles["TableBody"])],
        [Paragraph("Current-version check", styles["TableBody"]),
         Paragraph(f"v{VERSION} reports as current", styles["TableBody"])],
        [Paragraph("Offline launch", styles["TableBody"]),
         Paragraph("No system error; game remains fully playable", styles["TableBody"])],
    ]
    result_table = Table(result_rows, colWidths=[2.2 * inch, 4.8 * inch])
    result_table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), SURFACE),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("BACKGROUND", (0, 1), (-1, -1), colors.HexColor("#F8EFD9")),
        ("GRID", (0, 0), (-1, -1), 0.6, colors.HexColor("#B89C6C")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.append(result_table)
    story.append(Spacer(1, 8))
    story.append(Paragraph(
        "<b>Thank you.</b> Thoughtful criticism, confused moments, failed runs, and small "
        "observations all help shape the next version.",
        styles["BodyTCQ"],
    ))
    story.append(Spacer(1, 4))
    story.append(Paragraph(
        "Project releases: "
        "<link href='https://github.com/Rebelord/The-Chosen-Quest-Enhanced/releases' "
        "color='#9A6B24'><u>github.com/Rebelord/The-Chosen-Quest-Enhanced/releases</u></link>",
        styles["Small"],
    ))

    document.build(story)
    return OUTPUT


if __name__ == "__main__":
    print(build_pdf())
