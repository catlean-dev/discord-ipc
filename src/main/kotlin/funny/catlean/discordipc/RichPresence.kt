package funny.catlean.discordipc

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import funny.catlean.discordipc.drafts.DraftService

import kotlin.properties.Delegates

object RichPresence : DiscordIPC() {
    var appId: Long? by watchDog(true)

    var details: String? by watchDog()
    var state: String? by watchDog()

    var largeImage: Pair<String, String>? by watchDog()
    var smallImage: Pair<String, String>? by watchDog()

    var button1: Pair<String, String>? by watchDog()
    var button2: Pair<String, String>? by watchDog()

    private fun toJson(): JsonObject {
        val main = JsonObject()

        details?.let { main.addProperty("details", it) }
        state?.let { main.addProperty("state", it) }

        main.add("timestamps", JsonObject().apply {
            addProperty("start", initTime)
        })

        if (largeImage != null || smallImage != null) {
            main.add("assets", JsonObject().apply {
                largeImage?.let { (k, v) ->
                    addProperty("large_image", k)
                    addProperty("large_text", v)
                }

                smallImage?.let { (k, v) ->
                    addProperty("small_image", k)
                    addProperty("small_text", v)
                }
            })
        }

        if (button1 != null || button2 != null) {
            main.add("buttons", JsonArray().apply {
                button1?.let { (k, v) ->
                    add(JsonObject().apply {
                        addProperty("label", k)
                        addProperty("url", v)
                    })
                }

                button2?.let { (k, v) ->
                    add(JsonObject().apply {
                        addProperty("label", k)
                        addProperty("url", v)
                    })
                }
            })
        }

        return main
    }

    private fun <T> watchDog(start: Boolean = false, initial: T? = null) =
        Delegates.observable(initial) { _, old, new ->
            if (old != new) {
                if (start)
                    start(new as Long)
                else
                    setActivity(toJson())
            }
        }

    fun startRolling() {
        DraftService.currentId = 0
        DraftService.autoRoll = true
    }

    fun stopRolling() {
        DraftService.autoRoll = false
    }
}
