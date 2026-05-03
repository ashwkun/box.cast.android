package cx.aswin.boxcast.core.designsystem.components

import java.net.URLEncoder

/**
 * Optimizes an image URL by passing it through a resizing CDN (wsrv.nl).
 * This significantly improves loading times for lists and grids by preventing
 * the app from downloading 5MB uncompressed podcast cover arts.
 * 
 * @param width The desired maximum width in pixels.
 * @return The optimized URL, or the original if it's not an HTTP/HTTPS URL.
 */
fun String.optimizedImageUrl(width: Int = 400): String {
    if (this.isBlank() || (!this.startsWith("http://") && !this.startsWith("https://"))) {
        return this
    }
    
    // Some podcast servers block wsrv.nl, but for most standard URLs it works perfectly.
    // If we want to be safe, we can try-catch the encoding, though URLEncoder is standard.
    return try {
        val encodedUrl = URLEncoder.encode(this, "UTF-8")
        "https://wsrv.nl/?url=$encodedUrl&w=$width&output=webp"
    } catch (e: Exception) {
        this
    }
}
