package com.bss.server

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;


class WebServer(port:Int) {
    private val server = new Server(port)
    val ctx = new Context(server, "/", Context.SESSIONS)
   
    
}

class ListeningServlet extends HttpServlet {
    override def doGet(request:HttpServletRequest, response:HttpServletResponse) {
    
    }
}

