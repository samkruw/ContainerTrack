#!/usr/bin/env python3
"""Compare eight reviewed emulator goldens; missing images fail the check."""
from pathlib import Path
import argparse
from PIL import Image, ImageChops, ImageStat

NAMES = ('01-start', '02-delivery-setup', '03-ocr-scan', '04-delivery-photo',
         '05-return-logger', '06-return-setup', '07-export', '08-logger-due')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('goldens', type=Path)
    parser.add_argument('actual', type=Path)
    parser.add_argument('--max-mean-error', type=float, default=0.5)
    args = parser.parse_args()
    failures = []
    for name in NAMES:
        baseline, current = args.goldens / f'{name}.png', args.actual / f'{name}.png'
        if not baseline.is_file() or not current.is_file():
            failures.append(f'{name}: missing baseline or actual screenshot')
            continue
        with Image.open(baseline) as a, Image.open(current) as b:
            if a.size != b.size:
                failures.append(f'{name}: image size changed {a.size} -> {b.size}')
                continue
            diff = ImageChops.difference(a.convert('RGB'), b.convert('RGB'))
            error = sum(ImageStat.Stat(diff).mean) / 3
            if error > args.max_mean_error:
                failures.append(f'{name}: mean pixel error {error:.3f}')
                diff.save(args.actual / f'{name}-diff.png')
    print('\n'.join(failures) if failures else 'PASS: all eight screenshot baselines match')
    return bool(failures)

if __name__ == '__main__':
    raise SystemExit(main())
