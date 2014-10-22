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
package org.beangle.tomcat.configurer.model

import org.beangle.commons.lang.Numbers.toInt

object Container {
  def apply(xml: scala.xml.Elem): Container = {
    val conf = new Container
    conf.version = (xml \ "@version").text

    (xml \ "farm").foreach { farmElem =>
      val farm = new Farm((farmElem \ "@name").text)
      farm.jvmopts = (farmElem \ "jvm" \ "@opts").text

      (farmElem \ "http") foreach { httpElem =>
        val http = new HttpConnector
        readConnector(httpElem, http)
        readHttpAndAjpConnector(httpElem, http)
        if (!(httpElem \ "@disableUploadTimeout").isEmpty) http.disableUploadTimeout = (httpElem \ "@disableUploadTimeout").text == "true"
        if (!(httpElem \ "@connectionTimeout").isEmpty) http.connectionTimeout = toInt((httpElem \ "@connectionTimeout").text)
        if (!(httpElem \ "@compression").isEmpty) http.compression = (httpElem \ "@compression").text
        if (!(httpElem \ "@compressionMinSize").isEmpty) http.compressionMinSize = toInt((httpElem \ "@compressionMinSize").text)
        if (!(httpElem \ "@compressionMimeType").isEmpty) http.compressionMimeType = (httpElem \ "@compressionMimeType").text
        farm.http = http
      }

      (farmElem \ "ajp") foreach { httpElem =>
        val ajp = new AjpConnector
        readConnector(httpElem, ajp)
        readHttpAndAjpConnector(httpElem, ajp)
        farm.ajp = ajp
      }

      (farmElem \ "server") foreach { serverElem =>
        val server = new Server((serverElem \ "@name").text, toInt((serverElem \ "@shutdownPort").text))
        server.httpPort = toInt((serverElem \ "@httpPort").text)
        server.httpsPort = toInt((serverElem \ "@httpsPort").text)
        server.ajpPort = toInt((serverElem \ "@ajpPort").text)
        farm.servers += server
      }
      conf.farms += farm
    }

    (xml \ "webapp").foreach { webappElem =>
      conf.webapp.base = (webappElem \ "@base").text
      (webappElem \ "context").foreach { contextElem =>
        val context = new Context((contextElem \ "@path").text)
        context.reloadable = (contextElem \ "@path").text == "true"
        context.runAt = (contextElem \ "@runAt").text
        (contextElem \ "datasource").foreach { dsElem =>
          val ds = new DataSource((dsElem \ "@name").text)
          ds.driver = (dsElem \ "@driver").text
          ds.username = (dsElem \ "@username").text
          ds.url = (dsElem \ "@url").text
          for ((k, v) <- (dsElem.attributes.asAttrMap -- Set("name", "driver", "url", "username"))) {
            ds.properties.put(k, v)
          }
          context.dataSources += ds
        }
        conf.webapp.contexts += context
      }
    }
    conf
  }

  private def readConnector(xml: scala.xml.Node, connector: Connector) {
    if (!(xml \ "@protocol").isEmpty) connector.protocol = (xml \ "@protocol").text
    if (!(xml \ "@URIEncoding").isEmpty) connector.URIEncoding = (xml \ "@URIEncoding").text
    if (!(xml \ "@redirectPort").isEmpty) connector.redirectPort = Some(toInt((xml \ "@redirectPort").text))
    if (!(xml \ "@enableLookups").isEmpty) connector.enableLookups = (xml \ "@enableLookups").text == "true"
  }

  private def readHttpAndAjpConnector(xml: scala.xml.Node, connector: HttpAndAjp) {
    if (!(xml \ "@acceptCount").isEmpty) connector.acceptCount = toInt((xml \ "@acceptCount").text)
    if (!(xml \ "@maxThreads").isEmpty) connector.maxThreads = toInt((xml \ "@maxThreads").text)
    if (!(xml \ "@maxConnections").isEmpty) connector.maxConnections = Some(toInt((xml \ "@maxConnections").text))
    if (!(xml \ "@minSpareThreads").isEmpty) connector.minSpareThreads = toInt((xml \ "@minSpareThreads").text)
  }
}
class Container {

  var version = "7.0.50"

  val farms = new collection.mutable.ListBuffer[Farm]

  val webapp = new Webapp

  def farmNames: Set[String] = farms.map(f => f.name).toSet

  def dataSources: Map[String, DataSource] = {
    val datasources = new collection.mutable.HashMap[String, DataSource]
    for (context <- webapp.contexts) {
      farms.find(f => f.name == context.runAt).foreach { farm =>
        for (ds <- context.dataSources) datasources += (ds.name -> ds)
      }
    }
    datasources.toMap
  }

  def ports: List[Int] = {
    val ports = new collection.mutable.HashSet[Int]
    for (farm <- farms; server <- farm.servers) {
      if (server.httpPort > 0) ports += server.httpPort
      if (server.httpsPort > 0) ports += server.httpsPort
      if (server.ajpPort > 0) ports += server.ajpPort
    }
    ports.toList.sorted
  }

  def httpPorts: Set[Int] = {
    val httpPorts = new collection.mutable.HashSet[Int]
    for (farm <- farms; server <- farm.servers) {
      if (server.httpPort > 0) httpPorts += server.httpPort
    }
    httpPorts.toSet
  }
  def httpsPorts: Set[Int] = {
    val httpsPorts = new collection.mutable.HashSet[Int]
    for (farm <- farms; server <- farm.servers) {
      if (server.httpsPort > 0) httpsPorts += server.httpsPort
    }
    httpsPorts.toSet
  }

  def ajpPorts: Set[Int] = {
    val ajpPorts = new collection.mutable.HashSet[Int]
    for (farm <- farms; server <- farm.servers) {
      if (server.ajpPort > 0) ajpPorts += server.ajpPort
    }
    ajpPorts.toSet
  }
}