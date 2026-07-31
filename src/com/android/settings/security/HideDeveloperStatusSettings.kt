/*
 * SPDX-FileCopyrightText: Evolution X
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.security

import android.app.ActivityManager
import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settingslib.spa.framework.theme.SettingsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mist.settings.fragments.miscellaneous.AppListEntry
import org.mist.settings.fragments.miscellaneous.AppPickerItem
import org.mist.settings.fragments.miscellaneous.AppPickerSearchField
import org.mist.settings.fragments.miscellaneous.SpoofingEmptyState
import org.mist.settings.fragments.miscellaneous.SpoofingHeaderCard
import org.mist.settings.fragments.miscellaneous.SpoofingLoadingBox
import org.mist.settings.fragments.miscellaneous.filterInstalledApps
import org.mist.settings.fragments.miscellaneous.killPackages
import org.mist.settings.fragments.miscellaneous.targetedFirstComparator

// ---------------------------------------------------------------------------
// Settings helpers — current-user only. Single read-then-write per toggle,
// no whole-set round-trip on every fragment recreation (that round-trip was
// the source of the lost-write race in the old Java/RecyclerView version).
// ---------------------------------------------------------------------------

private fun readHiddenSet(cr: ContentResolver): Set<String> {
    val raw = Settings.Secure.getString(cr, Settings.Secure.HIDE_DEVELOPER_STATUS)
    if (raw.isNullOrBlank()) return emptySet()
    return raw.split(",").filter { it.isNotBlank() }.toSet()
}

private fun writeHiddenSet(cr: ContentResolver, packages: Set<String>) {
    Settings.Secure.putString(
        cr,
        Settings.Secure.HIDE_DEVELOPER_STATUS,
        packages.joinToString(","),
    )
}

// ---------------------------------------------------------------------------
// Fragment
// ---------------------------------------------------------------------------

class HideDeveloperStatusSettings : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().title = getString(R.string.hide_developer_status_title)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SettingsTheme {
                HideDeveloperStatusContent(context = requireContext())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Root composable
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HideDeveloperStatusContent(context: android.content.Context) {
    val pm = context.packageManager
    val activityManager = remember { context.getSystemService(ActivityManager::class.java) }
    val scope = rememberCoroutineScope()

    val hiddenPackages = remember {
        context.resources.getStringArray(R.array.hide_developer_status_hidden_apps).toSet()
    }

    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val allApps = remember { mutableStateListOf<AppListEntry>() }

    // -------------------------------------------------------------------
    // Load (re-runs when showSystemApps changes) — mirrors SensorBlock /
    // TensorTargets / PixelProps.
    // -------------------------------------------------------------------

    LaunchedEffect(showSystemApps) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val hidden = readHiddenSet(context.contentResolver)

            val installed = filterInstalledApps(
                pm = pm,
                showSystem = showSystemApps,
                targeted = hidden,
                hidden = hiddenPackages,
                extraFilter = { app ->
                    !app.packageName.contains("android.settings")
                },
            )
                .sortedWith(targetedFirstComparator(pm, hidden))
                .map { app ->
                    AppListEntry(
                        packageName = app.packageName,
                        label = pm.getApplicationLabel(app).toString(),
                        icon = runCatching { pm.getApplicationIcon(app) }.getOrNull(),
                        isSystem = app.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        isSelected = app.packageName in hidden,
                    )
                }

            // Prune stale package names (uninstalled apps) from the stored set.
            val installedPkgs = installed.map { it.packageName }.toSet()
            val pruned = hidden.filter { it in installedPkgs }.toSet()
            if (pruned.size < hidden.size) writeHiddenSet(context.contentResolver, pruned)

            withContext(Dispatchers.Main) {
                allApps.clear()
                allApps.addAll(installed)
                isLoading = false
            }
        }
    }

    val filteredApps = remember(searchQuery, allApps.toList()) {
        val q = searchQuery.lowercase()
        allApps.filter { app ->
            q.isEmpty() ||
                app.label.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
        }
    }

    val activeCount = allApps.count { it.isSelected }

    // -------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------

    Scaffold(containerColor = Color.Transparent) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SpoofingHeaderCard(
                title = stringResource(R.string.hide_developer_status_title),
                subtitle = if (activeCount == 0)
                    stringResource(R.string.ts_no_targets)
                else
                    stringResource(R.string.hide_developer_status_count, activeCount),
            ) {
                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AppPickerSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilterChip(
                selected = showSystemApps,
                onClick = { showSystemApps = !showSystemApps },
                label = { Text(stringResource(R.string.show_system_apps)) },
                leadingIcon = if (showSystemApps) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else null,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                SpoofingLoadingBox(modifier = Modifier.weight(1f))
            } else if (filteredApps.isEmpty()) {
                SpoofingEmptyState(
                    icon = Icons.Default.BugReport,
                    title = if (searchQuery.isBlank())
                        stringResource(R.string.hide_developer_status_no_apps_available)
                    else
                        stringResource(R.string.hide_developer_status_no_apps_found, searchQuery),
                    description = stringResource(R.string.hide_developer_status_summary),
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppPickerItem(
                            packageName = app.packageName,
                            label = app.label,
                            icon = app.icon,
                            isSystem = app.isSystem,
                            checked = app.isSelected,
                            onToggle = { nowHidden ->
                                val index = allApps.indexOfFirst {
                                    it.packageName == app.packageName
                                }
                                if (index < 0) return@AppPickerItem
                                allApps[index] = allApps[index].copy(isSelected = nowHidden)
                                val snapshot = allApps.filter { it.isSelected }
                                    .map { it.packageName }.toSet()
                                scope.launch(Dispatchers.IO) {
                                    writeHiddenSet(context.contentResolver, snapshot)
                                    // Force-stop so the app re-reads ADB_ENABLED /
                                    // DEVELOPMENT_SETTINGS_ENABLED on next launch.
                                    killPackages(activityManager, setOf(app.packageName))
                                }
                            },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
