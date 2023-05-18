package ru.dargen.nollen.util

import java.time.Duration
import java.time.Instant

class Timeout(val duration: Duration) {

    var timestamp: Instant = Instant.MIN

    fun isTimedOut() = Duration.between(timestamp, Instant.now()) > duration

    fun set() {
        timestamp = Instant.now()
    }

}