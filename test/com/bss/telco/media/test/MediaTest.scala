package com.bss.telco.media.test

import org.junit._
import java.io.File;
import java.io.IOException;
import java.util.Vector

import javax.media.CaptureDeviceInfo
import javax.media.CaptureDeviceManager
import javax.media.DataSink
import javax.media.Format
import javax.media.Manager
import javax.media.MediaLocator
import javax.media.NoProcessorException
import javax.media.NotRealizedError
import javax.media.Processor
import javax.media.control.FormatControl
import javax.media.control.TrackControl
import javax.media.format.AudioFormat
import javax.media.protocol.ContentDescriptor
import javax.media.protocol.DataSource
import javax.media.ProcessorModel
import javax.media.protocol.ContentDescriptor
import javax.media.protocol.FileTypeDescriptor

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
package com.bss.telco.media

import java.io.IOException
import java.net.InetAddress
import javax.media.Manager
import javax.media.MediaException
import javax.media.Player
import javax.media.Controller
import javax.media.Processor
import javax.media.format.AudioFormat
import javax.media.protocol.ContentDescriptor
import javax.media.protocol.DataSource
import javax.media.rtp.RTPManager
import javax.media.rtp.SendStream
import javax.media.rtp.SessionAddress
import javax.media.rtp.SessionManagerException
import javax.media.ControllerEvent
import javax.media.ControllerListener
import javax.media.EndOfMediaEvent


import javax.media.Player
import javax.media.Controller

class MediaTest extends junit.framework.TestCase {


    @Test
    def testSend() {
        println("@testFourth")
        val ip = "192.168.2.5"
        val port = 3000
        val proc = Manager.createProcessor( Manager.createDataSource( new MediaLocator( "file://C:\\Users\\runT1ME\\Desktop\\BlueScale\\BlueScaleRepo\\resources\\gulp.wav" ) ) )
        wait(proc, Processor.Configured)
        proc.setContentDescriptor( new ContentDescriptor(ContentDescriptor.RAW_RTP) )
        wait( proc, Controller.Realized )
        val dataOutput = proc.getDataOutput()
        val ds = Manager.createDataSink(dataOutput, new MediaLocator( "rtp://" +ip+ ":" +port+ "/audio" ) )
        println("SEEEEEEEEEEEEEEEEEEEEEEENDING")
        ds.open()
        ds.start()
        dataOutput.start()
        
        proc.start() 
        Thread.sleep(4000)
	}


    
    def wait(p:Processor, i:Int) = {
     		i match {
				case Processor.Configured => p.configure()
				case Controller.Realized => p.realize()
			}
			while ( p.getState != i) Thread.sleep(100)
     }

}
