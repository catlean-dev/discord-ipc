package funny.catlean.discordipc.drafts

import funny.catlean.discordipc.RichPresence
import funny.catlean.discordipc.drafts.DraftService.drafts

fun RichPresence.saveDraft(block: Draft.() -> Unit) =
    drafts.add(Draft().apply(block))

fun RichPresence.setDraft(draftId: Any) {
    drafts.firstOrNull { it.draftId == draftId }?.let {
        RichPresence.apply {
            details = it.details
            state = it.state
            largeImage = it.largeImage
            smallImage = it.smallImage
            button1 = it.button1
            button2 = it.button2
        }
    }?: println("Discord IPC - Draft with id $draftId not found")
}

object DraftService {
    val drafts = mutableListOf<Draft>()
    var autoRoll = false
    var currentId = 0

    fun handleRoll() {
        if (autoRoll) {
            RichPresence.setDraft(drafts[currentId].draftId)
            currentId = (currentId + 1) % drafts.size
        }
    }
}
