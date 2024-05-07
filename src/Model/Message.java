/*
 * free (adj.): unencumbered; not under the control of others
 * Written by Seth Holtzman in 2024 and released into the public domain
 * with no warranty of any kind, either expressed or implied.
 * It probably won't make your computer catch on fire, or eat
 * your children, but it might.  Use at your own risk.
 *
 * Oh but what's a constant among friends?
 */
package Model;

import merrimackutil.json.JSONSerializable;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;
import merrimackutil.json.types.JSONType;

import java.io.InvalidObjectException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON messages sent between nodes
 */
public class Message implements JSONSerializable {
    /**
     * Type of message we are sending
     *  Hello, Welcome, Data, Broadcast
     */
    private String type;
    /**
     * Source Address of message
     */
    private String srcAddr;
    /**
     * Source Port of message
     */
    private int srcPort;
    /**
     * Routing Table object
     */
    private ConcurrentHashMap<String,Node> routingTable;
    /**
     * Destination IP address
     */
    private String dstAddr;
    /**
     * Destination Port
     */
    private int dstPort;
    /**
     * Destination Data
     */
    private String data; //might change later to a different type
    /**
     * Vote to broadcast
     */
    private String vote;

    /**
     * New node to add to routing table
     */
    private Node newNode;

    /**
     * Creates Model.Message Object from Builder
     *
     * @param builder builder to create
     */
    private Message(Builder builder) {
        type = builder.type;
        srcAddr = builder.srcAddr;
        srcPort = builder.srcPort;
        routingTable = builder.routingTable;
        newNode = builder.newNode;
        dstAddr = builder.dstAddr;
        dstPort = builder.dstPort;
        data = builder.data;
        vote = builder.vote;
    }

    /**
     * Deserializes Message from JSON Object
     *
     * @param messageJSON JSONObject to deserialize
     * @throws InvalidObjectException Throws if JSON is invalid such as invalid fields
     */
    public Message(JSONObject messageJSON) throws InvalidObjectException {
        deserialize(messageJSON);
    }

    /**
     * Serializes the JSON object into JSON string representation
     *
     * @return JSON String
     */
    @Override
    public String serialize() {
        return toJSONType().toJSON();
    }

    /**
     * Deserialize the JSONObject into internal message representation
     *
     * @param jsonType JSONObject that we want to deserialize
     * @throws InvalidObjectException Throws if JSON does not contain expected keys or if it does not have JSONObject
     */
    @Override
    public void deserialize(JSONType jsonType) throws InvalidObjectException {
        if (!(jsonType instanceof JSONObject))
            throw new InvalidObjectException("JSONObject expected.");

        JSONObject messageJSON = (JSONObject) jsonType;

        if (!messageJSON.containsKey("type")) {
            throw new InvalidObjectException("Model.Message json does not have TYPE field");
        }

        //get Type of message
        type = messageJSON.getString("type");

        //deserialize JSONObject based on type of JSON
        switch(type) {
            case "HELLO":
                if (!(messageJSON.containsKey("srcAddr") && messageJSON.containsKey("srcPort"))) {
                    throw new InvalidObjectException("HELLO Model.Message should contain srcAddr and srcPort");
                }

                srcAddr = messageJSON.getString("srcAddr");
                srcPort = messageJSON.getInt("srcPort");
                break;
            case "WELCOME":
                if (!messageJSON.containsKey("routingTable")) {
                    throw new InvalidObjectException("WELCOME message should contain routingTable");
                }

                //create new Routing table as hashmap
                ConcurrentHashMap<String,Node> newRoutingTable = new ConcurrentHashMap<>();
                //get JSONArray and turn it into routingTable concurrency hashMap
                JSONArray nodeArray = messageJSON.getArray("routingTable");

                for(int i = 0;i < nodeArray.size();i++) {
                    JSONObject nodeJSON = nodeArray.getObject(i);
                    Node node = new Node(nodeJSON);
                    newRoutingTable.put(node.getUid(),node);
                }

                //set routingTable to new RoutingTable
                routingTable = newRoutingTable;
                break;
            case "BROADCAST":
                if (!(messageJSON.containsKey("newNodeAddr") || (messageJSON.containsKey("newNodePort")))) {
                    throw new InvalidObjectException("BROADCAST message should contain new Node");
                }

                //gets new node based on nodes addr and port
                newNode = new Node(messageJSON.getString("newNodeAddr"),messageJSON.getInt("newNodePort"));
                break;
            case "DATA":
                if (!(messageJSON.containsKey("dstAddr") || messageJSON.containsKey("dstPort") ||
                        messageJSON.containsKey("data"))) {
                    throw new InvalidObjectException("DATA message should contain dstAddr, dstPort, data");
                }

                dstAddr = messageJSON.getString("dstAddr");
                dstPort = messageJSON.getInt("dstPort");
                data = messageJSON.getString("data");
                break;
            case "VOTE_BROADCAST":
                if (!messageJSON.containsKey("vote")) {
                    throw new InvalidObjectException("VOTE_BROADCAST message should contain vote");
                }

                vote = messageJSON.getString("vote");
                break;
            case "ACK":
                if (!(messageJSON.containsKey("srcAddr") && messageJSON.containsKey("srcPort"))) {
                    throw new InvalidObjectException("ACK message should contain srcAddr and srcPort");
                }

                srcAddr = messageJSON.getString("srcAddr");
                srcPort = messageJSON.getInt("srcPort");
                break;
            default:
                throw new IllegalArgumentException("Bad type - Must be HELLO, WELCOME, BROADCAST, DATA, ACK");
        }
    }

    /**
     * Creates JSONObject based on type of message
     *
     * @return JSONObject of message
     */
    @Override
    public JSONType toJSONType() {
        JSONObject messageJSON = new JSONObject();

        //switch between types to and put needed fields into JSONObject
        switch (type) {
            case "HELLO":
                messageJSON.put("type",type);
                messageJSON.put("srcAddr",srcAddr);
                messageJSON.put("srcPort",srcPort);

                return messageJSON;
            case "WELCOME":
                messageJSON.put("type",type);

                //turn nodes in routing table into JSONArray
                JSONArray routingJSONArray = new JSONArray();
                routingJSONArray.addAll(routingTable.values());

                messageJSON.put("routingTable",routingJSONArray);

                return messageJSON;
            case "BROADCAST":
                messageJSON.put("type",type);
                messageJSON.put("newNodeAddr",newNode.getAddr());
                messageJSON.put("newNodePort",newNode.getPort());

                return messageJSON;
            case "DATA":
                messageJSON.put("type",type);
                messageJSON.put("dstAddr",dstAddr);
                messageJSON.put("dstPort",dstPort);
                messageJSON.put("data",data);

                return messageJSON;
            case "VOTE_BROADCAST":
                messageJSON.put("type",type);
                messageJSON.put("vote",vote);

                return messageJSON;
            case "ACK":
                messageJSON.put("type",type);
                messageJSON.put("srcAddr",srcAddr);
                messageJSON.put("srcPort",srcPort);

                return messageJSON;
            default:
                throw new IllegalArgumentException("Bad type - Must be HELLO, WELCOME, BROADCAST, DATA, ACK");
        }
    }

    @Override
    public String toString() {
        return toJSONType().getFormattedJSON();
    }

    /**
     * Gets type of message
     * @return String of type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets Source IP Address
     * @return String of IP Address
     */
    public String getSrcAddr() {
        return srcAddr;
    }

    /**
     * Gets Source Port
     * @return int of port
     */
    public int getSrcPort() {
        return srcPort;
    }
    /**
     * Gets Destination IP Address
     * @return String of IP Address
     */
    public String getDstAddr() {
        return dstAddr;
    }
    /**
     * Gets Destination Port
     * @return int of port
     */
    public int getDstPort() {
        return dstPort;
    }

    /**
     * Gets new node from the broadcast message
     * @return Node object of the new Node
     */
    public Node getNewNode() {
        return newNode;
    }

    /**
     * Gets data from message
     * @return String of data
     */
    public String getData() {
        return data;
    }


    public String getVote() {
        return vote;
    }

    /**
     *  Gets routingTable from welcome message
     * @return ConcurrentHashMap of routing table with keys being node UID and values being nodes
     */
    public ConcurrentHashMap<String,Node> getRoutingTable() {
        return routingTable;
    }

    /**
     * Builder class to make Model.Message Object
     */
    public static class Builder {
        private String type;
        private String srcAddr;
        private int srcPort;
        private ConcurrentHashMap<String,Node> routingTable;
        private String dstAddr;
        private int dstPort;
        private String data; //might change later to a different type
        private String vote;
        private Node newNode;

        /**
         * Creates basic message object
         * @param _type String type field of message: HELLO, WELCOME, BROADCAST, DATA
         */
        public Builder(String _type) {
            type = _type;
        }

        /**
         * Creates HELLO message object
         * @param _srcAddr Source IP address of Model.Jondo trying to join network
         * @param _srcPort Port Int address of Model.Jondo trying to join network
         *
         * @return this Builder
         */
        public Builder setHello(String _srcAddr, int _srcPort) {
            srcAddr = _srcAddr;
            srcPort = _srcPort;
            return this;
        }

        /**
         * Creates WELCOME message
         * @param _routingTable Routing Table to send to new node
         *
         * @return this Builder
         */
        public Builder setWelcome(ConcurrentHashMap<String,Node> _routingTable) {
            routingTable = _routingTable;
            return this;
        }

        /**
         * Creates BROADCAST message
         *
         * @param _newNode Model.Jondo to add to routingTable
         * @return this Builder
         */
        public Builder setBroadcast(Node _newNode) {
            newNode = _newNode;
            return this;
        }

        /**
         * Creates DATA message
         *
         * @param _dstAddr IP address of destination of message
         * @param _dstPort Port of destination of Model.Message
         * @param _data Data to send to destination
         *
         * @return this Builder
         */
        public Builder setData(String _dstAddr, int _dstPort, String _data) {
            dstAddr = _dstAddr;
            dstPort = _dstPort;
            data = _data;
            return this;
        }

        public Builder setAck(String _srcAddr, int _srcPort) {
            srcAddr = _srcAddr;
            srcPort = _srcPort;
            return this;
        }

        public Builder setVoteBroadcast(String _vote) {
            vote = _vote;
            return this;
        }

        /**
         * Builds Model.Message
         *
         * @return Model.Message from builder
         */
        public Message build() {
            return new Message(this);
        }
    }
}
