# 02: Load an existing view into the designer

**What to build:** The header offers a list of the project's `@Route` views
(the builder's own view excluded, only views extending a palette container
type). Picking one reads its source, rebuilds the designer-owned
`_initLayout()` as placeholders on the canvas (nesting and texts/labels intact)
and remembers it as the loaded view. A view without the method loads as an
empty canvas. A method containing a statement the codec does not recognise
shows an error naming the statement and leaves the canvas unchanged. Behind it:
the codec's `read` operation and the mapping design tree → placeholders. Adds a
small sample view with an `_initLayout()` so there is something to load.

**Blocked by:** 01

**Status:** done

- [x] View list shows `@Route` classes from the project's sources, not the builder
- [x] Loading the sample view shows its nested structure and texts on the canvas
- [x] Canvas root takes the view's container type
- [x] View without `_initLayout()` → empty canvas
- [x] Unrecognised statement → visible error naming it, canvas unchanged
- [x] JUnit: `read` of a missing method gives an empty tree
- [x] JUnit: `read` of an unrecognised statement fails with a message naming it
- [x] JUnit: round trip `read(write(source, tree))` equals `tree` for a nested tree
- [ ] Verified manually in the running app (dev loop) — pending: no browser in the container, the user verifies
