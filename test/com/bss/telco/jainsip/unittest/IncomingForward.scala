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
package com.bss.telco.jainsip.unittest


import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.bss.telco._

import com.bss.telco.jainsip._
import com.bss.telco.api._
import org.junit._
import Assert._

class IncomingForward extends TestHelper {

    val alice = telcoServer.createConnection("7147579999","555444333")

    val bob = telcoServer.createConnection("9495554444", "234567789")

    val latch = new CountDownLatch(1)

    var bobJoined = false

    @Test
    def testIncomingForward() : Unit = {
        //add ignoring phone number to b2bServer
        b2bServer.addIgnore("7147579999")
        val testCall = b2bServer.createConnection("7147579999", "5554443333")
        
        telcoServer.setIncomingCallback(answerCall)
        latch.await(5, TimeUnit.SECONDS)
        assertTrue(true)
        //add answerCall callback to sipServer.

        //b2b.createConnection
        

        //b2bServer.createConnection("")

    }

    def answerCall(call:SipConnection) : Unit ={
        //try the call that they're trying to go for.
        alice.connect( ()=> 
        call.join(alice, ()=>
        latch.countDown()
        ) )
        //start a timer, 
        Thread.sleep(2000)
        alice.cancel( ()=> 
        bob.connect( ()=>
        call.join(bob, ()=>println("here")
        //latch.countDown()
        )))
        //cancel, try another call.

    }
	
}

object IncomingForward {
    def main(args:Array[String]) { 
        val test = new IncomingForward()
        test.setUp()
        test.testIncomingForward()
        test.tearDown()
    }
}
