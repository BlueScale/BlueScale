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

import org.bluescale.telco.jainsip._
import org.bluescale.telco._
import org.bluescale.telco.Types._
import org.bluescale.telco.api._
import scala.collection.immutable.Map
import javax.sip.header._
import javax.sip._
import javax.sdp.SessionDescription
import javax.sip.message._
import org.bluescale._
import org.bluescale.util.BlueFuture._
import org.bluescale.util.LogHelper
import akka.dispatch.Future
import akka.dispatch.Promise

trait UACJainSipConnection extends BaseJainSipConnection with LogHelper {
    
    def connect(): Future[SipConnection] = connect(SdpHelper.getJoinable(sdp), false)//shouldn't be this.  that's weird

 	def connect(join:Joinable[_]) = connect(join, false) 

    protected[telco] def connect(join:Joinable[_], connectAnyMedia:Boolean) = wrapPromise[SipConnection](promise => orderedexec {
        joinedTo match {
            case Some(currentJoin) =>
              	for(unjoined <- currentJoin.unjoin();
              	    conn <- realConnect(join))
              			promise.success(this)
            case None => realConnect(join) foreach(_ => promise.success(this))
         }
    })

    //can only be called after unjoining whatever was connected previous
    protected def realConnect(join:Joinable[_]) = wrapPromise[SipConnection](promise => {
         _state match {
            case s:UNCONNECTED =>
                val t = telco.internal.sendInvite(from, to, join.sdp)
                clientTx = Some(t._2)
                connid = t._1
                addConnection()
            case s:CONNECTED =>
                transaction.foreach( tx => {
                  debug(" Sending a reinvite to " + this.destination + "With sdp of " + join.sdp)  
                  clientTx = Some(telco.internal.sendReinvite(tx, join.sdp) )
                    		
                })
                    
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
                        promise.success(this)
                    case _ =>
                      error("DID SOMETHING FAIL..........??????????????? response = " + responseCode)
                      //something went wrong, set to failed state
                      _state = FAILED()
                }
            })
           progressingCallback.foreach( _(this) )
        })
    })

    override def join(otherCall:Joinable[_]) = wrapPromise[SipConnection](promise => orderedexec {
        val f = ()=> {
            log(" Joing" + this + " to " + otherCall )
            for(
            _ <- otherCall.connect(this);
            _ = log("otherCall"+otherCall +" is , now trying to reinvite " + this);
            _ <- connect(otherCall)) {
            	log("WOAh, got to the other connect................YAY")
            	promise.success(this)
            }
        }
  	    joinedTo match { 
            case Some(joined) => 
                joined.connect(telco.silentJoinable()) foreach { _=> 
                	f() 
                 }
            case None => 
              	f()
  	    }
    })

	def disconnect() = wrapPromise[SipConnection](promise => orderedexec {
		transaction.foreach( tx => {
		    val newTx = telco.internal.sendByeRequest(tx)
		    clientTx = Some(newTx)
            setRequestCallback( newTx.getBranchId(), ()=> { //change callback singature
                _state = UNCONNECTED()
                onDisconnect()//BUG HERE? what if disconnect is CALLED from unjoin? 
                promise.success(this)
            })
        })
  	})

    def cancel() = wrapPromise[SipConnection](promise => orderedexec {
    	_state match {
    		case CANCELED() | FAILED() =>
    	    log(" ------------------NOT CANCELLING, state = " + _state)
    		promise.success(this)
    	  case _ =>
    	    	clientTx.foreach( tx=> {
    				clientTx = Some(telco.internal.sendCancel(tx))
    				callbacks += tx.getBranchId()->(() => {
    				 log(" Cancel(),  cancel worked!")
    				 promise.success(this)
    				})
    			})

    	}
 	})

    def unjoin() = wrapPromise[SipConnection](promise => orderedexec {
        disconnectOnUnjoin match {
            case true =>
                val maybeJoined = joinedTo
                _joinedTo = None
                disconnect() foreach {_ => 

                    disconnectCallback.foreach(_(this))
                    for (unjoined <- maybeJoined;
                        ucallback <- unjoinCallback) 
                    	ucallback(unjoined, this)
                    promise.success(this)
                }
            case false =>
                realConnect(telco.silentJoinable()) foreach { _ => promise.success(this) }
        }
    })

    def hold(f:FinishFunction) = orderedexec {
        throw new Exception("Not Implemented yet")
    }
    
    
    
	//wrong place how did it get moved?!?
    def setUAC(clientTx:ClientTransaction, responseCode:Int, newsdp:SessionDescription) = orderedexec {
  	    try {
            val previousSdp = sdp
            sdp = newsdp
            if (callbacks.contains(clientTx.getBranchId()))
                callbacks(clientTx.getBranchId()) match {
                    case f:((Int,SessionDescription)=>Unit) =>
                        //FIXME: do we want to leave them here forever?
                        f(responseCode, previousSdp)
                    case f:(()=>Unit) =>
                        f()
                    case _ => error("error")
                }
            else 
                error(" we couldn't find an entry for " + clientTx.getBranchId() + " | for " + this)
        } catch {
            case ex:Exception =>
                error(ex, ("Exception in setUAC = " + responseCode + " + responseCode"))
                //ex.printStackTrace()
        }
    }

}
