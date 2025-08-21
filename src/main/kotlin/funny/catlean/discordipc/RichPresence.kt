package funny.catlean.discordipc

import com.google.gson.JsonArray
import com.google.gson.JsonObject

import java.time.Instant
import kotlin.properties.Delegates

object RichPresence : DiscordIPC() {
    var appId: Long? by watchDog(true)

    var details: String? by watchDog()
    var state: String? by watchDog()

    var largeImage: Pair<String, String>? by watchDog()
    var smallImage: Pair<String, String>? by watchDog()

    var button1: Pair<String, String>? by watchDog()
    var button2: Pair<String, String>? by watchDog()

    fun toJson(): JsonObject {
        val main = JsonObject()

        details?.let { main.addProperty("details", it) }
        state?.let { main.addProperty("state", it) }

        val timeStamps = JsonObject()
        timeStamps.addProperty("start", Instant.now().epochSecond)
        main.add("timestamps", timeStamps)

        if (largeImage != null || smallImage != null) {
            val assets = JsonObject()

            largeImage?.let { (k, v) ->
                assets.addProperty("large_image", k)
                assets.addProperty("large_text", v)
            }

            smallImage?.let { (k, v) ->
                assets.addProperty("small_image", k)
                assets.addProperty("small_text", v)
            }

            main.add("assets", assets)
        }

        if (button1 != null || button2 != null) {
            val buttons = JsonArray()

            button1?.let { (k, v) ->
                val btn = JsonObject()
                btn.addProperty("label", k)
                btn.addProperty("url", v)
                buttons.add(btn)
            }

            button2?.let { (k, v) ->
                val btn = JsonObject()
                btn.addProperty("label", k)
                btn.addProperty("url", v)
                buttons.add(btn)
            }

            main.add("buttons", buttons)
        }

        return main
    }

    fun <T> watchDog(start: Boolean = false, initial: T? = null) =
        Delegates.observable(initial) { _, old, new ->
            if (old != new) {
                if (start)
                    start(new as Long)
                else
                    setActivity(this)
            }
        }
}
