package com.example.movieratings.util.parser

import com.example.movieratings.entity.Movie
import com.example.movieratings.entity.Review

import java.time.LocalDate
import java.time.Year
import scala.util.matching.Regex

object TextParserIntance {
  val ParseEx = "Text of %1$s can't be parsed: %2$s"
  val pattern2Commas: Regex = """^([^,]+),([^,]+),(.+)$""".r

  implicit val movieTitleParser: TextParser[Movie] = {
    (text: String) => {
      text match {
        case pattern2Commas(id, year, title) => Right(Movie(id, Year.parse(year), title))
        case _ => Left(new IllegalArgumentException(ParseEx.format("movie", text)))
      }
    }
  }

  implicit val reviewParser: TextParser[Review] = {
    (text: String) => {
      text match {
        case pattern2Commas(cId, r, d) => Right(Review(cId, r.toByte, LocalDate.parse(d)))
        case _ => Left(new IllegalArgumentException(ParseEx.format("review", text)))
      }
    }
  }
}
