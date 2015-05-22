package org.beangle.tomcat.configurer.shell

import java.io.{ File, FileInputStream }

import org.beangle.tomcat.configurer.model.Container
import org.beangle.tomcat.configurer.util.Template

object Gen {

  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      println("Usage: Gen /path/to/config.xml farm.server target")
      return
    }
    val configFile = args(0)
    val container = Container(scala.xml.XML.load(new FileInputStream(new File(configFile))))
    val target = args(1)
    val targetDir = args(2)

    container.farms foreach { farm =>
      if (farm.name == target) {
        Template.generate(container, farm, targetDir)
      } else {
        farm.servers foreach { server =>
          if (target == server.qualifiedName) {
            Template.generate(container, farm, server, targetDir)
          }
        }
      }
    }
  }
}