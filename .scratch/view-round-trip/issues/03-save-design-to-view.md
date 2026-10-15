# 03: Save the design into the loaded view

**What to build:** A Save action in the header, enabled only while a view is
loaded, writes the current design into that view's source file: the
designer-owned `_initLayout()` is replaced (or created and called at the end of
the constructor), imports for newly used component types are added, and
everything else in the file stays exactly as it was. The user gets visible
feedback that the save succeeded or failed.

**Blocked by:** 02

**Status:** done

- [x] Save is disabled until a view is loaded
- [x] Save writes the file; reloading the view shows the saved design
- [x] Code outside `_initLayout()` is unchanged after save
- [x] A view without the method gets it, plus a call at the end of the constructor
- [x] Imports for newly used component types are added
- [x] Success or failure is shown to the user (not seen in a browser yet)
- [x] JUnit: `write` preserves code outside the method unchanged
- [x] JUnit: `write` on a view without the method creates and calls it
- [ ] Verified manually in the running app (dev loop) — pending: no browser in the container, the user verifies
