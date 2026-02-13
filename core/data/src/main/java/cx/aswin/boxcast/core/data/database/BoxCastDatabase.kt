package cx.aswin.boxcast.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import cx.aswin.boxcast.core.data.database.entities.QueueItem
import cx.aswin.boxcast.core.data.database.dao.QueueDao

@Database(
    entities = [ListeningHistoryEntity::class, PodcastEntity::class, DownloadedEpisodeEntity::class, QueueItem::class],
    version = 11,
    exportSchema = false
)
abstract class BoxCastDatabase : RoomDatabase() {
    abstract fun listeningHistoryDao(): ListeningHistoryDao
    abstract fun podcastDao(): PodcastDao
    abstract fun downloadedEpisodeDao(): DownloadedEpisodeDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile
        private var INSTANCE: BoxCastDatabase? = null

        fun getDatabase(context: android.content.Context): BoxCastDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    BoxCastDatabase::class.java,
                    "boxcast_database"
                )
                .fallbackToDestructiveMigration() // For development simplicity
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
