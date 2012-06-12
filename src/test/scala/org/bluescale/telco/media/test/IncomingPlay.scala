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


import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.bluescale.telco._

import org.bluescale.telco.jainsip._
import org.bluescale.telco.api._
import org.junit._
import Assert._
import org.bluescale.telco.jainsip.unittest._
import org.bluescale.telco.media.jlibrtp._

class IncomingPlayFunctionalTest
extends TestHelper {

    val alice = telcoServer.createConnection("7147579999","555444333")

    //val bob = telcoServer.createConnection("9495554444", "234567789")

    val joinedLatch = new CountDownLatch(1)

    val playedLatch = new CountDownLatch(1)
    //var bobJoined = false


    @Test
    def testIncomingPlay() : Unit = {
        val testCall = b2bServer.createConnection("3334445555", "2223334444" )
        telcoServer.setIncomingCallback(acceptAndPlay)
        testCall.connect( ()=> println(" test call connected") )
        playedLatch.await()
        println("finished playing!")
    }

    def acceptAndPlay(conn:SipConnection) {
        val mediaConn = new JlibMediaConnection(telcoServer)
        conn.accept( ()=> {
            mediaConn.join(conn, ()=>
                mediaConn.play( "resources/gulp.wav", ()=> playedLatch.countDown() )
            )   
        })
    }

}

object IncomingPlayFunctionalTest {
    def main(args:Array[String]) { 
        println("Starting IncomingPlayFunctionalTest")
        val test = new IncomingPlayFunctionalTest()
        test.setUp()
        println("starting testIncomingPlay method")
        test.testIncomingPlay()
        test.tearDown()
    }
}

