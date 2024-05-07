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

import Model.Message;
import Model.Node;
import Model.Vote;
import merrimackutil.json.types.JSONObject;
import static merrimackutil.json.JsonIO.readObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

/**
 * Handles Connections for Blender specifically Hello Messages
 */
public class BlenderConnectionHandler implements Runnable {
    /**
     * Blender server which we are handling the connection of
     */
    private Blender blender;
    /**
     * Socket with the connection to new Jondo node
     */
    private Socket sock;

    /**
     * Handles connection to Blender specifically hello messages
     *
     * @param _blender Blender Server we are handling connections for
     * @param _sock    Socket we made connection on
     */
    public BlenderConnectionHandler(Blender _blender, Socket _sock) {
        blender = _blender;
        sock = _sock;
    }

    /**
     * Run method handles the connection on a separate thread
     */
    public void run() {
        try {
            // get input and output streams
            Scanner recv = new Scanner(sock.getInputStream());
            PrintWriter send = new PrintWriter(sock.getOutputStream(), true);

            // read message sent to server
            JSONObject recvJSON = readObject(recv.nextLine());

            // parse Message
            Message recvMessage = new Message(recvJSON);

            if (recvMessage.getType() == null) {
                System.err.println("Blender ConnectionHandler: Bad type of message closing connection");
                System.err.println("JSON message received: " + recvMessage.serialize());
                return;
            }

            switch (recvMessage.getType()) {
                case "HELLO":
                    // Get jondo from hello message
                    Node newNode = new Node(recvMessage.getSrcAddr(), recvMessage.getSrcPort());

                    // add Jondo to blender's routing table and broadcast new node
                    blender.addJondo(newNode);

                    // create Response Message with routing table
                    Message respondMessage = new Message.Builder("WELCOME").setWelcome(blender.getRoutingTable())
                            .build();

                    // Send message
                    send.println(respondMessage.serialize());
                    break;
                case "VOTE_CAST":
                    // Get vote from vote cast message
                    handleVoteCast(recvMessage.getVote());
                    break;
                case "VOTE_RESULTS_QUERY":
                    // Get vote from vote results query message
                    String srcAddr = recvMessage.getSrcAddr();
                    int srcPort = recvMessage.getSrcPort();
                    handleVoteResultsQuery(recvMessage.getVote(), srcAddr, srcPort);
                    break;
                default:
                    System.err.println("Blender ConnectionHandler: Bad type of message closing connection");
                    System.err.println("JSON message received: " + recvMessage);
                    return;
            }
            // make sure message is expected type blender should only receive HELLO messages
            // and VOTE_CAST messages
            if (!recvMessage.getType().equals("HELLO") && !recvMessage.getType().equals("VOTE_CAST")
                    && !recvMessage.getType().equals("VOTE_RESULTS_QUERY")) {
                System.err.println("Blender ConnectionHandler: Bad type of message closing connection");
                System.err.println("JSON message received: " + recvMessage);
                return;
            }
            // close connection
            sock.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleVoteCast(Vote vote) {
        blender.tallyVote(vote.getVoteId(), vote.getSelection());
        System.out.println("Vote for " + vote.getSelection() + " tallied for vote ID " + vote.getVoteId());
    }

    private void handleVoteResultsQuery(Vote vote, String srcAddr, int srcPort) {
        System.out.println("Vote results query received for vote ID " + vote.getVoteId() + " from " + srcAddr + ":" + srcPort);
        List<String> voteResults = blender.formatVoteResults(vote.getVoteId());

        Vote resultVote = new Vote.Builder(vote.getVoteId()).setResults(voteResults).build();
        Message respondMessage = new Message.Builder("VOTE_RESULTS")
                .setVoteResults(resultVote, srcAddr, srcPort, blender.getAddress(), blender.getPort()).build();
        try {
            Socket sock = new Socket(srcAddr, srcPort);
            PrintWriter send = new PrintWriter(sock.getOutputStream(), true);
            send.println(respondMessage.serialize());
            sock.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Vote results sent to " + srcAddr + ":" + srcPort);
    }
}
