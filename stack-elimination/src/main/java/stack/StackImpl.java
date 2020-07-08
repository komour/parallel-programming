package stack;

import kotlinx.atomicfu.*;

import java.util.Random;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

        Node() {
            this.x = Integer.MIN_VALUE;
            this.next = null;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);

    private static final long DURATION = 200;
    private static final int CAPACITY = 1000;
    private static final int LINE_SIZE = 4;

    private class MyTimeOutException extends Exception {
    }


    private class EliminationArray {

        private AtomicArray<Integer> buffer = new AtomicArray<>(CAPACITY);
        private Random R = new Random(0);


        EliminationArray() {
            for (int i = 0; i < CAPACITY; ++i) {
                buffer.get(i).setValue(null);
            }
        }

        Integer visit(Integer newVal) throws MyTimeOutException {
            int randInd = R.nextInt(StackImpl.CAPACITY);
            return exchange(randInd, newVal);

        }

        Integer exchange(int slotInd, Integer newValue) throws MyTimeOutException {
            long timeBound = System.nanoTime() + StackImpl.DURATION;
            if (System.nanoTime() > timeBound) {
                throw new MyTimeOutException();
            }
            int oldInd = slotInd;
                slotInd = oldInd;
                if (newValue == null) { //cash lines ?
                    while (slotInd < oldInd + LINE_SIZE && slotInd < CAPACITY - 1 && buffer.get(slotInd).getValue() == null) {
                        slotInd++;
                    }
                    if (buffer.get(slotInd).getValue() == null) {
                        slotInd = oldInd;
                    }
                    while (slotInd > oldInd - LINE_SIZE && slotInd > 0 && buffer.get(slotInd).getValue() == null) {
                        slotInd--;
                    }
                } else {
                    while (slotInd < oldInd + LINE_SIZE && slotInd < CAPACITY - 1 && buffer.get(slotInd).getValue() != null) {
                        slotInd++;
                    }
                    if (buffer.get(slotInd).getValue() != null) {
                        slotInd = oldInd;
                    }
                    while (slotInd > oldInd - LINE_SIZE && slotInd > 0 && buffer.get(slotInd).getValue() != null) {
                        slotInd--;
                    }
                }
                AtomicRef<Integer> herItem = buffer.get(slotInd);
            while (true) {
                if (System.nanoTime() > timeBound) {
                    throw new MyTimeOutException();
                }
                Integer oldValue = herItem.getValue();
                if (oldValue == null) {
                    if (herItem.compareAndSet(oldValue, newValue)) {
                        while (System.nanoTime() < timeBound) {
                            Integer tempItem = herItem.getValue();
                            if (tempItem == null) {
                                return null;
                            }
                        }
                        if (herItem.compareAndSet(oldValue, null)) {
                            throw new MyTimeOutException();
                        } else {
                            Integer tempItem = herItem.getValue();
                            herItem.setValue(null);
                            return tempItem;
                        }
                    }
                } else if (herItem.compareAndSet(oldValue, newValue)) {
                    return oldValue;
                }
            }
        }

    }

    private EliminationArray elimination = new EliminationArray();

    @Override
    public void push(int x) {

        try {
            Integer otherValue = elimination.visit(x);
            if (otherValue == null) {
                return;
            }
        } catch (MyTimeOutException ignored) {
        }

        while (true) {
            Node oldHead = head.getValue();
            Node newHead = new Node(x, oldHead);
            if (head.compareAndSet(oldHead, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {

        try {
            Integer otherValue = elimination.visit(null);
            if (otherValue != null) {
                return otherValue;
            }
        } catch (MyTimeOutException ignored) {
        }

        while (true) {
            Node oldHead = head.getValue();
            if (oldHead == null) {
                return Integer.MIN_VALUE;
            }
            Node newHead = oldHead.next.getValue();
            if (head.compareAndSet(oldHead, newHead)) {
                return oldHead.x;
            }
        }
    }
}
