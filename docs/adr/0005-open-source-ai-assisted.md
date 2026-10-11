# 5. Open source, developed with AI assistance

Date: 2026-10-12. Status: accepted.

## Context

UnderTone asks for powerful permissions; users should be able to verify what it does. It is
developed with Claude, and there is no single industry standard yet for AI-assisted projects.

## Decision

- License: **Apache-2.0**, with a NOTICE file. Copyright held by the maintainer.
- AI assistance is disclosed openly: a "Built with Claude" section in the README,
  `Co-Authored-By: Claude` trailers on assisted commits, and AI rules in CONTRIBUTING.
- The maintainer reviews every change; `CLAUDE.md` holds the project rules that both humans
  and AI sessions follow.

## Consequences

Contributions are accepted under Apache-2.0. Generated code is held to the same review
standard as hand-written code.
