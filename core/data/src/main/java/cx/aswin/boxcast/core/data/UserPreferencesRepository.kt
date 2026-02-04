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
}
