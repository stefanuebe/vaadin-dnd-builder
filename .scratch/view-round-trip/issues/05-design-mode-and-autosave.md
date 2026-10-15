# 05: Preview/design mode and autosave to a temporary file

**What to build:** The builder opens in preview mode. A button switches to
design mode and back; dragging, the trash and Save are only available in design
mode (replacing the old "Preview mode" checkbox). Entering design mode for the
loaded view creates a temporary file for that view in the system temp
directory, and every structure change rewrites it with the view source carrying
the current design (written by the codec). Loading a view whose temporary file
exists restores the design from it automatically. Save writes the real source
file and deletes the temporary file. See the spec's Implementation Decisions.

**Blocked by:** 03

**Status:** done

- [x] The builder starts in preview mode; a button toggles design mode
- [x] Drag and drop, trash and Save are only available in design mode
- [x] Entering design mode creates the view's temporary file
- [x] Every structure change rewrites the temporary file
- [x] Reloading the page after a change restores the unsaved design automatically (the loaded view is kept in the URL as `?view=…`)
- [x] Save writes the real file and deletes the temporary file
- [ ] Verified manually in the running app (dev loop) — pending: no browser in the container, the user verifies
