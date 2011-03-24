#BlueScale
BlueScale is an Open Source platform allowing web developers to create telcom and VoIP related products or services.
It uses a simple REST front end to control the scalabe SIP server at it's core.  



## Launching
After unzipping the distribution, you need to create a BlueScaleConfig.xml file by copying the example BlueScaleConfig.Sample.xml. 
ListeningAddress and ListeningPort tag values must be a local IP and Port the server can bind to,

ContactAddress must be the external IP your SIP trunking Provider will besending SIP packets to, and needs to be somehow forwarding to your machine.
If your machine only has an external IP (which would be weird...) this can be the same as the ListeningAddress.

DestAddress and DsestPort must be the gateway of your SIP trunking provider.

CallbackUrl should point to the address of your web application that BlueScale will post to when an incoming call occurs. 

Once you finish editing your config, launch the run.sh script and you're ready to start building telco apps!



## System Requirements

- Java 1.6 
- a SIP trunking provider/gateway

