import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class Node(val next: AtomicReference<Node>)

class Receiver<E>(
    val action: Continuation<E>,
    next: AtomicReference<Node> = AtomicReference<Node>()
) : Node(next)

class Sender<E>(
    val element: E,
    val action: Continuation<Unit>,
    next: AtomicReference<Node> = AtomicReference<Node>()
) : Node(next)

class Dummy() : Node(AtomicReference<Node>())

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    val dummy = Dummy()
    val head = AtomicReference<Node>(dummy)
    val tail = AtomicReference<Node>(dummy)

    override suspend fun send(element: E) {
        while (true) {

            val curTail = tail.get()

            if (head.get() != curTail && curTail !is Sender<*>) {
                val curHead = head.get()
                var nextTwoHead = curHead.next.get()
                if (nextTwoHead !is Receiver<*> || head.get() == tail.get()) {
                    continue
                }
                nextTwoHead = nextTwoHead as Receiver<E>
                if (head.compareAndSet(curHead, nextTwoHead) && curHead != tail.get()) {
                    nextTwoHead.action.resume(element)
                    return
                }

            } else {
                val result = suspendCoroutine<Any> l@ { continuation ->
                    val newTail = Sender(element, continuation)

                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail)
                    } else {
                        continuation.resume("again")
                        return@l
                    }
                }
                if (result == "again") continue
                return
            }

        }
    }

    override suspend fun receive(): E {
        while (true){
            val curTail = tail.get()
            if (head.get() != curTail && curTail !is Receiver<*>) {
                val curHead = head.get()

                var nextTwoHead = curHead.next.get()
                if (nextTwoHead !is Sender<*> || head.get() == tail.get()) {
                    continue
                }
                nextTwoHead = nextTwoHead as Sender<E>

                if (head.compareAndSet(curHead, nextTwoHead) && curHead != tail.get()) {
                    nextTwoHead.action.resume(Unit)
                    return nextTwoHead.element
                }
            } else {
                val result = suspendCoroutine<E?> l@ { continuation ->
                    val newTail = Receiver(continuation)

                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail)
                    } else {
                        continuation.resume(null)
                        return@l
                    }
                } ?: continue
                return result
            }
        }
    }
}