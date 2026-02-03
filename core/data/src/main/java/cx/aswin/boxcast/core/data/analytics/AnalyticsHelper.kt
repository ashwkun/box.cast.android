package cx.aswin.boxcast.core.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import cx.aswin.boxcast.core.data.privacy.ConsentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnalyticsHelper(
    private val context: Context,
    private val consentManager: ConsentManager
) {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Cached state for synchronous checks (default false for safety)
    private var isUsageConsented = false

    init {
        // Observe Usage Analytics Consent
        scope.launch {
            consentManager.isUsageAnalyticsConsented.collectLatest { consented ->
                isUsageConsented = consented
                firebaseAnalytics.setAnalyticsCollectionEnabled(consented)
            }
        }
        
        // Observe Crash Reporting Consent (managed by Firebase Analytics instance? No, separate)
        // Crashlytics usually auto-inits, we need to disable it in Manifest or code.
        // Assuming we rely on manifest meta-data for default disabling, and enable here.
        // For now, we focus on Analytics logic here. Crashlytics handling can be added if we inject it or use static instance.
    }

    fun logEvent(eventName: String, params: Map<String, String>) {
        if (!isUsageConsented) return // Strict privacy guard

        val bundle = Bundle()
        params.forEach { (key, value) ->
            bundle.putString(key, value)
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    fun logPlayEpisode(podcastTitle: String, episodeTitle: String) {
        logEvent("play_episode", mapOf(
            "podcast_title" to podcastTitle,
            "episode_title" to episodeTitle
        ))
    }

    fun logSubscribe(podcastTitle: String) {
        logEvent("subscribe_podcast", mapOf(
            "podcast_title" to podcastTitle
        ))
    }
    
    fun logUnsubscribe(podcastTitle: String) {
        logEvent("unsubscribe_podcast", mapOf(
            "podcast_title" to podcastTitle
        ))
    }

    fun logSearch(query: String) {
        logEvent(FirebaseAnalytics.Event.SEARCH, mapOf(
            FirebaseAnalytics.Param.SEARCH_TERM to query
        ))
    }

    // New "Deep" Metrics as per plan
    
    fun logScreenView(screenName: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName,
            FirebaseAnalytics.Param.SCREEN_CLASS to screenName
        ))
    }
    
    fun logPlaybackState(state: String, podcastId: String?, episodeId: String?) {
        logEvent("playback_state", mapOf(
            "state" to state,
            "podcast_id" to (podcastId ?: "unknown"),
            "episode_id" to (episodeId ?: "unknown")
        ))
    }
}
