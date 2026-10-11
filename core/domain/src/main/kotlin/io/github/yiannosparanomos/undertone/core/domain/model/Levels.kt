package io.github.yiannosparanomos.undertone.core.domain.model

/**
 * How a notification from an app is treated. Declared from least to most strict:
 * when two rules disagree, the stricter one wins (see [stricterOf]).
 */
enum class Level {
    /** Shown, and UnderTone plays the app's chosen sound. */
    RING,

    /** Shown in the shade, no sound or vibration. Logged. */
    SILENT,

    /** Not shown now; held (snoozed) until the app's level is no longer HIDDEN. Logged. */
    HIDDEN,

    /** Spam: dismissed immediately and never released. Counted, not logged in detail. */
    BLOCKED,
}

/** The stricter of two levels. */
fun stricterOf(
    a: Level,
    b: Level,
): Level = if (a.ordinal >= b.ordinal) a else b

/** Which top-level profile is active. Only [MY_RULES] runs the rule engine. */
enum class Profile {
    /** The user's full setup: per-app levels, hours, calendar rules, call tiers. */
    MY_RULES,

    /** UnderTone steps aside; the phone behaves normally and held notifications are released. */
    ALLOW_ALL,

    /** UnderTone steps aside; Android's own Do Not Disturb settings apply. */
    SYSTEM_DND,
}

/** Who may ring. Declared from least to most strict. */
enum class CallTier {
    EVERYONE,
    CONTACTS,
    ALLOW_LIST,
    NOBODY,
}

/** The stricter of two call tiers. */
fun stricterOf(
    a: CallTier,
    b: CallTier,
): CallTier = if (a.ordinal >= b.ordinal) a else b

/** Who can break through by calling again within the repeat window. */
enum class RepeatGroup {
    ALLOW_LIST,
    EVERYONE,
}
