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

class JainSipConnection(var connid:String,
                        val to:String,
                        val from:String, 
                        val dir:DIRECTION, 
                        val telco:SipTelcoServer) 
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
  
	protected[jainsip] var localSdp  = SdpHelper.getBlankSdp(telco.ip) //Should be private, can't be protected for testing purposes

	override def sdp = localSdp 

	protected[jainsip] def setConnectionid(id:String) = connid = id
 
 	protected[jainsip] def setState(s:VersionedState) : Unit = {
 		lock()
 		//debugStateMap(s)
 		state = s
 		if (stateFunc.contains(state) && stateFunc(state) != null) {
			val func = stateFunc(state)
			stateFunc(s)()
		} else if ( state.getState == UNCONNECTED() ) { 
			telco.fireDisconnected(this)
  
        } else if ( state.getState != PROGRESSING()) {
			stateFunc = Map[VersionedState,()=> Unit]()//WIPE IT ALL, shit happened not in the order we expected!
			telco.fireFailure(this)
		}
		unlock() 
	}

	def debugStateMap(s:VersionedState) = {
		debug(" **************** debug statemap ************* stateFunc size = " + stateFunc.size )
		debug( "s =" + s)
		for ( (key, value) <- stateFunc ) 
			debug( key + "->" + value )
	}

    
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
		stateFunc +=  VERSIONED_UNCONNECTED(clientTx.get.getBranchId()) -> disconnectCallback
	} 
  	 
	//IF ANYWHERE IS A RACE CONDITION CLUSTER FUCK, THIS IS IT
	override def join(otherCall:Joinable, joinCallback:()=>Unit) = wrapLock {
		//debug("OtherCall = " + otherCall)
		otherCall.reconnect(localSdp,()=>
		    //System.err.debug("other call has been RECONNECTED, this is in the join")
    		this.reconnect(otherCall.sdp, ()=>{
        		this.joinedTo = Some(otherCall)  
		        this.joinedTo.get.joinedTo = Some(this) 
	    		joinCallback() 
	    	}) )
	}

 
 	override def hold(f:()=>Unit) = wrapLock {
		//System.err.debug("holding! for" + dir.destination)
		SdpHelper.addMediaTo(localSdp, SdpHelper.getBlankSdp(telco.ip))
	
      	telco.internal.sendReinvite(this,localSdp) //SWAPED THIS	
      	stateFunc += new VERSIONED_HOLD(clientTx.get.getBranchId())->( ()=> {
			joinedTo.foreach( _.joinedTo = None)
			joinedTo = None
			f()
		})
      	
	}
  
	override def reconnect(sdp:SessionDescription, f:()=>Unit) : Unit =  wrapLock {
		//System.err.debug("reconnecting for " + dir.destination)
   		joinedTo match {
		  	
	  		case None 	=> //	System.err.debug("in reconnect, Nothing is joined, we can reconnect now!")                                                        
	  						telco.internal.sendReinvite(this, sdp)
	  						stateFunc += new VERSIONED_CONNECTED(clientTx.get.getBranchId())->f
	  				 	
	  		case Some(x) => //System.err.debug("We are joined to something, lets put the other call on hold!")
	  						x.hold(()=>this.reconnect(sdp, f))
	  	}
	}
 	 
 }
 

