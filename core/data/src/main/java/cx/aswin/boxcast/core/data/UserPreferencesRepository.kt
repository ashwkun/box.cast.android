package cx.aswin.boxcast.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(context: Context) {
    private val dataStore = context.userPreferencesDataStore

    private object Keys {
        val REGION = stringPreferencesKey("region")
        val THEME_CONFIG = stringPreferencesKey("theme_config")
        val USE_DYNAMIC_COLOR = androidx.datastore.preferences.core.booleanPreferencesKey("use_dynamic_color")
        val THEME_BRAND = stringPreferencesKey("theme_brand")
    }

    val regionStream: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[Keys.REGION] ?: "us"
        }

    suspend fun setRegion(region: String) {
        dataStore.edit { preferences ->
            preferences[Keys.REGION] = region
        }
    }

    // THEME PREFERENCES
    val themeConfigStream: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[Keys.THEME_CONFIG] ?: "system"
        }

    suspend fun setThemeConfig(themeConfig: String) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_CONFIG] = themeConfig
        }
    }

    val useDynamicColorStream: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[Keys.USE_DYNAMIC_COLOR] ?: true
        }

    suspend fun setUseDynamicColor(useDynamicColor: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.USE_DYNAMIC_COLOR] = useDynamicColor
        }
    }

    val themeBrandStream: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[Keys.THEME_BRAND] ?: "violet"
        }

    suspend fun setThemeBrand(themeBrand: String) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_BRAND] = themeBrand
        }
    }

    // TOOLTIP PREFERENCES (one-time tips)
    private object TooltipKeys {
        val HAS_SEEN_SWIPE_DISMISS_TIP = androidx.datastore.preferences.core.booleanPreferencesKey("has_seen_swipe_dismiss_tip")
        val HAS_SEEN_TITLE_TAP_TIP = androidx.datastore.preferences.core.booleanPreferencesKey("has_seen_title_tap_tip")
        val HAS_SEEN_SWIPE_MINIMIZE_TIP = androidx.datastore.preferences.core.booleanPreferencesKey("has_seen_swipe_minimize_tip")
        val HAS_SEEN_MARK_PLAYED_TIP = androidx.datastore.preferences.core.booleanPreferencesKey("has_seen_mark_played_tip")
    }

    val hasSeenSwipeDismissTip: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TooltipKeys.HAS_SEEN_SWIPE_DISMISS_TIP] ?: false }

    val hasSeenTitleTapTip: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TooltipKeys.HAS_SEEN_TITLE_TAP_TIP] ?: false }

    val hasSeenSwipeMinimizeTip: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TooltipKeys.HAS_SEEN_SWIPE_MINIMIZE_TIP] ?: false }

    suspend fun markSwipeDismissTipSeen() {
        dataStore.edit { it[TooltipKeys.HAS_SEEN_SWIPE_DISMISS_TIP] = true }
    }

    suspend fun markTitleTapTipSeen() {
        dataStore.edit { it[TooltipKeys.HAS_SEEN_TITLE_TAP_TIP] = true }
    }

    suspend fun markSwipeMinimizeTipSeen() {
        dataStore.edit { it[TooltipKeys.HAS_SEEN_SWIPE_MINIMIZE_TIP] = true }
    }

    val hasSeenMarkPlayedTip: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TooltipKeys.HAS_SEEN_MARK_PLAYED_TIP] ?: false }

    suspend fun markMarkPlayedTipSeen() {
        dataStore.edit { it[TooltipKeys.HAS_SEEN_MARK_PLAYED_TIP] = true }
    }

    // ANALYTICS KEYS
    private object AnalyticsKeys {
        val HAS_LOGGED_FIRST_PLAY = androidx.datastore.preferences.core.booleanPreferencesKey("has_logged_first_play")
    }

    val hasLoggedFirstPlay: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AnalyticsKeys.HAS_LOGGED_FIRST_PLAY] ?: false }

    suspend fun markFirstPlayLogged() {
        dataStore.edit { it[AnalyticsKeys.HAS_LOGGED_FIRST_PLAY] = true }
    }
}
