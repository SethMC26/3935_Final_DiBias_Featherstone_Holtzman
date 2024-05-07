/*
 * free (adj.): unencumbered; not under the control of others
 * Written by Seth Holtzman in 2024 and released into the public domain
 * with no warranty of any kind, either expressed or implied.
 * It probably won't make your computer catch on fire, or eat
 * your children, but it might.  Use at your own risk.
 *
 * Oh but what's a constant among friends?
 */
package Blender;

import Model.Node;
import Model.Vote;
import Model.Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Blender Server
 */
public class Blender {
    /**
     * String IP address of this server
     */
    private String addr;
    /**
     * Int Port of this Server
     */
    private int port;
    /**
     * Number of threads this server uses
     */
    private int threads;
    /**
     * ServerSocket object to accept connections
     */
    private ServerSocket server;
    /**
     * Routing Table - HashMap key is UID of Jondo and value is Jondo object
     * Concurrency helps us deal with case multiple threads(connections) try to
     * modify it at once
     */
    private ConcurrentHashMap<String, Node> routingTable;
    /**
     * HashMap key is voteId and value is HashMap of options and their tallies
     */
    private ConcurrentHashMap<String, HashMap<String, Integer>> voteTallies;
    /**
     * Pool of threads to handle connections
     */
    private ExecutorService pool;

    /**
     * Creates a new Blender server to run an specified IP address, port and on
     * threads
     *
     * @param _addr    String IP address of for this server to run on
     * @param _port    int Port of this server to run on
     * @param _threads int Number of threads of connections for this server
     */
    public Blender(String _addr, int _port, int _threads) {
        addr = _addr;
        port = _port;
        threads = _threads;

        // create new routing table
        routingTable = new ConcurrentHashMap<>();
        voteTallies = new ConcurrentHashMap<>();

        // create new pools for blender connections
        pool = Executors.newFixedThreadPool(threads);

        startServer();
    }

    /**
     * Adds a new Jondo to Blenders Routing Table and broadcasts new table to crowd
     * 
     * @param newNode Jondo to add to routing table
     */
    public synchronized void addJondo(Node newNode) {
        // check if node is already in our routing table
        if (routingTable.containsKey(newNode.getUid())) {
            System.err.println("Blender: Error adding Jondo, already in Routing Table");
            return;
        }

        routingTable.put(newNode.getUid(), newNode);

        // broadcast new node to crowd
        Message broadcast = new Message.Builder("BROADCAST").setBroadcast(newNode).build();

        Node currNode = null;

        try {
            System.out.println();
            // for each Jondo in routing table send broadcast message
            for (String uid : routingTable.keySet()) {

                if (!uid.equals(newNode.getUid())) {

                    // Current Jondo
                    currNode = routingTable.get(uid);

                    // Connect to Jondo and send broadcast message
                    Socket nodeSock = new Socket(currNode.getAddr(), currNode.getPort());

                    PrintWriter send = new PrintWriter(nodeSock.getOutputStream(), true);

                    System.out.println("Sending broadcast to " + nodeSock.getRemoteSocketAddress());

                    send.println(broadcast.serialize());

                    // close connection
                    nodeSock.close();
                }

            }

        } catch (ConnectException connectException) {
            System.err.println();
            System.err.println("Unable to connect to node: " + currNode.getAddr() + ":" + currNode.getPort());
        } catch (IOException e) {
            System.err.println("Error broadcasting message");
            e.printStackTrace();
        }
    }

    public synchronized void tallyVote(String voteId, String option) {
        voteTallies.computeIfAbsent(voteId, k -> new HashMap<>()).merge(option, 1, Integer::sum);
    }
    
    public synchronized HashMap<String, Integer> getVoteResults(String voteId) {
        return voteTallies.getOrDefault(voteId, new HashMap<>());
    }

    /**
     * Broadcasts a vote to all Jondos in the routing table
     * 
     * @param vote String vote to broadcast
     */
    public void broadcastVote(Vote vote) {
        Message voteMessage = new Message.Builder("VOTE_BROADCAST").setVoteBroadcast(vote).build();

        for (Node node : routingTable.values()) {
            try (Socket socket = new Socket(node.getAddr(), node.getPort());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println(voteMessage.serialize()); // Assuming you have a serialize method
                System.out.println("Broadcasting vote to " + node.getAddr() + ":" + node.getPort());
            } catch (IOException e) {
                System.err.println("Error broadcasting vote to " + node.getAddr() + ":" + node.getPort());
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets routing table
     * 
     * @return HashMap<String, Node> key is UID of Jondo, value is Jondo
     */
    public synchronized ConcurrentHashMap<String, Node> getRoutingTable() {
        return routingTable;
    }

    /**
     * Starts the server to listen for connections and handle them on a
     * separate thread
     */
    private void startServer() {
        // create a single thread to listen for connections
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();

                    System.out.println(
                            "Connected to " + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + "\n");

                    // deal with connection on new thread from the Connection Handler Pool
                    pool.execute(new BlenderConnectionHandler(this, clientSocket));
                }
            } catch (IOException e) {
                System.err.println("Error starting server on " + addr + ":" + port);
                e.printStackTrace();
            }
        });
    }

}
