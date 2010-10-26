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

package com.bss.telco.jainsip


import java.util.concurrent.ConcurrentHashMap
import com.bss.telco._
import com.bss.telco.api._
import com.bss.telco.jainsip._
import com.bss.util._


class SipTelcoServer(val ip:String, val port:Int, destIp:String, val destPort:Int) extends TelcoServer {
 
	private var incomingCallback:Option[SipConnection=>Unit] = None
	
	private var disconnectedCallback:Option[SipConnection=>Unit] = None
	
	private var failureCallback:Option[SipConnection=>Unit] = None
 
	private val connections = new ConcurrentHashMap[String, JainSipConnection]()
   
	protected[jainsip] val internal = new JainSipInternal(this, ip, port, destIp, destPort)
 
 
   	override def createConnection(dest:String, callerid:String) = 
	  new JainSipConnection( null, OUTGOING(dest, callerid), this) //this gets in the connections map when an ID is created
  
		
	override def findConnection(id:String): SipConnection = 
		connections.get(id)
	
	protected[jainsip] def getConnection(id:String) = connections.get(id)
	
	protected[jainsip] def addConnection(conn:JainSipConnection) : Unit = 
    	connections.put(conn.connectionid, conn)
    
    protected[jainsip] def removeConnection(conn:SipConnection) : Unit = 
    	connections.remove(conn.connectionid)
 
    
	override def start() {
		internal.start()
	}

	override def stop() {
		internal.stop()
	}
 
	override def setFailureCallback(f:(SipConnection)=>Unit) = failureCallback = Some(f)
		
	override def setIncomingCallback(f:(SipConnection)=> Unit) = incomingCallback = Some(f)
			
	override def setDisconnectedCallback(f:(SipConnection)=> Unit) = disconnectedCallback = Some(f)
	
	def fireFailure(c:SipConnection) 		= failureCallback.foreach( _(c) ) //maybeFire(c,failureCallback)
					
	def fireDisconnected(c:SipConnection) 	= disconnectedCallback.foreach( _(c) ) //maybeFire(c, disconnectedCallback)
	
	def fireIncoming(c:SipConnection) = {
		incomingCallback.foreach( _(c) )
	}
}
