import java.lang.ThreadLocal.withInitial

class Solution : AtomicCounter {

    private val top = Node(0)
    private val last = withInitial { top }


    override fun getAndAdd(x: Int): Int {
        var prev = 0
        var node = Node(x)
        while (last.get() != node) {
            prev = last.get().item
            node = Node(prev + x)
            last.set(last.get().next.decide(node))
        }
        return prev
    }

    private class Node(value: Int) {
        val item: Int = value
        val next: Consensus<Node> = Consensus()
    }
}