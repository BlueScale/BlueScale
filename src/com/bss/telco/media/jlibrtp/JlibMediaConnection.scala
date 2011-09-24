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

package com.bss.telco.media.jlibrtp

import javax.sdp._
import com.bss.telco.api._
import com.bss.telco._
import java.net.DatagramSocket
import jlibrtp.RTPSession
import jlibrtp.RTPAppIntf
import jlibrtp.DataFrame
import jlibrtp.Participant
import java.util.concurrent.ConcurrentHashMap

class JlibMediaConnection(telco:TelcoServer) extends MediaConnection {

    private var listeningSdp = SdpHelper.getBlankSdp(telco.contactIp)
    
    private var _joinedTo:Option[Joinable[_]] = None
    
    private var files = List[String]()
    
    var rtpSession:Option[RTPSession] = None
    
    private var connState = UNCONNECTED()
    
    override def joinedTo = _joinedTo
    
    override def join(connection:Joinable[_], f:()=>Unit) {
    		
    }
	
	override def recordedFiles = files
    
    override def sdp = 
      listeningSdp
    
    def connectionState = connState

    def joinPlay(url:String, joinable:Joinable[_], f:()=>Unit) {
       //get SDP info for incoming data. 
       joinable.connect(this, false, ()=> {
           this._joinedTo = Some(joinable) 
    	   	//reconnect should not take an SDP, should take a joinable...and jsut connect with the SDP from it. 
           	play(url, ()=>println("PLAYING"));
            //now take the other all's SDP and lets make sure we're listening to that.  now we can playw
            })
    }
    
    def receive(frame:DataFrame, participant:Participant)  =
      MediaFileManager.addMedia(this, frame.getConcatenatedData())
    
      
    def getRtpSockets() : (DatagramSocket,DatagramSocket) = {
      val i = 12345
      return (new DatagramSocket(i), new DatagramSocket(i+1))
    }
    
    def initListen() {
    	val rtpPort = getRtpSockets()
    	rtpSession = Some(new RTPSession(rtpPort._1, rtpPort._2))
    	listeningSdp = SdpHelper.createSdp(rtpPort._1.getLocalPort(), telco.contactIp)
    	rtpSession.foreach( session => {
    		session.naivePktReception(true);
    		session.RTPSessionRegister( new RTPAppIntf {
    			override def receiveData(frame:DataFrame, participant:Participant) =
    			  receive(frame, participant)
    			  
    			override def userEvent(etype:Int, participants:Array[Participant]) =
    			 	Unit
    			    
    			override def frameSize(payloadType:Int) = 1
    		},null, null);
    	})
    }
    
    override def play(url:String, f:()=>Unit) =
    	joinedTo.foreach( joined => {
    	  //fixme, do we need listening ports to be in the RTPSession?
    		val rtpSockets = this.getRtpSockets()
    		val rtpSession = new RTPSession(rtpSockets._1,rtpSockets._2)	//RTCP
    		rtpSession.addParticipant(new Participant("",
    		   SdpHelper.getMediaPort(joined.sdp), 		//RTP
    		   SdpHelper.getMediaPort(joined.sdp)+1)) 	//RTCP
    		val inputStream = MediaFileManager.getInputStream(url)
    		//TODO: send the packets
    		   
    	})
    
    override def cancel(f:()=>Unit) {
      
    }

    //PROTECTED STUFF FOR JOINABLE
    override protected[telco] def connect(join:Joinable[_], connectedCallback:()=>Unit) {
    	//store SDP somewhere
    	connectedCallback()
    }

    override protected[telco] def connect(join:Joinable[_], connectAnyMedia:Boolean, connectedCallback:()=>Unit) {//doesn't need to be here? 
    	connectedCallback()
	}
    
    protected[telco] def onConnect(f:()=>Unit) = f() //more to do? 

    protected[telco] def unjoin(f:()=>Unit) = {
      	files = MediaFileManager.finishAddMedia(this) :: files
    	f()
    	unjoinCallback.foreach(_(joinedTo.get,this))
    }
}

object JlibMediaConnection {
	//phone nunew mber
	def mediaConnections = new ConcurrentHashMap[String, JlibMediaConnection]()

}





	
