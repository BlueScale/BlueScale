#BlueScale

BlueScale is an Open Source platform allowing web developers to create telecom and VoIP related products or services.
It uses a simple REST front end to control the scalable SIP server at its core.  



## Launching

After unzipping the distribution, you need to create a BlueScaleConfig.xml file by copying the example BlueScaleConfig.Sample.xml. 
ListeningAddress and ListeningPort tag values must be a local IP and Port the server can bind to.

ContactAddress must be the external IP your SIP trunking Provider will be sending SIP to, and needs to be forwarding to your machine.

DestAddress and DestPort must be the gateway of your SIP trunking provider.

CallbackUrl should point to the address of your web application that BlueScale will post to when an incoming call occurs. 

Once you finish editing your config, launch the run.sh script and you're ready to start building telco apps!



## System Requirements

- Java 1.6 
- a SIP trunking provider/gateway


## Example: Forward Incoming Calls

 1. BlueScale will post a call status with the following parameters to URL specified in BlueScaleConifg.xml.  A sample post may look like this:
        
    "CallId" = " ",
    "From"   = "9494445566,
    "To"     = "7147779999,
    "CallStatus" = "Unconnected",
    "Direction"  = "Incoming"

  2. Your application will respond to the post with BlueML, an XML Dialect for controlling BlueScale.  This will cause the phone specified 
     in the number tag to ring, and connect it to the incoming call.  
    
    <Response>
        <Dial>
            <Number>" + number + "</Number>
                <Action>http://127.0.0.1:8081/Status</Action>
        </Dial>
    </Response>

  3. If and when the phone is answered that was specified in the Number tag, BlueScale will post a status for that call, with the same parameters as step 1.
    "CallId" = "B6789",
    "From"   = "9494445566"
    "To"     = "1112223333"
    "CallStatus" = "Connected",
    "Direction"  = "Outgoing"


  4. BlueScale will then post a conversation status wtih the following parameters.

    "FirstCallId"  = "A1234..."
    "SecondCallId" = "B6789..."
    "ConversationStatus" = "Connected"

