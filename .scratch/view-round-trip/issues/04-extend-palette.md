# 04: Extend the palette with Vaadin components

**What to build:** The palette on the left offers grouped lists (layouts,
inputs, actions, text) of common Vaadin components. Container types accept
drops like the existing layouts; leaf components are instantiated directly with
a readable default text/label. The list is the spec's candidate set narrowed to
types whose text/label is set via a single-`String` constructor, verified
against the Vaadin API (MCP), not from memory.

**Blocked by:** 02

**Status:** done

- [x] Palette shows the groups layouts, inputs, actions, text
- [x] Every listed type's single-`String` constructor is confirmed (javap against the 25.3.2 jars, plus a reflection test)
- [x] Container types (e.g. form layout) accept dropped components
- [x] JUnit: every palette type survives `read(write(...))` unchanged
- [ ] Verified manually in the running app (dev loop) — pending: no browser in the container, the user verifies
