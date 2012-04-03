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
* Please contact us at www.BlueScale.org
*
*/
package org.bluescale.server

import scala.xml._
import java.net.URL

import org.bluescale.telco.jainsip.B2BServer
import org.bluescale.telco.jainsip.SipTelcoServer
import org.bluescale.telco.jainsip._
import org.bluescale.telco.jainsip.SipTelcoServer

import org.bluescale.telco.jainsip._

class Launcher {
	println("test")
}
object Launcher {

	def main(args:Array[String]) : Unit = {
	    //TODO: take an arg for the config file.  
		val config = new ConfigParser("BlueScaleConfig.xml")

	
         config.isB2BTestServer() match {
            case "true"  =>
                val b = new B2BServer(  config.localIp(), 
                                        config.localPort(), 
                                        config.destIp(),
                                        config.destPort())
                b.start()

            case "false" => 
                	//TODO: determine what engine to start with... (ccxml, BlueML)
                val telcoServer  = new SipTelcoServer(  config.localIp(), 
                                                    config.contactIp(),
                                                    config.localPort(), 
                                                    config.destIp(),
                                                    config.destPort())
                
                val ws = new WebServer(config.webPort(), 8080, telcoServer, config.callbackUrl())

		        telcoServer.start()
		        ws.start()
		        println("Please make sure to set your BlueScaleConfig.xml")
		        println("Call REPL:")
		        val repl = new CallRepl(telcoServer)
                while (true) {
                    println( repl.evalLine(readLine()) )
                }
        
        }   
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
