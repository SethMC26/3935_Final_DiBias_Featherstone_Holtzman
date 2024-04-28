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
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static merrimackutil.json.JsonIO.readObject;

/**
 * Handle the incoming connections for each Jondo
 */
public class JondoConnectionHandler implements Runnable{
    /**
     * Socket of incoming connection
     */
    private Socket sock;
    /**
     * Routingtable
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
     * @param _sock Socket we received connection on
     * @param _routingTable Routing Table ConcurrentHashMap with keys being string UID and values being Node object
     */
    public JondoConnectionHandler(Socket _sock, ConcurrentHashMap<String, Node> _routingTable) {
        sock = _sock;

        //get routing table from JONDO
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
            //get input and output streams
            Scanner recv = new Scanner(sock.getInputStream());
            PrintWriter send = new PrintWriter(sock.getOutputStream(), true);

            //read message sent to server
            JSONObject recvJSON = readObject(recv.nextLine());

            //parse Message
            Message recvMessage = new Message(recvJSON);

            switch (recvMessage.getType()) {
                //we get broadcast from Blender of a new node joining network
                case "BROADCAST":
                    //get new Jondo to add from message and add it to routing table
                    Node newJondo = new Node(recvMessage.getSrcAddr(),recvMessage.getSrcPort());

                    //check if node is already in our routing table
                    if (routingTable.containsKey(newJondo.getUid())) {
                        System.err.println("Blender: Error adding Jondo, already in Routing Table");
                        return;
                    }

                    //put node in routing table
                    routingTable.put(newJondo.getUid(), newJondo);
                    break;
                //we are forwarded data from another node
                case "DATA":
                    Socket nodeSock = null;
                    Scanner nodeRecv = null;
                    PrintWriter nodeSend = null;

                    //flip a coin if heads(true) forward message if tails(false) send it to destination
                    //Coin flip merely determines who we send messages to(socket we connect to)
                    if (flipCoin()) {
                        //send message to random node
                        //get a list of our keys(Node UIDS)
                        Object[] nodeList = routingTable.keySet().toArray();
                        //get a random number to pick a random index from keys
                        int randNum = randGen.nextInt(nodeList.length);

                        //get a random node
                        Node randNode = routingTable.get((String) nodeList[randNum]);

                        //connect to random node
                        nodeSock = new Socket(randNode.getAddr(),randNode.getPort());
                        //get streams
                        nodeRecv = new Scanner(nodeSock.getInputStream());
                        nodeSend = new PrintWriter(nodeSock.getOutputStream());

                    }
                    else {
                        //coin flip was tails so we send node to destination
                        //connect to destination and get input streams
                        nodeSock = new Socket(recvMessage.getDstAddr(),recvMessage.getDstPort());
                        nodeRecv = new Scanner(nodeSock.getInputStream());
                        nodeSend = new PrintWriter(nodeSock.getOutputStream());
                    }

                    //forward original message to node/dst and wait for reply
                    nodeSend.println(recvMessage.serialize());

                    //get Reply string from node/dst
                    String nodeReply = nodeRecv.nextLine();

                    //send reply to original connected node
                    send.println(nodeReply);

                    //close connections
                    nodeSock.close();
                    sock.close();
                    break;
                default:
                    System.err.println("Jondo Connection: Message must be of DATA or Broadcast type");
                    System.err.println(recvMessage);
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Flips a biased coin the bias being probHead
     * We generate a random number between 0-100 if it is less than probHeads we return true
     * @return
     */
    private boolean flipCoin() {
        Random rand = new Random();
        int ranNum = rand.nextInt(100);

        return ranNum <= probHead;
    }
}
