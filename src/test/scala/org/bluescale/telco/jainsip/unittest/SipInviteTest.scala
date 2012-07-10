package org.bluescale.telco.jainsip.unittest


import org.bluescale.telco._
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

import javax.sip._
import javax.sip.address._
import javax.sip.header._
import javax.sip.message._
import java.util.Properties
import java.util.ArrayList

import gov.nist.javax.sip._

class SipInviteTest extends SipListener  {
   
	val sipFactory = SipFactory.getInstance()
	sipFactory.setPathName("gov.nist")
	val properties = new Properties()
	val transport = "udp"

	 val PEER_ADDRESS = "192.168.1.6"
	 val PEER_PORT = "65343"
	 val peerHostPort = PEER_ADDRESS + ":" + PEER_PORT
	   
	 var dialog:Dialog = null
	 var clientTx:ClientTransaction = null
	val port = 5060 
    properties.setProperty("javax.sip.STACK_NAME", "BSSJainSip"+this.hashCode())//name with hashcode so we can have multiple instances started in one VM
	//properties.setProperty("javax.sip.OUTBOUND_PROXY", destIp +  ":" + destPort + "/"+ transport)
	properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","debug_log" + port + ".log.txt") //FIXME
	properties.setProperty("gov.nist.javax.sip.SERVER_LOG","server_log" + port + ".log.txt")
	properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS","false")
	properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG")
	properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "1")	
    properties.setProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT", "false") 

	val sipStack = sipFactory.createSipStack(properties)
	//sipStack.asInstanceOf[SipStackImpl].getServerLog().setTraceLevel(ServerLog.TRACE)
	
	val headerFactory 	= sipFactory.createHeaderFactory()
	val addressFactory = sipFactory.createAddressFactory()
	val messageFactory = sipFactory.createMessageFactory()
	
	var udpListeningPoint:Option[ListeningPoint] = None 
	var sipProvider:SipProvider = null

	def start() {
		sipStack.start()
		udpListeningPoint = Some( sipStack.createListeningPoint("127.0.0.1", 5060, transport) )
        sipProvider = sipStack.createSipProvider(udpListeningPoint.get)
	    sipProvider.addSipListener(this)
	}

	def stop() {
		sipStack.stop()
		udpListeningPoint.foreach( sipStack.deleteListeningPoint(_) )
	}
 
	def sendInvite() {
            val fromName = "BigGuy";
            val fromSipAddress = "here.com";
            val fromDisplayName = "The Master Blaster";

            val toSipAddress = "192.168.1.6";
           // val toUser = "2134088442";
            val toUser = "blahblahblah"
            val toDisplayName = "2134088442";

            // create >From Header
            val fromAddress = addressFactory.createSipURI(
                    fromName, fromSipAddress);

            val fromNameAddress = addressFactory
                    .createAddress(fromAddress)
            fromNameAddress.setDisplayName(fromDisplayName)
            val randValue = (Math.random * Integer.MAX_VALUE).toString()
            val fromHeader = headerFactory
                    .createFromHeader(fromNameAddress, "12345");

            // create To Header
            val toAddress = addressFactory.createSipURI(
                    toUser, toSipAddress);
            val toNameAddress = addressFactory
                    .createAddress(toAddress);
            toNameAddress.setDisplayName(toDisplayName);
            val toHeader = headerFactory.createToHeader(
                    toNameAddress, null);

            // create Request URI
            val requestURI = addressFactory.createSipURI(
                    toUser, peerHostPort);

            // Create ViaHeaders

            val viaHeaders = new ArrayList[ViaHeader]();
            val port = sipProvider.getListeningPoint(transport)
                    .getPort();

            val viaHeader = headerFactory
                    .createViaHeader("192.168.1.6", port,
                            transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            // Create ContentTypeHeader
            val contentTypeHeader = headerFactory
                    .createContentTypeHeader("application", "sdp");

            // Create a new CallId header
            val callIdHeader = sipProvider.getNewCallId();

            // Create a new Cseq header
            val cSeqHeader = headerFactory
                    .createCSeqHeader(1L, Request.INVITE);

            // Create a new MaxForwardsHeader
            val maxForwards = headerFactory
                    .createMaxForwardsHeader(70);

            // Create the request.
            val request = messageFactory.createRequest(
                    requestURI, Request.INVITE, callIdHeader, cSeqHeader,
                    fromHeader, toHeader, viaHeaders, maxForwards);
            // Create contact headers

            // Create the contact name address.
            val contactURI = addressFactory.createSipURI(
                    fromName, "192.168.1.6");
            contactURI.setPort(sipProvider.getListeningPoint(
                    transport).getPort());

            val contactAddress = addressFactory
                    .createAddress(contactURI);

            // Add the contact address.
            contactAddress.setDisplayName(fromName);

            val contactHeader = headerFactory
                    .createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Add the extension header.
     //       val extensionHeader = headerFactory
     //               .createHeader("My-Header", "my header value");
     //       request.addHeader(extensionHeader);

            val sdpData = SdpHelper.getBlankSdp("192.168.1.6") 
            request.setContent(sdpData, contentTypeHeader);

            // The following is the preferred method to route requests
            // to the peer. Create a route header and set the "lr"
            // parameter for the router header.
/*
            val address = addressFactory
                    .createAddress("<sip:" + PEER_ADDRESS +  ":" + PEER_PORT
                           + ">");
            // SipUri sipUri = (SipUri) address.getURI();
            // sipUri.setPort(PEER_PORT);

            val routeHeader = headerFactory
                    .createRouteHeader(address);
            val sipUri = address.getURI().asInstanceOf[SipURI];
            sipUri.setLrParam();
            //request.addHeader(routeHeader);
 */          
            val callInfoHeader = headerFactory.createHeader(
                    "Call-Info", "<http://www.antd.nist.gov>");
            request.addHeader(callInfoHeader);

            // Create the client transaction.
            clientTx  = sipProvider.getNewClientTransaction(request);

            // send the request out.
            clientTx.sendRequest()
            
            this.dialog = this.clientTx.getDialog();
            println("created dialog " + dialog);
	}
  
	override def processRequest(re:RequestEvent) {
		println("re = " + re)
	}
	
	override def processResponse(re:ResponseEvent) {
		
		println("response = " + re.getResponse().getStatusCode())
		println("response = " + re.getResponse().toString())
		
	  
	}
	
	override def processTimeout(timeout:TimeoutEvent) {
	    error("==================================================r processTimeout!")
	}
 
	override def processIOException(ioException:IOExceptionEvent) {
	    error("processIOException!")
	}
 
	override def processTransactionTerminated(transactionTerminated:TransactionTerminatedEvent) {
	    error("processTransactionTerminated")
	}
 
	override def processDialogTerminated(dialogTerminated:DialogTerminatedEvent) {
	    error("processDialogTerminated")
	}
}
object SipInviteTest {
	def main(args:Array[String]) {
		println("SIP TEST")
		val test = new SipInviteTest()
		test.start()
		test.sendInvite()
	}
  
}

