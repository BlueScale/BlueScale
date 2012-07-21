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
    
    val localparticipant = RtpParticipant.createReceiver(new RtpParticipantInfo(1), telco.listeningIp, rtpport, rtpport+1)
    
    var effluxSession: Option[SingleParticipantSession] = None
    
    var totalBytesRead = 0
    var totalPacketsread = 0
    private def initRtp(conn:Joinable[_]) {
     	val mc = this
    	val mediaport = SdpHelper.getMediaPort(conn.sdp)
    	val remote1 = RtpParticipant.createReceiver(new RtpParticipantInfo(2), telco.listeningIp, mediaport, mediaport+1)
    	val session1 = new SingleParticipantSession(this.toString, 9, localparticipant, remote1)
    	effluxSession = Some(session1)
    	session1.addDataListener(new RtpSessionDataListener() {
    		def dataPacketReceived(session:RtpSession,  participant:RtpParticipantInfo, packet:DataPacket) {
    			//println("ading media on the receiver, packet sequence number = " + packet.getSequenceNumber())
    			val data = packet.getDataAsArray()
    			MediaFileManager.addMedia(mc, data)
    			totalBytesRead += data.length
    			totalPacketsread += 1
    			
           	}
    	})
    	//println("STARTED THE RTP LISTENER on port" + rtpport + " remotePort =  " + mediaport + " For " + this)
   		session1.init()
    }
    
    override def join(conn:Joinable[_]) = BlueFuture(callback => {
    	//should we only do this when we get a 200 OK? should  we put it in the connect callback? 
    	//get an SDP port
    	initRtp(conn)
    	for (_ <- conn.connect(this, false)) {
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
    	//KILL THE OLD SESSION AND MAKE A NEW ONE.
    	effluxSession.foreach(_.terminate())
    	joinedTo.foreach( joined => initRtp(joined))
        //println("~~~~~~~~~~~~~~~~~~~~~~~~  joinedMeidaChanged~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~do nothing here?")
    }

    protected[telco] def unjoin() = BlueFuture(callback => {
    	println("total received bytes = " + totalBytesRead)
    	println("total packets received = " + totalPacketsread)
    	MediaFileManager.finishAddMedia(this).foreach(newFile => _recordedFiles = newFile :: _recordedFiles)
    	println(" unjoin, mc = " + this.hashCode() + " files count = " + _recordedFiles.size)
    	callback()
    	unjoinCallback.foreach(_(joinedTo.get,this))
    })

    
    //def receive(frame:DataFrame, participant:Participant)  =
     // MediaFileManager.addMedia(this, frame.getConcatenatedData())
    
    def play(filestream:InputStream) = BlueFuture(f => {
    	val localport = 0
    	for(joined <- joinedTo;
    		session <- effluxSession) {
    		var totalSent = 0
    	   	val bytes = new Array[Byte](512)
    	   	var seq = 1 //TODO: get a random sequence number
    	   	var read = filestream.read(bytes)
             while (read != -1) {
            	 val packet = new DataPacket()
            	 packet.setData(bytes)
            	 packet.setSequenceNumber(seq)
            	 session.sendDataPacket(packet)
            	 read = filestream.read(bytes)
            	 totalSent += read
            	 seq += 1
            	 Thread.sleep(20)
             }
    		println("totalSENT = " + totalSent + " SEQ = " + seq)
    	}
    	f()
    })
    
    	
    override def cancel() = BlueFuture(callback => {
    	callback()
    }) 
    
    override protected[telco] def connect(join:Joinable[_], connectAnyMedia:Boolean ) = BlueFuture(callback => {//doesn't need to be here? 
    	initRtp(join)
    	_joinedTo = Some(join)
    	callback()
	})
    
    override protected[telco] def connect(join:Joinable[_])= connect(join, true)
    
}


object EffluxMediaConnection {
	val myarray = new Array[Byte](2000)	
	val Max = 5000
	val Min = 2000
	
	def getPort(): Int = {
	  val ran = Math.random
	  val ret = Min + (ran * ((Max - Min) + 1))
	  println("returning for getPort = " + ret)
	  ret.asInstanceOf[Int]
	}
	
	def putBackPort(port:Int): Unit = {
			println("fix me")
	}
}