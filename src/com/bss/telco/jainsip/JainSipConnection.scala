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
 
import com.bss.telco.jainsip._
import com.bss.telco._
import com.bss.telco.api._
import scala.collection.immutable.Map
import javax.sip.header._
import javax.sip.ServerTransaction
import javax.sdp.SessionDescription
import scala.actors.Actor
import scala.actors.Actor._
import com.bss.util._

class JainSipConnection protected[telco](
                        var connid:String,
                        val to:String,
                        val from:String, 
                        val dir:DIRECTION, 
                        val telco:SipTelcoServer,
                        val disconnectOnUnjoin:Boolean)
                        extends SipConnection
                     	with SipData
                     	with LogHelper 
                     	with Lockable 
                     	with OrderedExecutable {
	  
    override def destination = to
    
    override def origin = from 
	
	private var state:VersionedState = VERSIONED_UNCONNECTED("")
	 
	var contactHeader:Option[ContactHeader] = None
	
	private var stateFunc = Map[VersionedState,()=> Unit]()
 	
 	override def connectionState = state.getState
 
	override def connectionid = connid
 
	override def direction = dir
 
	override def protocol = "SIP"
 
	private var progressingCallback:(SipConnection)=>Unit = null
  	
	var localSdp  = SdpHelper.getBlankSdp(telco.contactIp) //Should be private, can't be for testing purposes, maybe make private anyway and use reflection?

	override def sdp = localSdp 

	protected[jainsip] def setConnectionid(id:String) = connid = id
 

 	override def connect( f:()=> Unit) = connect(localSdp, f)
  
	private def connect(sdp:SessionDescription, connectedCallback:()=> Unit) = wrapLock { 	
 	   	connid = telco.internal.sendInvite(this, sdp)
 	   	telco.addConnection(this)
	  	stateFunc += new VERSIONED_CONNECTED(clientTx.get.getBranchId())->connectedCallback   	
  		setState(VERSIONED_PROGRESSING( clientTx.get.getBranchId() ))
	}
 	
	override  def accept(connectedCallback:()=> Unit) = wrapLock { 
		telco.internal.sendResponse(200, this, localSdp.toString().getBytes())
  	  	stateFunc +=VERSIONED_CONNECTED(serverTx.get.getBranchId()) -> connectedCallback
	}
 
	override  def reject(rejectCallback:()=> Unit) = wrapLock {
		telco.internal.sendResponse(606, this, localSdp.toString().getBytes())
		stateFunc +=  VERSIONED_UNCONNECTED(serverTx.get.getBranchId()) -> rejectCallback
	}
 
	override  def disconnect(disconnectCallback:()=> Unit) = wrapLock {
		telco.internal.sendByeRequest(this)
        val f = ()=> {
                        onDisconnect()
                        disconnectCallback()
        }
        stateFunc +=  VERSIONED_UNCONNECTED(clientTx.get.getBranchId()) ->f
  	}

	override def join(otherCall:Joinable[_], joinCallback:()=>Unit) = wrapLock {
		//debug("OtherCall = " + otherCall)
		otherCall.reconnect(localSdp,()=>{
		   	this.reconnect(otherCall.sdp, ()=>{
    		    this.joinedTo = Some(otherCall)  
		        this.joinedTo.get.joinedTo = Some(this) 
	    		joinCallback()
	    	}) })
	}

    override def silence(f:()=>Unit) = wrapLock {
        println("~~~~~~~~~~SILENCING MYSELF dest = " + destination)
    	SdpHelper.addMediaTo(localSdp, SdpHelper.getBlankSdp(telco.contactIp))
	
      	telco.internal.sendReinvite(this,localSdp) //SWAPED THIS
      	println("bbbbbbbbranch ID for the tx = " + clientTx.get.getBranchId())
      	stateFunc += new VERSIONED_HOLD(clientTx.get.getBranchId())->f
    }

    override def hold(f:()=>Unit) : Unit = wrapLock {
        joinedTo match {
            case None => silence(f)
            case Some(otherConn) => otherConn.silence(()=>hold(f))
        }
    }    

	override def unjoin(f:()=>Unit) = wrapLock {
	    println(" UNJOIN>>>>>>")
	    disconnectOnUnjoin match {
	        case true => disconnect( ()=>disconnectCallback.foreach( _(this) )) 
	        case false=> unjoinCallback.foreach( _(this) )
	    }
	}
  
	override def reconnect(sdp:SessionDescription, f:()=>Unit) : Unit =  wrapLock {
		joinedTo match {	
	  		case None 	=> 	//System.err.println("in reconnect, Nothing is joined, we can reconnect now!")                                                        
	  						telco.internal.sendReinvite(this, sdp) //TODO: fix race condition, should pass in the stateFunc stuff to the sendReinvite method...
	  						stateFunc += new VERSIONED_CONNECTED(clientTx.get.getBranchId())->f
	  				 	
	  		case Some(otherConn) => otherConn.silence( ()=>{
	  		    otherConn.joinedTo = None
	  		    joinedTo = None
	  		    this.reconnect(sdp, f)} )
	  	}
	}


    protected[telco] def setState(s:VersionedState) : Unit = {
        println("ABOUT TO LOCK, LETS SEE IF WE BLOCK my destination =" +destination )
	 	println("my state = " + s)
	 	lock()
	 	println("made it past the lock")
 		debugStateMap(s)
 		state = s
 		if (stateFunc.contains(state) && stateFunc(state) != null) {
 		    println("executing")
			stateFunc(s)()
		} else if ( state.getState == UNCONNECTED() ) { 
		    //If it wasn't in the map, it's an unrequested disconnect that happened remotely.
            onDisconnect()  
			disconnectCallback.foreach( _(this) )

        } else if ( state.getState != PROGRESSING()) {
			stateFunc = Map[VersionedState,()=> Unit]()//WIPE IT ALL, shit happened not in the order we expected!
			telco.fireFailure(this)
		}
		unlock() 
	}

    protected def onDisconnect() = wrapLock {
        println("ON DiSCONNECT")
        joinedTo.foreach( joined=>{
                joinedTo = None 
                joined.joinedTo = None
                joined.unjoin(()=>Unit)
                //disconnect on unjoin should go here?
         })
    }

	def debugStateMap(s:VersionedState) = {
		debug(" ****** debug statemap ****** stateFunc size = " + stateFunc.size )
		debug( "s =" + s)
		for ( (key, value) <- stateFunc ) 
			debug( key + "->" + value )
	}
    
}
 

