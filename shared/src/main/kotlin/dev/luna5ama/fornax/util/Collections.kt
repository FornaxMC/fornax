package dev.luna5ama.fornax.util

import kotlinx.coroutines.channels.SendChannel

suspend fun <T> Iterable<T>.sendTo(channel: SendChannel<T>) {
    for (element in this) {
        channel.send(element)
    }
}

suspend fun <T> Sequence<T>.sendTo(channel: SendChannel<T>) {
    for (element in this) {
        channel.send(element)
    }
}