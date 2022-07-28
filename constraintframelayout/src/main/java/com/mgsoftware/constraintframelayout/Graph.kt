package com.mgsoftware.constraintframelayout

import java.util.*

class Graph(private var n: Int = 0) {
    companion object {
        var debug = false
    }

    private var adj = Array(n) { mutableSetOf<Int>() }

    fun addEdge(a: Int, b: Int) {
        adj[a].add(b)
    }

    fun sort(): Stack<Int> {
        val visitedArray = BooleanArray(n)
        val result = Stack<Int>()
        for (i in 0 until n) {
            val visited = visitedArray[i]
            if (!visited)
                rDfs(i, visitedArray, result)
        }
        return result
    }

    fun reset(n: Int) {
        if (adj.size != n) {
            this.n = n
            adj = Array(n) { mutableSetOf() }
        } else {
            adj.forEach { it.clear() }
        }
    }

    fun getAdj() = adj

    private fun rDfs(i: Int, visitedArray: BooleanArray, collector: Stack<Int>) {
        if (debug)
            println("$i=${visitedArray[i]} ==> true")
        visitedArray[i] = true
        adj[i].forEach { node ->
            if (!visitedArray[node])
                rDfs(node, visitedArray, collector)
            else if (!collector.contains(node)) {
                throw Exception("cycled")
            }
        }
        collector.push(i)
    }
}

fun main(args: Array<String>) {
    val graph = Graph(5)
    graph.addEdge(0, 1)
    graph.addEdge(1, 2)
    graph.addEdge(2, 0)
    graph.addEdge(0, 3)
    println(graph.sort())
}