package com.example.movieratings.entity

import java.time.LocalDate

case class Review(customerId: String, rating: Byte, date: LocalDate)
