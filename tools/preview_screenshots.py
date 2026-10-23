#!/usr/bin/env python3
"""Publish a small visual review sheet of fixed test fixtures in CI logs and artifacts."""
import base64
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw

directory = Path('app/build/screenshots')
names = ['20-reference-orders', '21-reference-new-order', '22-reference-photo-review', '23-reference-gallery', '24-reference-photo-selection', '25-reference-settings', '26-reference-upload']
missing = [name for name in names if not (directory / f'{name}.png').is_file()]
if missing: print('MISSING_REFERENCE_CAPTURES=' + ','.join(missing))
names = [name for name in names if (directory / f'{name}.png').is_file()]
if not names: raise SystemExit('No reference screenshots available; visual QA blocked')
sheet = Image.new('RGB', (len(names) * 320, 680), '#151a1d')
draw = ImageDraw.Draw(sheet)
for index, name in enumerate(names):
    with Image.open(directory / f'{name}.png') as screenshot:
        preview = ImageOps.contain(screenshot.convert('RGB'), (312, 646))
        sheet.paste(preview, (index * 320 + (320 - preview.width) // 2, 26))
    draw.text((index * 320 + 8, 6), name, fill='#bac7d8')
destination = directory / 'visual-review.jpg'
sheet.save(destination, quality=82)
# Only synthetic fixture screens; lets remote reviewers inspect CI without artifact credentials.
print('SCREENSHOT_PREVIEW_BASE64=' + base64.b64encode(destination.read_bytes()).decode('ascii'))
