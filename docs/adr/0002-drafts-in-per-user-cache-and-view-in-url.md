# 2. Drafts in a per-user cache, loaded view in the URL

Date: 2026-10-15

## Status

Accepted

## Context

The unsaved design must survive a browser crash or a closed tab (the user asked
for a "tmp file"), and a reload must bring the design back without asking.

## Decision

- In design mode the loaded view's design is written on every change to a draft
  in `~/.cache/dnd-builder-drafts/` (one file per view; content = the view
  source with the design written in, so restoring is a plain `read`). Save
  writes the real file and deletes the draft.
- The loaded view is the `view` query parameter of the builder's URL; a reload
  loads it again and a draft that differs from the saved design wins silently.
  The parameter is accepted only if it names one of the views the builder lists.

## Rejected alternatives

- **Shared system temp directory** (`/tmp/dnd-builder-drafts`): a security
  review flagged it — another local user could plant a draft, and drafts end up
  in source code on Save.
- **Remembering the view in the session**: lost with a crashed browser.
- **A second draft format (JSON)**: the view source itself already round-trips.

## Consequences

A stale draft wins over later edits of the real file's `_initLayout()`.
