import java.util.concurrent.atomic.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BlockingStackImpl<E> : BlockingStack<E> {

    // ==========================
    // Segment Queue Synchronizer
    // ==========================


    private class Receiver<E>(
        val action: Continuation<E>? = null,
        val next: AtomicReference<Receiver<E>> = AtomicReference<Receiver<E>>(null)
    )

    private val dummy = Receiver<E>(null)

    private val enqIdx = AtomicReference<Receiver<E>>(dummy)
    private val deqIdx = AtomicReference<Receiver<E>>(dummy)

    private suspend fun suspend(): E {
        return suspendCoroutine { continuation ->
            while (true) {
                val curTail = deqIdx.get()
                val newTail = Receiver(continuation)
                if (!curTail.next.compareAndSet(null, newTail)) {
                    continue
                } else {
                    deqIdx.compareAndSet(curTail, newTail)
                    break
                }
            }
        }
    }

    private fun resume(element: E) {
        while (true) {
            val curHead = enqIdx.get()
            if (curHead != deqIdx.get() && curHead.next.get() != null) {
                val newHead = curHead.next.get()
                if (!enqIdx.compareAndSet(curHead, newHead)) {
                    continue
                } else {
                    newHead.action?.resume(element)
                    return
                }
            }
        }
    }

    // ==============
    // Blocking Stack
    // ==============


    private val head = AtomicReference<Node<E>?>(null)
    private val elements = AtomicInteger()

    override fun push(element: E) {
        val elements = this.elements.getAndIncrement()
        if (elements >= 0) {
            // push the element to the top of the stack
            while (true) {
                val curHead = head.get()
                if (curHead?.element != SUSPENDED) {
                    if (head.compareAndSet(curHead, Node(element, AtomicReference(curHead)))) {
                        break
                    }
                } else {
                    if (head.compareAndSet(curHead, curHead.next.get())) {
                        resume(element)
                        return
                    } else {
                        continue
                    }
                }
                continue
            }
        } else {
            // resume the next waiting receiver
            resume(element)
        }
    }

    override suspend fun pop(): E {
        val elements = this.elements.getAndDecrement()
        if (elements > 0) {
            // remove the top element from the stack
            while (true) {
                val curHead = head.get()
                return if (curHead != null) {
                    if (head.compareAndSet(curHead, curHead.next.get())) {
                        curHead.element as E
                    } else continue
                } else {
                    if (head.compareAndSet(curHead, Node(SUSPENDED))) {
                        suspend()
                    } else {
                        continue
                    }
                }
            }
        } else {
            return suspend()
        }
    }
}

private class Node<E>(val element: Any?, val next: AtomicReference<Node<E>?> = AtomicReference(null))


private val SUSPENDED = Any()