/*
*  
* This file is part of BlueScale.
*
* BlueScale is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* BlueScale is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with BlueScale.  If not, see <http://www.gnu.org/licenses/>.
* 
* Copyright Vincent Marquez 2010
* 
* 
* Please contact us at www.BlueScaleSoftware.com
*
*/
package com.bss.server

import scala.xml._
import scala.collection.mutable.ListBuffer 
import scala.collection.immutable._
import org.mozilla.javascript.Context

class ConfigParser(val filename:String) {

	private val nodes = XML.loadFile(filename)
	
	private def parseTags(tag:String) : String = ((nodes \\ tag) \ "@value").text
	
	def localIp() : String = parseTags("ListeningAddress")
			
	def localPort()  = Integer.parseInt(parseTags("ListeningPort"))

	def destIp() : String = parseTags("DestAddress")

	def destPort() = Integer.parseInt(parseTags("DestPort"))

	def startUrl() : String = parseTags("StartingDoc")

	def webPort()  = Integer.parseInt(parseTags("WebPort"))

	def webIP() : String = parseTags("WebIP")

	def callbackUrl() : String = parseTags("CallbackUrl")

	def isB2BTestServer() : String = parseTags("B2BTestServer")

	def contactIp() : String = parseTags("ContactAddress")

}
