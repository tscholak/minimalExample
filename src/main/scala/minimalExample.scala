import frameless._
import frameless.syntax._
import frameless.functions.aggregate._
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.rng.Seed

object minimalExample extends App {

  type A = Int
  type B = Vector[Option[Int]]
  type T = (A, B)

  val conf: SparkConf = new SparkConf()
    .setAppName("minimal example")
    .setAll("spark.sql.crossJoin.enabled" -> "true" :: Nil)
  val spark = SparkSession.builder().config(conf).getOrCreate()

  implicit val sqlContext: SQLContext = spark.sqlContext

  Gen
    .listOfN[T](50, Arbitrary.arbitrary[T])
    .apply(Gen.Parameters.default, Seed(0L))
    .foreach((samples: List[T]) => {
      val tds: TypedDataset[T] = TypedDataset.create(samples)
      val u = tds.makeUDF[B, B](identity)
      tds
        .groupBy(tds('_1))
        .agg(collectList(u(tds('_2))))
        .show()
        .run()
    })

  spark.stop
}
