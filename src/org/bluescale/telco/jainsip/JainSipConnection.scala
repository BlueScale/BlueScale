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
package org.bluescale.telco.jainsip 
 
import org.bluescale.telco.jainsip._
import org.bluescale.telco._
import org.bluescale.telco.api._
import scala.collection.immutable.Map
import javax.sip.header._
import javax.sdp.SessionDescription

import org.bluescale.telco._
import org.bluescale.telco.Types._

import scala.actors.Actor
import scala.actors.Actor._
import org.bluescale.util._
import org.bluescale.telco.Types._

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
    
    private var _joinedTo:Option[Joinable[_]] = None
    
    override def joinedTo = _joinedTo
	
	private var state:VersionedState = VERSIONED_UNCONNECTED("")
	 
 	override def connectionState = state.getState  //FIXME: this should be a method that evaluates if soemthing is joined to it or not. 
 
	override def connectionid = connid
 
	override def direction = dir
 
	override def protocol = "SIP"

	private var progressingCallback:Option[(SipConnection)=>Unit] = None 
  	
    var listeningSdp = SdpHelper.getBlankSdp(telco.contactIp) //Should be private, can't be for testing purposes, maybe make private anyway and use reflection?

	override def sdp = listeningSdp 

	protected[jainsip] def setConnectionid(id:String) = connid = id
 
 	override def connect( f:FinishFunction) = connect(SdpHelper.getJoinable(listeningSdp), false, f)//shouldn't be this.  that's weird

 	override def connect(join:Joinable[_], callback:FinishFunction) : Unit = connect(join, false, callback) 

    protected[telco] override def connect(join:Joinable[_], connectAnyMedia:Boolean, callback:()=>Unit) = wrapLock {
        state.getState match {
            case s:UNCONNECTED =>
                newConnect(join, connectAnyMedia,callback)
            case s:CONNECTED =>
             reconnect(join, callback)
        }
    }
    
    private def newConnect(join:Joinable[_], connectAnyMedia:Boolean, callback:FinishFunction) = wrapLock {
    	val connectCallback = ()=> {
    		this._joinedTo = Some(join)
    		callback()
    	}//fixme how we deal with tx options
        telco.internal.sendInvite(this, join.sdp)
        
 	   	if (connectAnyMedia)
     	   	setFinishFunction( new VERSIONED_RINGING(clientTx.get.getBranchId()), connectCallback )
    
        setFinishFunction( new VERSIONED_CONNECTED(clientTx.get.getBranchId()), connectCallback )
 	   	
 	   	progressingCallback.foreach( _(this) )
    }

    private def fireReinvite(join:Joinable[_], f:FinishFunction) {
        telco.internal.sendReinvite(this, join.sdp)
        val toState = new VERSIONED_CONNECTED(clientTx.get.getBranchId())
	    setFinishFunction(toState, f)
    }

    private def reconnect(join:Joinable[_], callback:FinishFunction) : Unit = wrapLock {
    	val reconnectCallback = ()=>{
    	  _joinedTo = Some(join)
    	  callback()
    	}
      
        //if silencing this connction, when it's done, silence the jointwo conncetion, and so on. 
        joinedTo match {
            case None =>
                fireReinvite(join,reconnectCallback)
            case Some(otherConn) =>
                if ( !SdpHelper.isBlankSdp(join.sdp) )//why are we checking this here?
                    otherConn.connect(SdpHelper.getBlankJoinable(telco.contactIp), () => {
                        this._joinedTo = None
                        reconnect(join, reconnectCallback)
                    })
                else
                    fireReinvite(join, reconnectCallback)
        }
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
        val f = ()=> {
          this._joinedTo = Some(toJoin)
          toJoin.connect(this, connectedCallback)
        }
    	connectionState match {
            case UNCONNECTED() | RINGING() =>
		        telco.internal.sendResponse(200, serverTx, toJoin.sdp.toString().getBytes())  
                setFinishFunction(VERSIONED_CONNECTED(serverTx.get.getBranchId()), f)
                
	        case _ => 
	            throw new InvalidStateException(new UNCONNECTED(), connectionState)
	    }       
    }

    override def accept(connectedCallback:FinishFunction) = wrapLock {
	    accept(SdpHelper.getBlankJoinable(telco.contactIp), connectedCallback) ///BUG HERE?
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
		telco.internal.sendResponse(606, serverTx, listeningSdp.toString().getBytes())
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
                otherCall.connect(this, true, ()=>{
                    this.reconnect(otherCall, ()=>{
                        this._joinedTo = Some(otherCall)
                        otherCall.onConnect(joinCallback)    
                    })
               })
              
            case CONNECTED() =>
                joinConnected(otherCall, joinCallback)
        }
    }

	private def joinConnected(otherCall:Joinable[_], joinCallback:FinishFunction) = wrapLock {
		otherCall.connect(this,()=>{
		   	this.reconnect(otherCall, ()=>{
    		    this._joinedTo = Some(otherCall)  
	    		joinCallback()
	    	}) 
	    })
	}

    //maybe rename to Mute?  It makes it so this SipConnection stops receieve to anything...
    def silence(silenceCallback:FinishFunction) = wrapLock {
    	SdpHelper.addMediaTo(listeningSdp, SdpHelper.getBlankSdp(telco.contactIp))
      	telco.internal.sendReinvite(this,listeningSdp) //SWAPED THIS
        setFinishFunction(new VERSIONED_SILENCED(clientTx.get.getBranchId()), silenceCallback)
    }

    override def hold(f:FinishFunction) : Unit = wrapLock {
        joinedTo match {
            case None => this.reconnect(SdpHelper.getBlankJoinable(telco.contactIp), f)//silence(f)
            case Some(otherConn) => otherConn.connect(SdpHelper.getBlankJoinable(telco.contactIp), ()=>hold(f))
        }
    }    

	override def unjoin(f:FinishFunction) = wrapLock {
		val unjoined = this._joinedTo.get
		this._joinedTo = None
        disconnectOnUnjoin match {
	        case true =>
	            disconnect( ()=> {
	                disconnectCallback.foreach( _(this) )
	                unjoinCallback.foreach( _(unjoined,this) )//FIXME: should take an Option[JoinedTo]
	            })

	        case false =>
	            unjoinCallback.foreach( _(unjoined, this) )
	    }
	}
  
    protected def onDisconnect() = wrapLock {
        joinedTo.foreach( joined=>{
                _joinedTo = None 
                joined.unjoin(()=>Unit) //uuugh how did the ohter one get unjoined
         })
    }

    protected[telco] def setState(s:VersionedState) : Unit = {
        lock()
        //println(" SET STATE CALLED FOR " + this + " s = " + s)
        //var b = false
        //if ( state.getState == CONNECTED() && s.getState == CONNECTED() )
        //    b = true

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
	    "JainSipConnection " + direction + " TO:"+destination + " State = " + state + " Hashcode = " + hashCode

}
 

