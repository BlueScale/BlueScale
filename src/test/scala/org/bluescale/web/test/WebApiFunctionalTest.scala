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

/*
object WebApiFunctionalTest {
    def main(args:Array[String]) {
        val wt = new WebApiFunctionalTest()
        wt.setUp()
        wt.testClickToCall()
        //wt.testIncomingSendToVM()
        wt.tearDown()
     
        
    }
}
*/

@RunWith(classOf[JUnitRunner])
class WebApiFunctionalTest extends FunSuite with BeforeAndAfter {
    
    val telcoServer  = new SipTelcoServer( "127.0.0.1", 4000, "127.0.0.1", 4001)

    val ws = new WebServer(8200, 8080, telcoServer, "http://localhost:8100/")

    //testing
    val b2bServer = new B2BServer("127.0.0.1", 4001, "127.0.0.1", 4000)

    val testWS  = new SimpleWebServer(8100)

    var latch:CountDownLatch = null

    val gatewayNumber    = "4445556666"
    val aliceNumber     = "7778889999"
    val bobNumber       = "1112223333"
        

   	before {
   		b2bServer.start()
		telcoServer.start()
		ws.start()
		testWS.start()
		latch = new CountDownLatch(1)
	}	
	
	after {
        telcoServer.stop()
        b2bServer.stop()
        ws.stop()
        testWS.stop()
    }

    test("Incoming Call Web API test") {
        println("!!!!!!!!!! test incomingCall")  
        var callid:Option[String] = None
        val inConn = b2bServer.createConnection("7147773456", "7145555555")

        //BlueScale API is goign to post back an incoming call to in
        testWS.setNextResponse( request=> {
            println("got one resopnse")
            callid = Some( request.getParameter("CallId") )
            getDialResponse("9494443333")
        })
        testWS.setNextResponse( request=> "" ) //this is telling us the callID of the other end...

        //API is now going to tell us the call is connected!. We don't need to respond with anything
        testWS.setNextResponse( (request:HttpServletRequest)=> {
            Thread.sleep(500)//benign race condition here because of the fact the ack comes to this and gets sent to an actor... 
            assert( inConn.connectionState === CONNECTED() )
            println("got next response!")
            assert( "Connected" === request.getParameter("ConversationStatus"))
            assert( !SdpHelper.isBlankSdp(inConn.sdp))
            inConn.disconnect().run { println("disconnected") }
            ""
        })

        testWS.setNextResponse( (request:HttpServletRequest)=> {
            assert( inConn.connectionState === UNCONNECTED() )
            //check that it's posting the right info
            latch.countDown()
            ""
        })

        inConn.connect().run { println("connected!") }

        latch.await()
        println("Finished testINcomingCall")
    }


    def testIncomingSendToVM() {
        println("test incomingSendToVM()")
        var callid:Option[String] = None
        val joinedLatch = new CountDownLatch(1)
        val clientConn = b2bServer.createConnection(gatewayNumber, "1112223333")
       
        testWS.setNextResponse( request=>{
            callid = Some( request.getParameter("CallId") )
            getForwardVMResponse(aliceNumber, bobNumber)
        })

        testWS.setNextResponse( request=> {
            println(" ok i'm connected here")
            //assert(request.getParameter("To"), bobNumber)
            ""
        })

        testWS.setNextResponse( request=> {
            println("......joined")
            Thread.sleep(500)
            assert(request.getParameter("ConversationStatus") === "Connected")
            joinedLatch.countDown()
            ""
        })

        testWS.setNextResponse( request=> {
            println("disconnected")
            latch.countDown()
            ""
        })

        b2bServer.simulateCellVM = true
        clientConn.connect().run { println("connected") }
        joinedLatch.await()
        println("we've joined")
        WebUtil.postToUrl("http://localhost:8200/Calls/"+callid.get +"/Hangup", Map("Url"->"http://localhost:8100"))
        latch.await()
        println("finished testIncomingSendToVMForward")
    }


    test("Web API Incoming Forward TEST"){
        println("test incoming forward///////////////////")
        val joinedLatch = new CountDownLatch(1)
        var callid:Option[String] = None

        val clientConn = b2bServer.createConnection(gatewayNumber, "1112223333")

        testWS.setNextResponse( request=>{
            callid = Some( request.getParameter("CallId") )
            getForwardResponse(aliceNumber, bobNumber)
        })

        testWS.setNextResponse( request=> {
            println(" ok i'm connected here")
            //assert(request.getParameter("To"), bobNumber)
            ""
        })

        testWS.setNextResponse( request=> {
            println("......joined")
            assert(request.getParameter("ConversationStatus") === "Connected")
            joinedLatch.countDown()
            ""
        })

        testWS.setNextResponse( request=> {
            println("disconnected")
            latch.countDown()
            ""
        })

        b2bServer.addIgnore(aliceNumber)
        
        val inConn = b2bServer.createConnection(gatewayNumber, "4443332222")
        inConn.connect().run { ()=> println("connected") }
        joinedLatch.await()
        println("done waiting for joined --------")
        WebUtil.postToUrl("http://localhost:8200/Calls/"+callid.get +"/Hangup", Map("Url"->"http://localhost:8100"))
        latch.await()
        println("finished testIncomingForward")
    }


    test("Web API Click to Call test") { 
        println("~~~~~~~~~~~~~~~tESTcLIckTocall")
        b2bServer.ringSome = true
        
        var call1id:String = null
        var call2id:String = null

        val joinedLatch = new CountDownLatch(1)
       
        testWS.setNextResponse( request=> {
                call1id = request.getParameter("CallId")
                getDialResponse(bobNumber) 
            })

        testWS.setNextResponse( request=> {
            call2id = request.getParameter("CallId")
            println("connected")
            ""
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

        WebUtil.postToUrl("http://127.0.0.1:8200/Calls", Map("To"->aliceNumber,
                                                            "From"->bobNumber,
                                                            "Url"->"http://localhost:8100"))
        joinedLatch.await()

        val call1 = telcoServer.findConnection(call1id)
        val call2 = telcoServer.findConnection(call2id)        
        println(" call1 = " + call1)
        println(" call2 = " + call2)
        assert( telcoServer.areTwoConnected(call1, call2) )
        assert(!SdpHelper.isBlankSdp(call1.sdp))
        assert(!SdpHelper.isBlankSdp(call2.sdp))
        
        Thread.sleep(900)//our post might happen before the join callback finishes, could be a benign race conidtion in our test
        WebUtil.postToUrl("http://localhost:8200/Calls/"+call1id+"/Hangup", Map("Url"->"http://localhost:8100"))
        latch.await()
        
        println("finisehd click to call")
    }
    
    def getForwardVMResponse(dest:String, dest2:String) : String = 
        return (<Response>
                    <DialVoicemail>
                       <Number>{dest}</Number>
                       <Action>http://localhost:8100</Action>
                    </DialVoicemail>
                </Response>).toString()

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

