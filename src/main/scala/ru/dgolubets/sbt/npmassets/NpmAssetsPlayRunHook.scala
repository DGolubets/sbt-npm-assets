package ru.dgolubets.sbt.npmassets

import play.sbt.PlayRunHook

class NpmAssetsPlayRunHook(builder: NpmAssetsBuilder) extends PlayRunHook {

  override def beforeStarted(): Unit = {
    builder.run(true)
  }

  override def afterStopped(): Unit = {
    builder.stop()
  }
}
