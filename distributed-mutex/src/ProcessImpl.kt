package mutex

/**
 * Distributed mutual exclusion implementation.
 * All functions are called from the single main thread.
 *
 * @author Andrey Komarov
 */
class ProcessImpl(private val env: Environment) : Process {
    private var status = Status.THINKING

    private val dirty = BooleanArray(env.nProcesses + 1) { f -> true }

    //  some acyclic graph with all forks at the last process
    private val fork = BooleanArray(env.nProcesses + 1) { f -> f < env.processId }

    //  request token.. (initially = ~fork)
    private val reqf = BooleanArray(env.nProcesses + 1) { f -> f > env.processId }

//    init {
//        println("---------------")
//        print("${env.processId}: ")
//        for (i in fork.indices) {
//            print("($i, ${fork[i]}-${reqf[i]}), ")
//        }
//        println("\n---------------")
//    }

    // try to request fork from the srcId process
    private fun requestFork(srcId: Int) {
        if (status == Status.HUNGRY && reqf[srcId] && !fork[srcId]) { // have token and don't have fork
            env.send(srcId) {
                writeEnum(MsgType.REQ)
            }
            reqf[srcId] = false
        }
    }

    // try to give fork to the srcId process
    private fun releaseFork(srcId: Int) {
        if (status != Status.EATING && reqf[srcId] && dirty[srcId]) {
            dirty[srcId] = false
            fork[srcId] = false
            env.send(srcId) {
                writeEnum(MsgType.REL)
            }
        }
    }

    // check whether we have all the necessary resources (forks)
    private fun canEat(): Boolean {
        return (1..env.nProcesses).minus(env.processId).all { i -> fork[i] }
    }

    // dive into the critical section!
    private fun eat() {
        env.locked()
        // after eating all forks are dirty
        (1..env.nProcesses).minus(env.processId).forEach { i -> dirty[i] = true }
        status = Status.EATING
    }

    override fun onMessage(srcId: Int, message: Message) {
        message.parse {
            val type = readEnum<MsgType>()
            if (type == MsgType.REQ) { // receive the request for the fork => try to give it
                reqf[srcId] = true // with request we also receive the token
                releaseFork(srcId)
                requestFork(srcId) // instantly request if want to
            } else { // receive clean fork
                fork[srcId] = true
                dirty[srcId] = false
                if (status == Status.HUNGRY && canEat()) { // instantly try to eat if want to
                    eat()
                }
            }
        }
    }

    override fun onLockRequest() {
        check(status != Status.EATING)
        status = Status.HUNGRY
        if (canEat()) {
            eat()
        } else {
            (1..env.nProcesses).forEach { i -> requestFork(i) }
        }
    }

    override fun onUnlockRequest() {
        check(status == Status.EATING)
        env.unlocked()
        status = Status.THINKING
        (1..env.nProcesses).minus(env.processId).forEach { i ->
            releaseFork(i)
        }
    }

    enum class Status { THINKING, HUNGRY, EATING }
    enum class MsgType { REQ, REL }
}
