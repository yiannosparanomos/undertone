---
name: add-scenario
description: Add or change an UnderTone rule behaviour test-first - write the scenario, then change the engine, then update the test matrix and spec. Use for any change to how notifications or calls are decided.
---

# Add or change a rule behaviour

UnderTone's behaviour is defined by the scenarios in
`core/domain/src/test/kotlin/io/github/yiannosparanomos/undertone/core/domain/scenario/ScenarioCatalog.kt`.
Change behaviour in this order:

1. **Describe it as a scenario first.** Add a `NotificationScenario` or `CallScenario` to the
   right group in `ScenarioCatalog` (P profiles, L levels, H hours, C calendar, O overrides,
   K calls). Use the next free id in that group; ids are never reused or renumbered. Prefer
   the shared fixtures in `Fixtures.kt` (`myRules`, `Week`, `Apps`, `Callers`, `Events`); add a
   fixture there only if several scenarios need it. Set `expectReason` and, for hidden
   notifications, `expectReleaseAt`.
2. **Run it and watch it fail:** `./gradlew :core:domain:test`. A new behaviour that already
   passes needs no engine change; say so.
3. **Change the engine** in `core/domain/src/main/...`. Keep it pure Kotlin: no Android
   imports, no `LocalDateTime.now()`.
4. **Run all scenarios** until green. If an existing scenario breaks, stop: either the new
   behaviour conflicts with a promise already made (ask the maintainer), or the old scenario
   was wrong (explain why in the PR).
5. **Update the docs:** a row in `docs/testing/test-matrix.md` (id, behaviour, layers), the
   relevant section of `docs/spec/v1-spec.md`, and `docs/user-guide.md` if users would notice.
6. **Format:** `ktlint -F "**/*.kt" "**/*.kts"`.
