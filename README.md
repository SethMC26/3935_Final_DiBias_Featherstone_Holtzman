# 3935_DiBias_Featherstone_Holtzman

# Message 
Messages use JSON marshaling to send informations between all nodes on our network. Currently there are 4 types of Messages HELLO, WELCOME, BROADCAST, DATA

### Hello - Message
When a Jondo requests to join the Crowd, we send a HELLO message to the Blender server. 
- Type String - "HELLO"
- srcAddr String - the IP address of this Jondo requesting to join the crowd.
- srcPort int - The port that this Jondo will be listening on

Example Message: 
```JSON
{
"type": "HELLO",
"srcAddr": "127.0.0.1",
"srcPort": 6000
}
```

### Welcome - Message
When a Jondo requests to join the Crowd, we send a HELLO message to the Blender server. 
- type String - "WELCOME"
- routingTable JSONArray - A JSONArray with a Node JSONObjects of every other Jondo in the crowd

Example Message: 

```JSON
{
   "type" : "WELCOME",
   "routingTable" : [
      {
         "port" : 6001,
         "addr" : "127.0.0.1"
      },
      {
         "port" : 6002,
         "addr" : "127.0.0.1"
      },
      {
         "port" : 6003,
         "addr" : "127.0.0.1"
      }
   ]
}
```

### Broadcast - Message
When a Jondo joins the Crowd the Blender will send a broadcast message out to ever other node on the network so they can update their routing tables

- Type String - "HELLO"
- newNodeAddr String - the IP address of the new Jondo that joined the crowd.
- newNodePort int - The port that this the new Jondo that joined the crowd

Example Message: 
```JSON
{
"type": "BROADCAST",
"newNodeAddr": "127.0.0.1",
"newNodePort": 6000
}
```

### Data - Message
When we want to send and receive data we use a DATA message. This message is sent from the origin Jondo routed randomly through the crowd then to the destination, the response is sent along the same path(virtual tunnel) back to the orignal Jondo.

- type String - DATA
- dstAddr String - IP address of the destination to send data
- dstPort int - Port of destination to send data
- data String - Data being sent or received

Example Message:
```JSON
{
"type": "DATA",
"dstAddr": "10.162.58.74",
"dstPort": 7000,
"data": "Example_Data"
}
```

