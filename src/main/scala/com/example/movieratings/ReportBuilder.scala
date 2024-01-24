package com.example.movieratings

import com.example.movieratings.ReportBuilder.LowestReleaseYear
import com.example.movieratings.ReportBuilder.MinNumReviews
import com.example.movieratings.ReportBuilder.MvReviewFileIdTemplate
import com.example.movieratings.ReportBuilder.MvReviewFileNameTemplate
import com.example.movieratings.ReportBuilder.NullStringValue
import com.example.movieratings.ReportBuilder.NumThreads
import com.example.movieratings.ReportBuilder.UpperReleaseYear
import com.example.movieratings.entity.Movie
import com.example.movieratings.entity.MovieReport
import com.example.movieratings.entity.Review
import com.example.movieratings.util.CsvUtils
import com.example.movieratings.util.parser.Line
import com.example.movieratings.util.parser.TextParserIntance.movieTitleParser
import com.example.movieratings.util.parser.TextParserIntance.reviewParser
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.charset.CodingErrorAction
import java.time.Year
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.io.Codec
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

object ReportBuilder {
  val MvReviewFileNameTemplate = "/mv_%1$s.txt"
  val MvReviewFileIdTemplate = "0000000"
  val NullStringValue = "NULL"
  val LowestReleaseYear = Year.of(1970)
  val UpperReleaseYear = Year.of(1990)
  val MinNumReviews = 1000
  val NumThreads = 4

  def apply(mvTitlesPath: String, trainingSetPath: String): ReportBuilder = {
    new ReportBuilder(mvTitlesPath, trainingSetPath)
  }
}

class ReportBuilder(mvTitlesPath: String, trainingSetPath: String) {
  val logger = LoggerFactory.getLogger(getClass)
  implicit val codec: Codec = Codec("UTF-8")
  implicit val ctx: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(NumThreads))

  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

  def generateFullReport(reportFilePath: String): Unit = {
    buildReports(mvTitlesPath)
      .map(fs => Future.sequence(fs))
      .map(frOpts => frOpts.map(_.flatten))
      .foreach(f =>
        f.onComplete {
          case Success(rs) =>
            val sortedRs = rs.sortBy(r => (-r.avgRating, r.movie.title))
            writeReports(sortedRs, reportFilePath)
            logger.warn("Report has been generated. Press Any Key to terminate the application.")
          case Failure(ex) =>
            logger.error(ex.getMessage)
            throw ex
        }
      )
  }

  def writeReports(reports: List[MovieReport], reportPath: String): Unit = {
    def reportFile: File = new File(reportPath)

    val csvReports = toCsvDataFormat(reports)

    CsvUtils.writeToFile(csvReports, reportFile)
  }

  private def toCsvDataFormat(reports: List[MovieReport]): Iterable[List[Any]] = {
    reports.map(r => r.movie.title :: r.movie.year :: r.avgRating :: r.numOfReviews :: Nil)
  }

  def buildReports(mvTitlesPath: String): Option[List[Future[Option[MovieReport]]]] = {
    Using(Source.fromFile(mvTitlesPath)) { reader =>
      reader.getLines()
        .filterNot(_.contains(NullStringValue))
        .map(Line.decode[Movie])
        .map {
          case mv@Movie(_, year, _) if year.isAfter(LowestReleaseYear) && year.isBefore(UpperReleaseYear) =>
            Future { buildReportByMovie(mv) }
          case _ => Future.successful(None)
        }.toList
    }.toOption
  }

  private def buildReportByMovie(mv: Movie): Option[MovieReport] = {
    val mvReviewFileName = trainingSetPath ++ MvReviewFileNameTemplate.format(MvReviewFileIdTemplate.drop(mv.id.length) ++ mv.id)
    val reviewsOpt = readReviews(mvReviewFileName).filter(_.size > MinNumReviews)
    val avgRatingOpt = reviewsOpt.map(getAvgRating)

    logger.info(mv.toString)

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
