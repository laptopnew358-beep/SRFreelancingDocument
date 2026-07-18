package com.personal.freelancingdocument.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "freelancing_document_prefs")

/** Theme options remembered across launches. */
enum class ThemeOption { LIGHT, DARK, SYSTEM }

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_option")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val ACCOUNT_NAME = stringPreferencesKey("account_display_name")
        val DRIVE_ROOT_FOLDER_ID = stringPreferencesKey("drive_root_folder_id")
        val LAST_SYNC_TIME = stringPreferencesKey("last_sync_time")
    }

    val themeOption: Flow<ThemeOption> = context.dataStore.data.map { prefs ->
        ThemeOption.valueOf(prefs[Keys.THEME] ?: ThemeOption.SYSTEM.name)
    }

    suspend fun setThemeOption(option: ThemeOption) {
        context.dataStore.edit { it[Keys.THEME] = option.name }
    }

    val accountEmail: Flow<String?> = context.dataStore.data.map { it[Keys.ACCOUNT_EMAIL] }
    val accountName: Flow<String?> = context.dataStore.data.map { it[Keys.ACCOUNT_NAME] }
    val driveRootFolderId: Flow<String?> = context.dataStore.data.map { it[Keys.DRIVE_ROOT_FOLDER_ID] }
    val lastSyncTime: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_SYNC_TIME] }

    suspend fun setAccount(email: String, displayName: String?) {
        context.dataStore.edit {
            it[Keys.ACCOUNT_EMAIL] = email
            it[Keys.ACCOUNT_NAME] = displayName ?: email
        }
    }

    suspend fun setDriveRootFolderId(id: String) {
        context.dataStore.edit { it[Keys.DRIVE_ROOT_FOLDER_ID] = id }
    }

    suspend fun setLastSyncTime(iso: String) {
        context.dataStore.edit { it[Keys.LAST_SYNC_TIME] = iso }
    }

    /** Clears everything on logout so no trace of the previous account remains. */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
