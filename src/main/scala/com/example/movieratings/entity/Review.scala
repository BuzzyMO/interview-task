package com.example.movieratings.entity

import java.time.LocalDate

case class Review(customerId: Long, rating: Byte, date: LocalDate)
