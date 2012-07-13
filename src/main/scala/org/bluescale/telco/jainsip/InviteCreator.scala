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
* Please contact us at www.BlueScale.org
*
*/

package org.bluescale.telco.jainsip
import java.util._

import javax.sip._
import javax.sip.address._
import javax.sip.header._
import javax.sip.message._
import javax.sdp.MediaDescription
import org.bluescale.telco.jainsip._

class InviteCreator(val sipServer:JainSipInternal) {

    val magicSipCookie = "z9hG4bK"
    
    var ctr:Long = 1
    
    
    //SHOULD THIS EVER LOOK FOR THE DEFAULT DEST IP?
    def createRegister(from:String, dest:String, sdp:Array[Byte]): Request = {
    	val toSipAddress = dest.split("@")(1).split(":")(0)
    	val toPort = Integer.parseInt(dest.split("@")(1).split(":")(1))
    	val destname = dest.split("@")(0)
    	val callerid = from 
    	//val toAddress = sipServer.addressFactory.createSipURI(destname, toSipAddress)
      
    	return createRequest(Request.REGISTER,sipServer.headerFactory.createCSeqHeader(1l, Request.REGISTER), callerid, destname, toSipAddress, toPort, sdp)
    } 
    //NOTE: we parse the destinatino because if we're going out to the PSTN, we're going to forward to our SIP Trunk provider. 
    def createInviteRequest(callerid:String, dest:String, sdp:Array[Byte]): Request = {
    	val tosip = dest.contains("@") match {
		  	  case true => 
		  	    	(dest.split("@")(0).replace("sip:",""), dest.split("@")(1).split(":")(0), Integer.parseInt(dest.split("@")(1).split(":")(1)))
		  	  case false => 
		  	    	(dest,sipServer.destIp, sipServer.destPort)
		  	}
    	//val toAddress = sipServer.addressFactory.createSipURI(tosip._1, tosip._2)
    	return createRequest(Request.INVITE, sipServer.headerFactory.createCSeqHeader(1L,Request.INVITE),callerid,tosip._1, tosip._2, tosip._3 ,sdp)
    }
    
    
    //handle port
    private def createRequest(method:String, cSeqHeader:CSeqHeader,callerid:String, name:String, addr:String, port:Int, sdp:Array[Byte]): Request = {
			val fromName = callerid //"BlueScaleServer"
    		val fromDisplayName = callerid 

			// create >From Header
			val fromAddress = sipServer.addressFactory.createSipURI(callerid, sipServer.contactIp)
			val fromNameAddress = sipServer.addressFactory.createAddress(fromAddress)
			fromNameAddress.setDisplayName(fromDisplayName)
			val fromHeader = sipServer.headerFactory.createFromHeader(fromNameAddress, "12345")

			/*
			 *         SipURI toAddress = ProtocolObjects.addressFactory.createSipURI(
                    toUser, toSipAddress);
            Address toNameAddress = ProtocolObjects.addressFactory
                    .createAddress(toAddress);
            toNameAddress.setDisplayName(toDisplayName);
            ToHeader toHeader = ProtocolObjects.headerFactory.createToHeader(
                    toNameAddress, null);
			 * 
			 */
			
			val toNameAddress = sipServer.addressFactory.createAddress(sipServer.addressFactory.createSipURI(name, addr))
			
			toNameAddress.setDisplayName(name)
			
			val toHeader = sipServer.headerFactory.createToHeader(toNameAddress,null)
			
			val requestURI = sipServer.addressFactory.createSipURI(name, addr + ":" + port)
			val contentTypeHeader = sipServer.headerFactory.createContentTypeHeader("application", "sdp")
			val callIdHeader = sipServer.sipProvider.get.getNewCallId()
 		
			val maxForwards = sipServer.headerFactory.createMaxForwardsHeader(70)
 			// Create the request.
			val request = sipServer.messageFactory.createRequest(requestURI, 
                                                  		  		method, 
                                                  		  		callIdHeader, 
                                                  		  		cSeqHeader, 
                                                  		  		fromHeader,
                                                  		  		toHeader, 
                                                  		  		getViaHeader(), 
                                                  		  		maxForwards)
                                                  		  		
             //ContactHeader contactHeader1 = headerFactory.createContactHeader(addressFactory.createAddress("sip:here@somewhere:5070"));
                                                  		  		
			// Create the contact name address.
			val contactURI = sipServer.addressFactory.createSipURI(fromName, sipServer.contactIp)
			contactURI.setPort(sipServer.sipProvider.get.getListeningPoint(sipServer.transport).getPort())
			val contactAddress = sipServer.addressFactory.createAddress(contactURI)

			// Add the contact address.
			contactAddress.setDisplayName(fromName)
			val contactHeader = sipServer.headerFactory.createContactHeader(contactAddress)
			val str = contactAddress.toString
			println(str)
			request.addHeader(contactHeader)
			request.setContent(sdp, contentTypeHeader)

			val callInfoHeader = sipServer.headerFactory.createHeader("Call-Info","<http://www.antd.nist.gov>"); 
			request.addHeader(callInfoHeader)
			return request      
      
    }
   
    
    //do we need a callerid?	
    /*
    def getInviteRequest(callerid:String, dest:String, sdp:Array[Byte]):Request =  {
		  	val fromName = "BlueScaleServer"
			val fromDisplayName = callerid 

			val toSipAddress = dest.contains("@") match {
		  	  case true => dest.split("@")(1)
		  	  case false => sipServer.destIp
		  	}
			  
			// create >From Header
			val fromAddress = sipServer.addressFactory.createSipURI(callerid, sipServer.contactIp)
			val fromNameAddress = sipServer.addressFactory.createAddress(fromAddress)
			fromNameAddress.setDisplayName(fromDisplayName)
			val fromHeader = sipServer.headerFactory.createFromHeader(fromNameAddress, "12345")

			// create To Header
			val toAddress = sipServer.addressFactory.createSipURI(dest, toSipAddress)
			val toNameAddress = sipServer.addressFactory.createAddress(toAddress)
			toNameAddress.setDisplayName(dest)
			val toHeader = sipServer.headerFactory.createToHeader(toNameAddress,null)

			// create Request URI
			val requestURI = sipServer.addressFactory.createSipURI(dest.trim(), sipServer.destIp)

			
		 	// Create ContentTypeHeader
			val contentTypeHeader = sipServer.headerFactory.createContentTypeHeader("application", "sdp")
 			// Create a new CallId header
			val callIdHeader = sipServer.sipProvider.get.getNewCallId()
 			// Create a new Cseq header
			val cSeqHeader = sipServer.headerFactory.createCSeqHeader(1L,Request.INVITE)
 			// Create a new MaxForwardsHeader
			val maxForwards = sipServer.headerFactory.createMaxForwardsHeader(70)
 			// Create the request.
			val request = sipServer.messageFactory.createRequest(requestURI, 
                                                  		  		Request.INVITE, 
                                                  		  		callIdHeader, 
                                                  		  		cSeqHeader, 
                                                  		  		fromHeader,
                                                  		  		toHeader, 
                                                  		  		getViaHeader(), 
                                                  		  		maxForwards)
			// Create contact headers
			val contactUrl = sipServer.addressFactory.createSipURI(fromName, sipServer.contactIp)
			contactUrl.setPort(sipServer.udpListeningPoint.get.getPort())
			contactUrl.setLrParam()

			// Create the contact name address.
			val contactURI = sipServer.addressFactory.createSipURI(fromName, sipServer.contactIp)
			contactURI.setPort(sipServer.sipProvider.get.getListeningPoint(sipServer.transport).getPort())
			val contactAddress = sipServer.addressFactory.createAddress(contactURI)

			// Add the contact address.
			contactAddress.setDisplayName(fromName)
			val contactHeader = sipServer.headerFactory.createContactHeader(contactAddress)
			request.addHeader(contactHeader)
			request.setContent(sdp, contentTypeHeader)

			val callInfoHeader = sipServer.headerFactory.createHeader("Call-Info","<http://www.antd.nist.gov>"); 
			request.addHeader(callInfoHeader)
			return request
  	}
  	*/

  	def getViaHeader() : ArrayList[ViaHeader] = {
        // Create ViaHeaders
		val viaHeaders = new ArrayList[ViaHeader]
		val viaHeader = sipServer.headerFactory.createViaHeader(sipServer.contactIp,
                                                       			sipServer.port,
                                                       			sipServer.transport, 
                                                       			magicSipCookie + ctr )
        ctr = ctr+1
		// add via headers
		viaHeaders.add(viaHeader)
        return viaHeaders;
    }
}

