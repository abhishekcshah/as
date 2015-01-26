/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2014, Beangle Software.
 *
 * Beangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beangle is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.tomcat.configurer.shell

import java.io.File
import java.net.URL

import scala.Array.canBuildFrom

import org.beangle.commons.io.Files.{ / => / }
import org.beangle.commons.lang.Consoles.{ prompt, readPassword, shell }
import org.beangle.commons.lang.Strings.{ isBlank, isNotEmpty, split, substringAfter, trim }
import org.beangle.commons.lang.SystemInfo
import org.beangle.data.jdbc.script.{ OracleParser, Runner }
import org.beangle.data.jdbc.util.PoolingDataSourceFactory
import org.beangle.tomcat.configurer.model.{ Container, Resource }
import org.beangle.tomcat.configurer.util.DataSourceConfig

object Sql extends ShellEnv {

  var resource: Resource = _

  var sqlDir: String = _

  def main(args: Array[String]) {
    workdir = if (args.length == 0) SystemInfo.user.dir else args(0)
    read()
    sqlDir = workdir + / + "sql" + /
    if (sqlFiles.isEmpty) {
      println("Cannot find sql files in " + sqlDir)
      return
    }
    if (null != container) {
      val datasources = container.resources
      if (datasources.isEmpty) {
        info("Cannot find datasource")
        return
      }

      if (datasources.size == 1) resource = datasources.values.head
      println("Sql executor:help ls exec exit(quit/q)")
      shell({
        val prefix =
          if (null != resource) resource.name
          else "sql"
        prefix + " >"
      }, Set("exit", "quit", "q"), command => command match {
        case "ls" => info()
        case "help" => printHelp()
        case t => {
          if (t.startsWith("use")) use(trim(substringAfter(t, "use")))
          else if (t.startsWith("exec")) exec(trim(substringAfter(t, "exec")))
          else if (isNotEmpty(t)) println(t + ": command not found...")
        }
      })
    } else {
      info("Cannot find config.xml")
    }
  }

  def sqlFiles: Array[String] = {
    val file = new File(sqlDir)
    if (file.exists) {
      val files = for (f <- file.list() if f.endsWith(".sql")) yield f
      files.sorted
    } else Array.empty
  }

  def exec(file: String = null) {
    if (isBlank(file)) {
      println("Usage exec all or exec file1 file2")
    } else {
      var fileName = file
      if (file == "all") fileName = sqlFiles.mkString(" ")
      val urls = new collection.mutable.ListBuffer[URL]
      for (name <- split(fileName, " ")) {
        val f = new File(sqlDir + (if (name.endsWith(".sql")) name else name + ".sql"))
        if (f.exists) {
          urls += f.toURI().toURL()
        } else {
          println("file " + f.getAbsolutePath() + " doesn't exists")
        }
      }

      val runner = new Runner(OracleParser, urls: _*)
      if (null == resource) use(null)
      if (null == resource || urls.isEmpty) {
        println("Execute sql aborted.")
      } else {
        DataSourceConfig.config(resource)
        if (null == resource.password)
          resource.password = readPassword("enter datasource [%1$s] %2$s password:", resource.name, resource.username)

        val properties = resource.properties
        properties.remove("driverClassName")
        properties.remove("username")
        properties.remove("url")
        properties.remove("password")
        val ds = new PoolingDataSourceFactory(resource.driverClassName,
          resource.url, resource.username, resource.password, properties).getObject
        runner.execute(ds, true)
      }
    }
  }

  def printHelp() {
    println("""Avaliable command:
  ls                        print datasource and sql file
  exec [sqlfile1,sqlfile2]  execute simple file
  exec all                  execute all sql file
  help              print this help conent""")
  }

  def use(datasourceName: String = null) {
    val datasources = container.resources
    if (datasources.isEmpty) {
      println("datasource is empty")
    } else {
      if (!isBlank(datasourceName) && datasources.get(datasourceName).isDefined) {
        resource = datasources(datasourceName)
      } else {
        val selectName = prompt("choose datasource name?", null, name => datasources.contains(name))
        resource = datasources(selectName)
      }
    }
  }

  def info() {
    val infos = new collection.mutable.ListBuffer[String]
    var index = 0
    for (ds <- container.resources.values) {
      var prefix = if (ds == resource) "[*] " else "[" + index + "] "
      infos += prefix + ds.toString
    }
    println("Data sources:\n" + ("-" * 50))
    println(infos.mkString("\n"))
    println()
    println("Sql files:\n" + ("-" * 50))
    println(sqlFiles.mkString(" "))
  }
}