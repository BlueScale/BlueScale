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
package com.bss.ccxml.event 

import com.bss.telco.api.SipConnection

abstract class TelecomEvent(val nme:String) extends CCXMLEvent(nme);
  

// This is a bug in scala! can't override the same constructor 
abstract case class ConnectionEvent(
	nam:			String,  
    val connectionid:	String, 
    val protocol:		String, 
    val info:			EcmaObject,
	val connection:	SipConnection,
	val eventid: 		String,
	val eventsource:	String,
    val sourcetype:	String) 
	extends TelecomEvent(nam) 


class ConnectionMerged(
	mergeid:		String,
	name:			String, 
    connectionid:	String, 
    protocol:		String, 
	info:			EcmaObject,
	connection:		SipConnection,
	eventid: 		String,
	eventsource:	String,
	sourcetype:	 	String)
	extends ConnectionEvent(name, connectionid, protocol, info, connection, eventid, eventsource, sourcetype)
    

class ConnectionAlerting(
	conn:SipConnection)
    extends ConnectionEvent( "connection.alerting", conn.connectionid, conn.protocol, null, conn, "1111", "JainSip", "sourcetype") {}
        
        
class ConnectionDisconnected(
	reason: 		String,
	conn: 			SipConnection)
    extends ConnectionEvent("connection.disconnected", conn.connectionid, conn.protocol, null, conn, "1111", "JainSip", "sourcetype") {}

class ConnectionConnected(
	conn:SipConnection)
    extends ConnectionEvent(  "connection.connected", conn.connectionid, conn.protocol, null, conn, "1234", "BssTelcoServer", "") {}

class ConnectionProgressing(conn:SipConnection)
    extends ConnectionEvent("connection.progressing", conn.connectionid, conn.protocol, null, conn, "1234", "BsstelcoServer", "") {}

class ConnectionRedirected(
	reason: 		String,
	trigger: 		String,
	name:			String, 
    conn:SipConnection)
    extends ConnectionEvent(name, conn.connectionid, conn.protocol, null, conn, "1234", "1234", "1234") {}
      
abstract class ConferenceEvent(name:String)
	extends TelecomEvent(name)
	
	
	
class ConferenceJoined(
	callId1:String,
	callId2:String)
	extends ConferenceEvent("conference.joined")
