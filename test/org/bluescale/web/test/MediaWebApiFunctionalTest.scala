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
package org.bluescale.web.test
import org.bluescale.telco._
import org.bluescale.telco.api._
import org.bluescale.server._
import org.bluescale.telco.jainsip._
import java.util.concurrent._
import org.junit._
import Assert._
import javax.servlet.http.HttpServletRequest

class MediaWebApiFunctionalTest extends junit.framework.TestCase {
    
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
   		b2bServer.answerWithMedia = true
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
    def testPlayMediaIncomingCall() {
        
    	println("test incomingCall")  
        var callid:Option[String] = None
        val inConn = b2bServer.createConnection("7147773456", "7145555555")

        //BlueScale API is goign to post back an incoming call to in
        testWS.setNextResponse( request=> {
            callid = Some( request.getParameter("CallId") )
            getPlayResponse("9494443333")
        })
        
        testWS.setNextResponse( request=>  
          getHangupResponse() ) //this is telling us the callID of the other end...

        //API is now going to tell us the call is connected!. We don't need to respond with anything
        testWS.setNextResponse( (request:HttpServletRequest)=> {
            	latch.countDown()
        		""
        	})

        inConn.connect( ()=> println("connected!") )

        latch.await()
        println("Finished testINcomingPlay")
    }
    
    def getHangupResponse() =
      (<Response>
    		 <Hangup>
    		  		<Action>http://localhost:8100</Action> 
    		 </Hangup>
    	</Response>).toString()

    def getPlayResponse(url:String) : String = 
        return (<Response>
                    <Play>
        				<Action>http://localhost:8100</Action>
        				<MediaUrl>resources/gulp.wav</MediaUrl>
        			</Play>
                </Response>).toString()
}

object MediaWebApiFunctionalTest {
    def main(args:Array[String]) =  { 
        val test = new MediaWebApiFunctionalTest()
        test.setUp()
        test.testPlayMediaIncomingCall()
        test.tearDown()
    }
}
