package org.bluescale.telco.jainsip.unittest

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

import org.bluescale.telco.jainsip._
import org.bluescale.telco._
import org.bluescale.telco.api._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class RegisterRegistrar extends FunTestHelper {
	
	/*
	 *   This test involves three instances of the BlueScale server.  One to represent a SIP phone Client, 
	 *   One to represent the PSTN (b2b server), and another to represent our actual BlueScale server.  
	 *   
	 *   SIPClient -> (register) -> BlueScale 
	 *   B2BServer -> (invite) -> BlueScale 
	 *   BlueScale -> (invite) -> SipClient
	 *   BlueScale -> Join(sipclient, b2b)
	 * 
	 */
  
  
	var latch:CountDownLatch = null
  
	def getLatch = latch
	
	var addrOfRecord:String = ""
	
	//test("Test Register and Registrar capabilities") {
	def xtest() {
		val sipClientTelcoServer  = new SipTelcoServer( "127.0.0.1", 4002, "127.0.0.1", 4000) 
		sipClientTelcoServer.start()
		telcoServer.setIncomingCallback(incomingCallback)
		telcoServer.setRegisterCallback(incomingRegister)
		sipClientTelcoServer.sendRegisterRequest("","vince", "mypass", "bluescale.org") 
		println("running");
		//runConn()
		getLatch.await()
		println("finished")
	}
	
	
	def incomingCallback(conn:SipConnection): Unit = {
		assert(conn.destination === addrOfRecord)
		//now make an outgoing call to the sipclient!
		
		
		
	  
	}
	
	def incomingRegister(request:IncomingRegisterRequest): Unit = {
		request.successFunction("mypass")
		//record internal association
		val outgoingconn = b2bServer.createConnection("","")
		outgoingconn.connect(()=>println("connected"))
	}
	
 /* 
	def runConn() {
	    println("in runConn")
 		latch = new CountDownLatch(1)
 		val destNumber = "9495557777"
 		println("here1")
 		val alice = telcoServer.createConnection(destNumber, "4445556666")
 		println("here2")
 		alice.connect(()=>{ 
		  	assert(alice.connectionState === CONNECTED())
		  	println("OK i'm Connected now...how did that happen?")
		  	assert(!SdpHelper.isBlankSdp(alice.sdp))
		  	println("trying a remote hangup")
		  	b2bServer.findConnByDest(destNumber).foreach( _.disconnect( ()=> {
		  				println("disconnect has happened")
                        tryAssertEq(alice.connectionState,UNCONNECTED())
                        latch.countDown()
			        }))
		})
	}
	*/	
}

 


