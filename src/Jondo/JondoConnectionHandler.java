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
 * Handle the incoming connections for each Jondo
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
     * Creates new Connection hander
     * 
     * @param _sock         Socket we received connection on
     * @param _routingTable Routing Table ConcurrentHashMap with keys being string
     *                      UID and values being Node object
     */
    public JondoConnectionHandler(Socket _sock, ConcurrentHashMap<String, Node> _routingTable, String _addr,
            int _port) {
        addr = _addr;
        port = _port;
        sock = _sock;

        // get routing table from JONDO
        routingTable = _routingTable;

        // 2/3 chance of fording node
        probHead = 66;
        randGen = new Random();
    }

    /**
     * Run method handles connection new thread
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
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    private void forwardMessageToRandomNode(Message message) throws IOException {
        Node randNode = selectRandomNodeExcludingSender(message.getSrcAddr(), message.getSrcPort());
        if (randNode != null) {
            try (Socket nodeSock = new Socket(randNode.getAddr(), randNode.getPort());
                    PrintWriter nodeSend = new PrintWriter(nodeSock.getOutputStream(), true)) {
                nodeSend.println(message.serialize());
                System.out.println("Forwarded to random node: " + randNode.getAddr());
            }
        }
    }

    private void forwardMessageToDestination(Message message) throws IOException {
        try (Socket nodeSock = new Socket(message.getDstAddr(), message.getDstPort());
                PrintWriter nodeSend = new PrintWriter(nodeSock.getOutputStream(), true)) {
            nodeSend.println(message.serialize());
            System.out.println("Sent directly to destination: " + message.getDstAddr());
        }
    }

    private boolean thisNodeIsDestination(Message message) {
        return this.addr.equals(message.getDstAddr()) && this.port == message.getDstPort();
    }

    private Node selectRandomNodeExcludingSender(String srcAddr, int srcPort) {
        ArrayList<String> keys = new ArrayList<>(routingTable.keySet());
        keys.removeIf(key -> {
            Node node = routingTable.get(key);
            return node.getAddr().equals(srcAddr) && node.getPort() == srcPort;
        });

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

    private void processMessage(Message message) {
        // process message
        System.out.println("Processing message: " + message);
    }
}
