# 01: Output pane shows the nested `_initLayout()` from the new codec

**What to build:** The "Output" pane shows the current design as a formatted,
nested `private void _initLayout()` method, exactly as Save will later write it.
Texts/labels of palette components appear as constructor arguments. Behind it:
the design tree model, the view source codec's `write` operation (JavaParser,
current release resolved first), and the mapping placeholders → design tree.
The old output writer is removed. Placeholders carry their text/label so it can
reach the tree. See the spec's Implementation Decisions.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] JavaParser is a dependency, at a version resolved from its release source
- [x] The design tree holds type, optional text/label and ordered children
- [x] Output pane shows nested containers as nested `add(...)` calls on the right parent
- [x] Root children are added to `this`; one local variable per component
- [x] New palette components get a readable default text/label that shows in the output
- [x] The old output writer is gone
- [x] JUnit tests through `write` cover a nested tree and a text/label argument
