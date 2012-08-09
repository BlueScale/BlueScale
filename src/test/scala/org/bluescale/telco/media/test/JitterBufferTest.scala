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

package org.bluescale.telco.media.test

import org.bluescale.telco.media.JitterBuffer

import org.junit._
import Assert._
import org.bluescale.telco.media._
import org.bluescale.telco.api._
import org.bluescale.telco.jainsip.unittest.FunTestHelper
import java.util.concurrent.CountDownLatch
import scala.io.Source
import org.scalatest.FunSuite

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.TimeUnit
import java.io.FileInputStream
import java.util.Arrays
import java.util.Random
import java.util.Timer
import java.util.TimerTask
import java.util.Date
import scala.collection.mutable.LinkedList
import scala.collection.mutable.ArrayBuffer
import com.biasedbit.efflux.packet._
import java.io.ByteArrayInputStream
import java.util.Arrays

@RunWith(classOf[JUnitRunner])
class JitterBufferTest extends FunSuite {
	
  val randombytes = new Array[Byte](3200)
	new Random().nextBytes(randombytes)
	
	val finalbytes = new ArrayBuffer[Byte]()
	
	def handleData(bytes:Array[Byte]) = 
	  	finalbytes.appendAll(bytes)
		//bytes.foreach( b => finalbytes = finalbytes :+ b)
	  
  
	test("Testing the ordering of random bytes") {
		val latch = new CountDownLatch(1)
		val jitterbuffer = new JitterBuffer(8000, 160, handleData)
		val b = randombytes.toList
		var currentTimestamp = new Date().getTime()
		var seq = 0
		val read = new Array[Byte](160)
		val delay = 20
		println("randomBytes length= " + randombytes.length)
		val stream = new ByteArrayInputStream(randombytes)
		val timer = new Timer()
		val timerTask = new TimerTask{
			def run() {
				stream.read(read) match {
				  	case -1 =>
				    	Thread.sleep(5000)
				    	timer.purge()
				    	latch.countDown()
				  	case _ => 
				  	  	seq += 1
				  	  	currentTimestamp += seq*delay*8
				  		val packet1 = new DataPacket()
				  		packet1.setData(read.clone())
				  		packet1.setTimestamp(currentTimestamp)
				  		packet1.setSequenceNumber(seq)
				  		if (seq % 3 == 0  && stream.read(read) != -1) { 
				  			currentTimestamp += seq*delay*8
				  			seq += 1
				  			val packet2 = new DataPacket()
				  			packet2.setData(read.clone())
				  			packet2.setTimestamp(currentTimestamp)
				  			packet2.setSequenceNumber(seq)
				  			jitterbuffer.addToQueue(packet2)
				  		}
				  	  	println("adding packet to the queue with sequence = " + packet1.getSequenceNumber())
				  	  	jitterbuffer.addToQueue(packet1)
				}
			}
		}
		println("scheduling the timer task!")
		timer.schedule(timerTask, 0, 130) //packets are coming in a littel faster than they need to, we just want to test reordering here
		latch.await()
		
		val finalarr = finalbytes.toArray
		println(finalarr.length)
		//for (i<- 0 to 10) {
		//	println("randomData = " + randombytes(320+i)+ " | " + finalarr(320+i))
			
		//}
		println("source array lenght = " + randombytes.length)
		println("result array = " + finalbytes.toArray.length)
		val res = Arrays.equals(randombytes, finalbytes.toArray)
		println("result = " + res)
		assert(res)
		
	}
	
	
}
