package com.bss.server

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.mortbay.jetty.servlet.ServletHandler 
import org.mortbay.jetty.servlet.ServletHandler
import org.mortbay.jetty.handler.ContextHandlerCollection
import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.Context
import org.mortbay.jetty.servlet.ServletHolder
import org.mortbay.jetty.bio.SocketConnector
import org.mortbay.jetty.handler.AbstractHandler
import com.bss.telco.api.TelcoServer
import com.bss.telco.jainsip.SipTelcoServer


class WebServer(apiPort:Int,
                adminPort:Int) {
    private val telcoServer = new SipTelcoServer("", 4, "", 5)
    private val server = new Server()
    initWebServer()
    


    def initWebServer() = {
        val apiConnector = new SocketConnector()
        apiConnector.setPort(80)
        server.setConnectors( List(apiConnector).toArray )
        val handler = new ServletHandler()
        server.setHandler(handler)
        handler.addServletWithMapping("com.bss.telco.CallServlet", "/")
        server.start()
        server.join()
    }
}


class CallServlet(telcoServer:TelcoServer) extends HttpServlet {
    override def doGet(request:HttpServletRequest, response:HttpServletResponse) {
        val to      = request.getParameter("To")
        val from    = request.getParameter("From") 
        val url     = request.getParameter("Url")

        val conn = telcoServer.createConnection(to, from)
        conn.connect(() => {
            postBackStatus()
            //send status to the url
        })

        //print out XML to the page!

        val response = BlueML.getCallResponse(conn.connectionid, to, from, "progressing")
    }

    def postBackStatus() = {
        println("postBackStatus")
    }
}

object BlueML {
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


