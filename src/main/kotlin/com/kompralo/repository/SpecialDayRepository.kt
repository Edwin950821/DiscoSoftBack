package com.kompralo.repository

import com.kompralo.model.SpecialDay
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SpecialDayRepository : JpaRepository<SpecialDay, Long> {

    fun findByActiveTrue(): List<SpecialDay>

    fun findByDateBetweenAndActiveTrue(start: LocalDate, end: LocalDate): List<SpecialDay>

    fun findByDateAndActiveTrue(date: LocalDate): List<SpecialDay>

    fun findBySellerOrderByCreatedAtDesc(seller: User): List<SpecialDay>
}
