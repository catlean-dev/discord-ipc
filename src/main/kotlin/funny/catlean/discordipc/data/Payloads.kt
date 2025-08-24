package funny.catlean.discordipc.data

import funny.catlean.discordipc.Activity
import kotlinx.serialization.Serializable

@Serializable
data class HandshakePayload(val v: Int, val clientId: String)

@Serializable
data class FrameArgs(
    val pid: Int,
    val activity: Activity,
)

@Serializable
data class FramePayload(
    val cmd: Command,
    val args: FrameArgs,
)

@Serializable
@Suppress("unused")
enum class PacketEvt { ERROR, READY }

@Serializable
data class PacketData(val user: IPCUser?, val code: Int?, val message: String?)

@Serializable
data class PacketPayload(val evt: PacketEvt?, val cmd: Command?, val data: PacketData)