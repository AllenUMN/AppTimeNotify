package com.example.apptimenotify

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "usage_prefs")

object UsagePrefs {
    private val SELECTED_APP_PACKAGE = stringPreferencesKey("selected_app_package")
    private val SELECTED_APP_NAME = stringPreferencesKey("selected_app_name")
    private val LIMIT_HOURS = intPreferencesKey("limit_hours")
    private val LIMIT_MINUTES = intPreferencesKey("limit_minutes")

    suspend fun saveLimit(context: Context, packageName: String, appName: String, hours: Int, minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_APP_PACKAGE] = packageName
            preferences[SELECTED_APP_NAME] = appName
            preferences[LIMIT_HOURS] = hours
            preferences[LIMIT_MINUTES] = minutes
        }
    }

    fun getAppLimit(context: Context): Flow<AppLimit?> {
        return context.dataStore.data.map { preferences ->
            val packageName = preferences[SELECTED_APP_PACKAGE]
            val appName = preferences[SELECTED_APP_NAME]
            val hours = preferences[LIMIT_HOURS]
            val minutes = preferences[LIMIT_MINUTES]

            if (packageName != null && appName != null && hours != null && minutes != null) {
                AppLimit(packageName, appName, hours, minutes)
            } else {
                null
            }
        }
    }

    suspend fun clearLimit(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}

data class AppLimit(
    val packageName: String,
    val appName: String,
    val hours: Int,
    val minutes: Int
)
