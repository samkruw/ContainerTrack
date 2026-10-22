#!/usr/bin/env python3
"""Publish a small visual review sheet of fixed test fixtures in CI logs and artifacts."""
import base64
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw

directory = Path('app/build/screenshots')
names = ['01-start', '09-job-detail', '12-permissions', '14-gallery', '15-ready-for-fill', '16-upload-error']
sheet = Image.new('RGB', (len(names) * 320, 680), '#0c100f')
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
