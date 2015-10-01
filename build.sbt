lazy val root = Project("root", file("."))
  .aggregate(gpio)
  .settings(BaseSettings.settings: _*)

lazy val gpio = Project("gpio", file("gpio"))
  .settings(BaseSettings.settings: _*)
  .settings(Dependencies.gpio: _*)
  .settings(Assembly.gpioAssemblySettings: _*)