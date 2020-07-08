package faaqueue;

import kotlinx.atomicfu.*;

import static faaqueue.FAAQueue.Node.NODE_SIZE;


public class FAAQueue<T> implements Queue<T> {
    private static final Object DONE = new Object(); // Marker for the "DONE" slot state; to avoid memory leaks

    private AtomicRef<Node> head; // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private AtomicRef<Node> tail; // Tail pointer, similarly to the Michael-Scott queue

    public FAAQueue() {
        Node firstNode = new Node();
        head = new AtomicRef<>(firstNode);
        tail = new AtomicRef<>(firstNode);
    }

    @Override
    public void enqueue(T x) {

        global_loop:
        while (true) {
            Node curTail = this.tail.getValue();
            Node nextToCurTail = curTail.next.getValue();
            while (nextToCurTail != null) {
                if (tail.compareAndSet(curTail, nextToCurTail)) {
                    curTail = tail.getValue();
                    nextToCurTail = curTail.next.getValue();
                    continue;
                }
                continue global_loop;
            }

            int enqIdx = curTail.enqIdx.getAndIncrement();
            if (enqIdx < NODE_SIZE) {
                if (curTail.data.get(enqIdx).compareAndSet(null, x)) {
                    return;
                }
            } else {
                Node newTail = new Node(x);
                nextToCurTail = curTail.next.getValue();
                if (curTail != tail.getValue()) {
                    continue;
                }
                if (nextToCurTail != null) {
                    tail.compareAndSet(curTail, nextToCurTail);
                } else {
                    if (tail.getValue().next.compareAndSet(null, newTail)) {
                        break;
                    }
                }

            }

        }
    }

    @Override
    public T dequeue() {
        while (true) {
            Node curHead = head.getValue();
            if (curHead.isEmpty()) {
                Node nextToCurHead = curHead.next.getValue();
                if (nextToCurHead == null) {
                    return null;
                }
                head.compareAndSet(curHead, nextToCurHead);
            } else {
                int deqIdx = curHead.deqIdx.getAndIncrement();
                if (deqIdx < NODE_SIZE) {
                    Object res = curHead.data.get(deqIdx).getAndSet(DONE);
                    if (res == null) {
                        continue;
                    }
                    return (T) res;
                }
            }
        }
    }

    static class Node {
        static final int NODE_SIZE = 2; // CHANGE ME FOR BENCHMARKING ONLY

        private final AtomicRef<Node> next = new AtomicRef<>(null);
        private final AtomicInt enqIdx = new AtomicInt(0); // index for the next enqueue operation
        private final AtomicInt deqIdx = new AtomicInt(0); // index for the next dequeue operation
        private final AtomicArray<Object> data = new AtomicArray<>(NODE_SIZE);

        Node() {}

        Node(Object x) {
            this.enqIdx.setValue(1);
            this.data.get(0).setValue(x);
        }

        private boolean isEmpty() {
            return this.deqIdx.getValue() >= this.enqIdx.getValue();
        }
    }
}