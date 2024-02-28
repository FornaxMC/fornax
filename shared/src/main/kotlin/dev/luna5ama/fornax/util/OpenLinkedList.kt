package dev.luna5ama.fornax.util

import kotlin.reflect.KProperty1

class OpenLinkedList<E>(private val ptrFunc: KProperty1<E, NodePointer<E>>) {
    var head: NodePointer<E>? = null
    var tail: NodePointer<E>? = null
    var size = 0; private set

    fun pushFront(container: E) {
        assert(size >= 0)

        val ptr = ptrFunc.get(container)

        if (head == null) {
            tail = ptr
        } else {
            head!!.prev = ptr
            ptr.next = head
        }

        head = ptr
        size++

        assert(size >= 0)
    }

    fun pushBack(container: E) {
        assert(size >= 0)

        val ptr = ptrFunc.get(container)

        if (tail == null) {
            head = ptr
        } else {
            tail!!.next = ptr
            ptr.prev = tail
        }

        tail = ptr
        size++

        assert(size >= 0)
    }

    fun popFront(): E? {
        assert(size >= 0)

        val ptr = head ?: return null

        val next = ptr.next
        if (next == null) {
            head = null
            tail = null
        } else {
            next.prev = null
            head = next
        }

        ptr.next = null
        size--

        assert(size >= 0)

        return ptr.container
    }

    fun popBack(): E? {
        assert(size >= 0)

        val ptr = tail ?: return null

        val prev = ptr.prev
        if (prev == null) {
            head = null
            tail = null
        } else {
            prev.next = null
            tail = prev
        }

        ptr.prev = null
        size--

        assert(size >= 0)

        return ptr.container
    }

    fun remove(container: E) {
        assert(size >= 0)

        val ptr = ptrFunc.get(container)

        val prev = ptr.prev
        val next = ptr.next

        val isHead = head == ptr
        val isTail = tail == ptr

        if (isHead && isTail) {
            assert(size == 1)
            assert(prev == null)
            assert(next == null)
            head = null
            tail = null
            size = 0
        } else if (isHead) {
            assert(prev == null)
            assert(next != null)
            assert(size > 1)
            assert(next!!.prev == ptr)

            head = next
            next.prev = null
            size--
        } else if (isTail) {
            assert(next == null)
            assert(prev != null)
            assert(size > 1)
            assert(prev!!.next == ptr)

            tail = prev
            prev.next = null
            size--
        } else if (prev != null && next != null) {
            assert(size > 2)
            assert(prev.next == ptr)
            assert(next.prev == ptr)

            prev.next = next
            next.prev = prev
            size--
        } else {
            assert(prev == null && next == null)
        }

        ptr.next = null
        ptr.prev = null
    }

    class NodePointer<E>(val container: E) {
        var next: NodePointer<E>? = null
        var prev: NodePointer<E>? = null
    }
}