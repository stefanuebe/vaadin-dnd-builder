# 1. Write views back by positional text edits

Date: 2026-10-15

## Status

Accepted

## Context

Save must replace the designer-owned `_initLayout()` method of a view, add a
missing method, its constructor call and imports, and leave everything else in
the file exactly as the developer wrote it.

## Decision

`ViewSourceCodec.write` parses the source with JavaParser only to find
positions (method incl. Javadoc, constructor and class closing braces, last
import) and splices text at those offsets, back to front. Only the method itself
is printed by JavaParser's pretty printer (LF line endings).

## Rejected alternatives

- **JavaParser `LexicalPreservingPrinter`**: inserted nodes are printed with its
  own indentation and comment handling, which is hard to control; byte-identical
  preservation of the rest of the file was not guaranteed.
- **Regenerating the whole file**: destroys hand-written code.

## Consequences

The rest of the file stays byte-identical (tested, incl. idempotent re-write).
Known limits: CRLF files get LF-only inserted lines, tab-indented files get
space-indented additions, simple-name import clashes are not detected.
