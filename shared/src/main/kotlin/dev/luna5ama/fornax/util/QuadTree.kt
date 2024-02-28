package dev.luna5ama.fornax.util

class QuadTree<T : QuadTree.Node<T>> {
    var root: T? = null

    open class Node<T : Node<T>>(val parent: T?) {
        var c11: T? = null
        var c12: T? = null
        var c21: T? = null
        var c22: T? = null
    }
}