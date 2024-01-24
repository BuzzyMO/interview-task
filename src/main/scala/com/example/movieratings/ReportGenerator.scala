package com.example.movieratings

object ReportGenerator {

  def main(args: Array[String]): Unit = {
    val (mvTitlesPath, trainingSetPath, reportFilePath) = getFilePaths(args)
    val reportBuilder = ReportBuilder(mvTitlesPath, trainingSetPath)

    reportBuilder.generateFullReport(reportFilePath)
  }

  def getFilePaths(args: Array[String]): (String, String, String) = {
    args match {
      case Array(mt, ts, r, _*) => (mt, ts, r)
      case _ => throw new IllegalArgumentException("Program arguments should contain at least 3 parameters")
    }
  }
}
