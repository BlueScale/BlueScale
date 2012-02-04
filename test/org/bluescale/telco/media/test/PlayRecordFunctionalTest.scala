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

package org.bluescale.telco.media.test


import org.junit._
import Assert._
import org.bluescale.telco.media.jlibrtp._
import org.bluescale.telco.api._
import org.bluescale.telco.jainsip.unittest.TestHelper
import java.util.concurrent.CountDownLatch

class PlayRecordFunctionalTest extends TestHelper {
	
	def finishedPlaying(conn:SipConnection) {
	  //get file from server. 
	  //compare with sent file.
	  println("finishedPlaying")
	  conn.disconnect( ()=> {
	    Thread.sleep(1000)//lets let the connection finish writing the file
	  	val files = b2bServer.getMediaConnection("7145554444").recordedFiles
	    files.foreach( f => { 
	  		//compare.
	  		latch.countDown()
	  		println("countdown for the latch")
	  	})
	  })
	  
	  //get filename somehow.  
	  //compare to the recorded file
	}
	
	var conn:SipConnection = null

	val latch = new CountDownLatch(1)  
	
	@Test
	def testPlayRecord() {
		this.b2bServer.answerWithMedia = true
		//lets do client side stuff for now. will have to set stuff pup.
		conn = telcoServer.createConnection("7145554444", "7148889999")
		val media = new JlibMediaConnection(telcoServer)
		
		conn.connect( ()=> 
			media.join(conn, ()=>
			    media.play( "resources/gulp.wav", ()=> finishedPlaying(conn) ))
		)
		
		//lets see if we can get this working!	
		println("awaiting")
		latch.await()
		println("finished waiting, sleeping now...")
		Thread.sleep(1000)
		println("why are we not here????")
	}
}
