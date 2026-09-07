package cx.aswin.boxlore.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadedEpisodeMigration35To36Test {

    @Test
    fun migrate35To36AddsChaptersAndTranscriptColumnsPreservingExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config =
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(null)
                .callback(
                    object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(35) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS downloaded_episodes (
                                    episodeId TEXT NOT NULL PRIMARY KEY,
                                    podcastId TEXT NOT NULL,
                                    episodeTitle TEXT NOT NULL,
                                    episodeDescription TEXT,
                                    episodeImageUrl TEXT,
                                    podcastName TEXT NOT NULL,
                                    podcastImageUrl TEXT,
                                    durationMs INTEGER NOT NULL,
                                    publishedDate INTEGER NOT NULL,
                                    localFilePath TEXT NOT NULL,
                                    downloadId INTEGER NOT NULL,
                                    downloadedAt INTEGER NOT NULL,
                                    sizeBytes INTEGER NOT NULL,
                                    status INTEGER NOT NULL DEFAULT 0,
                                    isSmartDownloaded INTEGER NOT NULL DEFAULT 0
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    },
                ).build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = openHelper.writableDatabase

        // 1. Insert existing download in version 35
        db.execSQL(
            """
            INSERT INTO downloaded_episodes (
                episodeId, podcastId, episodeTitle, episodeDescription, episodeImageUrl,
                podcastName, podcastImageUrl, durationMs, publishedDate, localFilePath,
                downloadId, downloadedAt, sizeBytes, status, isSmartDownloaded
            ) VALUES (
                'ep-v35-1', 'pod-1', 'Existing Episode', 'Existing description', 'https://example.com/ep.png',
                'Tech Talk', 'https://example.com/pod.png', 1800000, 1700000000, '/local/ep1.mp3',
                101, 1700001000, 50000000, 2, 0
            )
            """.trimIndent(),
        )

        // 2. Run migration to 36
        BoxLoreDatabaseMigrations.migrate35To36(db)

        // 3. Verify existing row is preserved and has null for new columns
        val cursor = db.query(
            "SELECT episodeId, episodeTitle, localFilePath, chaptersUrl, transcriptUrl FROM downloaded_episodes WHERE episodeId = 'ep-v35-1'",
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("ep-v35-1", cursor.getString(0))
        assertEquals("Existing Episode", cursor.getString(1))
        assertEquals("/local/ep1.mp3", cursor.getString(2))
        assertNull(cursor.getString(3))
        assertNull(cursor.getString(4))
        cursor.close()

        // 4. Insert new download in version 36 with chaptersUrl and transcriptUrl
        db.execSQL(
            """
            INSERT INTO downloaded_episodes (
                episodeId, podcastId, episodeTitle, episodeDescription, episodeImageUrl,
                podcastName, podcastImageUrl, durationMs, publishedDate, localFilePath,
                downloadId, downloadedAt, sizeBytes, status, isSmartDownloaded,
                chaptersUrl, transcriptUrl
            ) VALUES (
                'ep-v36-2', 'pod-1', 'New Offline Episode', 'Description', 'https://example.com/ep2.png',
                'Tech Talk', 'https://example.com/pod.png', 2400000, 1700005000, '/local/ep2.mp3',
                102, 1700006000, 60000000, 2, 1,
                '/data/chapters/ep2.json', '/data/transcripts/ep2.txt'
            )
            """.trimIndent(),
        )

        val newCursor = db.query(
            "SELECT episodeId, chaptersUrl, transcriptUrl FROM downloaded_episodes WHERE episodeId = 'ep-v36-2'",
        )
        assertTrue(newCursor.moveToFirst())
        assertEquals("ep-v36-2", newCursor.getString(0))
        assertEquals("/data/chapters/ep2.json", newCursor.getString(1))
        assertEquals("/data/transcripts/ep2.txt", newCursor.getString(2))
        newCursor.close()

        // 5. Test idempotence: running migrate35To36 again should not fail
        BoxLoreDatabaseMigrations.migrate35To36(db)

        openHelper.close()
    }

    @Test
    fun migrateFrom33To36ChainsSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config =
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(null)
                .callback(
                    object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(33) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS downloaded_episodes (
                                    episodeId TEXT NOT NULL PRIMARY KEY,
                                    podcastId TEXT NOT NULL,
                                    episodeTitle TEXT NOT NULL,
                                    episodeDescription TEXT,
                                    episodeImageUrl TEXT,
                                    podcastName TEXT NOT NULL,
                                    podcastImageUrl TEXT,
                                    durationMs INTEGER NOT NULL,
                                    publishedDate INTEGER NOT NULL,
                                    localFilePath TEXT NOT NULL,
                                    downloadId INTEGER NOT NULL,
                                    downloadedAt INTEGER NOT NULL,
                                    sizeBytes INTEGER NOT NULL,
                                    status INTEGER NOT NULL DEFAULT 0,
                                    isSmartDownloaded INTEGER NOT NULL DEFAULT 0
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                    },
                ).build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = openHelper.writableDatabase

        // Execute migrations from 33 to 36 sequentially
        BoxLoreDatabaseMigrations.migrate33To34(db)
        BoxLoreDatabaseMigrations.migrate34To35(db)
        BoxLoreDatabaseMigrations.migrate35To36(db)

        // Verify folders table was created and has showPodcastGrid
        val folderInfo = db.query("PRAGMA table_info(folders)")
        var foundGridColumn = false
        while (folderInfo.moveToNext()) {
            val nameIndex = folderInfo.getColumnIndex("name")
            if (nameIndex != -1 && folderInfo.getString(nameIndex) == "showPodcastGrid") {
                foundGridColumn = true
                break
            }
        }
        folderInfo.close()
        assertTrue(foundGridColumn)

        // Verify downloaded_episodes table has chaptersUrl and transcriptUrl
        val downloadInfo = db.query("PRAGMA table_info(downloaded_episodes)")
        var foundChapters = false
        var foundTranscript = false
        while (downloadInfo.moveToNext()) {
            val nameIndex = downloadInfo.getColumnIndex("name")
            if (nameIndex != -1) {
                val col = downloadInfo.getString(nameIndex)
                if (col == "chaptersUrl") foundChapters = true
                if (col == "transcriptUrl") foundTranscript = true
            }
        }
        downloadInfo.close()
        assertTrue(foundChapters)
        assertTrue(foundTranscript)

        openHelper.close()
    }
}
