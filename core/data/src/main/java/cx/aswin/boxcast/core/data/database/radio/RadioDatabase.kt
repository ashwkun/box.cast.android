package cx.aswin.boxcast.core.data.database.radio

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FollowedStationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun followedStationDao(): FollowedStationDao

    companion object {
        @Volatile
        private var INSTANCE: RadioDatabase? = null

        fun getDatabase(context: android.content.Context): RadioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    RadioDatabase::class.java,
                    "boxcast_radio_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
