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
import org.bluescale.util._

protected trait BaseJainSipConnection extends SipConnection with Actorable { 
   
    val to:String

    val from:String

    val telco:SipTelcoServer
    
    var sdp = loadInitialSdp()
    
    var clientTx:Option[ClientTransaction] = None
    
    var serverTx:Option[ServerTransaction] = None

    val disconnectOnUnjoin:Boolean 
    
    var connid:String
    
    protected var callbacks = Map[String, AnyRef]()
   
    protected var _state:ConnectionState  = UNCONNECTED()
    
    protected var progressingCallback:Option[(SipConnection)=>Unit] = None
   
    protected var _joinedTo:Option[Joinable[_]] = None

    def protocol = "SIP"
    
    def joinedTo = _joinedTo

    def connectionid = connid

    def connectionState = _state

    protected def loadInitialSdp() = 
        telco.silentSdp()

    protected def clearCallbacks(tx:Transaction) = 
        callbacks = callbacks.filter( kv => tx.getBranchId() == kv._1)
    
    protected def setRequestCallback(branchId:String, f:(Int, SessionDescription)=>Unit) =
        callbacks += branchId->f

    protected def setRequestCallback(branchId:String, f:()=>Unit) =
        callbacks += branchId->f
    
    protected def transaction : Option[Transaction] =
        Option(clientTx.getOrElse( serverTx.get ))

    protected def onDisconnect() = {
        joinedTo.foreach( joined=>{
                this._joinedTo = None 
                joined.unjoin(()=>Unit) //uuugh how did the ohter one get unjoined
         })
         removeConnection()
    }

    protected def addConnection() : Unit

    def removeConnection() : Unit

    def joinedMediaChange() = 
        joinedTo.foreach( join => connect(join,()=>{}) )
    
}
