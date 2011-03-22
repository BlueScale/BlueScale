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
package com.bss.telco

import javax.sdp.MediaDescription
import javax.sdp.SdpException
import javax.sdp.SdpFactory
import javax.sdp.SdpParseException
import javax.sdp.SessionDescription
import javax.sdp.MediaDescription
 
import java.net.InetAddress
import javax.sdp.MediaDescription
import javax.sdp.SdpException
import javax.sdp.SdpFactory
import javax.sdp.SdpParseException
import javax.sdp.SessionDescription
import javax.sdp.MediaDescription
import java.util.Vector
import com.bss.util._
//FIXME: lets make this a lttle smarter on figuring out what kind of stuff we can transmit

object SdpHelper {

	private val RTP = "RTP/AVP"
  	private val blank_port = 1111
   
  	val sdpFactory = SdpFactory.getInstance() 
    
	 def getBlankSdp(ip:String) : SessionDescription = {
	 	val sdpip = "0.0.0.0"
		val sd =  sdpFactory.createSessionDescription()
		sd.setOrigin(sdpFactory.createOrigin("bss", sd.hashCode(), 1L, "IN", "IP4", ip))
		sd.setSessionName(sdpFactory.createSessionName("bssession"))
		sd.setConnection(sdpFactory.createConnection("IN", "IP4", sdpip))
				
		val md = sdpFactory.createMediaDescription("audio", blank_port, 1, RTP, new Array[Int](1) ) //need a 0 tacked onto the end for the RTP stuff
		sd.getMediaDescriptions(true).asInstanceOf[Vector[MediaDescription]].
		add(md)
		return sd
	 }
    
  	def isBlankSdp(sd: SessionDescription) : Boolean = {
	  if ( sd.getConnection.getAddress() != "0.0.0.0") 
		  return false
   
	  if ( sd.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription].getMedia().getMediaPort() != blank_port)
		  return false
   
	  return true
  	}
         	 
	def getSdp(content:Array[Byte]): SessionDescription = 
	   sdpFactory.createSessionDescription(new String(content, "utf-8"))
	 
  
	def addMediaTo(destination:SessionDescription, source:SessionDescription) : Unit = {
		val mediaSrc =  source.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription]
		 
		destination.setConnection(source.getConnection()) 
		destination.getMediaDescriptions(true).asInstanceOf[Vector[MediaDescription]].clear() 
		
		val mediadst = sdpFactory.createMediaDescription(mediaSrc.getMedia().getMediaType, 
                                                   			mediaSrc.getMedia().getMediaPort, 
                                                   			mediaSrc.getMedia().getPortCount(), 
                                                   			mediaSrc.getMedia().getProtocol(),
                                                   			new Array[Int](1))
        destination.getMediaDescriptions(false).asInstanceOf[Vector[MediaDescription]].add(mediadst)
		destination.setConnection(source.getConnection())
		destination.getOrigin().setSessionVersion( destination.getOrigin().getSessionVersion()+1)
	}

	def getMediaUrl(sdp:SessionDescription) : String = {
		val mediaSrc =  sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription]
		val listeningPort =	sdp.getMediaDescriptions(false).asInstanceOf[Vector[MediaDescription]].get(0).getMedia.getMediaPort()
		return 	mediaSrc.getMedia().getProtocol() match {
			case RTP => "rtp://"+mediaSrc.getConnection().getAddress()+":"+ listeningPort
			case _ => throw new ProtocolNotSupportedException( mediaSrc.getMedia().getProtocol())
		}
	}

	class ProtocolNotSupportedException(str:String) extends Exception
	
}

 
 
 
