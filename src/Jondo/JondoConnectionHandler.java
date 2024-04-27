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
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import static merrimackutil.json.JsonIO.readObject;

public class JondoConnectionHandler implements Runnable{
    private Socket sock;
    private Jondo jondo;
    private ConcurrentHashMap<String, Node> routingTable;

    public JondoConnectionHandler(Socket _sock, Jondo _jondo) {
        sock = _sock;
        jondo = _jondo;

        //get routing table from JONDO
        routingTable = jondo.getRoutingTable();
    }

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
                case "BROADCAST":
                    //get new Jondo to add from message and add it to routing table
                    Node newJondo = new Node(recvMessage.getSrcAddr(),recvMessage.getSrcPort());
                    jondo.addJondo(newJondo);
                    break;
                case "DATA":
                    throw new IllegalArgumentException("Data case must be handled");
                default:
                    System.err.println("Jondo Connection: Message must be of DATA or Broadcast type");
                    System.err.println(recvMessage);
                    break;
            }

        } catch (InvalidObjectException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
