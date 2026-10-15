# DnD Builder — Glossary

- **Designer component / placeholder**: a component on the builder's canvas.
  Containers are `*Placeholder` subclasses (`VerticalLayoutPlaceholder`, …);
  leaves are real Vaadin components. All carry the element property
  `DndUtils.PLACEHOLDER_MARKER` (not `placeholder`, which fields use).
- **Placeholder text**: a field's `setPlaceholder(...)` value. Never call it just
  "placeholder" — that word means designer components in the code.
- **Design tree**: `DesignNode` — type, text/label (constructor argument),
  placeholder text, children. The root stands for the view itself.
- **Designer-owned method**: `_initLayout()` in a view; generated, overwritten on
  every Save. Everything else in the view belongs to the developer.
- **Draft**: the unsaved design of a loaded view, kept while in design mode
  (see `docs/adr/0002-…`).
- **Preview mode / design mode**: the builder starts in preview; dragging, trash,
  Save and text editing exist only in design mode.
