package com.crownos.connect.protocol

enum class Feature { Mirror, Camera, Mic, Monitor, Unicursor, Calls, Clipboard, Notifications, Battery, Hotspot, Files }

enum class DeviceClass { Phone, Tablet, Watch, Computer }

enum class Topic { DeviceInfo, Battery, MediaSession, CallState, Notifications, Clipboard, Volume, Hotspot }

enum class Direction { ToPeer, FromPeer }

enum class Edge {
    Left, Right, Top, Bottom;

    val facing: Edge
        get() = when (this) {
            Left -> Right
            Right -> Left
            Top -> Bottom
            Bottom -> Top
        }
}

enum class CameraLens { Back, Front }

enum class VideoCodec(val mime: String) {
    Hevc("video/hevc"),
    H264("video/avc"),
    Av1("video/av01"),
}

enum class FeatureState { Disabled, Enabled, Active }

enum class MediaRole { Encoder, Decoder }

inline fun <reified E : Enum<E>> enumByName(name: String?): E? = enumValues<E>().firstOrNull { it.name == name }
