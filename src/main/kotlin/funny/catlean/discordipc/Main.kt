package funny.catlean.discordipc

import java.lang.Thread.sleep

// TODO example proj
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Starting Discord IPC")

        RichPresence.apply {
            appId = 1093053626198523935L

            details = "Details"
            state = "State"
            largeImage = "large" to "LARGE TEXT"
            smallImage = "small" to "small text"
            button1 = "button 1" to "https://example.ru/"
            button2 = "button 2" to "https://example.ru/"
        }

        sleep(10000)

        println("Stopping Discord IPC")
        RichPresence.stop()
    }
}
