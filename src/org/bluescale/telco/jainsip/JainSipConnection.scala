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
import javax.sip._
import javax.sdp.SessionDescription
import javax.sip.message._

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
                            with SipState
                     	    //with SipData
                     	    with LogHelper 
                     	    with Lockable 
                     	    with OrderedExecutable {
	  
    override def destination = to
    
    override def origin = from 
    
    private var _joinedTo:Option[Joinable[_]] = None // should be in the Joined trait
    
    override def joinedTo = _joinedTo
	
	override def connectionid = connid
 
	override def direction = dir
 
	override def protocol = "SIP"

	override def connectionState = state

	private var progressingCallback:Option[(SipConnection)=>Unit] = None 
  	
	protected[jainsip] def setConnectionid(id:String) = connid = id
 
 	override def connect( f:FinishFunction) = connect(SdpHelper.getJoinable(sdp), false, f)//shouldn't be this.  that's weird

 	override def connect(join:Joinable[_], callback:FinishFunction) : Unit = connect(join, false, callback) 

    protected[telco] override def connect(join:Joinable[_], connectAnyMedia:Boolean, callback:()=>Unit) = wrapLock {
        joinedTo match {
            case Some(currentJoin) =>
                currentJoin.unjoin( ()=>realConnect(join, callback))
            case None => realConnect(join, callback)
         }
    }

    //can only be called after unjoining whatever was connected previous
    private def realConnect(join:Joinable[_], callback:()=>Unit) {
         state match {
            case s:UNCONNECTED =>
                val t = telco.internal.sendInvite(this, join.sdp)
                clientTx = Some(t._2)
                connid = t._1
                telco.addConnection(this)
            case s:CONNECTED =>
                transaction.foreach( tx =>
                    clientTx = Some(telco.internal.sendReinvite(tx, join.sdp) ))
            }
        clientTx.foreach( tx => {        
            setRequestCallback(tx.getBranchId(), (responseCode, previousSdp) => {
                responseCode match {
                    case Response.RINGING =>
                        state = RINGING()
                        //only if it's different!
                        if (!previousSdp.toString().equals(sdp.toString()))
                            joinedTo.foreach( join => join.joinedMediaChange() )
                    case Response.OK =>
                        state = CONNECTED()
                        this._joinedTo = Some(join)
                        //if (!previousSdp.toString().equals(sdp.toString()))
                        //    joinedTo.foreach( join => join.joinedMediaChange() )
                        clearCallbacks(tx)
                        callback()
                }
            })
           progressingCallback.foreach( _(this) )
        })
    }
    
    private def clearCallbacks(tx:Transaction) =
        callbacks = callbacks.filter( kv => tx.getBranchId() == kv._1)
        

    override protected def loadInitialSdp() = 
        telco.silentSdp()
    
    private var callbacks = Map[String, AnyRef]()
    //private var callbacks = Map[String, (Int,SessionDescription)=>Unit]()

    private def setRequestCallback(branchId:String, f:(Int, SessionDescription)=>Unit) =
        callbacks += branchId->f

    private def setRequestCallback(branchId:String, f:()=>Unit) =
        callbacks += branchId->f
        
    
    override def joinedMediaChange() = wrapLock {
        
        joinedTo.foreach( join => connect(join,()=>{}) )
    }

  	override def join(otherCall:Joinable[_], joinCallback:FinishFunction) = wrapLock {
        val f = ()=> {
            otherCall.connect(this, ()=>{ 
                connect(otherCall, joinCallback)
            })
        }
  	    joinedTo match { 
            case Some(joined) => 
                joined.connect(telco.silentJoinable(), f)
            case None => f()
  	    }
    }

    private def incomingResponse(responseCode:Int, toJoin:Joinable[_], connectedCallback:FinishFunction) = wrapLock {
        serverTx.foreach( tx => {
            callbacks += tx.getBranchId()->(() => connectedCallback() )
		    telco.internal.sendResponse(200, tx, toJoin.sdp.toString().getBytes())  
	 	})
    }
                
    override def accept(toJoin:Joinable[_], connectedCallback:FinishFunction) = {
        incomingResponse(200, toJoin, ()=> {
            _joinedTo = Some(toJoin)
            connectedCallback()
        })
    }

    override def accept(connectedCallback:FinishFunction) =
	    accept(SdpHelper.getBlankJoinable(telco.contactIp), connectedCallback)

    override def ring(toJoin:Joinable[_]) =
        incomingResponse(183, toJoin, ()=>{})
	
    override def ring() =
        incomingResponse(180, telco.silentJoinable(), ()=>{}) 
 
	override def reject(rejectCallback:FinishFunction) = 
	    incomingResponse(606, telco.silentJoinable(), rejectCallback)

	override def disconnect(disconnectCallback:FinishFunction) = wrapLock {
		transaction.foreach( tx => {
		    val newTx = telco.internal.sendByeRequest(tx)
		    clientTx = Some(newTx)
            setRequestCallback( newTx.getBranchId(), ()=> { //change callback singature
                state = UNCONNECTED()
                onDisconnect()//BUG HERE. what if disconnect is CALLED from unjoin? 
                disconnectCallback()
            })
        })
  	}

  	override def setUAC(clientTx:ClientTransaction, responseCode:Int, newsdp:SessionDescription) = wrapLock {
  	    try {
            val callback = callbacks(clientTx.getBranchId())
            val previousSdp = sdp
            sdp = newsdp
  	        callback match {
                case f:((Int,SessionDescription)=>Unit) =>
                     //FIXME: do we want to leave them here forever?
                    f(responseCode, previousSdp)
                case f:(()=>Unit) =>
                    f()
                case _ => println("error")
            }
        } catch {
            case ex:Exception =>
                println("Exception in setUAC = "+ ex + " | responseCode = " + responseCode)
                //ex.printStackTrace()
        }
    }

    override def bye(tx:ServerTransaction) = wrapLock {
        state = UNCONNECTED()
        serverTx = Some(tx)

        telco.removeConnection(this)
        onDisconnect()
        disconnectCallback.foreach(_(this))
    }

    override def reinvite(tx:ServerTransaction, sdp:SessionDescription) = wrapLock {
        this.serverTx = Some(tx)
        this.sdp = sdp ///here is the weird part? 
        serverTx = Some(tx)
        joinedTo.foreach(join => join.joinedMediaChange())
        val joinable = joinedTo.getOrElse(telco.silentJoinable())
        incomingResponse(200, joinable, ()=>{})
    }

    override def invite(tx:ServerTransaction, sdp:SessionDescription) = wrapLock {
        this.sdp = sdp
        serverTx = Some(tx)
    }

    override def ack(newtx:ServerTransaction) {
        serverTx.foreach( tx => { 
            state = CONNECTED()
            callbacks(tx.getBranchId()) match {
                case f:( ()=>Unit ) => 
                    f()
                case _ => println("error")
            }
            clearCallbacks(tx)
        })
    }

  	override def cancel(f:FinishFunction) = wrapLock {
 	    clientTx.foreach( tx=> {
            clientTx = Some(telco.internal.sendCancel(tx))
            callbacks += tx.getBranchId()->(() => f())
 	    })
 	}

 	//FIXME: we could be too late.  Identify how...
 	override def cancel(cancelTx:ServerTransaction) = wrapLock {
        telco.internal.sendResponse(200, cancelTx, null)
        serverTx.foreach( tx => {
            telco.internal.sendResponse(487, tx, null)
            callbacks(tx.getBranchId) match {
                case f:(()=>Unit) =>
                    f()
            }
        })
 	}

    //should just inivte to silence
    override def hold(f:FinishFunction) : Unit = wrapLock {
        /*
        joinedTo match {
            case None => this.reconnect(SdpHelper.getBlankJoinable(telco.contactIp), f)//silence(f)
            case Some(otherConn) => otherConn.connect(SdpHelper.getBlankJoinable(telco.contactIp), ()=>hold(f))
        }
        */
    }    

	override def unjoin(f:FinishFunction) = wrapLock {
        disconnectOnUnjoin match {
            case true =>
                val maybeJoined = joinedTo
                _joinedTo = None
                disconnect( ()=>{
                    disconnectCallback.foreach(_(this))
                    //maybeJoined.foreach( unjoined => 
                    //    unjoinCallback.foreach( _(unjoined, this)) 
                    //)
                    f()
                })
            case false =>
                realConnect(telco.silentJoinable(), f)
        }
    }
	
  
    private def onDisconnect() = {
        joinedTo.foreach( joined=>{
                this._joinedTo = None 
                joined.unjoin(()=>Unit) //uuugh how did the ohter one get unjoined
         })
    }
       
    override def toString() = 
	    "JainSipConnection " + direction + " TO:"+destination + " State = " + state + " Hashcode = " + hashCode

}
 

