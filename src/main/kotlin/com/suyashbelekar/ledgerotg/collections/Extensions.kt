package com.suyashbelekar.ledgerotg.collections

fun <T> Sequence<T>.collectUntilChanged(predicate: (t1: T, t2: T) -> Boolean): Sequence<List<T>> {
    var list = mutableListOf<T>()

    var lastElementAdded: T? = null

    var count = 0

    return sequenceOf(this, sequenceOf(LastElement))
        .flatten()
        .zipWithNext()
        .flatMap {
            count++

            if (it.second is LastElement) {
                when {
                    list.isNotEmpty() -> sequenceOf(list)
                    count > 1 -> sequenceOf(listOf(it.first as T))
                    else -> sequenceOf()
                }
            } else {
                val pair = it as Pair<T, T>

                if (predicate.invoke(pair.first, pair.second)) {
                    if (pair.first !== lastElementAdded) {
                        list.add(pair.first)
                        lastElementAdded = pair.first
                    }
                    if (pair.second !== lastElementAdded) {
                        list.add(pair.second)
                        lastElementAdded = pair.second
                    }
                    sequenceOf()
                } else {
                    val listToReturn = mutableListOf<List<T>>()

                    if (list.isNotEmpty()) {
                        listToReturn.add(list)
                    } else {
                        listToReturn.add(listOf(pair.first))
                    }

                    list = mutableListOf(pair.second)
                    lastElementAdded = pair.second

                    listToReturn.asSequence()
                }
            }
        }
}

object LastElement