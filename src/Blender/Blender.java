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

import Model.Jondo;
import Model.Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *  Blender Server
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
     */
    private ConcurrentHashMap<String, Jondo> routingTable;

    /**
     * Creates a new Blender server to run an specified IP address, port and on threads
     *
     * @param _addr String IP address of for this server to run on
     * @param _port int Port of this server to run on
     * @param _threads int Number of threads of connections for this server
     */
    public Blender(String _addr, int _port, int _threads) {
        addr = _addr;
        port = _port;
        threads = _threads;

        routingTable = new ConcurrentHashMap<>();

        //create new pools for blender connections
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        try {
            //create new server
            server = new ServerSocket(port);

            //accept connections and send them on a separate thread to be dealt with
            while (true) {
                System.out.println();
                System.out.print("Server waiting for connections...");
                //get connection
                Socket sock = server.accept();

                System.out.print("Connected to " + sock.getInetAddress() + ":" + sock.getPort());

                //deal with connection on new thread
                pool.execute(new ConnectionHandler(this,sock));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a new Jondo to Blenders Routing Table and broadcasts new table to crowd
     * @param newJondo Jondo to add to routing table
     */
    public synchronized void addJondo(Jondo newJondo) {
        //check if node is already in our routing table
        if (routingTable.containsKey(newJondo.getUid())) {
            System.err.println("Blender: Error adding Jondo, already in Routing Table");
            return;
        }

        routingTable.put(newJondo.getUid(),newJondo);

        //broadcast new node to crowd
        Message broadcast = new Message.Builder("BROADCAST").setBroadcast(newJondo).build();


        try {
            //for each Jondo in routing table send broadcast message
            for (String uid : routingTable.keySet()) {
                //Current Jondo
                Jondo jondo = routingTable.get(uid);

                //Connect to Jondo and send broadcast message
                Socket sock = new Socket(jondo.getAddr(),jondo.getPort());

                PrintWriter send = new PrintWriter(sock.getOutputStream());

                send.println(broadcast);

                //close connection
                sock.close();
            }

        } catch (IOException e) {
            System.err.println("Error broadcasting message");
            e.printStackTrace();
        }
    }

    /**
     * Gets routing table
     * @return HashMap<String, Jondo> key is UID of Jondo, value is Jondo
     */
    public synchronized ConcurrentHashMap<String, Jondo> getRoutingTable() {
        return routingTable;
    }

}
