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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static merrimackutil.json.JsonIO.readObject;

/**
 * Jondo is a node in our network must be able to join network and send/receive
 * traffic
 */
public class Jondo {
    private String addr;
    private int port;
    private String blenderAddr;
    private int blenderPort;
    private int threads;
    private ServerSocket server;
    private ConcurrentHashMap<String, Node> routingTable;
    private JondoDriver jondoDriver;
    private Random randGen;

    public Jondo(String _addr, int _port, int _threads, String _blenderAddr, int _blenderPort,
            JondoDriver _jondoDriver) {
        addr = _addr;
        port = _port;
        blenderAddr = _blenderAddr;
        blenderPort = _blenderPort;
        threads = _threads;
        jondoDriver = _jondoDriver;

        routingTable = new ConcurrentHashMap<>();

        randGen = new Random();

        // Connect to blender and try to join crowd
        try {
            // connect to blender
            Socket sock = new Socket(blenderAddr, blenderPort);
            // Get io streams
            Scanner recv = new Scanner(sock.getInputStream());
            PrintWriter send = new PrintWriter(sock.getOutputStream(), true);

            // create hello message
            Message helloMessage = new Message.Builder("HELLO").setHello(addr, port).build();

            // send hello message
            send.println(helloMessage.serialize());

            String recvString = "";

            while (recv.hasNextLine()) {
                recvString += recv.nextLine();
            }

            // Wait for welcome response and read it
            JSONObject recvJSON = readObject(recvString);

            // Turn JSON into message object
            Message recvMsg = new Message(recvJSON);

            if (!recvMsg.getType().equals("WELCOME")) {
                System.err.println("Jondo cannot join crowd, blender did not respond with WELCOME");
                System.err.println(recvMsg);
                System.err.println("This is a Fatal error exiting...");
                System.exit(1);
            }

            routingTable = recvMsg.getRoutingTable();

            // close connection
            sock.close();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // start up service thread to handle incoming connects
        // Handle connections on separate thread so we can send data on this one
        Thread serviceThread = new Thread(new ServiceThread());
        serviceThread.start();

    }

    /**
     * Sends a message with data to a destination and gets response
     * This method is blocking and will wait until data is returned
     * 
     * @param data    String of data to send
     * @param dstAddr IP address of destination
     * @param dstPort Int of port
     * @return String of reply data, null if there is an error
     */
    public String send(String data, String dstAddr, int dstPort) {

        // random number gen
        Random randGen = new Random();

        Object[] nodeList = routingTable.keySet().toArray();
        // get a random number to pick a random index from keys
        int randNum = randGen.nextInt(nodeList.length);

        // get a random node
        Node randNode = routingTable.get((String) nodeList[randNum]);

        // create message with data to send to node
        Message dataMsg = new Message.Builder("DATA").setData(dstAddr, dstPort, data).build();

        try {
            // connect to random node
            Socket sock = new Socket(randNode.getAddr(), randNode.getPort());

            // get input streams
            Scanner recv = new Scanner(sock.getInputStream());
            PrintWriter send = new PrintWriter(sock.getOutputStream(), true);

            // send message to node
            send.println(dataMsg.serialize());

            // get reply and parse it
            String reply = recv.nextLine();
            // System.out.println("Reply: " + reply);
            Message replyMessage = new Message(readObject(reply));

            System.out.println("Reply TYPE: " + replyMessage.getType());
            System.out.println(replyMessage.getType().equals("ACK"));
            if (!replyMessage.getType().equals("ACK")) {
                System.out.println("Error: " + replyMessage.getType());
                System.err.println("Jondo Connection: Message must be of ACK Type");
                System.err.println(replyMessage);
                return null;
            }

            // return replyMessage.getData();
            // Crude i know, but we can clean this up
            return replyMessage.serialize();
        } catch (Exception e) {
            System.err.println("An error has occured while sending message returning null");
            e.printStackTrace();
            System.err.println("Attempting to recover gracefully");
            return null;
        }
    }

    protected void sendVoteCast(String voteId, String selection) {
        Vote vote = new Vote.Builder(voteId)
                .setSelection(selection)
                .build();
        Message voteCastMessage = new Message.Builder("VOTE_CAST")
                .setVoteCast(blenderAddr, blenderPort, vote)
                .build();
        try {
            forwardMessageToRandomNode(voteCastMessage);
        } catch (IOException e) {
            System.err.println("Error sending vote cast message");
            e.printStackTrace();
        }
    }

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

    private Node selectRandomNode(String srcAddr, int srcPort) {
        ArrayList<String> keys = new ArrayList<>(routingTable.keySet());

        if (keys.isEmpty()) {
            return null; // No available nodes to forward to
        }

        int randIndex = randGen.nextInt(keys.size());
        return routingTable.get(keys.get(randIndex));
    }

    /**
     * Service thread allows us to dedicate a thread with the task of dealing with
     * incoming connections and sending them
     * to new threads
     *
     * This means we can keep the main thread for sending and receiving data
     */
    private class ServiceThread implements Runnable {
        @Override
        public void run() {
            try {
                server = new ServerSocket(port);

                ExecutorService pool = Executors.newFixedThreadPool(threads);

                while (true) {
                    // get connection
                    Socket sock = server.accept();

                    System.out.println();
                    System.out.println("Connected to " + sock.getInetAddress() + ":" + sock.getPort());

                    // handle connections on new thread
                    pool.execute(new JondoConnectionHandler(sock, routingTable, addr, port, blenderAddr, blenderPort,
                            jondoDriver));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
