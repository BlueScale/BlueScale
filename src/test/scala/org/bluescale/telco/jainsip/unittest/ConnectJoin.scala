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
import java.util.concurrent.CountDownLatch
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(classOf[JUnitRunner])
class ConnectJoin extends FunTestHelper {
	
 
	test("Join Two connections, then remote hangup") {
		println("ConnectJoinTwo")
		runConn()
		val result = latch.await(5,TimeUnit.SECONDS)
		assert(result)
		println("FINISHED")
	}
	
	var latch:CountDownLatch = null	
 
 	def handleDisconnect(conn:SipConnection) = {
 	    //this should be bob!
 	    assert(bob.connectionState === UNCONNECTED())
		System.err.println("assert that alice is disconnected = " + alice.connectionState)
		System.err.println("assert that bob is disconnected = " + bob.connectionState)
		latch.countDown()
 	}

     	 
    telcoServer.setDisconnectedCallback( handleDisconnect )
 	val alice = telcoServer.createConnection("4445556666", "9495557777")
 	val bob = telcoServer.createConnection("1112223333", "7147773333")

 
	def runConn() {
 		latch = new CountDownLatch(1)

 		for(alice <- alice.connect();
 			_ = assert(alice.connectionState === CONNECTED());
 			(alice,bob) <- alice.join(bob)) { 
		        assert(alice.connectionState === CONNECTED())
                println( "ARE TWO CONNECTED = " + telcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]) )
                assert(telcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
                latch.countDown()
		  }
	}
}


