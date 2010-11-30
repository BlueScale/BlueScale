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
package com.bss.web.test


import org.junit._
import Assert._
import com.bss.server._
import com.bss.telco.jainsip.test.B2BServer
import java.net.URLEncoder
import com.bss.telco.api._
import com.bss.telco.jainsip._
import java.util.concurrent.CountDownLatch
import com.bss.server._
import javax.servlet.http.HttpServletRequest

object WebTest {
    def main(args:Array[String]) {
        val wt = new WebTest()
        wt.setUp()
        wt.testIncomingCall()
        
    }
}
class WebTest extends junit.framework.TestCase {
    
 	val config = new ConfigParser("resources/BlueScaleConfig.xml")
 
    val telcoServer  = new SipTelcoServer( "127.0.0.1", 4000, "127.0.0.1", 4001)

    val ws = new WebServer(82, 8080, telcoServer, "http://localhost:81/")

    //testing
    val b2bServer = new B2BServer("127.0.0.1", 4001, "127.0.0.1", 4000)

    val testWS  = new SimpleWebServer(81)

    val latch = new CountDownLatch(1)

	@Before
   	override def setUp() {
   		b2bServer.start()
		telcoServer.start()
		ws.start()
		testWS.start()
	}	
	
    @After
	override def tearDown() {
        telcoServer.stop()
        b2bServer.stop()
        ws.stop()
        testWS.stop()
    }

    @Test
    def testIncomingCall() {
        println("test")  
        var callId:Option[String] = None
        val inConn = b2bServer.createConnection("7147773456", "7145555555")

        //BlueScale API is goign to post back an incoming call to in
        testWS.setNextResponse( request=> {
            callId = Some( request.getParameter("CallId") )
            getDialResponse("9494443333")
        })

        //API is now going to tell us the call is connected!. We don't need to respond with anything
        testWS.setNextResponse( (request:HttpServletRequest)=> {
            println("CONNECTED CALLBACK ExECUTING")
            assertEquals( inConn.connectionState, CONNECTED )
            latch.countDown()
            ""
        })
        Console.readLine()
        inConn.connect( ()=> println("connected!") )
        latch.await()
   
    }


    //inConn-> BlueScaleWS -> SimpleWS
    //@Test
/*
    def XtestOutogingRemoteHangup() {
         testWS.setNextResponse( (request:HttpServletRequest) => {
                 println("ok, connected. Let's make sure this worked")
                 //assertEquals everything worked...
                 getDialResponse("9497773456") 
            })
        testWS.setNextResponse( (request:HttpServletRequest) =>{
            println("ok they're joined now..., hang them up! now")
            //hangup code here...
            latch.countDown()
            ""
        }) 
        val responseXml = BlueML.postToUrl( "127.0.0.1:80", 
                                             Map("To"->"9497773456",
                                                 "From"->"5555555555",
                                                 "Url"->"127.0.0.1:81"))
        
        latch.await()
        //TODO: assert they are disconnected.             
    }
*/


    def XtestOutgoingInitiatedHangup() {
        println("blah") 
    }

    def getDialResponse(dest:String) : String =
        return (<Response>
                    <Dial>{dest}</Dial>
                </Response>).toString() 

}

