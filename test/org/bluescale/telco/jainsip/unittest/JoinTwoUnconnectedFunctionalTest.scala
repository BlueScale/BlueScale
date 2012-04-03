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

import org.junit._
import Assert._
import java.util.concurrent.CountDownLatch

class JoinTwoUnconnectedFunctionalTest extends TestHelper { 

	var latch:CountDownLatch = null
 
	def getLatch = latch
	
  
 	def handleDisconnect(conn:SipConnection) = {
 	    //this should be bob!
 	    println(" oooooooKAY, lets see what the conncetion is, conn = " + conn)
 	    assertEquals(bob.connectionState, UNCONNECTED())
		System.err.println("assert that alice is disconnected = " + alice.connectionState)
		System.err.println("assert that bob is disconnected = " + bob.connectionState)
		//
		println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ COUNTING DOWN THE LATCH ~~~~~~~~")
		latch.countDown()
 	}

     	 
    telcoServer.setDisconnectedCallback( handleDisconnect )
 	val alice = telcoServer.createConnection("4445556666", "9495557777")
 	val bob = telcoServer.createConnection("1112223333", "7147773333")

 
	def runConn() {
 		latch = new CountDownLatch(1)
	          
 		alice.connect(()=>{
		  	assertEquals(alice.connectionState, CONNECTED())
		  	println( "alice connected" )
		  	bob.connect(()=>{
		  		assertEquals(bob.connectionState, CONNECTED())
				println(" bob connected" )
				//Thread.sleep(4000)
				alice.join(bob, ()=>{
				assertFalse(SdpHelper.isBlankSdp(alice.sdp)) 
				assertFalse(SdpHelper.isBlankSdp(bob.sdp))
				assertTrue(telcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				  System.err.println("are both connected = ? " + telcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				  
				  	alice.disconnect( ()=>{
				  	  println("alice connectionstate = "+ alice.connectionState)
				  		assertEquals(alice.connectionState, UNCONNECTED())
				  		//make sure bob is on hold now!
				  		//assertFalse(getTelcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				  		val b = SdpHelper.isBlankSdp(bob.sdp)
				  		System.err.println("b = " + b)
				  		//Now bob should be disconnected
					})
				})
			})
		})
	} 

	@Test
	def testJoinConn() = {
	 	runConn()
		getLatch.await()
	}
}

object JoinTwoUnconnectedFunctionalTest {
    def main(args:Array[String]) {
        val t= new JoinTwoUnconnectedFunctionalTest()
        t.setUp() 
        t.testJoinConn()
        t.latch.await()
        t.tearDown()
    }
}

 


