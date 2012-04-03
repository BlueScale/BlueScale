/*
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

import org.bluescale.telco.api._
import org.bluescale.telco.jainsip.SipTelcoServer
import java.net._
import scala.xml._
import java.io._
import scala.collection.immutable.PagedSeq

object WebUtil {
    
    def readAll(reader:BufferedReader) : String =
        PagedSeq.fromReader(reader).mkString


    def postToUrl(url:String, params:Map[String, String]) : String = {
        var response = ""
        val data = params
                    .map({ case (key, value) =>  
                        URLEncoder.encode( key, "UTF-8") + "=" +URLEncoder.encode( value, "UTF-8") })
                    .reduceLeft( (a,b)=> a +"&"+ b)
                    //.reduceLeft(_ + "&" + _)
        
        val urlConn = new URL(url).openConnection()
        urlConn.setDoOutput(true)
        //urlConn.setReadTimeout(50) //TODO: what's a good timeout? 
        val os = new OutputStreamWriter(urlConn.getOutputStream())
        try {
            os.write(data)
            os.flush()
            val reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream() ))
            response = readAll(reader)
            print(response)
            reader.close()
        } catch {
            case ex:Exception => 
                ex.printStackTrace()
        } finally {
            os.close()
        }
        return response
    }

    
    def getCallResponse(connId:String, to:String, from:String, status:String ) : String = {
        return  (<BlueXml>
                   <DateCreated></DateCreated>
                    <CallId>{connId}</CallId>
                    <To>{to}</To>
                    <From>{from}</From>
                    <Status>{status}</Status>
                    <Direction></Direction>
                </BlueXml>).toString()
    }
}

