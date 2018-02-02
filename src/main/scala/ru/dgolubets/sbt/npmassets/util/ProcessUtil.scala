package ru.dgolubets.sbt.npmassets.util

import java.io.File

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.{Kernel32, WinNT}

import scala.collection.JavaConverters._

object ProcessUtil {

  private def getProcessId(p: Process): Int = {
    p.getClass.getName match {
      case "java.lang.UNIXProcess" =>
        val f = p.getClass.getDeclaredField("pid")
        try {
          f.setAccessible(true)
          f.getInt(p)
        } finally {
          f.setAccessible(false)
        }
      case "java.lang.Win32Process" | "java.lang.ProcessImpl" =>
        val f = p.getClass.getDeclaredField("handle")
        try {
          f.setAccessible(true)
          val handle = f.getLong(p)
          val kernel = Kernel32.INSTANCE
          val hand = new WinNT.HANDLE()
          hand.setPointer(Pointer.createConstant(handle))
          kernel.GetProcessId(hand)
        } finally {
          f.setAccessible(false)
        }
      case _ =>
        -1
    }
  }

  def exec(command: String,
           args: Seq[String] = Seq.empty,
           envVars: Map[String, String] = Map.empty,
           cwd: Option[File] = None): Process = {
    val commands = OsInfo.current.kind match {
      case OsKind.Linux =>
        // start process with new session id to kill it all later
        "setsid" :: command :: args.toList
      case OsKind.Windows =>
        "cmd" :: "/C" :: command :: args.toList
    }

    val processBuilder = new ProcessBuilder(commands.asJava)
    for (dir <- cwd) {
      processBuilder.directory(dir)
    }
    processBuilder.environment().putAll(envVars.asJava)
    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    processBuilder.start()
  }

  def kill(p: Process): Unit = {
    val pid = getProcessId(p)
    OsInfo.current.kind match {
      case OsKind.Linux =>
        Runtime.getRuntime.exec(s"pkill -s $pid")
      case OsKind.Windows =>
        Runtime.getRuntime.exec(s"taskkill /pid $pid /t /f")
    }
  }
}
