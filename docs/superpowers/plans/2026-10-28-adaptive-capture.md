# Adaptive capture implementation

1. Inspect the fixed-height camera preview, scrolling capture screens and activity
   chrome; preserve camera IO and workflow gates.
2. Introduce a bounded shared workspace and testable camera frame/shutter layout.
3. Adapt scan, Delivery and Return capture/review; move saved photos into a sheet.
4. Add compact capture navigation and accessible reminders.
5. Check viewport bounds and interactions, review screenshots, run unit/lint and
   debug builds. Record actual validation results and any environment limits.
6. Publish the source branch to the existing GitHub debug workflow and provide the
   debug APK. Do not merge or create a release.
