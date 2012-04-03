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

package org.bluescale.telco.jainsip

import java.net.InetAddress;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sdp.MediaDescription;


import java.util.Vector

import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._
import org.bluescale.telco._
import scala.collection.JavaConversions._
import org.bluescale.telco.media.jlibrtp._
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic._

class B2BServer(ip:String, port:Int, destIp:String, destPort:Int) {
  
	var portCounter = 0 
 
    var ringSome = false
    
    var answerWithMedia = false

    var simulateCellVM = false

    var cellMap = new ConcurrentHashMap[String,AtomicInteger]()
    
	private val b2bTelcoServer    = new SipTelcoServer(ip, port, destIp, destPort)
	
	//Map of incoming phone number to Media connection it's joined wiht
	private val mediaConnmap = new ConcurrentHashMap[String, MediaConnection]()

	private var ignore = Set[String]()
 
	val sdpFactory = SdpFactory.getInstance()
     
	def getPort() : Int = { 
		  portCounter = portCounter+1;
		  portCounter
    }
   
    def createConnection(dest:String, callerid:String) : SipConnection = {
        val conn =	b2bTelcoServer.createConnection(dest, callerid)
        conn.asInstanceOf[SipConnectionImpl].sdp = getFakeSdp(ip)
        return conn
    }
  
	def handleIncoming(conn:SipConnection) : Unit = { 
		//set the joinedTo.get.sdp
        if (simulateCellVM) {
            cellMap.putIfAbsent(conn.destination,new AtomicInteger(3))
            val ringsleep = cellMap.get(conn.destination).getAndDecrement()
            Thread.sleep(500*ringsleep) 
        }


		if (ignore.contains(conn.destination))
		    return

		if (ringSome) { 
		    conn.ring( getFakeJoinable(ip))
		    Thread.sleep(1000)
		}
		
		answerWithMedia match {
		  case true		=>
		    	val mediaConn = new JlibMediaConnection(b2bTelcoServer)
		    	mediaConnmap.put(conn.destination, mediaConn)
		    	conn.accept(mediaConn, ()=> println("b2bserver accepted call with medai support to " + conn.destination))
		  case false	=>	conn.accept(getFakeJoinable(ip), ()=> Unit)//println("b2bServer accepted call to " + conn.destination ) )
		}
	}
	
	def getMediaConnection(phonedest:String) : MediaConnection ={
	  val m = mediaConnmap.get(phonedest)
	  return m
	}
	
    def findConnByDest(dest:String) : Option[SipConnection] = 
        b2bTelcoServer.connections.values.find( conn => if (conn.destination == dest) true else false)
            
    def getFakeJoinable(ip:String) : Joinable[_] = new SdpJoinable(Some(getFakeSdp(ip))) 

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
    
    def addIgnore(number:String) : Unit = 
        ignore += number

  
 	def start() : Unit = {
 	    println("Test Server STARTED")
 	    b2bTelcoServer.setIncomingCallback(handleIncoming);
 	    b2bTelcoServer.start()
    }

	def stop() : Unit = {
		b2bTelcoServer.stop()
        ignore = Set[String]()
	}
 	
  	
  	def handleConnected() = {
		println("connected...now we play the waiting game....")
	}
}

