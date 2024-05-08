# 3935_DiBias_Featherstone_Holtzman
To use our project feel free to skip ahead to the [Quick Start](#quick-start) and also see [releases](https://github.com/SethMC26/3935_Final_DiBias_Featherstone_Holtzman/releases/tag/v1.0.0)
# Voting via the Crowds network
We have created a voting system using the Crowds Overlay network is an anonymity network based on this [paper](https://web.archive.org/web/20051212103028/http://avirubin.com/crowds.pdf).

#### Crowds
Terminology:
- Blender - centralized server that the "master" routing table of all nodes(Jondo) in network. This server is used to join the Crowd and tells others when a node(Jondo) joins. 
- Jondo  - A Jondo is a node in our network.

The Crowds network provides sender anonymity, it is mathematically difficult for someone to figure out who sent a message.
It is protected as the number of nodes grows large from a local eavesdropper and is tolerant of malicious collaborating nodes in the network. 

It works by using a biased coin(Pr of heads > 1/2), when sending or receiving a message we flip this coin. If it is heads we
will randomly forward this node to another Jondo in the crowd, if it is tails we will send this message to the destination. If needed, we 
keep the circuit or virtual tunnel open and wait for a response before closing the circuit/virtual tunnel.

The receiver nor the other nodes in network can be certain of who the true sender of a message is. In our implementation we set
Prob. of heads to 0.66 or ~2/3. 

#### Length of Virtual Tunnel
The more nodes we send the message to the more "Anonymous" our message is however the longer it will take for a message to 
get to receiver. To find the length of messages let X be a Geometric Random variable with the probability of success being
1/3, to find expected number of times message will be forwarded before going to destination.

Using the Expected value of a Geometric random variable our expected circuit length is 3
![](https://media.discordapp.net/attachments/1225606930798612601/1237650472744259584/image.png?ex=663c6b53&is=663b19d3&hm=f5ac2f3f1040acc6817c51820e62ed9580462da5634835b7226bf71b51ca4a18&=&format=webp&quality=lossless)

Using the variance of Geometric random variable our variance is 6
![](https://media.discordapp.net/attachments/1225606930798612601/1237650568810332272/image.png?ex=663c6b6a&is=663b19ea&hm=38459437d36d08406e7f99589defeba2a1a2f33272db80d730315010e6d0a6dd&=&format=webp&quality=lossless&width=960&height=277)

We can use these two facts to use Chebyshev's inequality to find the probability we end up on a tail of the distribution.
We divide the result by 2 to get the right sided tail or probability our circuit length some number a or above
![](https://media.discordapp.net/attachments/1225606930798612601/1237650652553805845/image.png?ex=663c6b7e&is=663b19fe&hm=b6267872de478eb5ad3a230b4a1a0027963b94ebba4368a4af89179d32079053&=&format=webp&quality=lossless&width=768&height=468)

Graphically we can see the probabilities of large Circuit lengths get increasing less likely
- The Prob. of circuit length being 6 or more messages is 0.083(8.3%)
- The Prob. of circuit length being 9 or more messages is 0.037(3.7%)
- The Prob. of circuit length being 12 or more messages is 0.021(2.1%)
![](https://media.discordapp.net/attachments/1225606930798612601/1237652246192328744/image.png?ex=663c6cf9&is=663b1b79&hm=55700abd8bf6d1fe9f7fde6a26b6d95478e56d65aca1abf29cf5d55ffd8d71fc&=&format=webp&quality=lossless&width=688&height=468)
##### Joining Crowd
Once the blender is started nodes must join the crowd. 
1. New Jondo sends a [HELLO Message](#hello---message) to the Blender
2. Blender accepts new Jondo adds it to its own routing table and sends it a [WELCOME message](#welcome---message)
3. Blender sends a [BROADCAST message](#broadcast---message) to all of the nodes in its routing table(all nodes in crowd)
   4. Each Jondo will update their own routing table with the new Jondo

![](https://media.discordapp.net/attachments/1225606930798612601/1237507026159009862/blenderjoin5.jpg?ex=663be5ba&is=663a943a&hm=44527f054e393705c174abe8c938809c7aa60ca106871c0d20062c4931478020&=&format=webp&width=512&height=468)

##### Receiving data message
To send data we use a [DATA](#data---message)
1. Flip coin
   2. If heads forward message to a random Jondo(possibly ourselves)
   3. If tails send message to destination
2. Keep socket open, once a reply message comes across forward it to node that sent us message then close socket

#### Sending a message object
To Send a message object we send a [DATA](#data---message) to a random node in the Crowd
![](https://media.discordapp.net/attachments/1225606930798612601/1237645039400652840/send3.drawio.png?ex=663c6643&is=663b14c3&hm=3a406141f08c589f5ba9ffdb28c42e42ba1546e0ed26b69e940212bd9c7f6a8e&=&format=webp&quality=lossless&width=362&height=468)
![](https://media.discordapp.net/attachments/1225606930798612601/1237646261793067009/send4.drawio.png?ex=663c6767&is=663b15e7&hm=52453759f0883ddfe488da19eca7b6e9d736aaf00ec99f597bee1b120c7d3361&=&format=webp&quality=lossless&width=362&height=468)

#### Voting via crowds
We have added a simple CLI on top of the crowds network to allow the blender to issue a ballot and nodes to vote anonymously.
Each node is also able to see results of the ballot.


# Quick Start
## Releases
Our releases page has precompiled JAR files for both Blender and Jondo [here](https://github.com/SethMC26/3935_Final_DiBias_Featherstone_Holtzman/releases/tag/v1.0.0)
## Using ant to create jars 
Use this command via the terminal to build jar for Blender and Jondo . This will create a new directory called dist with Blender.jar and Jondo.jar files
```shell
ant dist
```
For only Jondo jar
```shell
ant dist-jondo
```
For only Blender jar
```shell
ant dist-blender
```
## Blender Quickstart
To start Blender server you can use
```bash
java -jar dist/Blender.jar  --ip <Blender IP> --port <Blender Port> --threads <Number of threads>
java -jar dist/Blender.jar  -i <Blender IP> -p <Blender Port> -t <Number of threads>
```
To use config file
```shell
java -jar dist/Blender.jar --config <config_file>
java -jar dist/Blender.jar -c <config_file>
```
To use a default config file 
```shell
java -jar dist/Blender.jar
```
#### Example config file 
- Type - String type of config "blender"
- addr - String IP this server is on
- port - Int port this server will listen on
- threads - Int number of threads to use

```JSON
{
  "type":"blender",
  "addr": "127.0.0.1",
  "port": 5001,
  "threads" : 10
}
```

## Jondo QuickStart
To start Blender server you can use
```bash
java -jar dist/Jondo.jar  --ip <Jondo IP> --port <Jondo Port> --threads <Number of threads> --blenderip <Blender Ip address> --blenderport <Blender Port>
java -jar dist/Jondo.jar  -i <Jondo IP> -p <Jondo Port> -t <Number of threads> -b <Blender Ip address> -r <Blender Port>
```
To use config file
```shell
java -jar dist/Jondo.jar --config <config_file>
java -jar dist/Jondo.jar -c <config_file>
```
To use a default config file
```shell
java -jar dist/Jondo.jar
```
Also see the example config files in src/example-config for more Jondo config files

#### Example config file
- Type - String type of config "jondo"
- addr - String IP this Jondo is on
- port - Int port this Jondo will listen on
- threads - Int number of threads to use
- blenderAddr - String blender IP address
- blenderPort - Int port of blender

```JSON
{
  "type":"jondo",
  "addr": "127.0.0.1",
  "port": 6000,
  "threads" : 3,
  "blenderAddr" : "127.0.0.1",
  "blenderPort" : 5001
}
```
## Voting Quickstart
### Creating a ballot on running blender
```shell
> .castvote
Enter a vote description: "Example vote"
Enter vote options (comma separated): "Option1,Option2,Option3"
```

### To vote on a ballot on a running Jondo
```shell
>.vote
Enter your choice (Number): 
1
```
### To get results of a ballot as a Jondo
```shell
>.results
1. KmkvBSg2gi+V7/2ToJjoOBK0VBn38TWpzeyA/SM5nsg= - Example vote

Select the number of the vote to query results: 1
```
# Blender
The Blender is responsible for allow new nodes to join the networking by giving a routing table to the new node and telling everyone else in the crowd a new node has joined. 

### Blender Object
Creating a new Blender object requires:
- _addr - String with IP address that this Blender will run on
- _Port - Int port that Blender will listen on
- _threads - Int number of threads Blender will use, determines amount of concurrent connections we can handle

# Jondo
The Blender is responsible for allow new nodes to join the networking by giving a routing table to the new node and telling everyone else in the crowd a new node has joined. 

### Jondo Object
Creating a new Jondo object requires:
- _addr - String with IP address that this Jondo will run on
- _Port - Int port that Jondo will listen on
- _threads - Int number of threads Jondo will use, determines the amount of concurrent connections we can handle
- blenderAddr - String IP Address of the blender server we use to join Crowd
- blenderPort - int Port of the blender server we use to join the Crowd

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

