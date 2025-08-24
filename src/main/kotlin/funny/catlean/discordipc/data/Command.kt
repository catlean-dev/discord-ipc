package funny.catlean.discordipc.data

import kotlinx.serialization.Serializable

@Serializable
enum class Command {
    DISPATCH, SET_ACTIVITY
}