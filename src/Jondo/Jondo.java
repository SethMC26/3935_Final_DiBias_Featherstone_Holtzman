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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static merrimackutil.json.JsonIO.readObject;

/**
 * Jondo is a node in our network must be able to join network and send/receive traffic
 */
public class Jondo {
    private String addr;
    private int port;
    private int threads;
    private ServerSocket server;
    private ConcurrentHashMap<String, Node> routingTable;

    public Jondo(String _addr, int _port, int _threads, String blenderAddr, int blenderPort) {
        addr = _addr;
        port = _port;
        threads = _threads;

        routingTable = new ConcurrentHashMap<>();

        //Connect to blender and try to join crowd
        try {
            //connect to blender
            Socket sock = new Socket(blenderAddr,blenderPort);

            //Get io streams
            Scanner recv = new Scanner(sock.getInputStream());
            PrintWriter send = new PrintWriter(sock.getOutputStream(),true);

            //create hello message
            Message helloMessage = new Message.Builder("hello").setHello(addr,port).build();

            //send hello message
            send.println(helloMessage.serialize());

            //Wait for welcome response and read it
            JSONObject recvJSON = readObject(recv.nextLine());

            //Turn JSON into message object
            Message recvMsg = new Message(recvJSON);

            if(!recvMsg.getType().equals("WELCOME")) {
                System.err.println("Jondo cannot join crowd, blender did not respond with WELCOME");
                System.err.println(recvMsg);
                System.err.println("This is a Fatal error exiting...");
                System.exit(1);
            }

            //NEEDS TO BE IMPLEMENTED
            routingTable = recvMsg.getRoutingTable();

            //close connection
            sock.close();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //create thread pool for incoming connections
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        try {
            //create new Server to accept connections
            server = new ServerSocket(port);

            while(true) {
                System.out.println();
                System.out.print("Jondo waiting for connections...");

                //get connection
                Socket sock = server.accept();

                System.out.print("Connected to " + sock.getInetAddress() + ":" + sock.getPort());

                //handle connections on new thread
                pool.execute(new JondoConnectionHandler(sock,this));

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String send(String data) {
        throw new IllegalArgumentException("Method send must be implemented");
    }

    public synchronized void addJondo(Node newJondo) {
        //check if node is already in our routing table
        if (routingTable.containsKey(newJondo.getUid())) {
            System.err.println("Blender: Error adding Jondo, already in Routing Table");
            return;
        }

        routingTable.put(newJondo.getUid(),newJondo);
    }

    /**
     * Gets routing table
     * @return HashMap<String, Jondo> key is UID of Jondo, value is Jondo
     */
    public synchronized ConcurrentHashMap<String, Node> getRoutingTable() {
        return routingTable;
    }

}
