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

package com.bss.telco.jainsip

import java.net.InetAddress;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sdp.MediaDescription;
import java.util.Vector

import com.bss.telco.jainsip._
import com.bss.telco.api._
import com.bss.telco._
import scala.collection.JavaConversions._

class B2BServer(ip:String, port:Int, destIp:String, destPort:Int) {
  
	var portCounter = 0; 
  
	private val b2bTelcoServer    = new SipTelcoServer(ip, port, destIp, destPort)

	private var doNotAnswer = Set[String]()
 
	val sdpFactory = SdpFactory.getInstance()
     
	def getPort() : Int = { 
		  portCounter = portCounter+1;
		  portCounter
    }
    
     
    def createConnection(dest:String, callerid:String) : SipConnection = {
        val conn =	b2bTelcoServer.createConnection(dest, callerid)
        conn.asInstanceOf[JainSipConnection].localSdp = getFakeSdp(ip)
        return conn
    }
  
  
	def handleIncoming(conn:SipConnection) : Unit = { 
		//set the joinedTo.get.sdp
		println("HANDLE INCOMING ")
		if (doNotAnswer.contains(conn.destination))
		    return
		    
	  	SdpHelper.addMediaTo(conn.asInstanceOf[JainSipConnection].sdp, getFakeSdp(ip))
		conn.accept(()=> println("b2bServer accepted call to " + conn.destination ) );
	}

    def findConnByDest(dest:String) : Option[SipConnection] = 
        b2bTelcoServer.connections.values.find( conn => if (conn.destination == dest) true else false)
            
   
   def getFakeSdp(ip:String) : SessionDescription = {
		val sd =  sdpFactory.createSessionDescription()
		sd.setOrigin(sdpFactory.createOrigin("bss", 13760799956958020L, 13760799956958020L, "IN", "IP4", ip))
		sd.setSessionName(sdpFactory.createSessionName("bssession"))
		sd.setConnection(sdpFactory.createConnection("IN", "IP4", ip))
				
		val md = sdpFactory.createMediaDescription("audio", getPort(), 1, "RTP/AVP", new Array[Int](1) ) //need a 0 tacked onto the end for the RTP stuff
		sd.getMediaDescriptions(true).asInstanceOf[Vector[MediaDescription]].
		add(md)
		return sd
	}           
    
    def addNoAnswer(number:String) : Unit = 
        doNotAnswer += number

  
 	def start() : Unit = {
 	    println("Test Server STARTED")
 	    b2bTelcoServer.setIncomingCallback(handleIncoming);
 	    b2bTelcoServer.start()
    }

	def stop() : Unit =
		b2bTelcoServer.stop()
 	
  	
  	def handleConnected() = {
		println("connected...now we play the waiting game....")
	}
}


