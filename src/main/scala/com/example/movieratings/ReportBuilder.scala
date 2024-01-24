package com.example.movieratings

import com.example.movieratings.ReportBuilder.LowestReleaseYear
import com.example.movieratings.ReportBuilder.MinNumReviews
import com.example.movieratings.ReportBuilder.MvReviewFileIdTemplate
import com.example.movieratings.ReportBuilder.MvReviewFileNameTemplate
import com.example.movieratings.ReportBuilder.NullStringValue
import com.example.movieratings.ReportBuilder.UpperReleaseYear
import com.example.movieratings.entity.Movie
import com.example.movieratings.entity.MovieReport
import com.example.movieratings.entity.Review
import com.example.movieratings.util.CsvUtils
import com.example.movieratings.util.parser.Line
import com.example.movieratings.util.parser.TextParserIntance.movieTitleParser
import com.example.movieratings.util.parser.TextParserIntance.reviewParser

import java.io.File
import java.nio.charset.CodingErrorAction
import java.time.Year
import scala.io.Codec
import scala.io.Source
import scala.util.Using

object ReportBuilder {
  val MvReviewFileNameTemplate = "/mv_%1$s.txt"
  val MvReviewFileIdTemplate = "0000000"
  val NullStringValue = "NULL"
  val LowestReleaseYear = Year.of(1970)
  val UpperReleaseYear = Year.of(1990)
  val MinNumReviews = 1000

  def apply(mvTitlesPath: String, trainingSetPath: String): ReportBuilder = {
    new ReportBuilder(mvTitlesPath, trainingSetPath)
  }
}

class ReportBuilder(mvTitlesPath: String, trainingSetPath: String) {
  implicit val codec = Codec("UTF-8")

  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

  def generateFullReport(reportFilePath: String): Unit = {
    buildReports(mvTitlesPath)
      .map(rs => rs.sortBy(r => (-r.avgRating, r.movie.title)))
      .foreach(rs => writeReports(rs, reportFilePath))
  }

  def writeReports(reports: List[MovieReport], reportPath: String): Unit = {
    def reportFile: File = new File(reportPath)

    val csvReports = toCsvDataFormat(reports)

    CsvUtils.writeToFile(csvReports, reportFile)
  }

  private def toCsvDataFormat(reports: List[MovieReport]): Iterable[List[Any]] = {
    reports.map(r => r.movie.title :: r.movie.year :: r.avgRating :: r.numOfReviews :: Nil)
  }

  def buildReports(mvTitlesPath: String): Option[List[MovieReport]] = {
    Using(Source.fromFile(mvTitlesPath)) { reader =>
      reader.getLines()
        .filterNot(_.contains(NullStringValue))
        .map(Line.decode[Movie])
        .flatMap {
          case mv@Movie(_, year, _) if year.isAfter(LowestReleaseYear) && year.isBefore(UpperReleaseYear) =>
            buildReportByMovie(mv)
          case _ => None
        }.toList
    }.toOption
  }

  private def buildReportByMovie(mv: Movie): Option[MovieReport] = {
    val mvReviewFileName = trainingSetPath ++ MvReviewFileNameTemplate.format(MvReviewFileIdTemplate.drop(mv.id.length) ++ mv.id)
    val reviewsOpt = readReviews(mvReviewFileName).filter(_.size > MinNumReviews)
    val avgRatingOpt = reviewsOpt.map(getAvgRating)

    for {
      reviews <- reviewsOpt
      avgRating <- avgRatingOpt
    } yield MovieReport(mv, avgRating, reviews.size)
  }

  def readReviews(mvReviewsFilePath: String): Option[List[Review]] = {
    Using(Source.fromFile(mvReviewsFilePath)) { reader =>
      reader.getLines().drop(1).foldLeft(Nil: List[Review])((acc, line) => {
        val review = Line.decode[Review](line)

        acc :+ review
      })
    }.toOption
  }

  private def getAvgRating(reviews: List[Review]): Double = {
    reviews.foldLeft(0.0)((acc, r) => acc + r.rating) / reviews.size
  }
}
