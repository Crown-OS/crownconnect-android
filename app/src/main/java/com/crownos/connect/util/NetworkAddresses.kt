package com.crownos.connect.util

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

fun InetAddress.isRoutableForPeers(): Boolean =
    !isLoopbackAddress && !isLinkLocalAddress && !isAnyLocalAddress && !isMulticastAddress

fun InetAddress.toSocketAddressString(port: Int): String = when (this) {
    is Inet6Address -> "[${hostAddress?.substringBefore('%')}]:$port"
    else -> "$hostAddress:$port"
}

fun InetAddress.plainHost(): String? = when (this) {
    is Inet4Address -> hostAddress
    is Inet6Address -> hostAddress?.substringBefore('%')
    else -> hostAddress
}
