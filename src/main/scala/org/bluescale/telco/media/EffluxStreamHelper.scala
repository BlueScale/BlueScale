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
* Copyright Vincent Marquez 2012
* 
* 
* Please contact us at www.BlueScale.org
*
*/

package org.bluescale.telco.media

import com.biasedbit.efflux.packet.DataPacket
import com.biasedbit.efflux.participant.RtpParticipant
import com.biasedbit.efflux.participant.RtpParticipantInfo
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.Queue
import java.util.Timer
import java.util.TimerTask
import com.biasedbit.efflux.session.RtpSession
import akka.actor.ActorSystem
import akka.actor.Cancellable
import akka.util.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.ThreadPoolExecutor

object EffluxStreamHelper {

	def scheduler = ActorSystem.create().scheduler
  
    private def tryGetPackets(count:Int, filestream:InputStream, queue:Queue[Array[Byte]]): Option[Array[Byte]] = {
    	val data = new Array[Byte](160)
    	var read = 0
    	if (queue.size() == 0) {
    		(1 to count)
    			.toStream
    			.takeWhile(c => filestream.read(data) != -1)
    			.foreach(c => queue.add(data.clone()))
    	}
    	return Option(queue.poll())	
    }
    
    def makePacket(data:Array[Byte], info:RtpStreamInfo, payloadType:Int): DataPacket = { 
    	val packet = new DataPacket()
    	packet.setPayloadType(payloadType)
       	packet.setData(data)
       	packet.setSequenceNumber(info.sequence)
       	packet.setTimestamp(info.startingtimestamp+(info.sequence*info.delay*8)) //justin karnegas figured this bug out!
       	packet
    }
	
    def streamMedia(filestream:InputStream, session:RtpSession, info:RtpStreamInfo, f:()=>Unit): ()=>Unit = {
		val queue = new LinkedBlockingQueue[Array[Byte]]()
		filestream.skip(100)
		val timer = new Timer()
		var cancellable:Option[Cancellable] = None
		
		var i = 0
		val timerTask = new TimerTask() {
			def run() {
			  tryGetPackets(50, filestream, queue) match {
			  	case Some(data) =>
			  		session.sendDataPacket(makePacket(data, info, info.payloadType))
			  	case None=>
			  		println("cancelling")
			  		timer.cancel()
			  		f()
			  }
			}
		}
		
		timer.scheduleAtFixedRate(timerTask,0, 20)
		/*
		scheduler.schedule(Duration.Zero, Duration.create(20, TimeUnit.MILLISECONDS)){
			println("running a scheduled task! i ="+ i)
			i+=1
			tryGetPackets(10, filestream, queue) match {
			  case Some(data) =>
			    session.sendDataPacket(makePacket(data, info, info.payloadType))
			  case None=>
			    println("HEEEERE")
			    cancellable.foreach(_.cancel)
			    f()
			}
		}
		*/
		
    	return ()=>timer.cancel()
	}
  
}
 