package com.example.movieratings.util.parser

object Line {
  def decode[T](text: String)(implicit parser: TextParser[T]): T = {
    parser.parse(text) match {
      case Left(ex) => throw ex
      case Right(v) => v
    }
  }
}
