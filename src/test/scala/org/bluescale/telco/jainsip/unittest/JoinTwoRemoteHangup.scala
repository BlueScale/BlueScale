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
import java.util.concurrent.CountDownLatch
import org.bluescale.telco.api._

class JoinTwoRemoteHangup extends FunTestHelper {
	
 	var latch:CountDownLatch = null
 
	def getLatch = latch
	

 	telcoServer.setDisconnectedCallback(disconnected)
 	

 	val alice = telcoServer.createConnection("4445556666", "9495557777")
 	val bob = telcoServer.createConnection("1112223333", "7147773333")
    
    def disconnected(call:SipConnection) {
        println(" alice = " + alice)
        println(" bob = " + bob )
        tryAssertEq(alice.connectionState,UNCONNECTED())
        tryAssertEq(bob.connectionState,UNCONNECTED())
        latch.countDown()
    }
 
	def runConn() {
 		latch = new CountDownLatch(1)
 		alice.connect(()=>{ 
		  	assert(alice.connectionState === CONNECTED())
		  	bob.connect(()=>{
		  		assert(bob.connectionState === CONNECTED())
				alice.join(bob, ()=>{
				    assert(telcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				    println("are both connected = ? " + telcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
			        //Now initiate a remote hangup.
			        b2bServer.findConnByDest("4445556666").foreach( _.disconnect( ()=> println("disconnected") ))
				})
			})
		})
	}

	def testJoinTwoRemoteHangup() = {
	    println("joinTwoRemoteHangup")
		runConn()
		getLatch.await()
	}
}

