package funny.catlean.discordipc

data class IPCUser(var id: String = "none", var username: String = "none", var avatar: String = "none") {
    fun avatarLink(size: Int = 128, forcePng: Boolean = false): String = when {
        avatar.startsWith("a_") && !forcePng -> "https://cdn.discordapp.com/avatars/$id/$avatar.gif?size=$size"
        else -> "https://cdn.discordapp.com/avatars/$id/$avatar.png?size=$size"
    }
}
