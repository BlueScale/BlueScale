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
import com.bss.util.WebUtil

class Engine(telcoServer:TelcoServer, defaultUrl:String) {
   
    def handleBlueML(conn:SipConnection, str:String) : Unit  =
        handleBlueML(conn, BlueMLParser.parse(str))

    
    def handleBlueML(conn:SipConnection, verbs:Seq[BlueMLVerb]) : Unit = 
        verbs.foreach( _ match { 
                case dial:Dial => handleDial(conn, dial)

                case play:Play => println("play")
            })

    
    def handleDial(conn:SipConnection, dial:Dial) = {
        conn.connectionState match {
            //need to figure out how you can transfer/hold 
            case u:UNCONNECTED  => conn.accept( ()=> {
                                                val destConn = telcoServer.createConnection(dial.number,"2222222222")
                                                destConn.connect( ()=>{ println("destConn connected!")
                                                    conn.join(destConn, 
                                                             ()=> postCallStatus(dial.url, getJoinedMap(conn, destConn), (s:String)=>println("shurg"))
                                                    )
                                                })  
                                            })

            case p:PROGRESSING  => println("progressing") //TODO: should we sleep and call again? 
        }
    }

    
    def handleIncomingCall(url:String, conn:SipConnection) = 
        postCallStatus(url, conn)

    def handleConnect(url:String, conn:SipConnection) =
          postCallStatus(url, conn)

    def postCallStatus(url:String, conn:SipConnection) : Unit =
        postCallStatus(url, getConnectionMap(conn),  (s:String)=>handleBlueML(conn, s) )

    def postCallStatus(url:String, map:Map[String,String], handleResponse:(String)=>Unit) : Unit =
        Option( WebUtil.postToUrl(url, map) ) match {
            case Some(xml)  => handleResponse(xml)
            case None       => //ok...
        }
   
    
    def getConnectionMap(conn:SipConnection) = 
        Map( "CallId"->conn.connectionid,
             "From"-> conn.origin,
             "To" -> conn.destination,
             "CallStatus" -> conn.connectionState.toString(),
             "Direction" -> conn.direction.toString() )    
    
    
    def getJoinedMap(conn1:SipConnection, conn2:SipConnection) = 
        Map( "FirstCallId"->conn1.connectionid,
             "SecondCallId"->conn2.connectionid,
             "ConversationStatus"-> getJoinedState(conn1, conn2) )


    def getJoinedState(conn1:SipConnection, conn2:SipConnection) : String =
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
        //val response = WebUtil.getCallResponse(conn.connectionid, to, from, "progressing")
    }

    def modifyCall(callid:String, action:BlueMLVerb) {
        val conn = telcoServer.findConnection(callid)
        action match {
            case h:Hangup =>  conn.disconnect( ()=> postCallStatus("url", conn) )
            case p:Play =>    println("join to media!")
            //case t:Transfer =>println("join to someone")
        }

    }


}

