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
package com.bss.ccxml

import scala.collection.mutable._
import scala.actors.Actor
import scala.actors.Actor._
import java.util.concurrent.ConcurrentHashMap
import com.bss.ccxml.tags._
import com.bss.ccxml.event._
import com.bss.telco.api._
import com.bss.telco.jainsip._
import com.bss.telco._
import com.bss.util._

//TODO:rename to ccxmlServer, take in a telcoserver
class Server(rootdoc:CCXMLDoc,
			ip:String, 
			port:Int, 
			destip:String, 
			destPort:Int) {
	
	protected val telcoServer = new SipTelcoServer(ip, port, destip, destPort)
		 
	val callSessionMap = new ConcurrentHashMap[String, Session]
                                            
	val ccxmlMap = new HashMap[String, CCXMLDoc] //filename, doc
	
	var sessionIdCount = 0
 
	private var connections = List[SipConnection]()
    
	private val connMap = new ConcurrentHashMap[String, SipConnection]
   
	var log = (s:String)=>{ println(s) }

	def start() {
		println("CCXML SERVER.start()")
		telcoServer.setIncomingCallback(incomingCallback  )
		//test
		//println("whaaaa")
		//println("BLAAAAAH")
		telcoServer.setDisconnectedCallback((s:SipConnection)=>{ callSessionMap.get(s.connectionid) ! new ConnectionDisconnected("Hung Up",s) })
	  	telcoServer.start()
	}

	def stop() {
		telcoServer.stop()
	}
	
  	def createConnection(destPhone:String, 
						callerId:String,
						disconnectCallback:(SipConnection=>Unit)
					) = telcoServer.createConnection(destPhone, callerId)
	
						
	def setupConn(conn:SipConnection) : Unit = {
	  
	}
   
	def incomingCallback(conn:SipConnection) : Unit = {

		val session = new Session(this, 1)
		println("in ccxmlserver IncomingCallback!")
		setupConn(conn)
		callSessionMap.put(conn.connectionid, session)
		session.setActiveDoc(rootdoc)
		session ! new ConnectionAlerting(conn)
		 //TODO: Handle Progressing!!! 
	}
 
    def addConnection(conn:SipConnection) {
      connections = List(conn) ::: connections 
      connMap.put(conn.connectionid, conn)
    }
    
    def findConnection(connid:String) = telcoServer.findConnection(connid)
    
       
    val loadHandler: Actor = actor {
     loop {
		  react {
		    case event:LoadDocument =>
		      			 val session = new Session(this,1)
		    			 //callSessionMap.put(event.connectionid, session) //FIXME: should we associate it for every time? 
		    			 session.setActiveDoc(rootdoc)
		    	
			  			
		
		  case _ => println("Unrecoginzed Command sent to the Server")
		  } 
	  }
 	}
}
