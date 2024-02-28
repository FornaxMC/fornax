package dev.luna5ama.fornax.util

import kotlin.reflect.KMutableProperty1

class OpenLinkedList<E>(prevPtr: KMutableProperty1<E, E?>, nextPtr: KMutableProperty1<E, E?>) {
    var head: E? = null
    var tail: E? = null
    var size = 0; private set
    
    private var E.prev by prevPtr
    private var E.next by nextPtr

    fun pushFront(e: E) {
        assert(size >= 0)
        
        val head =head

        if (head == null) {
            tail = e
        } else {
            head.prev = e
            e.next = head
        }

        this.head = e
        size++

        assert(size >= 0)
    }

    fun pushBack(e: E) {
        assert(size >= 0)

        val tail = tail

        if (tail == null) {
            head = e
        } else {
            tail.next = e
            e.prev = tail
        }

        this.tail = e
        size++

        assert(size >= 0)
    }

    fun popFront(): E? {
        assert(size >= 0)

        val head = head ?: return null

        val next = head.next
        if (next == null) {
            this.head = null
            tail = null
        } else {
            next.prev = null
            this.head = next
        }

        head.next = null
        size--

        assert(size >= 0)

        return head
    }

    fun popBack(): E? {
        assert(size >= 0)

        val tail = tail ?: return null

        val prev = tail.prev
        if (prev == null) {
            head = null
            this.tail = null
        } else {
            prev.next = null
            this.tail = prev
        }

        tail.prev = null
        size--

        assert(size >= 0)

        return tail
    }

    fun remove(e: E) {
        assert(size >= 0)

        val prev = e.prev
        val next = e.next

        val isHead = head == e
        val isTail = tail == e

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
            assert(next!!.prev == e)

            head = next
            next.prev = null
            size--
        } else if (isTail) {
            assert(next == null)
            assert(prev != null)
            assert(size > 1)
            assert(prev!!.next == e)

            tail = prev
            prev.next = null
            size--
        } else if (prev != null && next != null) {
            assert(size > 2)
            assert(prev.next == e)
            assert(next.prev == e)

            prev.next = next
            next.prev = prev
            size--
        } else {
            assert(prev == null && next == null)
        }

        e.next = null
        e.prev = null
    }
}