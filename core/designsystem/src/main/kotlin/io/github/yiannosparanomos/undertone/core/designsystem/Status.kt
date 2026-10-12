package io.github.yiannosparanomos.undertone.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The plain sentence that explains what is happening and why — "Teams quiet until Mon 09:00 · 4
 * held". Principle 3 of the UI direction: say it in words, not icons alone.
 */
@Composable
fun UtStatus(
    headline: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    needsAttention: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            // The accent marks the one thing that needs attention, and nothing else.
            color =
                if (needsAttention) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A large number with its label beneath, for counts like the held inbox. */
@Composable
fun UtCount(
    count: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
        UtLabel(label)
    }
}
