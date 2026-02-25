package com.corneye.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

object UserPreferences {
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_FULLNAME = stringPreferencesKey("fullname")
    private val KEY_EMAIL = stringPreferencesKey("email")

    suspend fun saveUser(context: Context, userId: String, fullname: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_FULLNAME] = fullname
            prefs[KEY_EMAIL] = email
        }
    }

    fun getFullname(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_FULLNAME] ?: "Farmer"
        }
    }

    fun getUserId(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_USER_ID] ?: ""
        }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}
