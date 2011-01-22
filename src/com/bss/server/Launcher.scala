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

import com.bss.ccxml._
import com.bss.ccxml.tags._
import scala.xml._
import java.net.URL
import com.bss.telco.jainsip._

class Launcher {
	println("test")
}
object Launcher {

	def main(args:Array[String]) : Unit = {
		val config = new ConfigParser("BlueScaleConfig.xml")
		//TODO: determine what engine to start with... (ccxml, BlueML)
        val telcoServer  = new SipTelcoServer(  config.localIp(), 
                                                config.localPort(), 
                                                config.destIp(),
                                                config.destPort())

        val ws = new WebServer(config.webPort(), 8080, telcoServer, config.callbackUrl())


        /*
		val s = new Server( getCCXML(config.startUrl()),
						config.localIp(),
					   	config.localPort(),
						config.destIp(),
				   		config.destPort())
		*/
		telcoServer.start()
		ws.start()
		println("main here")
	}

	def getCCXML(url:String) : CCXMLDoc = {
	    val filename =  url.indexOf(":") match {
	    	case -1 => url
	    	case _ =>   println("blah, url = " + url + " indexOf = " + url.indexOf(":"))
	    				url.split(":")(1).replace("'", "")
	    	}
		val xml = getXML(url)	
		return (new CCXMLDoc(filename, xml))
	}
	

	def getXML(url:String) : Elem = 
		url.startsWith("http://") match {
			case true => 	val conn = new URL(url).openConnection()
							return XML.load(conn.getInputStream())
			case false =>   return XML.loadFile(url)

		}
		/*
		if ( url.startsWith("file://") )
			return XML.loadFile(url)
		
		if ( url.startsWith("http://") ) {
			val conn = new URL(url).openConnection()
			return XML.load(conn.getInputStream())
		}
		throw new Exception("unable to retrieve file " + url)
		*/
	

}
