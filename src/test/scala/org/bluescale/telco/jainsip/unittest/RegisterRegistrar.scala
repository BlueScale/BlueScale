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
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.TimeUnit
import org.bluescale.util.ForUnitWrap._

@RunWith(classOf[JUnitRunner])
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
	
	var addrOfRecord = ""
	  
	val toNumber = "" 
	  
	var regRequest:Option[IncomingRegisterRequest] = None
	
	
	
	test("Test Register and Registrar capabilities") {
	  	latch = new CountDownLatch(1)
	  	val sipClientTelcoServer  = new SipTelcoServer( "127.0.0.1", 4002, "127.0.0.1", 4000)
	  	sipClientTelcoServer.setIncomingCallback((conn)=> conn.accept.run{ println("accepted!") } )
		sipClientTelcoServer.start()
		telcoServer.setIncomingCallback(incomingCallback)
		telcoServer.setRegisterCallback(incomingRegister)
		sipClientTelcoServer.sendRegisterRequest("7147570982@127.0.0.1:4000","7147570982", "mypass", "127.0.0.1") 
		println("running");
		assert(getLatch.await(500,TimeUnit.SECONDS))
		println("finished")
		sipClientTelcoServer.stop()
	}
	
	
	def incomingCallback(conn:SipConnection): Unit = {
		regRequest match {
			case Some(reg) =>
			  	assert(conn.destination === reg.registeredAddress)
				val sipconn = telcoServer.createConnection(reg.actualAddress,conn.origin)
				for (_ <- sipconn.connect();
					_ <- println("sip connection connected");
					_ <- conn.join(sipconn);
					_ <- println("joined!");
					_ <- sipconn.disconnect()){
						println("YAY")
						latch.countDown()
					}
			case None => 
				assert(false)
		  
		}
		
		//now make an outgoing call to the sipclient!
		
	}
	
	def incomingRegister(request:IncomingRegisterRequest): Unit = {
		//Thread.sleep(5000)
		val success = request.successFunction("mypass")
		println("success = " + success)
		//assert(success)
		regRequest = Some(request)
		//record internal association
		val outgoingconn = b2bServer.createConnection("7147570982","5554443333")
		outgoingconn.connect().run {println("connected")}
	}

}

 


