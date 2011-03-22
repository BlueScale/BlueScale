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
import java.net.URLEncoder
import com.bss.telco.api._
import com.bss.telco.jainsip._
import java.util.concurrent.CountDownLatch
import com.bss.server._
import javax.servlet.http.HttpServletRequest
import com.bss.util.WebUtil

object WebApiFunctionalTest {
    def main(args:Array[String]) {
        val wt = new WebApiFunctionalTest()
        wt.setUp()
        wt.testClickToCall()
        //wt.testIncomingCall()
        
    }
}

class WebApiFunctionalTest extends junit.framework.TestCase {
    
 	val config = new ConfigParser("resources/BlueScaleConfig.Sample.xml")
 
    val telcoServer  = new SipTelcoServer( "127.0.0.1", 4000, "127.0.0.1", 4001)

    val ws = new WebServer(8200, 8080, telcoServer, "http://localhost:8100/")

    //testing
    val b2bServer = new B2BServer("127.0.0.1", 4001, "127.0.0.1", 4000)

    val testWS  = new SimpleWebServer(8100)

    var latch:CountDownLatch = null

	@Before
   	override def setUp() {
   		b2bServer.start()
		telcoServer.start()
		ws.start()
		testWS.start()
		latch = new CountDownLatch(1)
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
        println("test incomingCall")  
        var callid:Option[String] = None
        val inConn = b2bServer.createConnection("7147773456", "7145555555")

        //BlueScale API is goign to post back an incoming call to in
        testWS.setNextResponse( request=> {
            callid = Some( request.getParameter("CallId") )
            getDialResponse("9494443333")
        })

        //API is now going to tell us the call is connected!. We don't need to respond with anything
        testWS.setNextResponse( (request:HttpServletRequest)=> { 
            println("  JJJJJJJJOOOOOOINED")
            assertEquals( inConn.connectionState, CONNECTED() )
            inConn.disconnect( ()=>println("disconnected") )
            //latch.countDown()
            ""
        })

        testWS.setNextResponse( (request:HttpServletRequest)=> {
            assertEquals( inConn.connectionState, UNCONNECTED() )
            //check that it's posting the right info
            println("-------------------------------------------------------")
            latch.countDown()
            ""
        })

        inConn.connect( ()=> println("connected!") )

        latch.await()
        println("Finished testINcomingCall")
    }

    //NOTE. issue is we are posting back antoher connected when we only want the joined.
/*
    @Test
    def testIncomingForward() {
        println("test incoming forward")
        val gatewayNumber    = "4445556666"
        val aliceNumber     = "7778889999"
        val bobNumber       = "1112223333"
        
        var callid:Option[String] = None

        val clientConn = b2bServer.createConnection(gatewayNumber, "1112223333")

        testWS.setNextResponse( request=>{
            callid = Some( request.getParameter("callId") )
            getForwardResponse(aliceNumber, bobNumber)
        })

        testWS.setNextResponse( request=> {
           println("CONNECTED")
           ""
        })

        b2bServer.addIgnore(aliceNumber)
        

        //val inConn = b2bserver.createConnection(clientNumber

    }
    */
   

    @Test
    def testClickToCall() { 
        println("testClickTocall")
        var callid:String = null
        val joinedLatch = new CountDownLatch(1)
       
        testWS.setNextResponse( request=> {
                callid = request.getParameter("CallId")
                getDialResponse("9494443333") 
            })
        
        testWS.setNextResponse( request=> {
            println(" joined NOW!")
            joinedLatch.countDown()
            ""
        })

        testWS.setNextResponse( request=> {
            println("hung up click to call")
            latch.countDown()
            ""
        })

        WebUtil.postToUrl("http://127.0.0.1:8200/Calls", Map("To"->"7147470982",
                                                            "From"->"4445556666",
                                                            "Url"->"http://localhost:8100"))
        joinedLatch.await()
        Thread.sleep(200)//our post might happen before the join callback finishes, could be a benign race conidtion in our test
        WebUtil.postToUrl("http://localhost:8200/Calls/"+callid+"/Hangup", Map("Url"->"http://localhost:8100"))
        latch.await()
        
    }
    
    
    def getForwardResponse(dest:String, dest2:String) : String = 
        return (<Response>
                    <Dial>
                        <Number>{dest}</Number>
                        <Action>http://localhost:8100</Action>
                        <RingLimit>4</RingLimit>
                    </Dial>
                    <Dial>
                        <Number>{dest2}</Number>
                        <Action>http://localhost:8100</Action>
                    </Dial>
                </Response>).toString()


  
    def getDialResponse(dest:String) : String =
        return (<Response>
                    <Dial>
                        <Number>{dest}</Number>
                        <Action>http://localhost:8100/</Action>
                    </Dial>
                </Response>).toString() 

}

