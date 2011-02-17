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
import javax.sdp.SessionDescription
import javax.sdp.MediaDescription


class SipTelcoServer(val listeningIp:String, val contactIp:String, val port:Int, val destIp:String, val destPort:Int) extends TelcoServer {
     
     def this(ip:String, port:Int, destIp:String, destPort:Int) =
        this(ip, ip, port, destIp, destPort)
 
	private var incomingCallback:Option[SipConnection=>Unit] = None
	
	private var disconnectedCallback:Option[SipConnection=>Unit] = None
	
	private var failureCallback:Option[SipConnection=>Unit] = None
 
	private val connections = new ConcurrentHashMap[String, JainSipConnection]()
	
   
	protected[jainsip] val internal = new JainSipInternal(this, listeningIp, contactIp, port, destIp, destPort)
 
 
   	override def createConnection(dest:String, callerid:String, disconnectOnUnjoin:Boolean) : SipConnection = {
   	    val conn = new JainSipConnection( null, dest, callerid, new OUTGOING, this, disconnectOnUnjoin)  //this gets in the connections map when an ID is created
   	    conn.disconnectCallback = disconnectedCallback
   	    return conn
   	}

   	
   	override def createConnection(dest:String, callerid:String) = 
        createConnection(dest, callerid, true) 
	   		
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

    override def areTwoConnected(conn1:SipConnection, conn2:SipConnection) : Boolean = {
        val mediatrans1 =  conn1.asInstanceOf[JainSipConnection].joinedTo.get.sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
        val medialist1 = conn2.asInstanceOf[JainSipConnection].sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
      
        val mediatrans2 =  conn2.asInstanceOf[JainSipConnection].joinedTo.get.sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
        val medialist2 = conn1.asInstanceOf[JainSipConnection].sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
      
        //TODO: Check Media Connection!
      
        if ( mediatrans1.getMedia().getMediaPort != medialist1.getMedia().getMediaPort() ) 
    	    return false
      
        if ( mediatrans2.getMedia().getMediaPort != medialist2.getMedia().getMediaPort() ) 
            return false
      
        if ( conn1.connectionState != CONNECTED() || conn2.connectionState != CONNECTED() ) 
            return false
      
        if ( conn1.joinedTo == None || conn2.joinedTo == None ) 
    	    return false
             
        if ( !conn1.joinedTo.get.equals(conn2) || !conn2.joinedTo.get.equals(conn1) ) 
    	    return false
            
        return true
    }

}


