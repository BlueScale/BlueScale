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

package org.bluescale.telco.jainsip

import java.util.concurrent.ConcurrentHashMap
import org.bluescale.telco._
import org.bluescale.telco.api._
import org.bluescale.telco.jainsip._
import org.bluescale.util._
import javax.sdp.SessionDescription
import javax.sdp.MediaDescription
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.TimeUnit


class SipTelcoServer(
    val listeningIp:String, 
    val contactIp:String, 
    val port:Int,
    val destIp:String,
    val destPort:Int,
    val authenticateRegister:Boolean = true) extends TelcoServer {
     
     def this(ip:String, port:Int, destIp:String, destPort:Int) =
        this(ip, ip, port, destIp, destPort)
        
    /*
     * Callbakcs users of the API can register for 
     */
 
	private var incomingCallback: Option[SipConnection=>Unit] = None
	
	private var disconnectedCallback: Option[SipConnection=>Unit] = None
	
	private var failureCallback: Option[SipConnection=>Unit] = None

	private var unjoinCallback: Option[(Joinable[_],SipConnection) => Unit] = None
	
	private var registerCallback: Option[(IncomingRegisterRequest)=>Unit] = None
	
	private val registerLock = new ReentrantLock()
	
	/*
	 * Internal collections
	 */
	
	protected[jainsip] val connections = new ConcurrentHashMap[String, SipConnectionImpl]()

	protected[jainsip] val registeredAddresses = new ConcurrentHashMap[String, String]()
	
	protected val registerAuthInfo = new ConcurrentHashMap[String, SipAuth]()
	
	protected[jainsip] val internal = new JainSipInternal(this, listeningIp, contactIp, port, destIp, destPort)

    
   	override def createConnection(dest: String, callerid :String, disconnectOnUnjoin: Boolean) : SipConnection = {
   	    //val conn = new JainSipConnection( null, dest, callerid, new OUTGOING, this, disconnectOnUnjoin)  //this gets in the connections map when an ID is created
   	    val conn = new SipConnectionImpl( null, dest, callerid, new OUTGOING, this, disconnectOnUnjoin)  //this gets in the connections map when an ID is created

   	    conn.disconnectCallback = disconnectedCallback
   	    conn.unjoinCallback = unjoinCallback
   	    return conn
   	}
   	
   	override def createConnection(dest: String, callerid: String) = 
        createConnection(dest, callerid, true) 
	   		
	override def findConnection(id: String): SipConnectionImpl = 
		connections.get(id)
	
	protected[jainsip] def getConnection(id: String) = connections.get(id)
	
	protected[jainsip] def addConnection(conn: SipConnectionImpl) : Unit = 
    	connections.put(conn.connectionid, conn)
    
    protected[jainsip] def removeConnection(conn:SipConnection) : Unit = 
    	connections.remove(conn.connectionid)
    
	override def start() {
		internal.start()
	}

	override def stop() {
		internal.stop()
	}
 
	override def setFailureCallback(f: SipConnection => Unit) = failureCallback = Some(f)
		
	override def setIncomingCallback(f: SipConnection => Unit) = incomingCallback = Some(f)
			
	override def setDisconnectedCallback(f: SipConnection => Unit) = disconnectedCallback = Some(f)

	override def setUnjoinCallback(f: (Joinable[_],SipConnection) => Unit) = unjoinCallback = Some(f)
	
	override def setRegisterCallback(f: IncomingRegisterRequest=>Unit ) = registerCallback = Some(f) 
	
	def fireFailure(c:SipConnection) = failureCallback.foreach( _(c) ) 

	def fireDisconnected(c:SipConnection) = disconnectedCallback.foreach( _(c) ) 
	
	def fireIncoming(c:SipConnection) = incomingCallback.foreach( _(c) )
	
	def fireRegister(r:IncomingRegisterRequest) = registerCallback.foreach( _(r))
	
	def silentSdp() =
	    SdpHelper.getBlankSdp(this.contactIp)

	def silentJoinable() =
	    SdpHelper.getBlankJoinable(this.contactIp)

    override def areTwoConnected(c1:SipConnection, c2:SipConnection) : Boolean = {
        if ( c1.connectionState != CONNECTED() || c2.connectionState != CONNECTED() ) 
            return false
      
        if ( c1.joinedTo == None || c2.joinedTo == None ) 
    	    return false

        for (
            conn1 <- c1.asInstanceOf[SipConnectionImpl].joinedTo;
            conn2 <- c2.asInstanceOf[SipConnectionImpl].joinedTo
        ) {
            val mediatrans1 =  conn1.asInstanceOf[SipConnectionImpl].joinedTo.get.sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
            val medialist1 = conn2.asInstanceOf[SipConnectionImpl].sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
      
            val mediatrans2 =  conn2.asInstanceOf[SipConnectionImpl].joinedTo.get.sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
            val medialist2 = conn1.asInstanceOf[SipConnectionImpl].sdp.getMediaDescriptions(false).get(0).asInstanceOf[MediaDescription];
      
            //TODO: Check Media Connection!
      
            if ( mediatrans1.getMedia().getMediaPort != medialist1.getMedia().getMediaPort() ) 
    	        return false
      
            if ( mediatrans2.getMedia().getMediaPort != medialist2.getMedia().getMediaPort() ) 
                return false
        }
    	
        return true
    }

	//FIXME: race condition here
	override def sendRegisterRequest(dest:String, user:String, password:String, domain:String) =
			safeLockRegisterAuthInfo(()=> {
				val txid = internal.sendRegisterRequest(user,dest)
				registerAuthInfo.put(txid, SipAuth(user,password, domain))
			})
	
	def safeLockRegisterAuthInfo(f:()=>SipAuth) =
		registerLock.tryLock(5,TimeUnit.SECONDS) match {
			case true =>
			  	try 
			  		f()
			    finally 
					registerLock.unlock()
			case false =>
				throw new Exception("Could not acquire lock, better to kill this thread than deadlock!")
		}
	
	def getRegistAuthInfo(str:String) = 
		safeLockRegisterAuthInfo( ()=>registerAuthInfo.get(str))
	 
    //TODO: fire callback
    def addSipBinding(regAddress:String, contactAddress:String): Unit = {
        registeredAddresses.put(regAddress, contactAddress)
    }


    def getSipAddrForPhone(phone:String): String = {
        //try the local cache, if not, webservice request.
        return registeredAddresses.get(phone)

    }
}

case class SipAuth(val user:String, val pass:String, val domain:String)




