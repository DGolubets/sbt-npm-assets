# sbt-npm-assets
## Installing
Modify plugins.sbt:
```scala
addSbtPlugin("com.commodityvectors" % "sbt-marathon" % "0.2.0")
```
## Example
```scala
import com.commodityvectors.sbt.marathon.model.MarathonApp
lazy val root = (project in file("."))
  .enablePlugins(MarathonPlugin)
    .settings(
      packageName in Docker := "test_app_image",
      dockerRepository in Docker := "docker-registry.loc",
      
      marathonEndpoint := "http://marathon.loc:8080",
      marathonApps := List(
        MarathonApp(
          "test-app1", 1, 256
        )
      ),
      
      publish := (publish in Marathon).value
    )

```
## Notes
* All settings are required
* Marathon application name should not contain upper case letters