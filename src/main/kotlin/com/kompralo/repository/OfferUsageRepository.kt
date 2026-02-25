package com.kompralo.repository

import com.kompralo.model.Offer
import com.kompralo.model.OfferUsage
import com.kompralo.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OfferUsageRepository : JpaRepository<OfferUsage, Long> {

    fun countByOfferAndUser(offer: Offer, user: User): Long

    fun countByOffer(offer: Offer): Long

    fun findByOffer(offer: Offer): List<OfferUsage>

    fun findByOfferIn(offers: List<Offer>): List<OfferUsage>
}
