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
* Please contact us at www.BlueScaleSoftware.com
*
*/

package com.bss.blueml

import com.bss.telco.api._
import com.bss.telco.jainsip.SipTelcoServer
import java.net._
import scala.xml._
import java.io._
import com.bss.blueml._

object WebUtil {


    /*
    def postCallStatus(url:String, conn:SipConnection, handleResponse:(SipConnection,String)=>Unit) = 
        Option( WebUtil.postToUrl(url, getConnectionMap(conn)) ) match {
            case Some(xml) => handleResponse(conn, xml)
            //case Some(xml) => Engine.HandleBlueML(conn, xml)
            case None => //nothing to do here
        }

    def postJoinedStatus(url:String, conn1:SipConnection, conn2:SipConnection) =
        WebUtil.postToUrl(url, getJoinedMap(conn1, conn2) )
    */

    def postCallStatus(url:String, map:Map[String,String], handleResponse:(String)=>Unit) =
        Option( WebUtil.postToUrl(url, map) ) match {
            case Some(xml)  => handleResponse(xml)
            case None       => //ok...
        }
                    
    
    def readAll(reader:BufferedReader) : String = 
        Option( reader.readLine() ) match {
            case None   => ""
            case Some(x)=> x.concat(readAll(reader))
        }                   
   
    
    def postToUrl(url:String, params:Map[String, String]) : String = {
        val data = params
                    .map({ case (key, value) =>  
                        URLEncoder.encode( key, "UTF-8") + "=" +URLEncoder.encode( value, "UTF-8") })
                    .reduceLeft(_ + "&" + _)
        
        val urlConn = new URL(url).openConnection()
        urlConn.setDoOutput(true)
        val os = new OutputStreamWriter(urlConn.getOutputStream())
        try {
            os.write(data)
            os.flush()
            val reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream() ))
            val response = readAll(reader)
            reader.close()
            return response
        } finally {
            os.close()
        }
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

