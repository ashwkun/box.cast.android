package cx.aswin.boxcast.core.data.analytics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.SystemClock
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
    
    // Timestamp when app was first opened (for time-to-first-play calculation)
    private val appOpenTimeElapsed = SystemClock.elapsedRealtime()

    // Milestone tracking for episode progress (scrub-safe)
    private var currentEpisodeMilestones = mutableSetOf<Int>()
    private var currentTrackingEpisodeKey: String? = null
    
    // Session tracking for listening_session_summary
    private var sessionStartElapsed: Long? = null

    init {
        scope.launch {
            consentManager.isUsageAnalyticsConsented.collectLatest { consented ->
                isUsageConsented = consented
            }
        }
    }

    // ── Core Logging ──────────────────────────────────────────
    
    private fun logEvent(eventName: String, params: Map<String, String>) {
        if (!isUsageConsented) return
        val bundle = Bundle()
        params.forEach { (key, value) -> bundle.putString(key, value) }
        firebaseAnalytics.logEvent(eventName, bundle)
    }
    
    /** Fires regardless of consent — used only for onboarding funnel tracking. */
    private fun logUngatedEvent(eventName: String, params: Map<String, String>) {
        val bundle = Bundle()
        params.forEach { (key, value) -> bundle.putString(key, value) }
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    // ── KPI-1: Activation ─────────────────────────────────────

    /** E1: First episode ever played. Call once per install (gate with DataStore). */
    fun logFirstEpisodePlayed(source: String) {
        val elapsedSinceOpen = SystemClock.elapsedRealtime() - appOpenTimeElapsed
        val bucket = when {
            elapsedSinceOpen < 60_000 -> "under_1m"
            elapsedSinceOpen < 300_000 -> "1_5m"
            else -> "over_5m"
        }
        logEvent("first_episode_played", mapOf(
            "source" to source,
            "time_to_first_play_bucket" to bucket
        ))
    }
    
    /** E14: Onboarding consent flow tracking. Bypasses consent gate. */
    fun logOnboardingStep(step: String, additionalParams: Map<String, String>? = null) {
        val params = mutableMapOf("step" to step)
        additionalParams?.let { params.putAll(it) }
        logUngatedEvent("onboarding_step", params)
    }

    // ── KPI-2: Engagement ─────────────────────────────────────

    /** E2: Episode playback started. */
    fun logEpisodeStarted(source: String, isDownloaded: Boolean) {
        logEvent("episode_started", mapOf(
            "source" to source,
            "is_downloaded" to isDownloaded.toString()
        ))
    }

    /** 
     * E3: Episode progress milestone reached. Scrub-safe.
     * Call from PlaybackRepository on progress tick. 
     * Only fires if the milestone is crossed during normal playback (not seek).
     */
    fun logEpisodeProgress(episodeKey: String, currentPercent: Int) {
        // Reset tracking on episode change
        if (episodeKey != currentTrackingEpisodeKey) {
            currentTrackingEpisodeKey = episodeKey
            currentEpisodeMilestones.clear()
        }
        
        val milestones = listOf(25, 50, 75, 100)
        for (milestone in milestones) {
            if (currentPercent >= milestone && milestone !in currentEpisodeMilestones) {
                currentEpisodeMilestones.add(milestone)
                logEvent("episode_progress", mapOf("milestone" to milestone.toString()))
            }
        }
    }
    
    /** 
     * Mark milestones as passed without firing events (on seek).
     * Call when user seeks forward past milestones.
     */
    fun markMilestonesPassedOnSeek(episodeKey: String, seekToPercent: Int) {
        if (episodeKey != currentTrackingEpisodeKey) {
            currentTrackingEpisodeKey = episodeKey
            currentEpisodeMilestones.clear()
        }
        val milestones = listOf(25, 50, 75, 100)
        for (milestone in milestones) {
            if (seekToPercent >= milestone) {
                currentEpisodeMilestones.add(milestone) // Mark as passed, don't fire
            }
        }
    }

    // ── KPI-3: Session Quality ────────────────────────────────

    /** Call when playback starts to begin session timing. */
    fun startListeningSession() {
        if (sessionStartElapsed == null) {
            sessionStartElapsed = SystemClock.elapsedRealtime()
        }
    }

    /** E4: Listening session summary. Call when playback stops. */
    fun endListeningSession() {
        val start = sessionStartElapsed ?: return
        sessionStartElapsed = null
        
        val durationMs = SystemClock.elapsedRealtime() - start
        val bucket = when {
            durationMs < 60_000 -> "under_1m"
            durationMs < 300_000 -> "1_5m"
            durationMs < 900_000 -> "5_15m"
            durationMs < 1_800_000 -> "15_30m"
            durationMs < 3_600_000 -> "30_60m"
            else -> "over_60m"
        }
        logEvent("listening_session_summary", mapOf("duration_bucket" to bucket))
    }

    // ── KPI-4: Subscriptions ──────────────────────────────────

    /** E5: Subscribe or unsubscribe action. */
    fun logSubscribeAction(isSubscribe: Boolean) {
        logEvent("subscribe_action", mapOf(
            "action" to if (isSubscribe) "subscribe" else "unsubscribe"
        ))
    }

    // ── KPI-5: Feature Adoption ───────────────────────────────

    /** E6: Power feature used. */
    fun logFeatureUsed(feature: String) {
        logEvent("feature_used", mapOf("feature" to feature))
    }

    // ── Discovery ─────────────────────────────────────────────

    /** E7: Search performed. No query text logged. */
    fun logSearchPerformed(hasResults: Boolean) {
        logEvent(FirebaseAnalytics.Event.SEARCH, mapOf(
            "has_results" to hasResults.toString()
        ))
    }

    /** E8: Explore vibe/category selected. */
    fun logExploreVibeSelected(vibeCategory: String) {
        logEvent("explore_vibe_selected", mapOf("vibe_category" to vibeCategory))
    }

    /** E9: Hero card tapped on home screen. */
    fun logHeroCardTapped(cardType: String, cardPosition: Int) {
        logEvent("hero_card_tapped", mapOf(
            "card_type" to cardType,
            "card_position" to cardPosition.toString()
        ))
    }

    /** E10: Screen view. */
    fun logScreenView(screenName: String) {
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, mapOf(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName,
            FirebaseAnalytics.Param.SCREEN_CLASS to screenName
        ))
    }

    // ── Reliability ───────────────────────────────────────────

    /** E11: Playback error with network context. */
    fun logPlaybackError(errorType: String) {
        logEvent("playback_error", mapOf(
            "error_type" to errorType,
            "network_state" to getNetworkState()
        ))
    }
    
    private fun getNetworkState(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "offline"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "unknown"
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                else -> "unknown"
            }
        } catch (_: Exception) { "unknown" }
    }

    // ── Retention ─────────────────────────────────────────────

    /** E12: In-app feedback interaction. */
    fun logAppFeedback(action: String) {
        logEvent("app_feedback", mapOf("action" to action))
    }

    /** E13: Notification opened or dismissed. */
    fun logNotificationInteraction(action: String) {
        logEvent("notification_interaction", mapOf("action" to action))
    }
}
