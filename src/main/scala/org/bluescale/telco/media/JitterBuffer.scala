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

import java.util.Comparator
import java.util.concurrent.PriorityBlockingQueue
import java.util.Timer
import java.util.TimerTask
import com.biasedbit.efflux.packet.DataPacket

class PacketComparator extends Comparator[DataPacket] {
	override def compare(t1: DataPacket, t2: DataPacket) =
		t1.getSequenceNumber.compare(t2.getSequenceNumber())
}

object JitterBuffer {
  	def printFirstFive(bytes:Array[Byte]) {
	  	for(i <- 1 to 10)
	  		print(bytes(i))
	  	println("")
  	}

}

class JitterBuffer(clockrate: Int,
					packetsize: Int,
					handleData: Array[Byte]=>Unit ) {
	
	private val buffersize = clockrate/1000 * 40
  
	private var jbsize = packetsize
	
	private var missedPackets = 0L
	
	private var lastsequence = -1L
	
	private var lastTimestamp = 0L
	
	private val queue = new PriorityBlockingQueue[DataPacket](packetsize*20, new PacketComparator())
	
	private val timer = new Timer()
	
	private var receivedData = 0
	//if the packet's timestamp has expired, send it anyway
	//if the packet's sequence is in front, send it 
	//
	private val timerTask = new TimerTask { 
		def run() {
			 queue.take() match {
				case packet if (packet.getTimestamp()-lastTimestamp) > buffersize =>
					takePacket(packet)
				case packet if lastsequence - packet.getSequenceNumber() == -1 =>
					takePacket(packet)
				case packet =>
					//println("ARE WE MISSING A PACKET???? is > than buffsize? = " + ((lastTimestamp-packet.getTimestamp()) + "buffersize = " +  buffersize+",lastTimestamp =  " + lastTimestamp + "| currenttimestamp =" + packet.getTimestamp() +  " | seq =" + packet.getSequenceNumber() + "|" + (lastsequence - packet.getSequenceNumber()))
				    queue.add(packet)//put it back!
					missedPackets += 1 //TOOD: to use at a later date for expanding the buffer size
			}
		}
	}
	println("JitterBuffer runninng, buffersize = " + buffersize)
	timer.scheduleAtFixedRate(timerTask, 0, packetsize)
	
	def takePacket(packet:DataPacket): Unit = {
		receivedData += packet.getDataSize()
		lastsequence = packet.getSequenceNumber()
		lastTimestamp = packet.getTimestamp()
		handleData(packet.getDataAsArray())
	}
	
	def addToQueue(packet:DataPacket) =
	  	queue.add(packet)
	  	
	def cancel() =
	  timer.cancel()
	  
	  

}