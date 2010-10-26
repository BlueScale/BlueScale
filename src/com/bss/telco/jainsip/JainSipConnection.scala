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
 
import scala.actors.Actor
import scala.actors.Actor._ 
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
                        val dir:DIRECTION, 
                        val telco:SipTelcoServer) 
                     	extends SipConnection
                     	with LogHelper 
                     	with Lockable {
	  
  
	private var state:VersionedState = VERSIONED_UNCONNECTED(0)
	 
	var contactHeader:Option[ContactHeader] = None
	 
	private var stateFunc = Map[VersionedState,()=> Unit]()
	
 	override def connectionState = state.getState
 
	override def connectionid = connid
 
	override def direction = dir
 
	override def protocol = "SIP"
 
	private var progressingCallback:(SipConnection)=>Unit = null
  
	protected[jainsip] var localSdp  = SdpHelper.getBlankSdp(telco.ip) //Should be private, can't be protected for testing purposes

	override def sdp = localSdp 

  	protected[jainsip] var sip:Option[SipData] = None
  
	protected[jainsip] def setConnectionid(id:String) = connid = id

	private var externalVersion = 0
	private var internalVersion = 0

	private def extVers : Int = {
		externalVersion += 1
		return externalVersion
	}

	private def intVers : Int = {
		internalVersion += 1
		return internalVersion
	}
 
 	protected[jainsip] def setState(s:ConnectionState) : Unit = {
 		lock()
 		//debugStateMap(s)
 		state = s match {
			case CONNECTED() => VERSIONED_CONNECTED(extVers)
			case HOLD() 	=> VERSIONED_HOLD(extVers)
			case UNCONNECTED() => VERSIONED_UNCONNECTED(extVers)
			case PROGRESSING() => VERSIONED_PROGRESSING(externalVersion) //Don't increment a progressing!
 		}

		if (stateFunc.contains(state) && stateFunc(state) != null) {
			

			val func = stateFunc(state)
			stateFunc(state)()
		} else if ( s == UNCONNECTED() ) { 
			telco.fireDisconnected(this)
  
		} else if ( s != PROGRESSING()) {
			stateFunc = Map[VersionedState,()=> Unit]()//WIPE IT ALL, shit happened not in the order we expected!
			telco.fireFailure(this)
			externalVersion = 0

			internalVersion = 0 
															     
		}
		unlock() 
	}

	def debugStateMap(s:ConnectionState) = {
		debug(" **************** debug statemap *************")
		debug( "s =" + s)
		debug(" ext vers = "+  externalVersion )
		debug(" int vers = " + internalVersion )
		for ( (key, value) <- stateFunc ) 
			debug( key + "->" + value )
	}

   
   /*
    * This is to multithread Our network stack a bit better
    * 
    */
    private val act:Actor = actor {
		loop { react {
    	   case f:( ()=>Unit ) => try {
    		   						f()
    	   						  } catch {
    	   						  	case ex:Exception => debug("damn, exception, ex = " + ex)
    	   						  						 ex.printStackTrace()
    	   						  						 telco.removeConnection(this)
    	   						  						 telco.fireFailure(this)
    	   						  }
            
         	  case _ => error("Something was sent to the execute method of " + this + " That shouldn't have been!") 
    	 }}  
     }
  
    def execute(f:()=>Unit) = act ! f
    
 	override def connect( f:()=> Unit) = connect(localSdp, f)
  
	private def connect(sdp:SessionDescription, connectedCallback:()=> Unit) = wrapLock { 	
 	   	connid = telco.internal.sendInvite(this, sdp)
		telco.addConnection(this)
		setState(PROGRESSING())
	  	stateFunc += new VERSIONED_CONNECTED(intVers)->connectedCallback   	
	}
 	
	override  def accept(connectedCallback:()=> Unit) = wrapLock { 
		telco.internal.sendResponse(200, this, localSdp.toString().getBytes())
  	  	stateFunc +=VERSIONED_CONNECTED(intVers) -> connectedCallback
	}
 
	override  def reject(rejectCallback:()=> Unit) = wrapLock {
		telco.internal.sendResponse(606, this, localSdp.toString().getBytes())
		stateFunc +=  VERSIONED_UNCONNECTED(intVers) -> rejectCallback
	}
 
	override  def disconnect(disconnectCallback:()=> Unit) = wrapLock {
		val client = telco.internal.getByeRequest(this)
		sip.get.dialog.sendRequest(client) 
		stateFunc +=  VERSIONED_UNCONNECTED(intVers) -> disconnectCallback
	} 
  	 
	//IF ANYWHERE IS A RACE CONDITION CLUSTER FUCK, THIS IS IT
	override def join(otherCall:Joinable, joinCallback:()=>Unit) = wrapLock {
		//debug("OtherCall = " + otherCall)
		otherCall.reconnect(localSdp,()=>{ 

		//System.err.debug("other call has been RECONNECTED, this is in the join")
		this.reconnect(otherCall.sdp, ()=>{

		this.joinedTo = Some(otherCall)  
		this.joinedTo.get.joinedTo = Some(this) 
	
		joinCallback() }) })
	}

 
 	override def hold(f:()=>Unit) = wrapLock {
		//System.err.debug("holding! for" + dir.destination)
		SdpHelper.addMediaTo(localSdp, SdpHelper.getBlankSdp(telco.ip))
		stateFunc += new VERSIONED_HOLD(intVers)->( ()=> {
			joinedTo.foreach( _.joinedTo = None)
			joinedTo = None
			f()
		})
      	telco.internal.sendReinvite(this,localSdp) //SWAPED THIS
	}
  
	override def reconnect(sdp:SessionDescription, f:()=>Unit) : Unit =  wrapLock {
		//System.err.debug("reconnecting for " + dir.destination)
   		joinedTo match {
		  	
	  		case None 	=> //	System.err.debug("in reconnect, Nothing is joined, we can reconnect now!")                                                        
	  						telco.internal.sendReinvite(this, sdp)
	  						stateFunc += new VERSIONED_CONNECTED(intVers)->f
	  				 	
	  		case Some(x) => //System.err.debug("We are joined to something, lets put the other call on hold!")
	  						joinedTo.get.hold(
	  						()=>{this.reconnect(sdp, f)})
	  	}
	}
 	 
 }
 
