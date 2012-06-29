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
package org.bluescale.web.test

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.bluescale.server._
import java.net.URLEncoder
import org.bluescale.telco.api._
import org.bluescale.telco.jainsip._
import org.bluescale.telco._
import java.util.concurrent.CountDownLatch
import org.bluescale.server._
import javax.servlet.http.HttpServletRequest
import org.bluescale.util.WebUtil
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.TimeUnit

@RunWith(classOf[JUnitRunner])
class SipClientWebApi extends FunSuite with BeforeAndAfter {
    
    val telcoServer  = new SipTelcoServer( "127.0.0.1", 4000, "127.0.0.1", 4001)

    val ws = new WebServer(8200, 8080, telcoServer, "http://localhost:8100/")

    //testing
    val b2bServer = new B2BServer("127.0.0.1", 4001, "127.0.0.1", 4000)

    var testWS:SimpleWebServer = null  

    var latch:CountDownLatch = null
    
    val sipClientTelcoServer  = new SipTelcoServer( "127.0.0.1", 4002, "127.0.0.1", 4000)

    val gatewayNumber    = "4445556666"
    val aliceNumber     = "7778889999"
    val bobNumber       = "1112223333"
   	
    before {
        testWS = new SimpleWebServer(8100)
   		b2bServer.start()
		telcoServer.start()
		ws.start()
    	sipClientTelcoServer.start()
		testWS.start()
		latch = new CountDownLatch(1)
	}	
	
	after {
		sipClientTelcoServer.stop()
		telcoServer.stop()
        b2bServer.stop()
        ws.stop()
        testWS.stop()
    }
	
	 
    test("Testing SIpAuth IncomingForward") {
    	val password = "asdf123"
    	val registerLatch = new CountDownLatch(1)
    	val joinedLatch = new CountDownLatch(1)
    	var contactAddress:String = null
    	var registeredAddress:String = null
    	
    	testWS.setNextResponse( (request:HttpServletRequest) => {
    		assert(request.getParameter("AuthType") === "Request")
    		getRegisterAuthResponse(password)
    	})
    	
    	testWS.setNextResponse( request => {
    	  //verify we were authorized 
    	  assert(request.getParameter("AuthType") === "Authenticated")
    	  contactAddress = request.getParameter("ContactAddress")
    	  registeredAddress = request.getParameter("RegisteredAddress")
    	  registerLatch.countDown()
    	  ""
    	})
    	
    	testWS.setNextResponse( request => {
    		// an incoming call, lets connect and join to the sip client
    		//assert(request.getParemeter(""))
    		getDialResponse(contactAddress)
    	})
    	
    	testWS.setNextResponse( request => {
    		//verify we're joined, hangup, and we're good to go!
    		assert(request.getParameter("ConversationStatus") === "Connected")
    		joinedLatch.countDown()
    		""
    	})
    
    	sipClientTelcoServer.sendRegisterRequest("7147570982@127.0.0.1:4000","7147570982",  password, "127.0.0.1")
    	assert(registerLatch.await(8, TimeUnit.SECONDS))
    	val incomingConn = b2bServer.createConnection("7147570982", "5554443333")
    	incomingConn.connect().run { println("connected") }
    	
    	assert(joinedLatch.await(8, TimeUnit.SECONDS))
    	sipClientTelcoServer.stop()
    }
    
  
    def getDialResponse(dest:String): String =
        return (<Response>
                    <Dial>
                        <Number>{dest}</Number>
                        <Action>http://localhost:8100/</Action>
                    </Dial>
                </Response>).toString()
    
    def getRegisterAuthResponse(password:String): String =
    	return(<Response>)
    				<Auth>
    					<Password>{password}</Password>
    				</Auth>
    			</Response>).toString()

}
