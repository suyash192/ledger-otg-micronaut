package com.suyashbelekar.ledgerotg.pagination

data class Page<T>(
    val content: List<T>,
    val total: Long,
    val pageable: Pageable?
)

data class Pageable(
    val pageNo: Int = 0,
    val size: Int = 20,
    val sort: List<Sort>
) {
    val offset = 0
}

data class Sort(
    val dir: Dir,
    val property: String
)

enum class Dir {
    ASC, DESC
}