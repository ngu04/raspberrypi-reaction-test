lazy val root = Project("root", file("."))
  .aggregate(gpio, akka)

lazy val common = Project("common", file("common"))
  .settings(BaseSettings.settings: _*)
  .settings(Dependencies.common: _*)

lazy val gpio = Project("gpio", file("gpio"))
  .dependsOn(common)
  .settings(BaseSettings.javaagentSettings: _*)
  .settings(Dependencies.gpio: _*)
  .settings(Assembly.gpioAssemblySettings: _*)

lazy val akka = Project("akka", file("akka"))
  .dependsOn(common)
  .settings(BaseSettings.javaagentSettings: _*)
  .settings(Dependencies.akka: _*)
  .settings(Assembly.akkaAssemblySettings: _*)