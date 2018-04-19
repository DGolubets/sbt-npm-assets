package ru.dgolubets.sbt.npmassets

import java.io.File

case class NpmAssetsBuilderSettings(scriptName: String,
                                    cwd: File,
                                    source: File,
                                    target: File,
                                    targetInAsync: File,
                                    envVars: Map[String, String],
                                    asyncDev: Boolean,
                                    autoInstall: Boolean)
