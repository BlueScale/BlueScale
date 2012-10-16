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
import org.bluescale.telco._
import org.bluescale.telco.api._
import org.bluescale.server._
import org.bluescale.telco.jainsip._
import java.util.concurrent._
import org.junit._
import Assert._
import javax.servlet.http.HttpServletRequest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.bluescale.telco.media.EffluxMediaConnection


@RunWith(classOf[JUnitRunner])
class MediaWebApiFunctionalTest extends FunSuite with BeforeAndAfter {
    
 	//val config = new ConfigParser("resources/BlueScaleConfig.Sample.xml")
 
    val telcoServer  = new SipTelcoServer( "127.0.0.1", 4000, "127.0.0.1", 4001)

    val ws = new WebServer(8200, 8080, telcoServer, "http://localhost:8100/")

    //testing
    val b2bServer = new B2BServer("127.0.0.1", 4001, "127.0.0.1", 4000)
    val testWS  = new SimpleWebServer(8100)
    var latch:CountDownLatch = null

    before {
   		b2bServer.start()
   		b2bServer.answerWithMedia = true
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
    /*
    test("Test playing media with a web command"){
        
        var callid:Option[String] = None
        val inConn = b2bServer.createConnection("7147773456", "7145555555")

        //BlueScale API is goign to post back an incoming call to in
        testWS.setNextResponse( request=> {
            callid = Some( request.getParameter("CallId") )
            getPlayResponse()
        })
        
        //API is now going to tell us the call is connected!. We don't need to respond with anything
        testWS.setNextResponse( (request:HttpServletRequest)=> {
        	println("we should be done playing here, status =" + request.getParameter("Status"))
        	Thread.sleep(1000)
        	latch.countDown()
        	""
        })

        inConn.connect().run { println("connected!") }

        latch.await()
        println("Finished incoming play with webAPI")
    }
    */
    
    
    test(" Test DTMF Web Commands") {
    	var callid:Option[String] = None
        val inConn = b2bServer.createConnection("7147773456", "7145555555")

        val mediaconn = new EffluxMediaConnection(telcoServer)
    	
    	
        //BlueScale API is goign to post back an incoming call to in
        testWS.setNextResponse( request=> {
            callid = Some( request.getParameter("CallId") )
            getPlayGatherResponse()
        })
        
        //API is now going to tell us the call is connected!. We don't need to respond with anything
        testWS.setNextResponse( (request:HttpServletRequest)=> {
        	println("going tojoin here!")
        	mediaconn.join(inConn).foreach( _ => {
        	  println("JOINED ~~~~~~~~")
        	})
        	Thread.sleep(1000)
        	println("we should be done playing here, status =" + request.getParameter("Status") +"joinedTo= "+ inConn.joinedTo)
        	for (mediaconn <- inConn.joinedTo) {
        		mediaconn match {
        			case m:MediaConnection =>
        			  	println("SEEEENDING DTMF")
        				m.sendDtmf(1)
        			case _=> println("ERROR matching, joined was = " + inConn.joinedTo)
        		}
        	}
        	println("HERE")
        	""
        })
        
        testWS.setNextResponse( (request:HttpServletRequest)=> {
        	println(" ok we're waiting for a DTMF event!")
        	//get digit?
        	assert(request.getParameter("Digits").contains("1"))
        	getHangupResponse()
        })
        
        testWS.setNextResponse( (request: HttpServletRequest) => {
        	println("we should send the hangup")
        	//veryify we gota hangup 
        	latch.countDown()
        	""
        })
      
    	for(_ <- inConn.connect()) {
        	println("We connected, now we are going to join......")
        	println("CONNECTED")
        }
    	
        latch.await()
        println("Finished testINcomingPlay")
    }
    
    def getHangupResponse() =
      (<Response>
    		 <Hangup>
    		  		<Action>http://localhost:8100</Action> 
    		 </Hangup>
    	</Response>).toString()

    def getPlayResponse(): String = 
        return (<Response>
                    <Play>
        				<Action>http://localhost:8100</Action>
        				<MediaUrl>src/scripts/examples/KeepingTheBladeIntroSmall.wav</MediaUrl>
        			</Play>
                </Response>).toString()
                
    def getPlayGatherResponse(): String =
      	return(<Response>
      				<Play>
      					<Action>http://localhost:8100</Action>
        				<MediaUrl>src/scripts/examples/KeepingTheBladeIntroSmall.wav</MediaUrl>
      					<Gather>
      						<DigitLimit>1</DigitLimit>
      						<Action>http://localhost:8100</Action>
      					</Gather>
      					
      				</Play>
      			</Response>).toString()
      		
     
      	    
}
