package ru.dgolubets.sbt.npmassets

import play.sbt.PlayRunHook

class NpmAssetsPlayRunHook(builder: NpmAssetsBuilder, isAsync: Boolean) extends PlayRunHook {

  override def beforeStarted(): Unit = {
    // set dev mode until Play is running
    builder.setDevMode(true)

    if(isAsync) {
      builder.run()
    }
  }

  override def afterStopped(): Unit = {
    if(isAsync) {
      builder.stop()
    }

    builder.setDevMode(false)
  }
}
