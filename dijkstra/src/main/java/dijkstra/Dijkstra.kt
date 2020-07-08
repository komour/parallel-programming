package dijkstra

import kotlinx.atomicfu.atomic
import java.util.*
import java.util.Collections.nCopies
import java.util.concurrent.Phaser
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.concurrent.thread

val random = Random(0)
val workers = Runtime.getRuntime().availableProcessors()

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> o1!!.distance.compareTo(o2!!.distance) }

private class MultiQueue {
    val queues = nCopies(amount, PriorityQueue(amount, NODE_DISTANCE_COMPARATOR))
    val locks = nCopies(amount, ReentrantLock(true))

    fun add(newNode: Node): Boolean {
        while (true) {
            val k = random.nextInt(amount)
            if (locks[k].tryLock()) {
                return try {
                    queues[k].offer(newNode)
                } finally {
                    locks[k].unlock()
                }
            }
        }
    }

    fun lockTwo(firstInd: Int, secondInd: Int) {
        while (true) {
            if (locks[firstInd].tryLock()) {
                break
            }
        }
        while (true) {
            if (locks[secondInd].tryLock()) {
                break
            }
        }
    }

    fun poll(): Node? {
        var cur: Node? = null
        if (active.value > 0) {
            val firstInd = random.nextInt(amount)
            var secondInd = random.nextInt(amount)
            while (firstInd == secondInd) {
                secondInd = random.nextInt(amount)
            }
            lockTwo(firstInd, secondInd)
            try {
                cur = minOf(queues[firstInd], queues[secondInd], compareBy(nullsLast(NODE_DISTANCE_COMPARATOR), Queue<Node>::peek)).poll()
            } finally {
                locks[secondInd].unlock()
                locks[firstInd].unlock()
            }
        }
        return cur
    }
}

val active = atomic(0)
val amount = workers * 2

fun shortestPathParallel(start: Node) {
    start.distance = 0
    val onFinish = Phaser(workers + 1)
    active.incrementAndGet()
    val mq = MultiQueue()
    mq.queues[0].add(start)
    repeat(workers) {
        thread {
            while (true) {
                val cur = mq.poll() ?: break
                for (e in cur.outgoingEdges) {
                    val newDist = cur.distance + e.weight
                    var oldDist = e.to.distance
                    while (oldDist > cur.distance + e.weight) {
                        if (e.to.casDistance(oldDist, newDist)) {
                            mq.add(e.to)
                            active.incrementAndGet()
                        }
                        oldDist = e.to.distance
                    }
                }
                active.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}