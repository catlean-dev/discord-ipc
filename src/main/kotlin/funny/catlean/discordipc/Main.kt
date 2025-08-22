package funny.catlean.discordipc

import funny.catlean.discordipc.drafts.saveDraft
import funny.catlean.discordipc.drafts.setDraft
import java.lang.Thread.sleep

// TODO example proj
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Starting Discord IPC")

        RichPresence.apply {
            appId = 1093053626198523935L

            details = "Init Details"
            state = "Init State"
        }

        sleep(6000)

        State.entries.forEach {
            RichPresence.saveDraft {
                draftId = it

                details = "Draft ${it.details}"
                state = "Draft ${it.state}"
            }
        }

        RichPresence.startRolling()
        sleep(60000)
        RichPresence.stopRolling()

        println("Stopping Discord IPC")
        RichPresence.stop()
    }

    enum class State(val details: String, val state: String) {
        Hello("hello", "world!"),
        Kowk("Mew", "Meow")
    }
}
