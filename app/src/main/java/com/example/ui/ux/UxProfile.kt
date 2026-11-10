package com.example.ui.ux

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment

/**
 * Experience profiles for Clg 1%.
 * LEDGER is the shipping UI. Other profiles may only change presentation.
 * Colour still comes from ClgPalette / ThemeMode — never from here.
 */
enum class UxProfile(
    val id: String,
    val settingsLabel: String,
    val subtitle: String,
) {
    LEDGER(
        id = "ledger",
        settingsLabel = "UX-1 Ledger",
        subtitle = "Current layout. Nothing moves.",
    ),
    FOCUS_DESK(
        id = "focus_desk",
        settingsLabel = "UX-2 Focus Desk",
        subtitle = "Calmer spacing. One number at a time.",
    ),
    GLANCE_BOARD(
        id = "glance_board",
        settingsLabel = "UX-3 Glance Board",
        subtitle = "Home looks like the widget.",
    );

    companion object {
        fun fromId(id: String?): UxProfile =
            entries.firstOrNull { it.id == id } ?: LEDGER
    }
}

object UxProfileStore {
    private const val PREFS = "clg1_ux"
    private const val KEY = "ux_profile"

    fun load(context: Context): UxProfile {
        val id = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, UxProfile.LEDGER.id)
        return UxProfile.fromId(id)
    }

    fun save(context: Context, profile: UxProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, profile.id)
            .apply()
    }
}

@Composable
fun UxProfilePicker(
    selected: UxProfile,
    onSelect: (UxProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Experience", style = MaterialTheme.typography.titleMedium)
        Text(
            "Colour themes stay as they are. This only changes layout feel. Ledger is the original.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        UxProfile.entries.forEach { profile ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile == selected,
                        onClick = { onSelect(profile) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = profile == selected, onClick = { onSelect(profile) })
                Column(Modifier.padding(start = 8.dp).clickable { onSelect(profile) }) {
                    Text(profile.settingsLabel, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        profile.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
