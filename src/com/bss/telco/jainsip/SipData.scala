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

import javax.sip._
import javax.sip.header._

trait SipData {

    protected[jainsip] var contactHeader:Option[ContactHeader] = None
    
    protected[jainsip] var serverTx:Option[ServerTransaction] = None
    
    protected[jainsip] var serverCancelTx:Option[ServerTransaction] = None

    protected[jainsip] var clientTx:Option[ClientTransaction] = None
   
    protected[jainsip] var dialog:Option[Dialog] = None
}
