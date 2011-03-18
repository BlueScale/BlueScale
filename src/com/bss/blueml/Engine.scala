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

package com.bss.blueml

import com.bss.telco.api._
import com.bss.util._

class Engine(telcoServer:TelcoServer, defaultUrl:String) extends Util {
   
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
            case dial:Dial => handleDial(conn, dial, verbs.tail)
            case play:Play => println("playing...")
                              handleBlueML(conn, verbs.tail)
        }
    }
    
    protected def handleDial(conn:SipConnection, dial:Dial, verbs:Seq[BlueMLVerb]) = 
        conn.connectionState match {
            case c:CONNECTED =>
                                dialJoin(conn, dial, verbs)
                                
            //need to figure out how you can transfer/hold 
            case u:UNCONNECTED  => 
                            conn.accept( ()=> dialJoin(conn, dial, verbs) )
           
           case p:PROGRESSING  => 
                                println("progressing") //TODO: should we sleep and call again? 
        }
    

    protected def dialJoin(conn:SipConnection, dial:Dial, verbs:Seq[BlueMLVerb]) = {
        val destConn = telcoServer.createConnection(dial.number,"2222222222")
        destConn.connect( ()=> 
            conn.join(destConn, ()=> postCallStatus(dial.url, getJoinedMap(conn, destConn), None) ))
        dial.ringLimit match {
            case -1 => println("ok nothing to do but hope it connects")

            case _ => 
                Thread.sleep(dial.ringLimit*(1000))
                destConn.cancel( ()=> handleBlueML(conn, verbs) )                
        }
    }

    
    def handleIncomingCall(url:String, conn:SipConnection) = 
        postCallStatus(url, conn)

    def handleConnect(url:String, conn:SipConnection) =
          postCallStatus(url, conn)

    def postCallStatus(url:String, conn:SipConnection) : Unit =
        postCallStatus(url, getConnectionMap(conn), Some( (s:String)=>handleBlueML(conn, s) ) )

    def postCallStatus(url:String, map:Map[String,String], handleResponse:Option[(String)=>Unit]) : Unit =
        Option( WebUtil.postToUrl(url, map) ) match {
            case Some(xml)  => handleResponse.foreach( _(xml) )
            case None       => //ok...
        }
   
    
    protected def getConnectionMap(conn:SipConnection) = 
        Map( "CallId"->conn.connectionid,
             "From"-> conn.origin,
             "To" -> conn.destination,
             "CallStatus" -> conn.connectionState.toString(),
             "Direction" -> conn.direction.toString() )    
    
    
    protected def getJoinedMap(conn1:SipConnection, conn2:SipConnection) = 
        Map( "FirstCallId"->conn1.connectionid,
             "SecondCallId"->conn2.connectionid,
             "ConversationStatus"-> getJoinedState(conn1, conn2) )


    protected def getJoinedState(conn1:SipConnection, conn2:SipConnection) : String =
        telcoServer.areTwoConnected(conn1,conn2) match {
                    case true => "Connected"
                    case false=> "ConnectionFailed"
                    }

    
     def newCall(to:String, from:String, url:String) {
        //todo: make sure it's all valid
        val conn = telcoServer.createConnection(to, from)
        conn.connect(
            () => handleConnect(url, conn)
            //send status to the url
        )
    }

    def modifyCall(callid:String, action:BlueMLVerb) {
        val conn = telcoServer.findConnection(callid)
        action match {
            case h:Hangup =>  
                println("hanging up.....")
                conn.disconnect( ()=> postCallStatus(h.url, conn) )
            case p:Play =>    
                println("join to media!")
        }
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

