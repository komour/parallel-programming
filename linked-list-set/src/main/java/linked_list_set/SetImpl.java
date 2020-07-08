package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private interface Node {
        boolean removed();
    }

    private class Window {
        Normal prev, cur;
        Node next;
    }

    private final AtomicRef<Normal> head = new AtomicRef<>(new Normal(Integer.MIN_VALUE, new Normal(Integer.MAX_VALUE, null)));

    /**
     * Returns the {@link Window}, where prev.x < x <= cur.x
     */
    private Window findWindow(int x) {
        Window w = new Window();

        global_loop:
        while (true) {
            w.prev = head.getValue();
            w.cur = (Normal) w.prev.next.getValue();

            local_loop:
            while (true) {
                if (w.cur.x >= x) {
                    return w;
                }
                w.next = w.cur.next.getValue();
                if (w.next.removed()) {
                    Removed updRemoved = (Removed) w.next;
                    if (!w.prev.next.compareAndSet(w.cur, updRemoved.normalRef)) {
                        continue global_loop;
                    } else {
                        w.cur = updRemoved.normalRef;
                        continue local_loop;
                    }
                } else {
                    if (w.cur.x < x) {
                        w.prev = w.cur;
                        w.cur = (Normal) w.next;
                        continue local_loop;
                    } else {
                        return w;
                    }
                }
            }

        }
    }

    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);

            if (w.cur.x == x && !w.cur.next.getValue().removed()) {
                return false;
            } else {
                Node newNode = new Normal(x, w.cur);
                if (w.prev.next.compareAndSet(w.cur, newNode)) {
                    return true;
                }
            }
        }
    }

    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.cur.x != x) {
                return false;
            }

            w.next = w.cur.next.getValue();
            if (w.cur.next.getValue().removed()) {
                return false;
            } else {
                Normal updNorm = (Normal) w.next;
                if (w.cur.next.compareAndSet(w.next, new Removed(updNorm))) {
                    w.prev.next.compareAndSet(w.cur, w.next);
                    return true;
                }
            }
        }
    }

    public boolean contains(int x) {
        Window w = findWindow(x);
        if (w.cur.x == x) {
            return !w.cur.next.getValue().removed();
        }
        return false;
    }
    private class Normal implements Node {
        int x;
        AtomicRef<Node> next;

        Normal(int x, Node next) {
            this.x = x;
            this.next = new AtomicRef<>(next);
        }

        public boolean removed() {
            return false;
        }
    }

    private class Removed implements Node {
        Normal normalRef;

        Removed(Normal normalRef) {
            this.normalRef = normalRef;
        }

        public boolean removed() {
            return true;
        }
    }
}