# Contributing to UnderTone

Thanks for helping. UnderTone is small and opinionated: privacy and predictable behaviour come
before features.

## Before you start

- For anything bigger than a typo, open an issue first so we can agree on the behaviour.
- Read [CLAUDE.md](CLAUDE.md): its "Non-negotiable invariants" apply to every contributor,
  human or AI.

## Workflow

1. Branch from `main`: `feat/short-name`, `fix/short-name`, `docs/short-name`.
2. **Behaviour changes are test-first**: add or change a scenario in `ScenarioCatalog`
   before changing the engine (see `.claude/skills/add-scenario/SKILL.md`, which is useful
   for humans too).
3. Run `./gradlew :core:domain:test assembleDebug lintDebug` and `ktlint -F "**/*.kt" "**/*.kts"`.
4. Commit with [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`,
   `docs:`, `test:`, `build:`, `ci:`, `refactor:`, `chore:`.
5. Open a pull request and fill in the template. CI must be green before review.

## AI-assisted contributions

AI tools are welcome; the project itself is built with Claude. The rules:

- **You are responsible for what you submit.** Read and understand every line, including
  generated code, before opening a PR.
- **Say so.** Mention AI assistance in the PR description, and keep tool-added
  `Co-Authored-By` trailers in commits.
- **No unreviewed bulk changes.** Large generated refactors need an issue and agreement first.

## Code style

- Kotlin, ktlint `ktlint_official` (configured in `.editorconfig`).
- Compose UI uses `core:designsystem` components and theme tokens only.
- All user-visible text lives in `strings.xml`.

## License

By contributing you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
