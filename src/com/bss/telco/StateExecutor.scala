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
package com.bss.telco

import com.bss.telco.api._
import com.bss.telco.Types._

trait StateExecutor {
   
    protected var expectedState : Option[VersionedState] = None

    protected var function : Option[FinishFunction] = None

    protected def setFinishFunction(es:VersionedState, f:FinishFunction) = {
        expectedState = Some(es)
        function = Some(f)
    }

    protected def setAndExecute(state:VersionedState) : Boolean = {
        var ret = false

        expectedState.foreach(s => {
            var f = function
            expectedState = None
            function = None
            if (s.equals(state)) {
                ret = true
                f.foreach(_()) //might want to run in it's own thread
            } 
        })
        return ret
    }

    def debugStateExecutor() = {
        println(" ########           expectedState = " + expectedState + "!")
    }
}