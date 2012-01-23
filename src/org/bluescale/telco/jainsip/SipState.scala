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

import javax.sip._
import javax.sip.header._
import javax.sdp.SessionDescription
import javax.sip.message._


protected[telco] trait SipState {

    var sdp = loadInitialSdp()
    
    var clientTx:Option[ClientTransaction] = None
    
    var serverTx:Option[ServerTransaction] = None
    
    //def setUAC(clientTx:ClientTransaction, sdp:SessionDescription)

    def setUAC(clientTx:ClientTransaction, responseCode:Int, sdp:SessionDescription)

    def invite(tx:ServerTransaction, sdp:SessionDescription)

    def bye(tx:ServerTransaction)

    def reinvite(tx:ServerTransaction, sdp:SessionDescription)

    def ack(tx:ServerTransaction)

    def cancel(tx:ServerTransaction)

    def transaction : Option[Transaction] =
        Option(clientTx.getOrElse( serverTx.get ))

    protected def loadInitialSdp():SessionDescription

}

