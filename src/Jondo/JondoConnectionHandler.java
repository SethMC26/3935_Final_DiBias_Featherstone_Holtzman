/*
 * free (adj.): unencumbered; not under the control of others
 * Written by Seth Holtzman in 2024 and released into the public domain
 * with no warranty of any kind, either expressed or implied.
 * It probably won't make your computer catch on fire, or eat
 * your children, but it might.  Use at your own risk.
 *
 * Oh but what's a constant among friends?
 */
package Jondo;

import Model.Message;
import Model.Node;
import Model.Vote;
import merrimackutil.json.types.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static merrimackutil.json.JsonIO.readObject;

/**
 * Handles incoming connections for each Jondo, processing messages and managing routing operations.
 */
public class JondoConnectionHandler implements Runnable {
    /**
     * Address of this Jondo
     */
    private String addr;
    /**
     * Port of this Jondo
     */
    private int port;
    /**
     * Address of Blender
     */
    private String blenderAddr;
    /**
     * Port of Blender
     */
    private int blenderPort;
    /**
     * Socket of incoming connection
     */
    private Socket sock;
    /**
     * Routingtable that this current Jondo knows about
     */
    private ConcurrentHashMap<String, Node> routingTable;
    /**
     * A number between 1-100 which represent the percentage chance of heads * 100
     */
    private int probHead;
    /**
     * Random number generator
     */
    private Random randGen;
    /**
     * JondoDriver of this Jondo
     */
    private JondoDriver jondoDriver;

    /**
     * Constructs a new connection handler for a Jondo.
     *
     * @param _sock         The socket through which the connection was received.
     * @param _routingTable The current routing table of the Jondo.
     * @param _addr         The IP address of this Jondo.
     * @param _port         The port number of this Jondo.
     * @param _blenderAddr  The IP address of the Blender.
     * @param _blenderPort  The port number of the Blender.
     * @param _jondoDriver  The driver for managing Jondo operations.
     */
    public JondoConnectionHandler(Socket _sock, ConcurrentHashMap<String, Node> _routingTable, String _addr,
            int _port, String _blenderAddr, int _blenderPort, JondoDriver _jondoDriver) {
        addr = _addr;
        port = _port;
        blenderAddr = _blenderAddr;
        blenderPort = _blenderPort;
        sock = _sock;
        jondoDriver = _jondoDriver;

        // get routing table from JONDO
        routingTable = _routingTable;

        // 2/3 chance of fording node
        probHead = 66;
        randGen = new Random();
    }

    /**
     * The run method processes incoming messages received through the socket.
     */
    @Override
    public void run() {
        try {
            // get input and output streams
            Scanner recv = new Scanner(sock.getInputStream());
            PrintWriter send = new PrintWriter(sock.getOutputStream(), true);

            String recvString = recv.nextLine();

            // read message sent to server
            JSONObject recvJSON = readObject(recvString);

            // parse Message
            Message recvMessage = new Message(recvJSON);

            switch (recvMessage.getType()) {
                // we get broadcast from Blender of a new node joining network
                case "BROADCAST":
                    handleBroadcast(recvMessage);
                    break;
                // we are forwarded data from another node
                case "DATA":
                    handleData(recvMessage, send);
                    break;
                case "VOTE_BROADCAST":
                    handleVoteBroadcast(recvMessage);
                    break;
                case "VOTE_CAST":
                    handleVoteCast(recvMessage);
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles the BROADCAST message type to update the routing table.
     *
     * @param recvMessage The received broadcast message.
     */
    private void handleBroadcast(Message recvMessage) {
        // get new Jondo to add from message and add it to routing table
        Node newJondo = recvMessage.getNewNode();
        // check if node is already in our routing table
        if (!routingTable.containsKey(newJondo.getUid())) {
            // put node in routing table
            routingTable.put(newJondo.getUid(), newJondo);
            System.out.println("Got Broadcast message updating routingTable");
        } else {
            // print error message
            System.err.println("Blender: Error adding Jondo, already in Routing Table");
        }
    }

    /**
     * Handles the DATA message type, determining whether to process or forward the message.
     *
     * @param recvMessage The received data message.
     * @param send        The PrintWriter to send responses.
     * @throws IOException if there is an error sending the response.
     */
    private void handleData(Message recvMessage, PrintWriter send) throws IOException {
        if (thisNodeIsDestination(recvMessage)) {
            processMessage(recvMessage);
        } else if (flipCoin()) {
            forwardMessageToRandomNode(recvMessage);
        } else {
            forwardMessageToDestination(recvMessage);
        }
        Message ackMessage = new Message.Builder("ACK")
                .setAck(addr, port)
                .build();
        System.out.println("Sending ACK: " + ackMessage.serialize());
        send.println(ackMessage.serialize());
    }

    /**
     * Handles VOTE_CAST messages by determining whether to forward the message based on a probabilistic decision.
     *
     * @param recvMessage The received vote cast message.
     */
    private void handleVoteCast(Message recvMessage) {
        // If we recieve another nodes vote cast message, we need to flip a coin and
        // forward it to the Blender
        try {
            if (flipCoin()) {
                forwardMessageToRandomNode(recvMessage);
            } else {
                forwardMessageToDestination(recvMessage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles VOTE_BROADCAST messages by updating the current vote in the JondoDriver.
     *
     * @param message The received vote broadcast message.
     */
    public void handleVoteBroadcast(Message message) {
        Vote vote = message.getVote(); // Assuming getVote() method exists
        jondoDriver.setCurrentVote(vote);
        System.out.println("New Vote Received: " + vote.getQuestion());
        for (int i = 0; i < vote.getOptions().size(); i++) {
            System.out.println((i + 1) + ". " + vote.getOptions().get(i));
        }
        System.out.println("Please cast your vote by using the command '.vote'");
    }

    /**
     * Forwards a message directly to its destination.
     *
     * @param message The message to forward.
     * @throws IOException if there is an error during forwarding.
     */
    private void forwardMessageToDestination(Message message) throws IOException {
        try (Socket nodeSock = new Socket(message.getDstAddr(), message.getDstPort());
                PrintWriter nodeSend = new PrintWriter(nodeSock.getOutputStream(), true)) {
            nodeSend.println(message.serialize());
            System.out.println("Sent directly to destination: " + message.getDstAddr());
        }
    }

    /**
     * Forwards a message to a randomly selected node in the routing table.
     *
     * @param message The message to forward.
     * @throws IOException if there is an error during forwarding.
     */
    private void forwardMessageToRandomNode(Message message) throws IOException {
        Node randNode = selectRandomNode(message.getSrcAddr(), message.getSrcPort());
        if (randNode != null) {
            try (Socket nodeSock = new Socket(randNode.getAddr(), randNode.getPort());
                    PrintWriter nodeSend = new PrintWriter(nodeSock.getOutputStream(), true)) {
                nodeSend.println(message.serialize());
                System.out.println("Forwarded to random node: " + randNode.getAddr());
            }
        }
    }

    /**
     * Determines if this node is the destination for a received message.
     * This method checks if the destination address and port in the message match this node's address and port.
     *
     * @param message The message to check.
     * @return true if this node is the destination; false otherwise.
     */
    private boolean thisNodeIsDestination(Message message) {
        return this.addr.equals(message.getDstAddr()) && this.port == message.getDstPort();
    }

    /**
     * Selects a random node from the routing table.
     *
     * @return The randomly selected Node, or null if no nodes are available.
     */
    private Node selectRandomNode(String srcAddr, int srcPort) {
        ArrayList<String> keys = new ArrayList<>(routingTable.keySet());

        if (keys.isEmpty()) {
            return null; // No available nodes to forward to
        }

        int randIndex = randGen.nextInt(keys.size());
        return routingTable.get(keys.get(randIndex));
    }

    /**
     * Flips a biased coin the bias being probHead
     * We generate a random number between 0-100 if it is less than probHeads we
     * return true
     * 
     * @return boolean indicating if the result is heads (true) or tails (false)
     */
    private boolean flipCoin() {
        SecureRandom rand = new SecureRandom();
        int ranNum = rand.nextInt(100);

        return ranNum <= probHead;
    }

    /**
     * Processes the received message
     *
     * @param message The message to process.
     */
    private void processMessage(Message message) {
        // process message
        System.out.println("Processing message: " + message);
    }
}
