package ru.dgolubets.sbt.npmassets.util

import ru.dgolubets.sbt.npmassets.util.OsKind.OsKind

case class OsInfo(kind: OsKind)

object OsInfo {
  def current: OsInfo = {
    val os = sys.props("os.name").toLowerCase
    if(os.contains("windows")){
      OsInfo(OsKind.Windows)
    } else {
      OsInfo(OsKind.Linux)
    }
  }
}