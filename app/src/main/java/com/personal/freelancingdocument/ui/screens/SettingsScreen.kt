package com.personal.freelancingdocument.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.freelancingdocument.util.ThemeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    accountEmail: String?,
    currentTheme: ThemeOption,
    onThemeChange: (ThemeOption) -> Unit,
    onSyncNow: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            accountEmail?.let {
                Text("Signed in as", style = MaterialTheme.typography.labelMedium)
                Text(it, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
            }

            Text("Appearance", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = currentTheme == option,
                        onClick = { onThemeChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeOption.entries.size)
                    ) {
                        Text(
                            when (option) {
                                ThemeOption.LIGHT -> "Light"
                                ThemeOption.DARK -> "Dark"
                                ThemeOption.SYSTEM -> "System"
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            ListItem(
                headlineContent = { Text("Sync Now") },
                supportingContent = { Text("Push local changes and pull the latest from Drive") },
                leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = ListItemDefaults.colors(),
            )
            OutlinedButton(onClick = onSyncNow, modifier = Modifier.fillMaxWidth()) {
                Text("Sync Now")
            }

            Spacer(Modifier.height(16.dp))
            ListItem(
                headlineContent = { Text("About App") },
                supportingContent = { Text("Freelancing Document · v1.0 · Personal use only") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}
