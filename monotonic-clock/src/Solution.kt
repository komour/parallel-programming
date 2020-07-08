/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 */
class Solution : MonotonicClock {
    private var c1 by RegularInt(0)
    private var c2 by RegularInt(0)
    private var c3 by RegularInt(0)

    private var d1 by RegularInt(0)
    private var d2 by RegularInt(0)
    private var d3 by RegularInt(0)

    override fun write(time: Time) {
        d1 = time.d1
        d2 = time.d2
        d3 = time.d3

        // write right-to-left
        c3 = time.d3
        c2 = time.d2
        c1 = time.d1
    }

    override fun read(): Time {
        val vc1 = c1
        val vc2 = c2
        val vc3 = c3

        val vd3 = d3
        val vd2 = d2
        val vd1 = d1

        return if (vc1 == vd1) {
            if (vc2 == vd2) {
                if (vc3 == vd3) {
                    Time(vc1, vc2, vc3)
                } else {
                    Time(vc1, vc2, vd3)
                }
            } else {
                Time(vc1, vd2, 0)
            }
        } else {
            Time(vd1, 0, 0)
        }
    }
}