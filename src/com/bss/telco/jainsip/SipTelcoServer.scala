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


class SipTelcoServer(
    val listeningIp:String, 
    val contactIp:String, 
    val port:Int,
    val destIp:String,
    val destPort:Int) extends TelcoServer {
     
     def this(ip:String, port:Int, destIp:String, destPort:Int) =
        this(ip, ip, port, destIp, destPort)
 
	private var incomingCallback:Option[SipConnection=>Unit] = None
	
	private var disconnectedCallback:Option[SipConnection=>Unit] = None
	
	private var failureCallback:Option[SipConnection=>Unit] = None

	private var unjoinCallback:Option[(Joinable[_],SipConnection) => Unit] = None

	protected[jainsip] val connections = new ConcurrentHashMap[String, JainSipConnection]()
	
	protected[jainsip] val internal = new JainSipInternal(this, listeningIp, contactIp, port, destIp, destPort)
 
   	override def createConnection(dest:String, callerid:String, disconnectOnUnjoin:Boolean) : SipConnection = {
   	    val conn = new JainSipConnection( null, dest, callerid, new OUTGOING, this, disconnectOnUnjoin)  //this gets in the connections map when an ID is created
   	    conn.disconnectCallback = disconnectedCallback
   	    conn.unjoinCallback = unjoinCallback
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
 
	override def setFailureCallback(f:(SipConnection) => Unit) = failureCallback = Some(f)
		
	override def setIncomingCallback(f:(SipConnection) => Unit) = incomingCallback = Some(f)
			
	override def setDisconnectedCallback(f:(SipConnection) => Unit) = disconnectedCallback = Some(f)

	override def setUnjoinCallback(f:(Joinable[_],SipConnection) => Unit) = unjoinCallback = Some(f)
	
	def fireFailure(c:SipConnection) 		= failureCallback.foreach( _(c) ) 

	def fireDisconnected(c:SipConnection) 	= disconnectedCallback.foreach( _(c) ) 
	
	def fireIncoming(c:SipConnection)   = incomingCallback.foreach( _(c) )

    override def areTwoConnected(c1:SipConnection, c2:SipConnection) : Boolean = {
        println(" ARE TWO CONNECTED ?????????????") 
        if ( c1.connectionState != CONNECTED() || c2.connectionState != CONNECTED() ) 
            return false
      
        println("MADE IT HERE c1.joinedTo = " + c1.joinedTo + " | c2.joinedTo = "+ c2.joinedTo)
        if ( c1.joinedTo == None || c2.joinedTo == None ) 
    	    return false
    
        println(" ------------------------------ are two connceted --------------------")
    	c1.asInstanceOf[JainSipConnection].joinedTo.foreach( conn1 =>
            c2.asInstanceOf[JainSipConnection].joinedTo.foreach( conn2 => {

            val mediatrans1 =  conn1.asInstanceOf[JainSipConnection].joinedTo.get.sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
            val medialist1 = conn2.asInstanceOf[JainSipConnection].sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
      
            val mediatrans2 =  conn2.asInstanceOf[JainSipConnection].joinedTo.get.sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
            val medialist2 = conn1.asInstanceOf[JainSipConnection].sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
      
            //TODO: Check Media Connection!
            println(" OK we're checking here")
      
            if ( mediatrans1.getMedia().getMediaPort != medialist1.getMedia().getMediaPort() ) 
    	        return false
      
            if ( mediatrans2.getMedia().getMediaPort != medialist2.getMedia().getMediaPort() ) 
                return false
        }))
        println(" OK NOW IT'S WEIRD< RETURNING TRUE")
        return true
    }

}


