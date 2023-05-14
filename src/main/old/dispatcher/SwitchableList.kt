package ru.dargen.nollen.dispatcher

import java.util.*

open class SwitchableList<E>(collection: Collection<E>) : LinkedList<E>(collection) {
    constructor() : this(emptyList())

    open fun switch(): E = synchronized(this) {
        removeFirst().apply { addLast(this) }
    }

}