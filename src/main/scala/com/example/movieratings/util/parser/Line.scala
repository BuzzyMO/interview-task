package com.example.movieratings.util.parser

import org.slf4j.LoggerFactory

object Line {
  val logger = LoggerFactory.getLogger(getClass)

  def decode[T](text: String)(implicit parser: TextParser[T]): T = {
    parser.parse(text) match {
      case Left(ex) =>
        logger.error(ex.getMessage)
        throw ex
      case Right(v) => v
    }
  }
}
