package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head = new AtomicRef<>(new Node());
    private AtomicRef<Node> tail = new AtomicRef<>(new Node());

    public MSQueue() {
        Node dummy = new Node();
        this.head.setValue(dummy);
        this.tail.setValue(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newNode = new Node(x);
        while (true) {
            Node curTail = tail.getValue();
            AtomicRef<Node> nextToTail = curTail.next;
            if (nextToTail.compareAndSet(null, newNode)) {
                tail.compareAndSet(curTail, newNode);
                return;
            } else {
                tail.compareAndSet(curTail, curTail.next.getValue());
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            Node curTail = tail.getValue();
            Node nextToHead = curHead.next.getValue();
            if (head.getValue() == curHead) {
                if (nextToHead == null) {
                    return Integer.MIN_VALUE;
                } else {
                    if (curTail == curHead) {
                        tail.compareAndSet(curTail, nextToHead);
                    }
                    if (head.compareAndSet(curHead, nextToHead)) {
                        return nextToHead.x;
                    }
                }
            }
        }
    }

    @Override
    public int peek() {
        while (true) {
            Node curTail = tail.getValue();
            Node curHead = head.getValue();
            Node nextToHead = curHead.next.getValue();
            if (head.getValue() == curHead && nextToHead == null) {
                return Integer.MIN_VALUE;
            } else {
                if (head.getValue() == curHead && curHead == curTail) {
                    tail.compareAndSet(curTail, nextToHead);
                }
                if (head.getValue() == curHead) {
                    return nextToHead.x;
                }
            }
        }
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            this.next = new AtomicRef<>(null);
        }

        Node() {
            this.x = Integer.MIN_VALUE;
            this.next = new AtomicRef<>(null);
        }
    }
}