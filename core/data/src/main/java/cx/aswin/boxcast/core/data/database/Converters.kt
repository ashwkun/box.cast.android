package cx.aswin.boxcast.core.data.database

import androidx.room.TypeConverter
import cx.aswin.boxcast.core.model.Episode
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromEpisode(episode: Episode?): String? {
        return episode?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toEpisode(episodeString: String?): Episode? {
        return episodeString?.let {
            try {
                gson.fromJson(it, Episode::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
