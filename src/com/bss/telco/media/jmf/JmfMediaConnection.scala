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
package com.bss.telco.media.jmf

import com.bss.telco.api._

import com.bss.telco._
import com.bss.util._
import javax.media.Player
import javax.media.Controller
import javax.media.Processor
import javax.media.format.AudioFormat
import javax.media.Manager
import javax.media.MediaLocator
import javax.media.protocol.ContentDescriptor
import javax.media.protocol.FileTypeDescriptor
import javax.sdp.SessionDescription

//NOTE: JMF can't handle localhost and 127.0.0.1, needs to be a real IP...
class JmfMediaConnection(file:String,
                         mediaServer:JmfMediaServer) extends MediaConnection
                         							with Lockable
                         							with LogHelper {

	val processor = initProc()

	var listeningSdp	 = SdpHelper.getBlankSdp(mediaServer.ip)//fixme, should be an open port on our server...

	override def connectionid = this.hashCode().toString()

    override def connectionState = CONNECTED()
 
    override def protocol = "RTP"

    protected[telco] var disconnectedCallback:Option[()=>Unit] = None //NEEDED?

    override def sdp = listeningSdp

	override def play(finished:()=>Unit) = wrapLock {
		val dataOutput = processor.getDataOutput()
		val ds = Manager.createDataSink(dataOutput, new MediaLocator("") )
		ds.open()
		ds.start()
		dataOutput.start()
		processor.start() 	//started playing!	
		log("Playing media to " + joinedTo.get.sdp.toString())
	}

	override def cancel(f:()=>Unit) = println("pausing")

	override def reconnect(sdp:SessionDescription, f:()=>Unit) = wrapLock {
		listeningSdp = sdp
		f()
	}

	override def join(call:Joinable, f:()=>Unit) = wrapLock {
		call.reconnect(listeningSdp, ()=>
			this.reconnect(call.sdp, ()=>{
				this.joinedTo = Some(call)
				this.joinedTo.get.joinedTo = Some(this)
				f()
			})
		)
	}

	override def hold(f:()=>Unit) = wrapLock {
		//TODO: pause? cancel? 
	}


	//TODO: Make Asynchronous?
    def initProc() : Processor = {
		val proc = Manager.createProcessor( Manager.createDataSource( new MediaLocator( file ) ) )
		wait(proc, Processor.Configured)
		proc.setContentDescriptor( new ContentDescriptor(ContentDescriptor.RAW_RTP) )
        wait( proc, Controller.Realized )
        return proc
    }
     
    def wait(p:Processor, i:Int) = {
    	i match {
			case Processor.Configured => p.configure()
			case Controller.Realized => p.realize()
		}
		while ( p.getState != i) Thread.sleep(100)
    }
}
