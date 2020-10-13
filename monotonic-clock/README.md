# Монотонные часы

В рамках данного задания необходимо реализовать алгоритм монотонных часов на регулярных регистрах.

В файле [`src/MonotonicClock.kt`](src/MonotonicClock.kt) 
находится описание интерфейса часов, которые вам предстоит реализовать. Время, которое показывают часы, 
состоит из трёх целочисленных переменных `d1`, `d2` и `d3`, задающих время от старшего разряда к младшему. 
Все времена упорядочены лекскографически. 

```kotlin
data class Time(val d1: Int, val d2: Int, val d3: Int)
```

Интерфейс часов должен реализовывать операции записи и чтения. Один поток пишет монотонно увеличивающееся время, 
а другой поток его читает. Не обязательно, чтобы чтения другого потока были линеаризуемы, но важно, чтобы другой поток 
видел монотонно возрастающие времена и видел правильное время, если чтения времени не происходит параллельно 
с его записью.

```kotlin
interface MonotonicClock {
    fun write(time: Time)
    fun read(): Time
}
```

В вашем распоряжении есть класс [`RegularInt`](src/RegularInt.kt), который моделирует регулярный
регистр с операциями чтения и записи.

Вася Пупкин узнал про трюк с чтением в одном порядке и записью в другом и попытался его реализовать.
Вы найдете его решения:
* На языке Kotlin в файле [`src/SolutionTemplateKt.kt`](src/SolutionTemplateKt.kt) 
* На языке Java в файле [`src/SolutionTemplateJava.java`](src/SolutionTemplateJava.java)

Однако, Васины решения не проходят тесты и показывают немонотонное время при чтении.

## Задание

Возмите за основу одно из предоставленных решений Васи Пупкина и скопируйте его в файл `src/Solution.(kt|java)`.
Вы должны написать в этом файле реализую монотонных часов, используя фиксированное число переменных типа `RegularInt` для хранения
текущего записанного времени. Использование блокировок и других механизмов синхронизации запрещено. Ваше решение должно поддерживать
весь диппазон потенциальных времен, то есть должно сохранять переданное в функцию `write` время как минимум в 
трех регулярных целочисленных переменных (но может использовать и больше).

Для проверки корректности вашего решения запустите `./gradlew run` из корня репозитория. 

Один из вариантов решения описан в работе Лесли Лампорта 
[Concurrent Reading and Writing of Clocks](http://lamport.azurewebsites.net/pubs/lamport-concurrent-clocks.pdf).

## Формат сдачи

Выполняйте задание в этом репозитории. Инструкции по сдаче заданий находятся в 
[этом документе](https://docs.google.com/document/d/1GQ0OI_OBkj4kyOvhgRXfacbTI9huF4XJDMOct0Lh5og). 