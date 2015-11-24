lazy val root = Project("root", file("."))
  .aggregate(actor)

lazy val common = Project("common", file("common"))
  .settings(BaseSettings.settings: _*)
  .settings(Dependencies.common: _*)

lazy val actor = Project("actor", file("actor"))
  .dependsOn(common)
  .settings(BaseSettings.javaagentSettings: _*)
  .settings(Dependencies.actor: _*)
  .settings(Assembly.actorAssemblySettings: _*)