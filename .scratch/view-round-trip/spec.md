Status: ready-for-agent

# Spec: Load existing views, save designs back to code, larger palette

## Problem Statement

The DnD builder can only design from scratch. Its "Output" pane shows a rough,
flat code sketch that has to be copied by hand, and the palette offers six
components. A developer who already has a view cannot open it in the designer,
and nothing the designer produces ends up in the project's code.

## Solution

The developer picks one of the project's `@Route` views in the builder. The
builder reads the view's designer-owned layout method and rebuilds it as
placeholders on the canvas. The developer rearranges it with drag and drop and
presses Save: the builder rewrites only that method in the view's source file,
formatted, and leaves everything else in the file untouched. The palette on the
left offers a wider set of common Vaadin components, grouped by kind.

This is a prototype/demo. Every decision below takes the simplest version that
does the core job.

## User Stories

1. As a developer, I want to see a list of the project's `@Route` views in the
   builder, so that I can pick the one I want to edit.
2. As a developer, I want the builder itself to be excluded from that list, so
   that I cannot accidentally edit the designer with itself.
3. As a developer, I want to load a view and see its components as placeholders
   on the canvas, so that I can continue designing where the code left off.
4. As a developer, I want nested layouts to load with their nesting intact, so
   that the canvas matches the structure in code.
5. As a developer, I want labels and texts given in code (e.g. a button's text,
   a field's label) to show on the loaded placeholders, so that I recognise my
   components.
6. As a developer, I want a view without a designer-owned method to load as an
   empty canvas, so that I can start designing an existing view from scratch.
7. As a developer, I want a clear error message when the designer-owned method
   contains code the designer does not understand, so that my hand-written code
   is never silently dropped.
8. As a developer, I want a Save action that writes the current design back
   into the loaded view's source file, so that I don't have to copy code by
   hand.
9. As a developer, I want Save to replace only the designer-owned method, so
   that my imports, fields, listeners and other methods stay exactly as I wrote
   them.
10. As a developer, I want Save to create the designer-owned method and call it
    from the constructor when the view does not have it yet, so that a first
    save just works.
11. As a developer, I want the imports for newly used component types added on
    save, so that the saved view compiles.
12. As a developer, I want the generated method formatted like ordinary Java
    code, so that it reads well in my IDE.
13. As a developer, I want Save to be unavailable while no view is loaded, so
    that I don't wonder where the design went.
14. As a developer, I want the "Output" pane to show the method exactly as Save
    would write it, so that I can preview the result before saving.
15. As a developer, I want the palette grouped into layouts, inputs, actions
    and text, so that I find components quickly.
16. As a developer, I want container components in the palette (e.g. form
    layout) to accept dropped components, so that I can build nested
    structures with them.
17. As a developer, I want every palette component to survive a save and a
    reload unchanged, so that I can trust the round trip.
18. As a developer, I want new components dropped from the palette to get a
    readable default text or label, so that they are visible on the canvas and
    in the code.
19. As a developer, I want the generated method to stand out as generated (a
    leading underscore and a Javadoc saying so), so that nobody edits it by
    hand by mistake.
20. As a developer, I want the builder to open in preview mode and switch to a
    design mode by a button, so that I look at the view before changing it.
21. As a developer, I want Save to appear only in design mode, so that saving
    is tied to designing.
22. As a developer, I want my unsaved design kept in a temporary file that is
    created when I enter design mode and updated on every change, so that a
    browser crash or an accidentally closed tab loses nothing.
23. As a developer, I want the builder to restore that temporary design
    automatically when I come back to the view, so that I continue where I
    stopped.
24. As a developer, I want Save to move the design into the real Java file and
    discard the temporary file, so that the source file is the only truth
    after saving.
25. As a developer, I want to click a component's label (or text, for
    components without a label) in design mode and change it in a small
    popover, so that I don't have to edit code for a caption.
26. As a developer, I want to type a field's placeholder text directly into
    the field in design mode, so that editing it feels like using the form.
27. As a developer, I want edited labels, texts and placeholder texts to show
    in the output and survive save and reload, so that the round trip stays
    complete.

## Implementation Decisions

- **Designer-owned method.** The designer owns exactly one method in a view,
  `private void _initLayout()` (name fixed for the prototype; the underscore
  and a Javadoc stating that the builder generates and overwrites it make the
  ownership obvious in code). It contains only
  component creation, the constructor's text/label argument, and `add(...)`
  calls. Everything outside it is the developer's and is never modified.
- **Design tree.** A small model, independent of Vaadin components and of the
  DnD wiring: each node has a component type, an optional text/label string, an
  optional placeholder text and ordered children. The root node represents the view itself; its children map
  to `add(...)` calls on `this`.
- **View source codec** (the one tested seam). A plain Java module with two
  operations:
  - `read(source) → design tree`: parses the view source and returns the tree
    built in the designer-owned method. Missing method → empty tree. Any
    statement it does not recognise → an exception naming that statement.
  - `write(source, tree) → source`: returns the source with the
    designer-owned method replaced (or created and called at the end of the
    constructor), required imports added, the rest of the file preserved.
  It uses JavaParser (new dependency; resolve the current release, don't
  guess). Only the designer-owned method is pretty-printed with JavaParser's
  printer; the rest of the file is kept as is (lexical preservation).
- **Generated code shape.** One local variable per component, named after its
  type plus a counter; text/label passed as the single `String` constructor
  argument; each child added to its parent via `add(...)`.
- **Replaces `Parser`.** The existing output writer goes away; the "Output"
  pane shows the codec's rendering of the designer-owned method.
- **Design tree ↔ placeholders.** A mapping in the UI layer converts the tree
  to placeholders when loading and placeholders to the tree when saving or
  updating the output. Placeholders need to carry the text/label so it
  survives the round trip.
- **View discovery.** The builder lists the `@Route` classes found in the
  project's `src/main/java` (resolved from the working directory), excluding
  the builder's own view. Loading and saving read and write that source file
  directly.
- **Root type.** A view is only offered when it extends one of the palette's
  container types; the canvas root becomes that type.
- **Preview and design mode.** The existing "Preview mode" checkbox is
  replaced: the builder starts in preview mode, a button switches to design
  mode (and back). Dragging, the trash and Save exist only in design mode.
- **Autosave to a temporary file.** Entering design mode for a view creates a
  temporary file for it (one file per view, in the per-user cache directory
  `~/.cache/dnd-builder-drafts` rather than the shared system temp directory,
  because drafts end up in source code and no other local user may plant one);
  every structure change rewrites it. Its content is the view source with the
  designer-owned method written by the codec, so restoring is a plain `read`
  and needs no second format. When a view is loaded and its temporary file
  exists, the design is restored from it without asking. Save writes the real
  source file and deletes the temporary file.
- **Editing texts in design mode.** "Placeholder text" means a field's
  `setPlaceholder` value, not the designer's stand-in components (which the
  code calls placeholders). Clicking a component's label, or its text when it
  has no label, opens a Vaadin `Popover` with an input to change it (and one
  for the placeholder text of `HasPlaceholder` components). For text fields,
  what the developer types into the field in design mode becomes its
  placeholder text; fields that cannot hold free text as value (date, time,
  number, combo box) get theirs through the popover. The
  generated method sets it with one `setPlaceholder(...)` call after the
  component's declaration; `read` accepts exactly that call.
- **Palette.** Grouped lists (layouts, inputs, actions, text). Candidates:
  vertical/horizontal/form layout; text, password, email, integer, number and
  text area fields, date and time pickers, checkbox, combo box; button; headings,
  paragraph, span. The final list is the subset whose text/label can be set via
  a single-`String` constructor, verified against the Vaadin API (MCP), not
  from memory. Containers get a drop-target placeholder like the existing
  layouts; leaf components keep being instantiated directly.

## Testing Decisions

- Good tests here go through the codec's `read`/`write` only, with Java source
  strings in and out. They check behaviour (the tree that comes out, the source
  that comes out), not how the codec walks the syntax tree.
- Tested module: the view source codec, with JUnit (`spring-boot-starter-test`
  is already on the test classpath).
  - round trip: `read(write(source, tree))` equals `tree`, for every palette
    component type and for nested containers;
  - code outside the designer-owned method is unchanged after `write`;
  - `write` on a view without the method creates it and calls it from the
    constructor;
  - `read` without the method gives an empty tree;
  - `read` with an unrecognised statement in the method fails with a message
    naming it.
- Not automated: the UI wiring (tree ↔ placeholders, view list, Save button,
  palette). Verified manually in the running app via the dev loop.
- Prior art: none, the project has no tests yet. These are the first.

## Out of Scope

- Editing properties other than text/label and placeholder text in the
  designer.
- Properties beyond text/label and placeholder text (sizes, variants, styles,
  listeners, data binding).
- Round-tripping arbitrary code: anything outside the designer-owned method is
  neither read nor written.
- Creating new view classes, choosing routes, or loading files outside this
  project's `src/main/java`.
- Undo, conflict detection when the file changed on disk after loading, and
  backups beyond the single temporary file per view.
- Saving from a production build (the source tree only exists in development).

## Further Notes

- Saving a view's source file can trigger the dev loop's reload; the designer
  state may be lost when the builder itself reloads. Acceptable for the demo.
- The palette's `getReadableName` default text is reused for new components.
