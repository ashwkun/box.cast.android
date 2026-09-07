package cx.aswin.boxlore.feature.player.v2

import androidx.compose.runtime.saveable.SaverScope
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FullPlayerUiStateSaverTest {
    @Test
    @Suppress("UNCHECKED_CAST")
    fun `saver preserves showRemoveDownloadDialog state`() {
        val original = FullPlayerUiState().apply {
            showRemoveDownloadDialog = true
        }

        val scope = SaverScope { true }
        val saved = with(FullPlayerUiStateSaver) { scope.save(original) } as List<Boolean>
        val restored = FullPlayerUiStateSaver.restore(saved)

        assertTrue(restored != null)
        assertTrue(restored!!.showRemoveDownloadDialog)
    }

    @Test
    fun `restores safely from legacy saved state without out of bounds`() {
        // Prior versions had only 10 elements
        val legacyList = List(10) { false }
        val restored = FullPlayerUiStateSaver.restore(legacyList)

        assertTrue(restored != null)
        assertFalse(restored!!.showRemoveDownloadDialog)
    }
}
