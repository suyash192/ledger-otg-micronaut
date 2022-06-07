package com.suyashbelekar.ledgerotg.persistence.daos

interface Dao<E, ID> {
    fun findAll(): List<E>

    fun findById(id: ID): E?

    fun insert(element: E): E

    fun update(element: E): Boolean

    fun deleteById(id: ID): E?
}