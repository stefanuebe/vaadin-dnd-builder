# 06: Edit labels, texts and placeholder texts in design mode

**What to build:** In design mode, clicking a component's label (or its text
when it has no label, e.g. a button or heading) opens a Vaadin `Popover` with an
input; confirming it changes the label/text on the canvas. For fields with a
placeholder text (`HasPlaceholder`), whatever the developer types into the
field in design mode becomes its placeholder text. Both show in the output pane
and survive save and reload: the design tree carries an optional placeholder
text, the generated method sets it with a `setPlaceholder(...)` call, and
`read` accepts that call. "Placeholder text" is the field property, not the
designer's stand-in components. Before wiring the label click, confirm via the
Vaadin MCP how a field's label is rendered (slotted light-DOM element or not),
so the click on the label can be told apart from a click into the input.

**Blocked by:** 05

**Status:** done

- [x] In design mode, clicking a label/text opens a popover with an input; confirming updates the component
- [x] Nothing opens in preview mode
- [x] Typing into a text field in design mode sets its placeholder text (date, time, number and combo box fields: via the popover)
- [x] Output pane shows the new label/text and a `setPlaceholder(...)` call
- [x] Save and reload keep labels, texts and placeholder texts
- [x] JUnit: round trip of a node with text and placeholder text
- [x] JUnit: `read` accepts `setPlaceholder(...)` on a declared component
- [ ] Verified manually in the running app (dev loop) — pending: no browser in the container, the user verifies
