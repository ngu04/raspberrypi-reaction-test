lazy val root = Project("root", file("."))
  .aggregate(gpio, rx)

lazy val common = Project("common", file("common"))
  .settings(BaseSettings.settings: _*)
  .settings(Dependencies.common: _*)

lazy val gpio = Project("gpio", file("gpio"))
  .dependsOn(common)
  .settings(BaseSettings.settings: _*)
  .settings(Dependencies.gpio: _*)
  .settings(Assembly.gpioAssemblySettings: _*)

lazy val rx = Project("rx", file("rx"))
  .dependsOn(common)
  .settings(BaseSettings.settings: _*)
  .settings(Dependencies.rx: _*)
  .settings(Assembly.rxAssemblySettings: _*)
