package cx.aswin.boxcast.core.data.privacy

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_consent")

class ConsentManager(private val context: Context) {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val CRASH_REPORTING_CONSENT = booleanPreferencesKey("crash_reporting_consent")
        val USAGE_ANALYTICS_CONSENT = booleanPreferencesKey("usage_analytics_consent")
        val CONSENT_DECIDED = booleanPreferencesKey("consent_decided") // True if user has made a choice (even if turned all off)
    }

    // NULL implies "Not yet decided"
    val isCrashReportingConsented: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // Default to FALSE (Opt-in)
            preferences[PreferencesKeys.CRASH_REPORTING_CONSENT] ?: false
        }

    val isUsageAnalyticsConsented: Flow<Boolean> = dataStore.data
        .catch { exception ->
             if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // Default to FALSE (Opt-in)
            preferences[PreferencesKeys.USAGE_ANALYTICS_CONSENT] ?: false
        }
        
    // Has the user seen the dialog and made a choice?
    val hasUserSetConsent: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CONSENT_DECIDED] ?: false
        }

    suspend fun setConsent(crashReporting: Boolean, usageAnalytics: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CRASH_REPORTING_CONSENT] = crashReporting
            preferences[PreferencesKeys.USAGE_ANALYTICS_CONSENT] = usageAnalytics
            preferences[PreferencesKeys.CONSENT_DECIDED] = true
        }
    }
}
