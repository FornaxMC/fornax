package dev.luna5ama.fornax.util

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow

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

suspend fun <T> Flow<T>.sendTo(channel: SendChannel<T>) {
    collect {
        channel.send(it)
    }
}