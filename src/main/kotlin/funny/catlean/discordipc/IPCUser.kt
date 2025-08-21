package funny.catlean.discordipc

data class IPCUser(var id: String = "none", var username: String = "none", var avatar: String = "none") {
    val avatarLink : String
        get() = "https://cdn.discordapp.com/avatars/$id/$avatar.png"
}
