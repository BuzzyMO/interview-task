package com.example.movieratings.util.parser

import com.example.movieratings.entity.Movie
import com.example.movieratings.entity.Review

import java.time.LocalDate
import java.time.Year

object TextParserIntance {
  val ParseEx = "Text of %1$s can't be parsed: %2$s"

  implicit val movieTitleParser: TextParser[Movie] = {
    (text: String) => {
      text.split(',') match {
        case Array(id, year, title) => Right(Movie(id, Year.parse(year), title))
        case _ => Left(new IllegalArgumentException(ParseEx.format("movie", text)))
      }
    }
  }

  implicit val reviewParser: TextParser[Review] = {
    (text: String) => {
      text.split(',') match {
        case Array(cId, r, d) => Right(Review(cId, r.toByte, LocalDate.parse(d)))
        case _ => Left(new IllegalArgumentException(ParseEx.format("review", text)))
      }
    }
  }
}
