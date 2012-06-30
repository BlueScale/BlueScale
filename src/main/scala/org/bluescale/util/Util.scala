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

package org.bluescale.util


trait Util {
	//lopex from freenode made this better:
    def StrOption(s:String) =
    	Option(s).filter(!_.isEmpty)

    
    def GetNonEmpty(str1:String, str2:String) : String = {
        StrOption(str1) match {
            case Some(s) => s
            case None    => str2
        }
    }

    def FixPhoneNumber(number:String) : String = {
        var ret = number.replace(" ","")
        if (ret.startsWith("sip:"))
          return ret
        return ret.head match {
            case '+' => ret
            case '1' => "+"+ret
            case _ => "+1"+ret
        }
    }

    def scrubNumbers(number:String) : String = {
        val ret = number.replace("+","").replace(" ","")
        return ret.head match {
            case '1' => ret.substring(1,ret.length)
            case _ => ret
        }


    }
}


