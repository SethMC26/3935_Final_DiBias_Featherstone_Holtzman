# 3935_DiBias_Featherstone_Holtzman

# Blender
The Blender is responsible for allow new nodes to join the networking by giving a routing table to the new node and telling everyone else in the crowd a new node has joined. 

### Blender Object
Creating a new Blender object requires:
- _addr - String with IP address that this Blender will run on
- _Port - Int port that Blender will listen on
- _threads - Int number of threads Blender will use, determines amount of concurrent connections we can handle
  
Example:
```java
Blender blender = new Blender("127.0.0.1", 5000, 10)
```

### Quick Start
add more later
```bash
java -jar dist/Blender.jar -c example_config
```


# Jondo
The Blender is responsible for allow new nodes to join the networking by giving a routing table to the new node and telling everyone else in the crowd a new node has joined. 

### Jondo Object
Creating a new Jondo object requires:
- _addr - String with IP address that this Jondo will run on
- _Port - Int port that Jondo will listen on
- _threads - Int number of threads Jondo will use, determines the amount of concurrent connections we can handle
- blenderAddr - String IP Address of the blender server we use to join Crowd
- blenderPort - int Port of the blender server we use to join the Crowd
  
Example
```java
Jondo jondo = new Jondo("127.0.0.1", 6000, 3, "127.0.0.1", 5000)
```

### Quick Start
Add more later
```bash
java -jar dist/Jondo.jar -c example_config
```

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

