package Jondo;

import merrimackutil.cli.OptionParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import Model.Message;
import Model.Vote;
import merrimackutil.cli.LongOption;
import merrimackutil.util.Tuple;

public class JondoDriver {
    private static Jondo jondo;
    private static String jondoAddr;
    private static int jondoPort;
    private static String blenderAddr;
    private static int blenderPort;
    private static Vote currVote;
    private static HashMap<String, Vote> sentVotes = new HashMap<>(); // Store sent votes

    private static void usage() {
        System.out.println(
                "Usage: java JondoDriver --ip <Jondo IP> --port <Jondo Port> --threads <Threads> --blenderip <Blender IP> --blenderport <Blender Port>");
        System.exit(1);
    }

    public static void main(String[] args) {
        JondoDriver jondoDriver = new JondoDriver();
        jondoDriver.start(args);
    }

    public void start(String[] args) {
        OptionParser parser = new OptionParser(args);
        LongOption[] opts = {
                new LongOption("ip", true, 'i'),
                new LongOption("port", true, 'p'),
                new LongOption("threads", true, 't'),
                new LongOption("blenderip", true, 'b'),
                new LongOption("blenderport", true, 'r')
        };
        parser.setLongOpts(opts);
        parser.setOptString("i:p:t:b:r:");

        jondoAddr = null;
        jondoPort = 0;
        int threads = 0;
        blenderAddr = null;
        blenderPort = 0;

        Tuple<Character, String> currOpt;
        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getLongOpt(false);
            switch (currOpt.getFirst()) {
                case 'i':
                    jondoAddr = currOpt.getSecond();
                    break;
                case 'p':
                    jondoPort = Integer.parseInt(currOpt.getSecond());
                    break;
                case 't':
                    threads = Integer.parseInt(currOpt.getSecond());
                    break;
                case 'b':
                    blenderAddr = currOpt.getSecond();
                    break;
                case 'r':
                    blenderPort = Integer.parseInt(currOpt.getSecond());
                    break;
                case '?':
                    usage();
                    break;
            }
        }

        if (jondoAddr == null || jondoPort == 0 || threads == 0 || blenderAddr == null || blenderPort == 0) {
            usage();
        }

        jondo = new Jondo(jondoAddr, jondoPort, threads, blenderAddr, blenderPort, this);
        runCLI(jondo);
    }

    public void setCurrentVote(Vote vote) {
        currVote = vote;
    }

    public HashMap<String, Vote> getSentVotes() {
        return sentVotes;
    }

    public static void queryVoteResults(int voteIndex) {
        if (voteIndex < 1 || voteIndex > sentVotes.size()) {
            System.out.println("Invalid vote selection.");
            return;
        }
        List<String> keys = new ArrayList<>(sentVotes.keySet());
        String voteId = keys.get(voteIndex - 1);
        
        // create a new message with the voteId and send it to the blender
        Message message = new Message.Builder("VOTE_RESULTS_QUERY")
                .setVoteResultsQuery(blenderAddr, blenderPort, sentVotes.get(voteId), jondoAddr, jondoPort)
                .build();
        try {
            jondo.forwardMessageToDestination(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void displaySentVotes() {
        if (sentVotes.isEmpty()) {
            System.out.println("No votes have been sent.");
            return;
        }
        System.out.println("Sent Votes:");
        int index = 1;
        for (String voteId : sentVotes.keySet()) {
            System.out.println(index++ + ". " + voteId + " - " + sentVotes.get(voteId).getQuestion());
        }
    }

    private static void runCLI(Jondo jondo) {
        Scanner scanner = new Scanner(System.in);
        String command;
        boolean running = true;

        System.out.println("Type '.vote' to cast vote, '.quit' to exit.");

        while (running) {

            System.out.print("> ");
            // if the next line is an integer then it is a vote choice
            command = scanner.nextLine().trim();

            switch (command.toLowerCase()) {
                /*
                 * case "send":
                 * System.out.print("Enter destination IP: ");
                 * String dstAddr = scanner.nextLine().trim();
                 * System.out.print("Enter destination port: ");
                 * int dstPort = Integer.parseInt(scanner.nextLine().trim());
                 * System.out.print("Enter message: ");
                 * String message = scanner.nextLine().trim();
                 * 
                 * String reply = jondo.send(message, dstAddr, dstPort);
                 * System.out.println("Reply: " + reply);
                 * JSONObject rply = readObject(reply);
                 * try {
                 * Message rplyMsg = new Message(rply);
                 * if (!rplyMsg.getType().equals("ACK")) {
                 * System.out.println("Error for message TYPE: " + rplyMsg.getType());
                 * } else {
                 * System.out.println("ACK Received");
                 * }
                 * } catch (InvalidObjectException e) {
                 * System.out.println("Error parsing reply: " + e.getMessage());
                 * e.printStackTrace();
                 * }
                 * break;
                 */
                case ".vote":
                    System.out.println("Vote Received");
                    System.out.println("Enter your choice (number): ");
                    int choice = scanner.nextInt();
                    if (choice < 1 || choice > currVote.getOptions().size()) {
                        System.out.println("Invalid choice");
                        System.out.println("Vote not sent");
                        // TODO Handle more gracefully
                        break;
                    }
                    scanner.nextLine(); // consume newline
                    jondo.sendVoteCast(currVote.getVoteId(), currVote.getOptions().get(choice - 1));
                    System.out.println("Vote sent");
                    currVote = null; // reset current vote
                    break;
                case ".results":
                    displaySentVotes();
                    System.out.print("Select the number of the vote to query results: ");
                    int voteChoice = scanner.nextInt();
                    scanner.nextLine(); // consume newline
                    queryVoteResults(voteChoice);
                    break;
                case ".quit":
                    running = false;
                    break;
                default:
                    System.out.println("Unknown command.");
                    break;

            }
        }
        scanner.close();
    }
}
