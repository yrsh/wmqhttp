# WMQhttp
Standalone Websphere MQ http-bridge with zero configuration.

## Why
The official implementation of the IBM requires an application
 server and JMS settings for the application. 
 Here everything is simpler, it's enough just to start the 
 service and send a request with a header containing 
 information about the queue, the service itself will try 
 to connect and cache this connection with the queue. 
 It is also possible to read the cursor without deleting 
 messages.
 
## Examples
You need to write a wmq-params header with the parameters 
of the queue manager and the queue.  
- Receive a message or wait while it appears:  
curl -X GET -H 'wmq-params: {"type":"rm","host":"127.0.0.1","port":1414,"channel":"CH1","queue":"q1","manager":"QM1","message":{"wait":true}}' http://localhost:3000/wmq
- Upload message:  
curl -X POST -d 'message test' -H 'wmq-params: {"type":"wr","host":"127.0.0.1","port":1414,"channel":"CH1","queue":"q1","manager":"QM1","message":{"wait":true}}' http://localhost:3000/wmq
- Read messages by cursor< without removal. You will need to set
 a unique id for your reading session, in the "cursor":  
curl -X GET -H 'wmq-params: {"type":"rd","host":"127.0.0.1","port":1414,"channel":"CH1","queue":"q1","manager":"QM1", "message":{"wait":true, "cursor":"a6aa55bb"}}' http://localhost:3000/wmq

 
## Build
The application uses IBM proprietary libraries for work, I can 
not upload them here. So you need to put them in the folder 
libs/ibm. List of necessary jars:
- com.ibm.mq.commonservices.jar
- com.ibm.mq.headers.jar
- com.ibm.mq.jar
- com.ibm.mq.jmqi.jar
- connector.jar  

Then just run the "gradle build" and execute jar with port number:  
java -jar wmqhttp.jar 8080