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
import scala.collection.mutable.Stack

/*
* This class is for testing call functionality from the server's command line.
* not near as well tested as the rest of the app, shouldn't be actually used for production.
*/

class CallRepl(telco:TelcoServer) {

    var processing = false

    var callmap = Map[String,SipConnection]()
    
    val labelStack = new Stack[String]()

    labelStack.push("a") 
    labelStack.push("b")
    labelStack.push("c")
    labelStack.push("d") 
	
	def evalLine(line:String) : String = {
	    if ( processing ) return "please try again when your previous action completes"
        val command  = line.split(" ")(0)
        val arg = line.split(" ")(1) 
        processing = true
        return command match {

            case "call" => 
                val conn = telco.createConnection(arg, "4443332222")
                val callLabel = labelStack.pop()
                callmap += callLabel->conn 
                conn.connect( ()=> {
                        println("..." + arg + " Connected. CallID = "+ callLabel) 
                        processing = false
                    })
                "connecting..."

            case "join" =>
                if ( line.split(" ").size < 3 )
                    "must provide two call labels to join"
                val c1 = callmap(arg)
                val c2 = callmap(line.split(" ")(2))
                c1.join(c2, ()=> {
                        println("..." + c1 + " joined to " + c2 )
                        processing = false
                    }
                )
                "joining..."

            case "hold" =>
                val c1 = callmap(arg)
                
                c1.hold( ()=> {
                    println("... call " + arg + " is now on hold")
                    processing = false
                    })
                "holding..." 
            /*  
            case "silence" =>
                val c1 = callmap(arg)
                c1.silence( ()=> {
                    println("call " + arg + " is now silenced")
                    processing = false
                })*/

            case "hangup" =>
                callmap(arg).disconnect( ()=> { 
                        println("...call" + arg + " is disconnected") 
                        labelStack.push(arg)
                        processing = false
                    })
                 //remove from map
                "hanging up..."
        }
    }

}
