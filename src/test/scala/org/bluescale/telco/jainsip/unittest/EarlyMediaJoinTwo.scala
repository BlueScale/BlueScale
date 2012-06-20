
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

import org.bluescale.telco.api._
import org.bluescale.telco.jainsip._
import org.bluescale.telco._
import java.util.concurrent.CountDownLatch
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(classOf[JUnitRunner])
class EarlyMediaJoinTwo extends FunTestHelper {
	
	var latch:CountDownLatch = null
  
    def getTelcoServer() = telcoServer 

 	def handleDisconnect(conn:SipConnection) = {
 	    //this should be bob!
 	    assert(conn.connectionState ===  UNCONNECTED())
		System.err.println("assert that alice is disconnected = " + alice.connectionState)
		System.err.println("assert that bob is disconnected = " + bob.connectionState)
		//
		latch.countDown()
 	}

     	 
    getTelcoServer.setDisconnectedCallback( handleDisconnect )
 	val alice = getTelcoServer().createConnection("4445556666", "9495557777")
 	val bob = getTelcoServer().createConnection("1112223333", "7147773333")

    test("Early Media test"){
        runJoinTwoConnected()
        val result = latch.await(5,TimeUnit.SECONDS)
		assert(result)
	}
    
    test("Join Early Media test") {
        runJoinConnect()
        val result = latch.await(5,TimeUnit.SECONDS)
		assert(result)
    }

    def runJoinConnect() {
        b2bServer.ringSome = true
        latch = new CountDownLatch(1)
 		alice.connect().run { 
		  	assert(alice.connectionState === CONNECTED())
		    alice.join(bob).run {
		        println(" what is the state of bob = " + bob.connectionState + " alice = " + alice.connectionState )

		        assert(alice.connectionState === CONNECTED())
		        assert(bob.connectionState === CONNECTED())
		        println(" alice.joinedTo = " + alice.joinedTo + " | bob.joinedTo = " + bob.joinedTo )
		        //println("are two connected = " + getTelcoServer().areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]) )
                assert(getTelcoServer().areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
                bob.disconnect().run {
                    println("disconnected")
                    tryAssertEq(bob.connectionState,UNCONNECTED() )
                    latch.countDown()
                }
            }
		  } 
    }

	def runJoinTwoConnected() {
 		b2bServer.ringSome = true

 		latch = new CountDownLatch(1)
	          
 		alice.connect().run {
		  	assert(alice.connectionState === CONNECTED())
		  	bob.connect().run {
		  		assert(bob.connectionState === CONNECTED())
				alice.join(bob).run {
				assert(getTelcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				  System.err.println("are both connected = ? " + getTelcoServer().areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				  
				  	alice.disconnect().run {
				  	  println("alice connectionstate = "+ alice.connectionState)
				  		tryAssertEq(alice.connectionState,UNCONNECTED())
				  		//make sure bob is on hold now!
				  		//assertFalse(getTelcoServer.areTwoConnected(alice.asInstanceOf[SipConnection], bob.asInstanceOf[SipConnection]))
				  		val b = SdpHelper.isBlankSdp(bob.sdp)
				  		System.err.println("b = " + b)
				  		//Now bob should be disconnected
				    }
				}
			}
		}
	}
 }


