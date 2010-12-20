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
package com.bss.web.test


import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io._
import org.mortbay.jetty.Request;
import org.mortbay.jetty.servlet.ServletHandler 
import org.mortbay.jetty.servlet.ServletHandler
import org.mortbay.jetty.handler.ContextHandlerCollection
import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.Context
import org.mortbay.jetty.servlet.ServletHolder
import org.mortbay.jetty.bio.SocketConnector
import org.mortbay.jetty.handler.AbstractHandler
import java.net._
import org.mortbay.jetty.HttpConnection;


class SimpleWebServer(port:Int) {
    
    private val wserver = new Server()
    initWebServer()
 
    private var responses = List[(HttpServletRequest)=>String]()
    
    private val servlet = new CallbackServlet(this)

    def initWebServer() = {
        val apiConnector = new SocketConnector()
        apiConnector.setPort(port)
        wserver.setConnectors( List(apiConnector).toArray )
        val context = new Context(wserver, "/", Context.SESSIONS)
        context.addServlet( new ServletHolder( new CallbackServlet(this) ), "/*" )
   }

   def setNextResponse(respFunction:(HttpServletRequest)=>String) = 
        this.responses = responses :+ respFunction
    
    def getNextResponse() : Option[HttpServletRequest=>String] = 
        Option(responses) match {
            case Some(r) => val head = r.head
                            responses = r.filterNot( _.equals(head) )
                            return Some(head)
            case None => None
        }

    def start() =
        wserver.start()

    def stop() = 
        wserver.stop()
    
}

class CallbackServlet(ws:SimpleWebServer) extends HttpServlet {
        
    override def doGet(request:HttpServletRequest, response:HttpServletResponse) = 
        ws.getNextResponse().foreach( cb =>{
            response.setContentType("text/xml") //XML
            response.setStatus(HttpServletResponse.SC_OK)
            response.getWriter().println( cb(request) )
            response.getWriter().flush()
            response.getWriter().close()
            })
    
    

    override def doPost(request:HttpServletRequest, response:HttpServletResponse) {
        doGet(request, response)
    }
}

