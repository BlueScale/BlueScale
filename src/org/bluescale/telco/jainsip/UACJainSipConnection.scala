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
import org.bluescale.telco.Types._
import org.bluescale.telco.api._
import scala.collection.immutable.Map
import javax.sip.header._
import javax.sip._
import javax.sdp.SessionDescription
import javax.sip.message._
import org.bluescale.util.Lockable

trait UACJainSipConnection extends BaseJainSipConnection with Lockable {
    
    def connect( f:FinishFunction) = connect(SdpHelper.getJoinable(sdp), false, f)//shouldn't be this.  that's weird

 	def connect(join:Joinable[_], callback:FinishFunction) : Unit = connect(join, false, callback) 

    protected[telco] def connect(join:Joinable[_], connectAnyMedia:Boolean, callback:()=>Unit) = wrapLock {
        joinedTo match {
            case Some(currentJoin) =>
                currentJoin.unjoin( ()=>realConnect(join, callback))
            case None => realConnect(join, callback)
         }
    }

    //can only be called after unjoining whatever was connected previous
    private def realConnect(join:Joinable[_], callback:()=>Unit) {
         _state match {
            case s:UNCONNECTED =>
                val t = telco.internal.sendInvite(from, to, join.sdp)
                clientTx = Some(t._2)
                connid = t._1
                addConnection()
            case s:CONNECTED =>
                transaction.foreach( tx =>
                    clientTx = Some(telco.internal.sendReinvite(tx, join.sdp) ))
                    
            }
        clientTx.foreach( tx => {
            setRequestCallback(tx.getBranchId(), (responseCode, previousSdp) => {
                responseCode match {
                    case Response.RINGING =>
                        _state = RINGING()
                        //only if it's different!
                        if (!previousSdp.toString().equals(sdp.toString()))
                            joinedTo.foreach( join => join.joinedMediaChange() )
                    case Response.OK =>
                        _state = CONNECTED()
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

    def join(otherCall:Joinable[_], joinCallback:FinishFunction) = wrapLock {
        val f = ()=> {
            println(" join for " + this + " to " + otherCall )
            otherCall.connect(this, ()=>{
                println(" other call is " + otherCall + " now connected")
                connect(otherCall, joinCallback)
            })
        }
        println(" ok here....")
  	    joinedTo match { 
            case Some(joined) => 
                joined.connect(telco.silentJoinable(), f)
            case None => f()
  	    }
    }

	def disconnect(callback:FinishFunction) = wrapLock {
		transaction.foreach( tx => {
		    val newTx = telco.internal.sendByeRequest(tx)
		    clientTx = Some(newTx)
            setRequestCallback( newTx.getBranchId(), ()=> { //change callback singature
                _state = UNCONNECTED()
                onDisconnect()//BUG HERE. what if disconnect is CALLED from unjoin? 
                callback()
            })
        })
  	}

    def cancel(f:FinishFunction) = wrapLock {
 	    clientTx.foreach( tx=> {
            clientTx = Some(telco.internal.sendCancel(tx))
            callbacks += tx.getBranchId()->(() => f())
 	    })
 	}

    def unjoin(f:FinishFunction) = wrapLock {
        disconnectOnUnjoin match {
            case true =>
                val maybeJoined = joinedTo
                _joinedTo = None
                disconnect( ()=>{

                    disconnectCallback.foreach(_(this))
                    for (unjoined <- maybeJoined;
                        callback <- unjoinCallback) callback(unjoined, this)
                    f()
                })
            case false =>
                realConnect(telco.silentJoinable(), f)
        }
    }

    def hold(f:FinishFunction) = wrapLock {
        throw new Exception("Not Implemented yet")
    }

}
