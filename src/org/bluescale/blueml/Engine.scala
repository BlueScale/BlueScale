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

package org.bluescale.blueml

import org.bluescale.telco.api._
import org.bluescale.util._
import java.util.concurrent._
import org.bluescale.telco._
import org.bluescale.telco.media.jlibrtp._

class Engine(telcoServer:TelcoServer, defaultUrl:String) extends Util {

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
            case play:Play => 
                handlePlay(conn, play, verbs.tail)    
            case hangup:Hangup =>
            	conn.disconnect( ()=> postCallStatus(hangup.url,conn))
            	
        }
    }

    protected def handlePlay(conn:SipConnection, play:Play, verbs:Seq[BlueMLVerb]) = {
        val mediaConn = new JlibMediaConnection(telcoServer)
        val f = ()=>
            mediaConn.joinPlay(play.mediaUrl, conn, ()=> handleBlueML(conn,  postMediaStatus(play.url, mediaConn, conn) ))
        conn.direction match {
            case i:INCOMING => 
                conn.connectionState match {
                    case CONNECTED() => f()
                    case UNCONNECTED() => conn.accept(f)
                } 
            case o:OUTGOING => 
                        f()
        }
        handleBlueML(conn, verbs)
    }
    
    protected def handleDial(conn:SipConnection, dial:Dial, verbs:Seq[BlueMLVerb]) = { 
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
    

    protected def dialJoin(conn:SipConnection, dial:Dial, verbs:Seq[BlueMLVerb]) = {
        val destConn = telcoServer.createConnection(dial.number, dial.from)
        conn.connectionState match {
            case c:CONNECTED =>
                conn.join(destConn, ()=>{
                    postCallStatus(dial.url, destConn)
                    postConversationStatus(addConvoInfo(dial.url, conn, destConn))
                })
            
            case _ => connectAnswer(conn, destConn, dial)
                
        }
    
        dial.ringLimit match {
            case -1 => println("ok nothing to do but hope it connects")

            case _ => 
                Thread.sleep(dial.ringLimit*(1000))
                try {
                    destConn.cancel( ()=> { 
                    handleBlueML(conn, verbs)
                    })

                } catch {
                    case ex:InvalidStateException => println("first connect succeeded")
                    case ex:Exception=> throw ex
                }
        }
    }

    protected def connectAnswer(conn:SipConnection, destConn:SipConnection, dial:Dial) = {
        destConn.connect(()=> {
            postCallStatus(dial.url, destConn)
            conn.direction match {
                case i:INCOMING =>
                            conn.accept( ()=> {
                                 conn.join(destConn, ()=> 
                                    postConversationStatus(addConvoInfo(dial.url, conn, destConn)))
                               } )
                                
                case o:OUTGOING =>
                            conn.join(destConn, ()=> 
                                postConversationStatus(addConvoInfo(dial.url, conn, destConn)))
            }
        })
    }

    def addConvoInfo(url:String, conn1:SipConnection, conn2:SipConnection) : ConversationInfo = {
        val ci = new ConversationInfo(conn1, conn2, url)
        conversationMap.put(conn1, ci)
        conversationMap.put(conn2, ci)
        return ci
    }
    
    def handleIncomingCall(url:String, conn:SipConnection) = { 
        postCallStatus(url, conn)
    }

    def handleConnect(url:String, conn:SipConnection) =
          postCallStatus(url, conn)

    def postCallStatus(url:String, conn:SipConnection) : Unit =
        postCallStatus(url, getConnectionMap(conn), Some( (s:String)=>handleBlueML(conn, s) ) )

    def postCallStatus(url:String, map:Map[String,String], handleResponse:Option[(String)=>Unit]) : Unit =
        Option( SequentialWebPoster.postToUrl(url, map) ) match {
            case Some(xml)  => handleResponse.foreach( _(xml) )
            case None       => //ok...
        }
    
    //def postMediaStatus(url:String, )

    def postConversationStatus(convo:ConversationInfo) = 
        postCallStatus(convo.url,getJoinedMap(convo),None)
        
    def postMediaStatus(url:String, media:MediaConnection, conn:SipConnection) =
      SequentialWebPoster.postToUrl(url,
          Map( "CallId" -> conn.connectionid,
               "MediaUrl" -> media.playedFiles.firstOption.getOrElse("")
          ))
    
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
        val conn = telcoServer.createConnection(to, from)
        conn.connect(() => {
            handleConnect(url, conn)
            //send status to the url
        })
    }

    def modifyCall(callid:String, action:BlueMLVerb) {
        val conn = telcoServer.findConnection(callid)
        action match {
            case h:Hangup =>  
                val joinedTo = conn.joinedTo
                conn.disconnect( () => joinedTo match {
                    case Some(x) => println("The unjoin will fire and post back the status, no need to tell it we're hanging up")
                    case None => 
                        postCallStatus(h.url, conn)
                })
                //conn.disconnect( ()=> println("no need to postCall status, should be posting joined status soon"))//postCallStatus(h.url, conn) )
            case p:Play =>    
                println("join to media!")
        }
    }

    def handleDisconnect(url:String, conn:SipConnection) {
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



