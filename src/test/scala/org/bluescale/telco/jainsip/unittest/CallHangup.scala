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

import org.bluescale.telco.jainsip._

import org.bluescale.telco.api._
import org.bluescale.telco._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner


@RunWith(classOf[JUnitRunner])
class CallHangup extends FunTestHelper {
	
	
	test("Simple Call with Remote Hangup") {
		println("running");
		runConn()
		val result = getLatch.await(5,TimeUnit.SECONDS)
		assert(result)
		println("finished")
	}
	
	var latch:CountDownLatch = null
  
	def getLatch = latch
	
  
	def runConn() {
	    println("in runConn")
 		latch = new CountDownLatch(1)
 		val destNumber = "9495557777"
 		var alice:SipConnection = null
 		
 		telcoServer.setDisconnectedCallback( c=>{
 			println("alice connectionstate = " + alice.connectionState)
 			tryAssertEq(alice.connectionState,UNCONNECTED())
 			latch.countDown()
 		})
 		
 		alice = telcoServer.createConnection(destNumber, "4445556666")
 		
 		for(alice <- alice.connect()) { 
		  	assert(alice.connectionState === CONNECTED())
		  	println("OK i'm Connected now...how did that happen?")
		  	assert(!SdpHelper.isBlankSdp(alice.sdp))
		  	println("trying a remote hangup")
		  	b2bServer.findConnByDest(destNumber).foreach( _.disconnect().foreach( c=>
		  				println("disconnect has happened")
			        ))
		}
	}	
}

 


