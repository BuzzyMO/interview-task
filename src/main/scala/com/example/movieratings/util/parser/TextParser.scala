package com.example.movieratings.util.parser

trait TextParser[T] {
  def parse(text: String): Either[Throwable, T]
}
