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
import org.bluescale.telco.Types
import org.bluescale.util._

trait UASJainSipConnection extends BaseJainSipConnection with Lockable {

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
                    case _ => println("error")
                }
            else 
                println(" we couldn't find an entry for " + clientTx.getBranchId() + " | for " + this)
        } catch {
            case ex:Exception =>
                println("Exception in setUAC = "+ ex + " | responseCode = " + responseCode)
                //ex.printStackTrace()
        }
    }
    
    def bye(tx:ServerTransaction) = orderedexec {
        _state = UNCONNECTED()
        serverTx = Some(tx)

        telco.removeConnection(this)
        onDisconnect()
        disconnectCallback.foreach(_(this))
    }

    def reinvite(tx:ServerTransaction, sdp:SessionDescription) = orderedexec {
        this.serverTx = Some(tx)
        this.sdp = sdp ///here is the weird part? 
        serverTx = Some(tx)
        joinedTo.foreach(join => join.joinedMediaChange())
        val joinable = joinedTo.getOrElse(telco.silentJoinable())
        incomingResponse(200, joinable, ()=>{})
    }

    def invite(tx:ServerTransaction, sdp:SessionDescription) = orderedexec {
        this.sdp = sdp
        serverTx = Some(tx)
    }
    
    def cancel(cancelTx:ServerTransaction) = orderedexec {
        telco.internal.sendResponse(200, cancelTx, null)
        serverTx.foreach( tx => {
            telco.internal.sendResponse(487, tx, null)
            callbacks(tx.getBranchId) match {
                case f:(()=>Unit) =>
                    f()
            }
        })
 	}
 	
 	def ack(newtx:ServerTransaction) = orderedexec {
        serverTx.foreach( tx => { 
            _state = CONNECTED()
            callbacks(tx.getBranchId()) match {
                case f:( ()=>Unit ) =>
                    clearCallbacks(tx)
                    f()
                case _ => println("error")
            }
        })
    }

    private def incomingResponse(responseCode:Int, toJoin:Joinable[_], connectedCallback:FinishFunction) = wrapLock {
        serverTx.foreach( tx => {
            callbacks += tx.getBranchId()->(() => connectedCallback() )
		    telco.internal.sendResponse(200, tx, toJoin.sdp.toString().getBytes())  
	 	})
    }
    
    def accept(toJoin:Joinable[_], connectedCallback:FinishFunction) = orderedexec {
        incomingResponse(200, toJoin, ()=> {
            _joinedTo = Some(toJoin)
            connectedCallback()
        })
    }

    def accept(connectedCallback:FinishFunction) =
	    accept(SdpHelper.getBlankJoinable(telco.contactIp), connectedCallback)

    def ring(toJoin:Joinable[_]) =
        incomingResponse(183, toJoin, ()=>{})
	
    def ring() =
        incomingResponse(180, telco.silentJoinable(), ()=>{}) 
 
	def reject(rejectCallback:FinishFunction) = 
	    incomingResponse(606, telco.silentJoinable(), rejectCallback)



    //def transaction : Option[Transaction] =
    //    Option(clientTx.getOrElse( serverTx.get ))

    protected def loadInitialSdp():SessionDescription
   
}
