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

package com.bss.blueml

import com.bss.telco.api._

class Engine(telcoServer:TelcoServer) {
   
    def HandleBlueML(conn:SipConnection, str:String) : Unit  =
        HandleBlueML(conn, BlueMLParser.parse(str))

    def HandleBlueML(conn:SipConnection, verbs:Seq[BlueMLVerb]) : Unit = 
        verbs.foreach( _ match {
                
                case dial:Dial => handleDial(conn, dial)

                case play:Play => println("play")

            })

    def handleDial(conn:SipConnection, dial:Dial) = {
        conn.connectionState match {
            //need to figure out how you can transfer/hold 
            case u:UNCONNECTED  => conn.accept( ()=> {
                                                println("ACCEPTED")
                                                val destConn = telcoServer.createConnection(dial.number,"2222222222")
                                                println("about to connect for destConn to " + dial.number )
                                                destConn.connect( ()=>{ println("destConn connected!")
                                                    conn.join(destConn, ()=> println("joined"))

                                                })  
                                            })

            case p:PROGRESSING  => println("progressing")
        }
    }
}
