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

class IncomingForwardFunctionalTest extends TestHelper {

    val alice = telcoServer.createConnection("7147579999","555444333")

    val bob = telcoServer.createConnection("9495554444", "234567789")

    val joinedLatch = new CountDownLatch(1)

    var bobJoined = false

    var incomingCall:SipConnection = null

    @Test
    def testIncomingForward() : Unit = {
        //add ignoring phone number to b2bServer
        b2bServer.addIgnore("7147579999")
        val testCall = b2bServer.createConnection("7147579999", "5554443333")
        telcoServer.setIncomingCallback(answerCall)
        testCall.connect( ()=> println("b2b INCOMING CALL IS...........connected") )
        joinedLatch.await()

        println(        telcoServer.areTwoConnected(incomingCall, bob) )
    }

    def answerCall(call:SipConnection) : Unit ={
        println("!!!!!  answer CALL is called here!!!!")
        //try the call that they're trying to go for.
        call.accept( ()=> {
        println("ACCEPTED!!!!!!!!!!!!!!")
        incomingCall = call
        alice.connect( ()=>assertFalse(true) ) //shouldn't succeed...
        //start a timer, 
        Thread.sleep(1000)
        println("about to cancel, forgot about this...")
        alice.cancel( ()=> 
            bob.connect( ()=> {
                bob.join(incomingCall, ()=>{
                //call.join(bob, ()=> {
                    println("~~~~~~~JOINED~~~~~~")
                    joinedLatch.countDown()
                    })
            }))
        })
    }
	
}

object IncomingForwardFunctionalTest {
    def main(args:Array[String]) { 
        val test = new IncomingForwardFunctionalTest()
        test.setUp()
        test.testIncomingForward()
        test.tearDown()
    }
}
