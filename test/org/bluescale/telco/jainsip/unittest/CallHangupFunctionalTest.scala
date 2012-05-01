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
package org.bluescale.telco.jainsip.unittest

import org.junit._
import Assert._
import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._
import org.bluescale.telco._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class CallHangupFunctionalTest extends TestHelper {
	
	
	@Test
	def testSimpleConn() = {
		println("running");
		assertEquals("blah", "blah")
		runConn()

		getLatch.await()
	}
	
	var latch:CountDownLatch = null
  
	def getLatch = latch
	
  
	def runConn() {
 		latch = new CountDownLatch(1)
 		val destNumber = "9495557777" 
 		val alice = telcoServer.createConnection(destNumber, "4445556666")
 		 
 		alice.connect().run(status=>{ 
		  	assertEquals(alice.connectionState, CONNECTED())
		  	println("OK i'm Connected now...how did that happen?")
		  	assertFalse(SdpHelper.isBlankSdp(alice.sdp))
		  	b2bServer.findConnByDest(destNumber).foreach( _.disconnect().run(status => {
                        Thread.sleep(50)
                        println("Is alice disconnected alice = " + alice.connectionState)
                        assertEquals(alice.connectionState,UNCONNECTED())
                        latch.countDown()
			        }))
		})
	}	
}

object CallHangupFunctionalTest {
	def main(args:Array[String]) {
		println("running")
		val ch = new CallHangupFunctionalTest()
		ch.setUp()
		ch.runConn()
		ch.getLatch.await()
	}
}

 


