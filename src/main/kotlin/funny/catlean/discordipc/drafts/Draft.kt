package funny.catlean.discordipc.drafts

class Draft(
    var draftId: Any = 0,
    var details: String? = null,
    var state: String? = null,
    var largeImage: Pair<String, String>? = null,
    var smallImage: Pair<String, String>? = null,
    var button1: Pair<String, String>? = null,
    var button2: Pair<String, String>? = null
)