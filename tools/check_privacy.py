#!/usr/bin/env python3
"""Source audit, or fail-closed merged-manifest audit after an Android build."""
import argparse
from pathlib import Path
import re
import sys
import tomllib
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
TOOLS = '{http://schemas.android.com/tools}'
ALLOWED = {
    'android.permission.CAMERA', 'android.permission.POST_NOTIFICATIONS',
    'android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE', 'android.permission.WAKE_LOCK',
    'android.permission.SCHEDULE_EXACT_ALARM', 'android.permission.RECEIVE_BOOT_COMPLETED',
    'ch.zerogen.containertrack.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION',
}

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--merged', action='store_true')
    parser.add_argument('--apk-permissions', type=Path)
    args = parser.parse_args()
    manifests = [ROOT / 'app/src/main/AndroidManifest.xml']
    if args.merged:
        manifests = sorted(p for p in (ROOT / 'app/build/intermediates').rglob('AndroidManifest.xml')
                           if 'merged_manifest' in str(p) and 'debug' in str(p).lower() and 'androidtest' not in str(p).lower())
        if not manifests:
            sys.exit('FAIL: No debug merged manifest. Run ./gradlew assembleDebug first.')
    failures = []
    if args.apk_permissions:
        permissions = set(args.apk_permissions.read_text().split())
        unexpected = permissions - ALLOWED
        if unexpected:
            failures.append(f'APK unexpected permissions: {sorted(unexpected)}')
    for manifest in manifests:
        tree = ET.parse(manifest)
        permissions = {node.get(ANDROID + 'name') for node in tree.getroot()
                       if node.tag.startswith('uses-permission') and node.get(TOOLS + 'node') != 'remove'}
        unexpected = permissions - ALLOWED
        if unexpected:
            failures.append(f'{manifest}: unexpected permissions {sorted(unexpected)}')
        app = tree.find('application')
        if app is None or app.get(ANDROID + 'allowBackup') != 'false':
            failures.append(f'{manifest}: backups must be disabled')
    banned_code = re.compile(r'\b(?:WebView|FirebaseAnalytics|FirebaseCrashlytics|OkHttpClient|Retrofit)\b|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----')
    network_code = re.compile(r'https?://|java\.net\.|android\.net\.(?:ConnectivityManager|NetworkRequest)')
    network_adapters = {'ch/zerogen/containertrack/' + name for name in (
        'GraphPhotoClient.kt', 'MsalAccountProvider.kt', 'MicrosoftConfig.kt', 'NetworkMonitor.kt')}
    source_root = ROOT / 'app/src/main/java'
    for source in source_root.rglob('*.kt'):
        text = source.read_text()
        if banned_code.search(text):
            failures.append(f'{source}: forbidden WebView/analytics/private key')
        if network_code.search(text) and source.relative_to(source_root).as_posix() not in network_adapters:
            failures.append(f'{source}: network code outside designated Microsoft/connectivity adapter')
    catalog = tomllib.loads((ROOT / 'gradle/libs.versions.toml').read_text())
    for name, library in catalog['libraries'].items():
        module = library.get('module', '')
        if any(banned in module.lower() for banned in ('firebase', 'okhttp', 'retrofit', 'supabase', 'analytics', 'crashlytics')):
            failures.append(f'Forbidden direct dependency: {name} = {module}')
    if failures:
        print('\n'.join(failures), file=sys.stderr)
        return 1
    print(f'PASS: {"merged" if args.merged else "source"} manifest permissions, backups, network adapter boundaries, direct dependencies')
    if not args.merged:
        print('Source audit only; does not verify transitive dependencies or the packaged APK.')
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
