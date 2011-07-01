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
import javax.sdp.SessionDescription
import scala.actors.Actor
import scala.actors.Actor._
import com.bss.util._
import com.bss.telco.Types._

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
                     	    with OrderedExecutable 
                     	    with StateExecutor {
	  
    override def destination = to
    
    override def origin = from 
	
	private var state:VersionedState = VERSIONED_UNCONNECTED("")
	 
 	override def connectionState = state.getState
 
	override def connectionid = connid
 
	override def direction = dir
 
	override def protocol = "SIP"

	private var progressingCallback:Option[(SipConnection)=>Unit] = None 
  	
    var localSdp  = SdpHelper.getBlankSdp(telco.contactIp) //Should be private, can't be for testing purposes, maybe make private anyway and use reflection?

	override def sdp = localSdp 

	protected[jainsip] def setConnectionid(id:String) = connid = id
 
 	override def connect( f:FinishFunction) = connect(localSdp, false, f)

    override def connect(sdp:SessionDescription, connectAnyMedia:Boolean, callback:FinishFunction) = wrapLock {
        connid = telco.internal.sendInvite(this, sdp)
        telco.addConnection(this)
        
 	   	if (connectAnyMedia)
     	   	setFinishFunction( new VERSIONED_RINGING(clientTx.get.getBranchId()), callback )
    
        setFinishFunction( new VERSIONED_CONNECTED(clientTx.get.getBranchId()), callback )
 	   	
 	   	progressingCallback.foreach( _(this) )
    }

    override def onConnect(callback:FinishFunction) = wrapLock {
        connectionState match {
            case CONNECTED() =>
                callback()
            case  UNCONNECTED() | RINGING() =>
                setFinishFunction(new VERSIONED_CONNECTED(clientTx.get.getBranchId()), callback )
        }
    }
                
    override def accept(toJoin:Joinable[_], connectedCallback:FinishFunction) = wrapLock {
        connectionState match {
            case UNCONNECTED() | RINGING() =>
		        telco.internal.sendResponse(200, serverTx, toJoin.sdp.toString().getBytes())
                setFinishFunction(VERSIONED_CONNECTED(serverTx.get.getBranchId()), connectedCallback)
	        case _ => 
	            throw new InvalidStateException(new UNCONNECTED(), connectionState)
	    }       
    }

    override def accept(connectedCallback:FinishFunction) = wrapLock {
	    accept(SdpHelper.getBlankJoinable(telco.contactIp), connectedCallback) 
	}

    override def ring() =
        ring(SdpHelper.getBlankJoinable(telco.contactIp))
    
	override def ring(toJoin:Joinable[_]) = wrapLock {
        connectionState match {
            case UNCONNECTED() =>
                telco.internal.sendResponse(180, serverTx, toJoin.sdp.toString().getBytes())
            case _ =>
                throw new InvalidStateException(new UNCONNECTED, connectionState)
        }
	}
 
	override def reject(rejectCallback:FinishFunction) = wrapLock {
		telco.internal.sendResponse(606, serverTx, localSdp.toString().getBytes())
		setFinishFunction(VERSIONED_UNCONNECTED(serverTx.get.getBranchId()), rejectCallback) 
	}
 
	override def disconnect(disconnectCallback:FinishFunction) = wrapLock {
		telco.internal.sendByeRequest(this)
        val f = ()=> {
                        onDisconnect()
                        disconnectCallback()
        }
        setFinishFunction(VERSIONED_UNCONNECTED(clientTx.get.getBranchId()), f)
  	}

  	override def cancel(f:FinishFunction) =  
  	    dir match {
            case INCOMING() => cancelIncoming(f)
            case OUTGOING() => cancelOutgoing(f)
      	}

  	protected def cancelIncoming(cancelCallback:FinishFunction) = wrapLock {
        connectionState match {
            case UNCONNECTED() => 
                state = VERSIONED_CANCELED("") //not waiting on anything, no need
                //need to wipe the state map!
                telco.internal.sendResponse(487, serverTx, null)
                telco.internal.sendResponse(200, serverCancelTx, null)
                cancelCallback()//we need to make sure it got cancelled....?
                //there is a race condition if someone had called accept, and it isn't connected yet, and then someone calls cancel

            case CONNECTED() => println("too late!")//don't think I gotta do anything here according to SIP
        }
  	}

  	protected def cancelOutgoing(cancelCallback:FinishFunction) = wrapLock {
        //to implement
        connectionState match {
            case PROGRESSING() | UNCONNECTED() | RINGING() =>
                clientTx.foreach( tx => {
                    telco.internal.sendCancel(this)
                    setFinishFunction(VERSIONED_CANCELED(clientTx.get.getBranchId()), cancelCallback)
                    })
            
            case _ => 
                throw new InvalidStateException(new PROGRESSING(), connectionState )
        }
  	}
    
  	override def join(otherCall:Joinable[_], joinCallback:FinishFunction) = wrapLock {
  	    
  	    if (connectionState != CONNECTED())
  	        throw new InvalidStateException( new CONNECTED(), connectionState )
        
        otherCall.connectionState match {
            case UNCONNECTED() =>
                otherCall.connect(localSdp, true, ()=>{
                    otherCall.joinedTo = Some(this)
                    this.reconnect(otherCall.sdp, ()=>{
                        this.joinedTo = Some(otherCall)
                        //this.joinedTo.get.joinedTo = Some(this)
                        otherCall.onConnect(joinCallback)    
                    })
               })
              
            case CONNECTED() =>
                joinConnected(otherCall, joinCallback)
        }
    }

	private def joinConnected(otherCall:Joinable[_], joinCallback:FinishFunction) = wrapLock {
		otherCall.reconnect(localSdp,()=>{
		    otherCall.joinedTo = Some(this)
		   	this.reconnect(otherCall.sdp, ()=>{
    		    this.joinedTo = Some(otherCall)  
	    		joinCallback()
	    	}) 
	    })
	}

    //maybe rename to Mute?  It makes it so this SipConnection stops TRANSMITTING to anything...
    override def silence(silenceCallback:FinishFunction) = wrapLock {
    	SdpHelper.addMediaTo(localSdp, SdpHelper.getBlankSdp(telco.contactIp))
	
      	telco.internal.sendReinvite(this,localSdp) //SWAPED THIS
        setFinishFunction(new VERSIONED_HOLD(clientTx.get.getBranchId()), silenceCallback)
    }

    override def hold(f:FinishFunction) : Unit = wrapLock {
        joinedTo match {
            case None => silence(f)
            case Some(otherConn) => otherConn.silence(()=>hold(f))
        }
    }    

	override def unjoin(unjoinedFrom:Joinable[_], f:FinishFunction) = wrapLock {
        disconnectOnUnjoin match {
	        case true =>
	            disconnect( ()=> {
	                disconnectCallback.foreach( _(this) )
	                unjoinCallback.foreach( _(unjoinedFrom, this) )
	            })

	        case false =>
	            unjoinCallback.foreach( _(unjoinedFrom, this) )
	    }
	}
  
	override def reconnect(sdp:SessionDescription, reconnectCallback:FinishFunction) : Unit =  wrapLock {
		joinedTo match {	
	  		case None 	=> 	
	  		    telco.internal.sendReinvite(this, sdp) //TODO: fix race condition, should pass in the stateFunc stuff to the sendReinvite method...
	  			setFinishFunction(new VERSIONED_CONNECTED(clientTx.get.getBranchId()), reconnectCallback)
	  				 	
	  		case Some(otherConn) => 
	  		    otherConn.silence( ()=>{
	  		        otherConn.joinedTo = None
	  		        joinedTo = None
	  		        this.reconnect(sdp, reconnectCallback)
	  		    })
	  	}
	}

    protected def onDisconnect() = wrapLock {
        joinedTo.foreach( joined=>{
                joinedTo = None 
                joined.joinedTo = None
                joined.unjoin(this,()=>Unit)
         })
    }

    protected[telco] def setState(s:VersionedState) : Unit = {
        lock()
        //println(" SET STATE CALLED FOR " + this + " s = " + s)
        var b = false
        if ( state.getState == CONNECTED() && s.getState == CONNECTED() )
            b = true

        state = s
        stateMap.contains(state) match {
            case true =>
                    val f = stateMap(state)
                    stateMap = stateMap.filter { case (key, value) => !value.equals(f) } 
                    f() //no deadlock here, it's reentrant
            case false =>
                    if (state.getState == UNCONNECTED()) {
                        onDisconnect()
                        disconnectCallback.foreach( _(this) )
                    }
        }
        unlock()
    }

    override def toString() = 
	    "JainSipConnection " + direction + " TO:"+destination + " Hashcode = " + hashCode

       
}
 

