# Container Track 1.5.1 — adaptive capture

Version code 13. Native Android debug build.

- The shutter and photo actions occupy reserved space; the preview adapts to the
  remaining viewport instead of using a fixed 350 dp height.
- Landscape places context and secondary actions beside the preview.
- Capture pages use a compact top bar. The reminder button opens pending reminders;
  due Logger alerts and Android notifications remain active.
- Delivery and Return photos are accessible through a separate photo sheet.
- Photo confirmation, retaking, discarding and Return categories stay accessible
  without scrolling the capture page. Buttons retain 48 dp touch targets.
- Manual container-number entry pauses OCR while its dialog is open.

Return documentation counts, the 24-hour reminder, whole-order email attachments
and explicit sent confirmation remain as documented in UPDATE_1_5_0.md. No database
migration is needed. Install over an APK signed with the same certificate to retain
local orders and photos.

Validation results are recorded in QA_STATUS.md.
