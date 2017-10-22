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

  val samples: Option[List[(A, B)]] = Gen
    .listOfN[T](50, Arbitrary.arbitrary[T])
    .apply(Gen.Parameters.default, Seed(0L))

  val res: Option[Seq[(A, Vector[B])]] =
    samples
      .map(TypedDataset.create(_))
      .map((tds: TypedDataset[T]) => {
        val u = tds.makeUDF[B, B](identity)
        tds
          .groupBy(tds('_1))
          .agg(collectList(u(tds('_2))))
          .collect()
          .run()
      })

  val exp: Option[Map[A, Vector[B]]] =
    samples.map(_.groupBy(_._1).mapValues(_.map(_._2).toVector))

  assert(res.map(_.toMap) == exp)

  spark.stop
}
