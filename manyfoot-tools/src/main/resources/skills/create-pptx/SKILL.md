---
name: create-pptx
description: >
  Create PowerPoint presentations (PPTX) using Python and python-pptx. Handles
  timelines, charts, diagrams, slide layouts, custom colors, shapes, connectors,
  text formatting, and slide transitions. Knows WPS compatibility pitfalls.
  Use when the user asks to: generate a PPT/PPTX file, create a presentation,
  make a slide deck, draw a timeline, visualize data as slides, or export
  something to PowerPoint. Also use when the user says "做成 PPT"、"生成幻灯片"、
  "做个演示文稿"、"做个 pptx".
---

# Create PowerPoint (python-pptx)

You are not just exporting slides. You are designing a polished presentation.
Every deck must have a coherent visual identity, strong hierarchy, generous
spacing, and reusable components. A slide that looks like plain text on a blank
background is a failure unless the user explicitly asks for a raw draft.

## ⚡ MANDATORY FIRST STEPS — do these before writing any code

```bash
# Step 1: Install dependency
pip install python-pptx --break-system-packages

# Step 2: Copy helper library to working directory
cp /workspace/skills/create-pptx/scripts/pptx_helpers.py /workspace/code/pptx_helpers.py
```

All scripts go in `/workspace/code/`. All output `.pptx` files go in `/workspace/output/`.
When saving: `prs.save('/workspace/output/output.pptx')`

---

## 🎨 Design-first rules — mandatory for every deck

Before writing Python, define a mini design system and use it consistently:

1. **Style direction** — choose one clear look: executive dark, clean consulting,
   editorial light, futuristic tech, warm education, luxury minimal, etc.
2. **Palette** — define background, surface, text, muted text, primary accent,
   secondary accent, warning/success if needed. Use named `RGBColor` constants only.
3. **Typography scale** — title, section title, body, caption, KPI. Do not use one
   font size everywhere.
4. **Layout grid** — use fixed margins and columns. No random coordinates.
5. **Reusable components** — create functions for title bars, cards, KPI blocks,
   section labels, dividers, chips, timelines, and footers.
6. **Visual hierarchy** — each slide needs one dominant message, one accent area,
   and restrained supporting detail.

### Beauty-first workflow — do this before coding

Do not jump directly into `python-pptx` coordinates. First write a short visual
brief in comments at the top of the script:

```python
"""
VISUAL BRIEF
Style: premium dark / editorial light / futuristic tech / warm education / luxury minimal
Audience: executives / classroom / investors / product team / public talk
Mood: calm, sharp, confident, energetic, elegant
Layout rhythm: cover → section divider → insight slides → data slides → conclusion
Signature motif: gradient ribbon / glowing orbs / editorial blocks / thin-line grid / large numbers
"""
```

If the visual brief is vague, the PPT will look generic. Choose one strong visual
motif and repeat it across slides.

### Aesthetic scorecard — target 8/10 or higher

Before saving, rate every deck against this checklist:

| Dimension | Good PPT behavior | Bad PPT behavior |
|-----------|-------------------|------------------|
| Composition | clear focal point, aligned edges, balanced whitespace | scattered text, random positions |
| Hierarchy | title > key number/message > details | everything same size/weight |
| Palette | 1 background, 1 surface, 1–2 accents | many unrelated colors |
| Typography | 4–6 consistent sizes, bold used intentionally | tiny text, inconsistent sizes |
| Components | cards, chips, KPI blocks, dividers, timelines | raw paragraphs and bullets |
| Rhythm | slide layouts vary but share a motif | every slide identical or chaotic |
| Data styling | chart integrated into theme | default Office chart pasted in |
| Finish | footer, page number, margins, subtle detail | unfinished empty corners |

If a slide scores poorly, redesign it before generating the final PPTX.

### Use the right backend for visual quality

`python-pptx` is best for editable Office-native slides, but it is coordinate-based.
For highly visual reports, prefer an HTML-rendered backend because CSS layout,
gradients, shadows, charts, and modern typography are easier to make beautiful.

| Desired result | Preferred backend | Tradeoff |
|----------------|-------------------|----------|
| Editable business PPT | `python-pptx` | beautiful only if carefully styled |
| Polished visual report | HTML/CSS screenshot → PPTX | mostly image-based, not deeply editable |
| JS-native editable deck | PptxGenJS | needs Node, still coordinate-based |
| Markdown lecture slides | Marp | fast, less custom/editable |

Default decision rule:
- If user says **must edit in PowerPoint/WPS** → use `python-pptx`.
- If user says **要好看、有设计感、像报告/海报/发布会** → use HTML/CSS screenshot mode when available.
- If user provides Markdown content → consider Marp.
- If user wants native charts and editable objects → use `python-pptx` or PptxGenJS.

### High-aesthetic slide patterns

Use these patterns instead of ordinary bullet pages:

1. **Magazine cover** — huge title, short subtitle, oversized abstract shape,
   tiny metadata. Good for opening slides.
2. **Big number story** — one giant metric, 2–3 supporting cards, minimal text.
3. **Editorial split** — left 40% narrative, right 60% diagram/chart/cards.
4. **Dashboard panel** — dark background, 3 KPI cards, one large chart panel,
   one insight sidebar.
5. **Strategy map** — 3 horizontal lanes or 2×2 matrix with strong labels.
6. **Timeline ribbon** — thick axis, milestone cards, alternating top/bottom notes.
7. **Quote slide** — large quote, small attribution, elegant accent line.
8. **Section poster** — huge section number, 1 sentence, full-bleed abstract block.

Never make a slide that is only a title plus 6 bullets unless the user explicitly
asks for a simple outline.

### Default premium style system

Use this unless the user asks for a specific style:

```python
# ── Canvas / grid ─────────────────────────────────────────────────────────────
SW, SH = 12192000, 6858000
M = 520000                    # outer margin
G = 220000                    # grid gap
CONTENT_W = SW - 2 * M
CONTENT_H = SH - 2 * M

# ── Premium dark theme ───────────────────────────────────────────────────────
C_BG       = RGBColor(0x07, 0x10, 0x1F)   # deep navy
C_BG_2     = RGBColor(0x0B, 0x1B, 0x33)   # subtle panel navy
C_SURFACE  = RGBColor(0x10, 0x25, 0x44)   # card surface
C_SURFACE2 = RGBColor(0x13, 0x2D, 0x52)
C_TEXT     = RGBColor(0xF8, 0xFA, 0xFC)
C_MUTED    = RGBColor(0x94, 0xA3, 0xB8)
C_LINE     = RGBColor(0x2A, 0x43, 0x63)
C_ACCENT   = RGBColor(0x38, 0xBD, 0xF8)   # cyan
C_ACCENT2  = RGBColor(0xA7, 0x5F, 0xFF)   # violet
C_SUCCESS  = RGBColor(0x4A, 0xDE, 0x80)
C_WARN     = RGBColor(0xF5, 0xA6, 0x23)

# ── Typography scale ─────────────────────────────────────────────────────────
FS_HERO    = 38
FS_TITLE   = 26
FS_SECTION = 18
FS_BODY    = 12
FS_CAPTION = 9
FS_KPI     = 32
```

### Professional slide composition rules

- Use **blank layout only** (`slide_layouts[6]`) and draw all elements yourself.
- Keep margins consistent: outer margin around `500000–650000` EMU.
- Use cards/panels for grouped content; avoid floating bullets on empty space.
- Prefer 2–3 columns, asymmetric hero areas, KPI strips, timelines, and callout
  cards over default bullet lists.
- Limit text: max 3–5 bullets per slide; each bullet should be short.
- Add subtle structure: top label, title, divider line, footer/page marker.
- Use one strong accent per slide; do not rainbow-color every element.
- Use muted gridlines and low-contrast surfaces to make charts look integrated.
- No emoji icons by default. Use simple geometric badges, numbers, or text chips
  because emoji rendering is inconsistent in WPS/PowerPoint.

### Visual quality checklist before saving

- [ ] Every slide has a clear title or section label.
- [ ] All content aligns to the same margins/grid.
- [ ] There is visible hierarchy: title > key message/KPI > body > caption.
- [ ] At least one designed component is used: card, KPI, timeline, split panel,
      chart panel, section divider, or callout.
- [ ] No default white slide background unless intentionally using a light theme.
- [ ] No giant unstyled bullet list.
- [ ] Output path is `/workspace/output/output.pptx`.

---

## Key units

**EMU** (English Metric Units): 1 pt = 12700 EMU, 1 cm ≈ 360000 EMU.
Standard 16:9 slide = 12192000 × 6858000 EMU.

```python
from pptx.util import Pt, Emu, Inches, Cm
```

---

## Workflow

1. **Understand the content** — milestones, categories, colors, # slides
2. **Plan layout** — compute X/Y positions in EMU up front; avoid magic numbers
3. **Read reference files** — choose which references apply (see below), read them
4. **Choose backend** — editable PPTX, beautiful image-based PPTX, JS backend, or Markdown deck
5. **Define design system** — theme constants, spacing grid, typography scale
6. **Write reusable components** — title bar, cards, KPI, footer, section divider
7. **Write the script** — import helpers, use constants, avoid magic numbers
8. **Run the script** — command depends on backend, see command guide below
9. **Present the file** — call `present_files(['/workspace/output/output.pptx'])`

---

## Tool command guide — choose and explain commands clearly

Always tell yourself which backend you are using and why. Do not blindly use
`python-pptx` when the user mainly wants visual polish.

### Backend A: `python-pptx` — default editable Office PPTX

Use when the user needs editable PowerPoint/WPS objects: text boxes, shapes,
tables, charts, or corporate templates.

```bash
# Install the PPTX authoring library.
# --break-system-packages is needed in this sandbox because Python packages are
# installed into the container's system Python environment.
pip install python-pptx --break-system-packages

# Copy helper functions from the synchronized skill directory to the code path.
# Scripts import from /workspace/code, so this makes `from pptx_helpers import ...` work.
cp /workspace/skills/create-pptx/scripts/pptx_helpers.py /workspace/code/pptx_helpers.py

# Run the generation script. The script must save to /workspace/output/*.pptx.
python3 /workspace/code/script.py
```

Python save path:

```python
OUT = '/workspace/output/output.pptx'
prs.save(OUT)
```

Best for:
- editable business reports
- native Office charts
- WPS/PowerPoint-compatible decks
- template-driven decks

Not best for:
- highly polished poster-like visuals
- CSS-style gradients/shadows/layouts
- complex modern dashboards

### Backend B: HTML/CSS screenshot → PPTX — highest visual quality

Use when the user says the PPT must be beautiful, designed, poster-like,
dashboard-like, or visually impressive. This mode renders each slide as an HTML
page, screenshots it with Chromium/Playwright, then places each image full-slide
inside a PPTX. The result looks much better, but slide content is mostly images
and not deeply editable.

Install dependencies:

```bash
# Python package for assembling screenshots into a PPTX.
pip install python-pptx pillow --break-system-packages

# Browser automation. Use this only if Node/Playwright is available in the sandbox.
npm init -y
npm install playwright
npx playwright install chromium
```

Expected files:

```text
/workspace/code/slides.html          # visual design source
/workspace/code/render_slides.js     # screenshots HTML slides
/workspace/code/build_from_images.py # inserts screenshots into PPTX
/workspace/output/slide-01.png       # rendered slide image
/workspace/output/output.pptx        # final PPTX
```

Render command flow:

```bash
# 1. Render HTML slides to PNG images.
node /workspace/code/render_slides.js

# 2. Pack PNG images into a PPTX.
python3 /workspace/code/build_from_images.py
```

Minimal Python packer:

```python
from pathlib import Path
from pptx import Presentation
from pptx.util import Emu

SW, SH = 12192000, 6858000
prs = Presentation()
prs.slide_width = Emu(SW)
prs.slide_height = Emu(SH)
blank = prs.slide_layouts[6]

for img in sorted(Path('/workspace/output').glob('slide-*.png')):
    slide = prs.slides.add_slide(blank)
    slide.shapes.add_picture(str(img), 0, 0, width=Emu(SW), height=Emu(SH))

prs.save('/workspace/output/output.pptx')
```

Best for:
- maximum aesthetics
- landing-page-like report slides
- charts from ECharts/Plotly/Chart.js
- gradients, shadows, glassmorphism, complex layouts

Tradeoff:
- text/charts are images inside PPTX, not native editable PPT elements.

### Backend C: PptxGenJS — optional JS editable backend

Use when Node is available and you want a JS-native PPTX generator with good
slide master, shapes, tables, and chart support.

```bash
# Create a small Node project in /workspace/code if package.json does not exist.
cd /workspace/code && npm init -y

# Install PPTX generator.
npm install pptxgenjs

# Run the JS generation script.
node /workspace/code/generate_pptx.js
```

The script must write:

```javascript
await pptx.writeFile({ fileName: '/workspace/output/output.pptx' });
```

Best for:
- editable PPTX from JavaScript
- JSON-driven slide specs
- teams more comfortable with JS than Python

Tradeoff:
- still coordinate-based; it does not automatically make slides beautiful.

### Backend D: Marp — Markdown to slides

Use when the user gives Markdown or wants a quick lecture/tutorial deck.

```bash
cd /workspace/code && npm init -y
npm install @marp-team/marp-cli
npx marp /workspace/code/slides.md --pptx --output /workspace/output/output.pptx
```

Best for:
- text-first presentation decks
- teaching material
- quick Markdown conversion

Tradeoff:
- less custom than hand-designed PPTX/HTML slides.

### Backend E: LibreOffice headless — conversion/preview only

Do not use LibreOffice as the main authoring tool. Use it for conversion or
preview when available.

```bash
# Convert PPTX to PDF for preview/compatibility checking.
libreoffice --headless --convert-to pdf --outdir /workspace/output /workspace/output/output.pptx
```

Best for:
- generating a PDF preview
- smoke-testing whether the PPTX can be opened by an Office-compatible engine

Tradeoff:
- heavy dependency; rendering can differ from PowerPoint/WPS if fonts are missing.

### Backend selection examples

```text
User: “我要能在 WPS 里继续改文字和图表”
→ Use python-pptx or PptxGenJS.

User: “一定要好看，像咨询报告/发布会/海报一样”
→ Use HTML/CSS screenshot → PPTX.

User: “这是 Markdown，帮我转成课件”
→ Use Marp.

User: “帮我检查 PPT 能不能预览成 PDF”
→ Use LibreOffice headless if installed.
```

### Premium script skeleton (copy-paste start)

```python
#!/usr/bin/env python3
"""Description of this presentation."""
from pptx import Presentation
from pptx.util import Emu, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
import sys, os
sys.path.insert(0, '/workspace/code')
from pptx_helpers import bg_rect, h_line, v_line, textbox, oval_shape, \
                          rounded_rect, multiline_textbox, stat_card, \
                          add_fade_transition

# ── Canvas / grid ─────────────────────────────────────────────────────────────
SW, SH = 12192000, 6858000
M = 560000
G = 220000
CONTENT_W = SW - 2 * M

# ── Theme ────────────────────────────────────────────────────────────────────
C_BG       = RGBColor(0x07, 0x10, 0x1F)
C_BG_2     = RGBColor(0x0B, 0x1B, 0x33)
C_SURFACE  = RGBColor(0x10, 0x25, 0x44)
C_SURFACE2 = RGBColor(0x13, 0x2D, 0x52)
C_TEXT     = RGBColor(0xF8, 0xFA, 0xFC)
C_MUTED    = RGBColor(0x94, 0xA3, 0xB8)
C_LINE     = RGBColor(0x2A, 0x43, 0x63)
C_ACCENT   = RGBColor(0x38, 0xBD, 0xF8)
C_ACCENT2  = RGBColor(0xA7, 0x5F, 0xFF)

FS_HERO, FS_TITLE, FS_SECTION, FS_BODY, FS_CAPTION, FS_KPI = 38, 26, 18, 12, 9, 32

# ── Presentation setup ────────────────────────────────────────────────────────
prs = Presentation()
prs.slide_width  = Emu(SW)
prs.slide_height = Emu(SH)
BLANK = prs.slide_layouts[6]             # blank layout — always use index 6

# ── Reusable components ──────────────────────────────────────────────────────
def slide_bg(slide):
    bg_rect(slide, C_BG, SW, SH)
    # subtle accent geometry for depth
    rounded_rect(slide, SW - 2600000, -250000, 3200000, 1150000, C_BG_2, radius_emu=120000)
    rounded_rect(slide, -450000, SH - 900000, 2100000, 1000000, C_BG_2, radius_emu=140000)

def title_block(slide, eyebrow, title, subtitle=''):
    textbox(slide, M, 330000, CONTENT_W, 180000, eyebrow.upper(), FS_CAPTION, C_ACCENT, bold=True)
    textbox(slide, M, 560000, CONTENT_W, 420000, title, FS_TITLE, C_TEXT, bold=True)
    if subtitle:
        textbox(slide, M, 980000, CONTENT_W, 260000, subtitle, FS_BODY, C_MUTED)
    h_line(slide, M, 1280000, M + 1200000, C_ACCENT, 2.5)

def footer(slide, page_label=''):
    h_line(slide, M, SH - 420000, SW - M, C_LINE, 0.6)
    textbox(slide, M, SH - 330000, 3000000, 180000, page_label, FS_CAPTION, C_MUTED)

def card(slide, x, y, w, h, title, body, accent=C_ACCENT):
    rounded_rect(slide, x, y, w, h, C_SURFACE, C_LINE, 0.8, radius_emu=90000)
    rounded_rect(slide, x, y, 70000, h, accent, radius_emu=90000)
    textbox(slide, x + 170000, y + 140000, w - 280000, 220000, title, FS_SECTION, C_TEXT, bold=True)
    multiline_textbox(slide, x + 170000, y + 430000, w - 280000, h - 520000,
                      body.split('\n'), FS_BODY, C_MUTED)

def kpi_card(slide, x, y, w, h, value, label, accent=C_ACCENT):
    rounded_rect(slide, x, y, w, h, C_SURFACE2, C_LINE, 0.8, radius_emu=100000)
    textbox(slide, x + 160000, y + 130000, w - 320000, 380000, value, FS_KPI, accent, bold=True)
    textbox(slide, x + 160000, y + 560000, w - 320000, 220000, label, FS_BODY, C_MUTED)

# ── Slides ────────────────────────────────────────────────────────────────────
slide = prs.slides.add_slide(BLANK)
slide_bg(slide)
title_block(slide, 'Executive Brief', 'Presentation title goes here', 'A concise subtitle that explains the outcome')

# Example KPI strip
kpi_w = (CONTENT_W - 2 * G) // 3
kpi_card(slide, M, 1600000, kpi_w, 950000, '42%', 'Growth in target metric', C_ACCENT)
kpi_card(slide, M + kpi_w + G, 1600000, kpi_w, 950000, '3.8x', 'Efficiency improvement', C_ACCENT2)
kpi_card(slide, M + 2 * (kpi_w + G), 1600000, kpi_w, 950000, '12M', 'Projected timeline', C_ACCENT)

# Example content cards
card(slide, M, 2850000, (CONTENT_W - G) // 2, 1900000,
     'Key insight', 'Short explanation line one\nShort explanation line two', C_ACCENT)
card(slide, M + (CONTENT_W + G) // 2, 2850000, (CONTENT_W - G) // 2, 1900000,
     'Recommended action', 'Decision-oriented takeaway\nImplementation implication', C_ACCENT2)
footer(slide, '01 / Executive summary')
add_fade_transition(slide)

# ── Save ──────────────────────────────────────────────────────────────────────
OUT = '/workspace/output/output.pptx'
prs.save(OUT)
print(f'✓ Saved: {OUT}')
```

---

## Common patterns

### Slide layout recipes — choose one per slide

Never start from an empty slide and randomly place text. Pick one of these
composition recipes first:

| Layout | Use when | Structure |
|--------|----------|-----------|
| Hero + KPI strip | executive summary, opening insight | big title, short subtitle, 3 KPI cards |
| 2-column split | compare, problem/solution, before/after | left narrative panel, right cards/chart |
| 3-card grid | capabilities, options, pillars | three equal cards with accent bars |
| Timeline | roadmap, milestones | horizontal spine, dated cards, alternating labels |
| Chart + insight | data-heavy slide | chart panel 65%, insight cards 35% |
| Section divider | chapter transition | oversized section number, short statement |
| Quote/callout | key message | large quote box, source/caption, accent shape |

### Card component rules

Cards make PPTs feel designed. Use them for almost every grouped idea.

```python
def metric_row(slide, y, metrics):
    w = (CONTENT_W - G * (len(metrics) - 1)) // len(metrics)
    for i, item in enumerate(metrics):
        x = M + i * (w + G)
        kpi_card(slide, x, y, w, 900000, item['value'], item['label'], item.get('accent', C_ACCENT))
```

Card styling defaults:
- Surface fill: `C_SURFACE` or `C_SURFACE2`
- Border: subtle `C_LINE`, width `0.6–1.0 pt`
- Corner radius: `80000–120000` EMU
- Inner padding: at least `140000–180000` EMU
- Accent: one left bar, top bar, small chip, or highlighted number — not all at once

### Title hierarchy

Use a consistent top structure on content slides:

```text
EYEBROW / SECTION LABEL   9pt accent uppercase
Main slide title          24–30pt bold
Subtitle / takeaway       11–13pt muted
Accent divider            1 short cyan/violet line
```

Do not center every title. For professional decks, left-aligned titles with
strong margins usually look more modern and readable.

### Light theme option

If the user asks for clean/light/business style, switch to this palette:

```python
C_BG       = RGBColor(0xF8, 0xFA, 0xFC)
C_BG_2     = RGBColor(0xEE, 0xF2, 0xF7)
C_SURFACE  = RGBColor(0xFF, 0xFF, 0xFF)
C_SURFACE2 = RGBColor(0xF1, 0xF5, 0xF9)
C_TEXT     = RGBColor(0x0F, 0x17, 0x2A)
C_MUTED    = RGBColor(0x47, 0x55, 0x69)
C_LINE     = RGBColor(0xCB, 0xD5, 0xE1)
C_ACCENT   = RGBColor(0x25, 0x63, 0xEB)
C_ACCENT2  = RGBColor(0x7C, 0x3A, 0xED)
```

For light themes, use stronger text contrast and visible borders. Avoid pale gray
body text and invisible white-on-white cards.

### Anti-ugly rules

- Do not use default PowerPoint placeholders.
- Do not use a plain white background with black text unless the rest of the
  slide has deliberate editorial styling.
- Do not place raw paragraphs directly on the slide; convert them to cards,
  callouts, timelines, or diagrams.
- Do not use more than 2 font families or more than 2 accent colors.
- Do not use tiny dense text; split into multiple slides instead.
- Do not make all shapes fully saturated. Use deep/subtle surfaces and reserve
  bright color for emphasis.
- Do not mix random alignments. Left edges must line up.
- Do not leave charts with default Office styling; set transparent background,
  muted axes, and theme-colored series.

### Colors & theme

Define all colors as `RGBColor` constants at the top of the script.
Dark backgrounds look premium — use near-black (`0x0A, 0x14, 0x2E`) with bright accents.
**Never hardcode hex strings inline** — always reference a named constant.

### Multi-slide instead of click animations (WPS-safe default)

WPS does not reliably support click-triggered PowerPoint animations.
**Always use multiple slides** to reveal content progressively:

```
Slide 1 → skeleton / structure only
Slide 2 → skeleton + first data layer
Slide 3 → skeleton + all data layers
```

```python
def draw_skeleton(slide): ...
def draw_layer_a(slide): ...
def draw_layer_b(slide): ...

for show_a, show_b in [(False, False), (True, False), (True, True)]:
    s = prs.slides.add_slide(BLANK)
    draw_skeleton(s)
    if show_a: draw_layer_a(s)
    if show_b: draw_layer_b(s)
    add_fade_transition(s)
```

If the user explicitly asks for animations AND they are using Microsoft PowerPoint
(not WPS), you may attempt XML-based animations — but read `wps-compat.md` first.

### Timeline layout

```python
TL_L, TL_R = 850000, 11950000        # left/right margins (EMU)
TL_W  = TL_R - TL_L
M_STEP = TL_W // 11                   # 12 months → 11 intervals

def month_x(m):                       # 1-based month → EMU x-position
    return TL_L + M_STEP * (m - 1)
```

### Collision resolution (for timelines with dense labels)

```python
def resolve_collisions(events, card_w, gap):
    events.sort(key=lambda e: e['cx'])
    need = card_w + gap
    for _ in range(120):
        moved = False
        for i in range(len(events) - 1):
            a, b = events[i], events[i+1]
            if b['cx'] - a['cx'] < need:
                push = (need - (b['cx'] - a['cx'])) / 2
                a['cx'] -= push; b['cx'] += push; moved = True
        if not moved:
            break
```

### Shape IDs for animation

```python
shp = slide.shapes.add_shape(...)
shape_id = shp.shape_id        # use this in animation XML
```

---

## Reference files — read the relevant one(s) based on the task

| File | When to read |
|------|-------------|
| `pptx-patterns.md` | EMU 单位速查、连接器 XML、过渡 XML、多段落文字框、脚本结构模板 |
| `charts.md` | 需要柱状/折线/饼图/散点等数据图表时 |
| `standard-slides.md` | 需要完整 PPT 结构（封面、目录、内容页、结尾）时 |
| `wps-compat.md` | 用户提到 WPS 或动画效果异常时 |

Read these from `/workspace/skills/create-pptx/references/<filename>` using the `view` tool.

---

## Common mistakes to avoid

| Mistake | Fix |
|---------|-----|
| `pip install python-pptx` fails | Add `--break-system-packages` |
| `from pptx_helpers import ...` fails | Run the `cp` command in Mandatory First Steps |
| Saving to `/workspace/code/output.pptx` | Save to `/workspace/output/output.pptx` |
| Using `prs.slide_layouts[1]` (has placeholders) | Always use `prs.slide_layouts[6]` (blank) |
| Forgetting `add_fade_transition(slide)` | Call it on every slide before saving |
| `NameError: Emu` or `RGBColor` | Add the missing import at the top of the script |
