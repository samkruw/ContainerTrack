# Adaptive native capture

The user reports having to scroll to the shutter and asks for controls that fit
the phone. Continue the approved Graphite/Lime native Android design.

- Reserve space for controls before measuring the camera/photo preview.
- Portrait: compact context, flexible preview, fixed action dock.
- Landscape: context and secondary actions in a side column; preview and shutter
  alongside them. Preserve at least 48 dp touch targets.
- Replace the normal brand/back/tab stack with a compact capture toolbar. Keep
  reminders accessible from its notification button and preserve due alerts.
- Put saved-photo lists in an explicit photo sheet. Long histories and supporting
  information may scroll there; capturing and confirming photos must not.
- Manual scan input uses a dialog and pauses recognition while editing.
- Keep all Return counting, pending-photo restoration, delete/replace, read-only,
  email and reminder rules unchanged.

Validate 320×480, 360×640 and 640×320 dp, including space consumed by system bars
and the toolbar. Include 1.5 font scaling and real pending-photo screens. Confirm
control bounds, minimum touch size, non-overlap, positive preview area and actions.
Build a debug APK only.
