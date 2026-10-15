# DnD Builder

Prototype of a drag-and-drop UI builder for Vaadin Flow.

## How I work here

- Answer in the language the user writes in, and keep it for the whole session —
  through tool output, skills and compacts written in another language. Code,
  comments, agent docs and commit messages are always in English.
- Code and document for humans. They have to understand and maintain this
  application after you are gone from the conversation.
- Don't guess, confirm with docs / sources / research results / MCP. Being
  uncertain is fine and saying so is fine; stating an assumption as fact is not.
- If there is no solution or answer, say it. Acknowledging failure is better than
  trying to hide it.
- Answer the question you were asked before you edit anything.
- Never self-dispatch after a question: if you ask something, wait for the
  answer before acting.
- Review before the gate and before the commit — see *Review before the gate,
  review before the commit*. That order is not yours to reorder.
- The only thing from the session that goes into a commit message is the
  `Co-Authored-By:` trailer Claude Code adds. A session link such as
  `https://claude.ai/code/session_…` never does. In cloud and Remote Control
  sessions Claude Code adds that link unless `attribution.sessionUrl` is `false`
  in `.claude/settings.json`, which it is here.
- Pick the cheapest model that fits a subagent. Always pass `model` explicitly
  — the inherited default is Opus or better, which is expensive for mechanical work.
  Restate the critical rules in each subagent's prompt. When unsure, start cheaper
  and escalate only if the output is shallow.

  | Subagent role | Model |
  |---|---|
  | Mechanical implementer (plan specifies the exact code) | Haiku |
  | Explore / search ("where is X defined") | Haiku |
  | Multi-file integration / pattern matching | Sonnet |
  | Per-phase code-quality or spec-compliance review | Sonnet |
  | Final whole-branch / holistic / deep design review | Opus or better |

## Stack

- **Vaadin** 25.3.2 (commercial) on **Spring Boot** 4.1.1, **JDK** 21+
- Base package: `com.example.application`
- Build/verify gate: `mvn clean verify`. It builds the production frontend bundle
  (the project has no `production` profile; `vaadin-dev` is optional and stays out
  of the jar) and runs no browser test.
- Frontend sources live in `src/main/frontend/`.

## Module structure

Single module: the Spring Boot app at the project root.

## State management

This project uses **classic** state.

Use plain component fields and Spring beans for state; wire UI updates explicitly
(here: the project's own `EventBus`). Do not introduce Signals unless a view is
refactored to them deliberately.

## Testing

There are no tests yet. Browserless layer: **none**. Browser e2e: **no**. All tests
run with `mvn clean verify`.

- **JUnit** — unit tests.

Mirror existing tests when adding new ones.

## Review before the gate, review before the commit

The order is fixed:

1. the tests covering what changed are green — the targeted run, not the gate:
   `mvn -pl <module> test` for one module, `mvn -Dtest=<Class> test` for one class
2. the review: **always three agents**, in parallel. `mattpocock-skills:code-review`
   brings two (Standards, Spec). The third takes an axis from the change itself:
   framework/API usage, a wrapped library, or test quality (do the tests prove the
   behaviour, or pass around it?). Without a spec `code-review` skips Spec, so add
   a second axis of that kind. The review loops: fix every major and run the
   next round on the whole scope under review; the agents judge what is major.
   Pass each round the findings already rejected, with the reason. Once a round
   finds no major, fix directly the minors in test code and the obvious ones (a
   typo, a stale comment, a wrong name), report the other minors and ask whether
   to fix them and review again. If round 5 still finds a major, stop and ask how
   to go on.
3. the full gate `mvn clean verify`
4. commit

Not optional and not something to wait to be asked for. Reviewing after the gate
pays for the long production build twice; committing before the review means the
commit is not the reviewed state.

Step 2, now that it happens before the commit:

- It has to look at the **working tree**. `mattpocock-skills:code-review` pins its
  diff as `git diff <fixed-point>...HEAD`, which at that moment still shows the
  state *before* the change: it then either stops on an empty diff (it checks) or
  reviews the previous commit instead of the change in front of it. Give it the
  fixed point and have it diff the working tree, `git diff <fixed-point>`.
- **Name the fixed point yourself**, don't make the user supply it: `HEAD` for a
  single change on top of the current branch, the branch point for work spread
  over several commits. Before the project's first commit there is no fixed point
  — review the working tree as it stands.
- It expects `docs/agents/issue-tracker.md` and will send you to
  `/setup-matt-pocock-skills` if that file is missing. Run that once per project;
  without a spec the Spec axis reports "no spec available" and the Standards axis
  still does its work.

**This overrides any skill that orders it differently.** `implement` runs "the full
test suite once at the end", then the review, then the commit — the review lands
after the expensive run.

## Dev server

The app runs under the Vaadin dev loop (preview), not under Maven: a daemon owns
the process, `apply` makes edits live and its exit code says whether they are.
The `vaadin-devloop` skill describes the cycle — follow it.

- `.vaadin/vaadin-dev status | start | apply | restart | stop` — serves on port 8080.
- Never start the app with `spring-boot:run` or an IDE run configuration: a
  second launcher fights the daemon for the port.
- Maven stays for tests and the gate; `apply` is for making edits live while you work.
- Run `.vaadin/vaadin-dev shutdown` before the gate: `mvn clean` deletes `target`
  under the running app.
- The loop runs only in the devcontainer. The host opens the app at the
  container's IP, which the status line shows.
- After a Vaadin upgrade run `mvn vaadin:install-dev-cli` again.

## Release

The agent does all of this except the push, and only when the maintainer asks.

1. Remove `-SNAPSHOT` from the version in `pom.xml`. Where the README shows the
   version, set it there too.
2. Commit as `Release <version>` and tag it with
   `git tag -a <version> -m "Release <version>"`.
3. The maintainer pushes the branch and the tag together, and creates the GitHub
   release.
4. Only then bump to the next snapshot, usually the next patch (`1.0.0` becomes
   `1.0.1-SNAPSHOT`), and commit as `Bump to <next>-SNAPSHOT`.

## Conventions

- Vaadin views: `@Route` only. The project has no Spring Security, so views carry
  no access annotation (`@AnonymousAllowed` / `@PermitAll`). Add them together with
  security.
- The MCP `vaadin` server is the source of truth for Vaadin API — prefer it over
  memory when reaching for component APIs.
- Don't pin dependency versions to a guessed "latest" — resolve the current
  release first. A version marked as pinned under *Stack* stays where it is.
- Code style: read and follow `CODING_STANDARDS.md`.

## Agent skills

### Issue tracker

Issues and specs live as local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default labels (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`), recorded as a `Status:` line. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and `docs/adr/` at the repo root, created lazily. See `docs/agents/domain.md`.
