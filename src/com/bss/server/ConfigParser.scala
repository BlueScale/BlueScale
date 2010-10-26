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
	
	def localIp() : String = parseTelcoTags("ListeningAddress")
			
	def localPort()  = Integer.parseInt(parseTelcoTags("ListeningPort"))

	def destIp() : String = parseTelcoTags("DestAddress")

	def destPort() = Integer.parseInt(parseTelcoTags("DestPort"))

	private def parseTelcoTags(tag:String) : String = ((nodes \\ tag) \ "@value").text

	def startUrl() : String = parseTelcoTags("StartingDoc")
	
}
