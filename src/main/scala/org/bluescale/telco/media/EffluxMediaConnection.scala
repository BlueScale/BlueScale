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

package org.bluescale.telco.media

import javax.sdp._

import org.bluescale.telco.SdpHelper;
import org.bluescale.telco.api._
import org.bluescale.telco._
import org.bluescale.util.BlueFuture
import java.io.InputStream
import com.biasedbit.efflux._
import com.biasedbit.efflux.packet.DataPacket
import com.biasedbit.efflux.participant.RtpParticipant
import com.biasedbit.efflux.participant.RtpParticipantInfo
import com.biasedbit.efflux.session._

class EffluxMediaConnection(telco:TelcoServer) extends MediaConnection {
  
    private var _joinedTo:Option[Joinable[_]] = None
    
    private var _recordedFiles = List[String]()
    private var _playedFiles   = List[String]()
    
    //private var 
    
    override def playedFiles = _playedFiles 
    
	override def recordedFiles = _recordedFiles

	private var connState = UNCONNECTED()
    
    override def joinedTo = _joinedTo
    
    val rtpport = EffluxMediaConnection.getPort()
    
    val listeningSdp = SdpHelper.createSdp(rtpport, telco.contactIp)
    
    override def join(conn:Joinable[_]) = BlueFuture(callback => {
    	//get an SDP port
    	for (_ <- conn.connect(this, false)) {
    		println(" conn " + conn + " Is now Reconnected and listening to the Media's CONNECTION INFO") 
    		this._joinedTo = Some(conn)
    		callback()
    	}
    })
  
    override def sdp = 
      listeningSdp
    
    def connectionState = connState

    def joinPlay(filestream:InputStream, conn:Joinable[_]) = BlueFuture( callback => { 
    	for(_ <- join(conn);
    		_ <- play(filestream))
    	  callback()
    })
    
    override def joinedMediaChange() {
        println("do nothing here?")
    }

    protected[telco] def unjoin() = BlueFuture(callback => {
    	finishListen()
    	println(" unjoin, mc = " + this.hashCode() + " files count = " + _recordedFiles.size)
    	callback()
    	unjoinCallback.foreach(_(joinedTo.get,this))
    })

    
    //def receive(frame:DataFrame, participant:Participant)  =
     // MediaFileManager.addMedia(this, frame.getConcatenatedData())
    
    def play(filestream:InputStream) = BlueFuture(f => {
    	val localport = 0
    	joinedTo.foreach( joined => {
    		/*
    		 * 
        	RtpParticipant local1 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), "127.0.0.1", 6000, 6001);
        	RtpParticipant remote1 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), "127.0.0.1", 7000, 7001);
        	this.session1 = new SingleParticipantSession("Session1", 8, local1, remote1);
    		 */
    		val mediaport = SdpHelper.getMediaPort(joined.sdp)
    		val local1 = RtpParticipant.createReceiver(new RtpParticipantInfo(1), telco.listeningIp, localport, localport+1)
    		val remote1 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), telco.listeningIp, mediaport, mediaport+1)
    		val session1 = new SingleParticipantSession("Session1", 9, local1, remote1)
    		session1.addDataListener(new RtpSessionDataListener() {
    			def dataPacketReceived(session:RtpSession,  participant:RtpParticipantInfo, packet:DataPacket) {
    				//System.err.println("Session 1 received packet: " + packet + "(session: " + session.getId() + ")");
    				//latch.countDown();
             	}
             })
             val bytes = new Array[Byte](1024)
             while (filestream.read(bytes) != -1) {
            	 val packet = new DataPacket()
            	 session1.sendDataPacket(packet)
             }
    	})
    	println("done playing")
    })
    
    private def finishListen() =
    	MediaFileManager.finishAddMedia(this).foreach(newFile => _recordedFiles = newFile :: _recordedFiles)
    
    override def cancel() = BlueFuture(callback => {
    	callback()
    }) 
    
     override protected[telco] def connect(join:Joinable[_], connectAnyMedia:Boolean ) = BlueFuture(callback => {//doesn't need to be here? 
    	
      callback()
	})
    
    override protected[telco] def connect(join:Joinable[_])= connect(join, true)
    
}


object EffluxMediaConnection {
	val myarray = Array[Byte]()	
  
	
	def getPort(): Int = {
	  return 1234//fixme: walk the array, etc.
	}
	
	def putBackPort(port:Int): Unit = {
			println("fix me")
	}
}