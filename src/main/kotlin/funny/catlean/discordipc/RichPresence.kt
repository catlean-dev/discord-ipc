package funny.catlean.discordipc

import funny.catlean.discordipc.drafts.DraftService
import kotlinx.serialization.Serializable

import kotlin.properties.Delegates

@Serializable
data class ActivityButton(val label: String, val url: String)

@Serializable
data class ActivityAsset(
    val largeImage: String?,
    val largeText: String?,
    val smallImage: String?,
    val smallText: String?,
)

@Serializable
data class ActivityTimestamp(val start: Long, val end: Long? = null)

@Serializable
data class Activity(
    val details: String?,
    val state: String?,
    val timestamps: ActivityTimestamp,
    val assets: ActivityAsset?,
    val buttons: List<ActivityButton>?,
)

object RichPresence : DiscordIPC() {
    var appId: Long? by watchDog(true)

    var details: String? by watchDog()
    var state: String? by watchDog()

    var largeImage: Pair<String, String>? by watchDog()
    var smallImage: Pair<String, String>? by watchDog()

    var button1: Pair<String, String>? by watchDog()
    var button2: Pair<String, String>? by watchDog()

    internal val activity: Activity
        get() {
            val buttonsList = mutableListOf<ActivityButton>()
            button1?.let { buttonsList.add(ActivityButton(it.first, it.second)) }
            button2?.let { buttonsList.add(ActivityButton(it.first, it.second)) }

            return Activity(
                details = details,
                state = state,
                buttons = buttonsList,
                assets = if (largeImage != null || smallImage != null)
                    ActivityAsset(
                        largeImage = largeImage?.first,
                        largeText = largeImage?.second,
                        smallImage = smallImage?.first,
                        smallText = smallImage?.second,
                    ) else null,
                timestamps = ActivityTimestamp(start = initTime),
            )
        }

    private fun <T> watchDog(start: Boolean = false, initial: T? = null) =
        Delegates.observable(initial) { _, old, new ->
            if (old != new) {
                if (start) start(new as Long)
                else setActivity(activity)
            }
        }

    @Suppress("unused")
    fun startRolling() {
        DraftService.currentId = 0
        DraftService.autoRoll = true
    }

    @Suppress("unused")
    fun stopRolling() {
        DraftService.autoRoll = false
    }
}
