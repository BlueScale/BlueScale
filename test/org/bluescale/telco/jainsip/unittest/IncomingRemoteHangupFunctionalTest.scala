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
* Please contact us at www.BlueScaleSoftware.com
*
*/
package org.bluescale.telco.jainsip.unittest


import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.bluescale.telco._

import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._
import org.junit._
import Assert._

class IncomingRemoteHangupFunctionalTest extends TestHelper {

    val alice = telcoServer.createConnection("9494443456", "9494443456")

    val joinedLatch = new CountDownLatch(1)

    val disconnectLatch = new CountDownLatch(1)

    var incomingCall:SipConnection = null

    @Test
    def testIncomingRemoteHangup() : Unit = {
        //add ignoring phone number to b2bServer
        val testCall = b2bServer.createConnection("7147579999", "5554443333")
        telcoServer.setIncomingCallback(answerCall)
        testCall.connect( ()=> println("connected") )
        disconnectLatch.await()
        println("MADE IT HERE!")
        assertFalse(telcoServer.areTwoConnected(incomingCall, alice) )
    }

    def answerCall(call:SipConnection) : Unit ={
        //try the call that they're trying to go for.
        alice.disconnectCallback = Some( (c:SipConnection)=> disconnectLatch.countDown())
        incomingCall = call
        alice.connect( ()=> {
                println("alice connected!")
                incomingCall.accept( ()=> {
                    incomingCall.join(alice, ()=> {
                        Thread.sleep(1000)
                        b2bServer.findConnByDest("9494443456").foreach( _.disconnect( ()=>println( "remote call disconnected!")))
                        
                    })
                })
            })
        
    }
	
}


object IncomingRemoteHangupFunctionalTest {
    def main(args:Array[String]) { 
        val test = new IncomingRemoteHangupFunctionalTest()
        test.setUp()
        test.testIncomingRemoteHangup()
        test.tearDown()
    }
}

