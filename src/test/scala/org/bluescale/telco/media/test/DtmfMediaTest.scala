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


import org.junit._
import Assert._
import org.bluescale.telco.media._
import org.bluescale.telco.api._
import org.bluescale.telco.jainsip.unittest.FunTestHelper
import java.util.concurrent.CountDownLatch
import scala.io.Source

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.TimeUnit
import java.io.FileInputStream

@RunWith(classOf[JUnitRunner])
class DtmfMediaTest extends FunTestHelper {
	
	def finishedPlaying(conn:SipConnection) {
	  //get file from server. 
	  //compare with sent file.
	  conn.disconnect().run {
	    println("DISCONNECT FINISHED")
	    Thread.sleep(2000)//need to let the disconnect propgate to the remote connection so it can finish up saving the file to disk
	  	val files = b2bServer.getMediaConnection("7145554444").recordedFiles
	    files.foreach( f => { 
	  		//compare.
	  		latch.countDown()
	  		println("countdown for the latch")
	  	})
	  	println("files = " + files )
	  }
	  //TODO: compare to the recorded file
	}
	
	var conn:SipConnection = null

	val latch = new CountDownLatch(3)  
	
	test("Test Playing media and detecting DTMF packets") {
		this.b2bServer.answerWithMedia = true
		//lets do client side stuff for now. will have to set stuff pup.
		conn = telcoServer.createConnection("7145554444", "7148889999")
		val media = new EffluxMediaConnection(telcoServer)
		val filestream = new FileInputStream("src/scripts/examples/KeepingTheBladeIntroSmall.wav")
		
		media.dtmfEventHandler = Some(event => event.digit match {
		  case 1 =>
		  	latch.countDown()
		  case 2 =>
		  	latch.countDown()
		  case 3 =>
		    latch.countDown()
		  case _ =>
		    println("WOah we're here at least")
		})
		
		for (_ 		<- conn.connect();
			 _ 		<- media.join(conn);
			 _ 		<- media.play(filestream);
			 sconn 	<- b2bServer.findConnByDest("7145554444");
			 smedia <- sconn.joinedTo) {
			smedia.asInstanceOf[MediaConnection].sendDtmf(1)
			smedia.asInstanceOf[MediaConnection].sendDtmf(2)
			smedia.asInstanceOf[MediaConnection].sendDtmf(3)
			println("done playing, awaiting input!")
		}
		
		//lets see if we can get this working!	
		assert(latch.await(3000, TimeUnit.SECONDS))
		println("finished!")
	}
}
