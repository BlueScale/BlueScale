/* This file is part of BlueScale.
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

package org.bluescale.blueml

import org.bluescale.telco.api._
import org.bluescale.util._
import java.util.concurrent._
import org.bluescale.telco._
import org.bluescale.telco.media._

case class Engine(telcoServer:TelcoServer, defaultUrl:String) extends Util {

    val conversationMap = new ConcurrentHashMap[SipConnection,ConversationInfo]()
    
    protected def handleBlueML(conn:SipConnection, str:String) : Unit = {
        StrOption(str.trim()) match {
            case Some(x) => handleBlueML(conn, BlueMLParser.parse(str))
            case None => Unit 
        }
    }
    
    protected def handleBlueML(conn:SipConnection, verbs:Seq[BlueMLVerb]) : Unit = {
        if (verbs.isEmpty)
            return
        verbs.head match {
            case dial:Dial => 
                dialJoin(conn, dial, verbs.tail)
            case dialVM:DialVoicemail =>
                dialVoicemail(conn, dialVM, verbs.tail) 
            case play:Play => 
                handlePlay(conn, play, verbs.tail)
            case hangup:Hangup =>
            	conn.disconnect().foreach(coonn =>  postCallStatus(hangup.url,conn) )//verify we are recursing here?
            //dcase auth:Auth =>
        }
    }

    protected def handlePlay(conn:SipConnection, play:Play, verbs:Seq[BlueMLVerb]) = {
        val mediaConn = new EffluxMediaConnection(telcoServer)
        val mediaFile = MediaFileManager.getInputStream(play.mediaUrl)
        println("HANDLE PLAY--------->")
        
        for(gather <- play.gather)
        	handleGather(gather, mediaConn, conn)
        
        val f = ()=>
            for(mediaConn <- mediaConn.joinPlay(mediaFile, conn)) { 
            	handleBlueML(conn,  postMediaStatus(play.url, mediaConn, conn, "FinishedPlaying") ) 
            }
        conn.direction match {
            case i:INCOMING => 
                conn.connectionState match {
                    case CONNECTED() => 
                      	f()
                   case UNCONNECTED() => 
                      	println("UNCONNECTED< going to accept!")
                      	for (_ <- conn.accept()) {
                      		println("Accepted from ENGINE ...............now we should play")
                      		f()
                      	}
                } 
            case o:OUTGOING => 
                f()
        }
        handleBlueML(conn, verbs)
    }
    
    protected def handleGather(gather:Gather, mediaConn:MediaConnection, conn:SipConnection) {
    	var digits:List[Int] = List()
    	mediaConn.dtmfEventHandler = Some(dtmfEvent => {
    		digits = digits:+dtmfEvent.digit
    		if (digits.length == gather.digitLimit) {
    			handleBlueML(conn, postDtmfEvent(gather.url, digits,conn))
    		}
    	})
    } 
    
    protected def handleDial(conn:SipConnection, dial:Dial, verbs: Seq[BlueMLVerb]) = {
        conn.connectionState match {
            case c:CONNECTED =>
                                dialJoin(conn, dial, verbs)
                                
            //need to figure out how you can transfer/hold 
            case u:UNCONNECTED  => 
                                dialJoin(conn, dial, verbs) 
           
           case p:PROGRESSING  => 
                                println("progressing") //TODO: should we sleep and call again? 
        }
    }
    
    protected def dialVoicemail(conn:SipConnection, dialVM:DialVoicemail, verbs:Seq[BlueMLVerb]) = {
        //lets try incrementing to deal with loopback issues
        var str = "714444330"
        val connections = List(telcoServer.createConnection(FixPhoneNumber(dialVM.number), str+"0"),
                        telcoServer.createConnection(dialVM.number, str+"1"),
                        telcoServer.createConnection(dialVM.number, str+"2"),
                        telcoServer.createConnection(dialVM.number, str+"3"),
                        telcoServer.createConnection(dialVM.number, str+"4"),
                        telcoServer.createConnection(dialVM.number, str+"5"))

        val callback = ()=> {
            connections.foreach( conn => if (conn.connectionState != CONNECTED()) conn.cancel().foreach( c=> println("cancelling") ))
            val connectedConn = connections.find(conn => conn.connectionState == CONNECTED())
            connectedConn.foreach( connected =>
               connectAnswer(conn, connected, dialVM.url)())
        }
        //connectAnswer should only happen once since we're cancelling the others, so there should be only one successful connect!
        connections.foreach( conn => conn.connect().foreach(c => callback()))
    }

    protected def dialJoin(conn:SipConnection, dial:Dial, verbs:Seq[BlueMLVerb]) = {
        val destConn = telcoServer.createConnection(FixPhoneNumber(dial.number), dial.from)
        conn.connectionState match {
            case c:CONNECTED =>
                for(conn <- conn.join(destConn)) {
                    postCallStatus(dial.url, destConn)
                    postConversationStatus(addConvoInfo(dial.url, conn, destConn))
                }
            
            case _ =>
                conn.incomingCancelCallback = Some((c:SipConnection)=>
                  	for(destConn <- destConn.cancel()) {
                  	  	println("cancelled")
                  	})
                for(destConn <- destConn.connect()) { 
                	connectAnswer(conn, destConn, dial.url)() 
                }
        }
        println("dial.ringLimit= " + dial.ringLimit)
        dial.ringLimit match {
            case x if x > 0 =>
                    Thread.sleep(dial.ringLimit*(1000))
                    try {
                        for(destConn <- destConn.cancel()) { 
                            handleBlueML(conn, verbs)
                        }
                    } catch {
                        case ex:InvalidStateException => println("first connect succeeded")
                        case ex:Exception=> throw ex
                    }

           case _ => 
                println("no ring limit, nothing to do but hope it connects!")
           }
    }
    
    protected def connectAnswer(conn:SipConnection, destConn:SipConnection, url:String) = 
        ()=> {
            postCallStatus(url, destConn)
            conn.direction match {
                case i:INCOMING =>
                  			for (
                  			  _ <- conn.accept();
                  			  _ = println("we have accepted the call and now we will join it!");
                  			  _ <- conn.join(destConn)) 
                  				postConversationStatus(addConvoInfo(url, conn, destConn))
                  			
                case o:OUTGOING =>
                            for(conn <- conn.join(destConn)) {
                                postConversationStatus(addConvoInfo(url, conn, destConn))
                            }
            }
        }
    

    def addConvoInfo(url:String, conn1:SipConnection, conn2:SipConnection) : ConversationInfo = {
    	val ci = new ConversationInfo(conn1, conn2, url)
        conversationMap.put(conn1, ci)
        conversationMap.put(conn2, ci)
        return ci
    }
    
    def handleIncomingCall(url:String, conn:SipConnection) = { 
        try {
        	postCallStatus(url, conn)
        } catch {
          case ex:Exception=>
            conn.reject()
        }
    }

    def handleConnect(url:String, conn:SipConnection) =
          postCallStatus(url, conn)

    def postCallStatus(url:String, conn:SipConnection) : Unit =
        postCallStatus(url, getConnectionMap(conn), Some( (s:String)=>handleBlueML(conn, s) ) )

    def postCallStatus(url:String, map:Map[String,String], handleResponse:Option[(String)=>Unit]) : Unit =
      	for(xml <- StrOption(SequentialWebPoster.postToUrl(url, map));
      		response <- handleResponse;
      		_ = response(xml)) 
      			println("finished posting callstatus to " + url)
     
    def postConversationStatus(convo:ConversationInfo) = {
        println("---" + getJoinedMap(convo))
    	postCallStatus(convo.url,getJoinedMap(convo),None)
    }
        
    def postMediaStatus(url:String, media:MediaConnection, conn:SipConnection, status:String) =
      SequentialWebPoster.postToUrl(url,
          Map( "CallId" -> conn.connectionid,
               "MediaUrl" -> media.playedFiles.firstOption.getOrElse(""),
               "Status" -> status
          )
       )
       
    def postDtmfEvent(url:String, digits:List[Int], conn:SipConnection) =
      	SequentialWebPoster.postToUrl(url,
      		Map( "CallId" -> conn.connectionid,
      		    "Digits"  -> digits.mkString(","))
      	)
    
    //TODO: reject
    def handleRegisterRequest(url:String, authInfo:IncomingRegisterRequest): Unit = { 
    	val parameters = Map("AuthType" -> "Request",
    					"RegisterAddress" -> authInfo.registeredAddress,
    	        		"ContactAddress" -> authInfo.actualAddress)
    	try {
    		val bluemlverbs = BlueMLParser.parse(SequentialWebPoster.postToUrl(url+"/register/",parameters))
    		bluemlverbs
    			.collectFirst({case auth:Auth => auth})
    			.foreach( a => 
    		  		authInfo.successFunction(a.password) match {
    		  			case true => postSuccesfulAuth(url + "/register/", authInfo)
    		  			case false =>println("auth rejected")
    		  		})
    	 } catch {
    	   case ex:Exception => 
    	     	println("error here" + ex)
    			authInfo.rejectFunction()  
         }
    }
	
	def postSuccesfulAuth(url:String, authInfo:IncomingRegisterRequest): Unit = {
    	val parameters = Map("AuthType" -> "Authenticated",
    					"RegisterAddress" -> authInfo.registeredAddress,
    					"ContactAddress" -> authInfo.actualAddress)	
    	SequentialWebPoster.postToUrl(url, parameters)       		
	}
    
    
    protected def getConnectionMap(conn:SipConnection) = 
        Map( "CallId"->conn.connectionid,
             "From"-> conn.origin,
             "To" -> conn.destination,
             "CallStatus" -> conn.connectionState.toString(),
             "Direction" -> conn.direction.toString() )    
    
    
    protected def getJoinedMap(convo:ConversationInfo) = 
        Map( "FirstCallId"->convo.conn1.connectionid,
             "SecondCallId"->convo.conn2.connectionid,
             "ConversationStatus"-> getJoinedState(convo))


    protected def getJoinedState(convo:ConversationInfo) : String =
        telcoServer.areTwoConnected(convo.conn1,convo.conn2) match {
            case true => "Connected"
            case false=> "Unconnected"
        }

     def newCall(to:String, from:String, url:String) {
        //todo: make sure it's all valid
        val conn = telcoServer.createConnection(FixPhoneNumber(to), from)
        for(conn <- conn.connect()) {
            handleConnect(url, conn)
            //send status to the url
        }
    }

    def modifyCall(callid:String, action:BlueMLVerb) {
        val conn = telcoServer.findConnection(callid)
        action match {
            case h:Hangup =>  
                val joinedTo = conn.joinedTo
                for(conn <- conn.disconnect()) { joinedTo match {
                    case Some(x) => println("The unjoin will fire and post back the status, no need to tell it we're hanging up")
                    case None => 
                        postCallStatus(h.url, conn)
                	}
                }
                //conn.disconnect( ()=> println("no need to postCall status, should be posting joined status soon"))//postCallStatus(h.url, conn) )
            case p:Play =>    
                println("join to media!")
        }
    }

    def handleDisconnect(url:String, conn:SipConnection) {
        println(" OK WE GOT a handleDisconnect.....weird")
        //ok, wtf. do we post a join callback? or what?
    }

    def handleUnjoin(url:String, unjoiner:Joinable[_], conn:SipConnection) = {
        val convo = conversationMap.get(conn)
        postConversationStatus(convo)
        conversationMap.remove(convo.conn1)
        conversationMap.remove(convo.conn2)
    }

    def handleConnect(conn:SipConnection) {
        try {
            println("not supported now")     
        } catch {
            case ex:InvalidStateException => println("its OK, it was already connected")
            case ex:Exception => 
            
            println(ex)
        }
    }

    //note if the conn state map changed, we have to abandon the state change!

}

class ConversationInfo( val conn1:SipConnection, 
                        val conn2:SipConnection,
                        val url:String) {
}



