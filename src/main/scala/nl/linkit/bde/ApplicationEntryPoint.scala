package nl.linkit.bde

import java.io.File

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

object ApplicationEntryPoint extends App {

  val sparkSession = SparkSession.builder().appName("LinkitBDE")
    .enableHiveSupport()
    .getOrCreate()

  val basePathString = "/root/linkit/data-spark"

  def getListOfFile(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists() && d.isDirectory) {
      d.listFiles.filter(_.isFile).filter(_.getName.endsWith(".csv")).toList
    } else {
      List[File]()
    }
  }

  def listOfDataFrame(targets: List[File]): Map[String, DataFrame] = {
    targets.map(file => (file.getName.substring(0,file.getName.lastIndexOf(".")), sparkSession.read.format("csv").option("header", "true").option("inferSchema", "true").csv("file:///" + file.getAbsolutePath))).toMap
  }

  val dfs = listOfDataFrame(getListOfFile(basePathString))

  def sanitizeColumns(t: DataFrame): DataFrame = {
    t.select(t.columns.map {
      c => t.col(c).as(if (c.contains("-")) c.replace("-", "_") else c)
    }: _*)
  }

  dfs.foreach(f => {
    sanitizeColumns(f._2).write.format("ORC").mode(SaveMode.Append).saveAsTable("linkit." + f._1)
  })


  sparkSession.sql("use linkit")

  val driverInfo = sparkSession.sql("SELECT a.driverid, a.name, t.hours_logged, t.miles_logged FROM drivers a LEFT JOIN timesheet t ON a.driverid=t.driverid")

  driverInfo.show(5)

}
