import sbt.Keys._
import sbt._

object ResolverSettings {

  lazy val settings = Seq(resolvers ++=
    Seq(
      Resolver.defaultLocal,
      Resolver.mavenLocal,
      Resolver.typesafeRepo("snapshots"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype OSS Release" at "https://oss.sonatype.org/content/repositories/releases"
    )
  )
}
