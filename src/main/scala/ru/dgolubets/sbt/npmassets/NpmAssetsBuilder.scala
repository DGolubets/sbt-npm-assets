package ru.dgolubets.sbt.npmassets

/**
  * NPM build manager
  */
class NpmAssetsBuilder(settings: NpmAssetsBuilderSettings){

  private val npm: Npm = new Npm()

  def run(isDevMode: Boolean = false): Unit = {
    if (npm.isRunning) {
      // async process is still running
    }
    else {
      npm.stop()
      npm.start(settings.scriptName, settings.cwd, environmentVariables(isDevMode))

      if(!isDevMode || !settings.asyncDev) {
        val exitCode = npm.waitFor()
        if(exitCode != 0){
          sys.error(s"NPM exit with code: $exitCode")
        }
      }
    }
  }

  def stop(): Unit = {
    npm.stop()
  }

  private def environmentVariables(isDevMode: Boolean) = {
    Map(
      "SOURCE_DIR" -> settings.source.getAbsolutePath,
      "TARGET_DIR" -> settings.target.getAbsolutePath,
      "DEV_MODE" -> isDevMode.toString
    ) ++ settings.envVars
  }
}