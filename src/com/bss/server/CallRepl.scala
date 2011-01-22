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
package com.bss.server

import com.bss.telco.api._
import scala.collection.immutable._

class CallRepl(telco:TelcoServer) {

    var processing = false

    var callmap = Map[String,SipConnection]()
	
	
	def CallREPL(line:String) : String = {
	    if ( processing ) return "please try again when your previous action completes"
        val command  = line.split(" ")(0)
        val callnum = line.split(" ")(1) 
        processing = true
        return command match {
            case "call" => 
                val conn = telco.createConnection(callnum, "4443332222")
                callmap += callnum->conn 
                conn.connect( ()=> println("..." + callnum + " Connected") )
                "connecting..."

            case "join" =>
                if ( line.split(" ").size < 3 )
                    "must provide two numbers to join"
                val c1 = callmap(callnum)
                val c2 = callmap(line.split(" ")(2))
                c1.join(c2, ()=> println("..." + c1 + " joined to " + c2 ))
                "joining..."
            case "hangup" =>
                callmap(callnum).disconnect( ()=> println("..." + callnum + " is disconnected") )
                 //remove from map
                "hanging up..."
        }
    }

}
