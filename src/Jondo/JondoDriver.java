package Jondo;

import Model.Configuration;
import merrimackutil.cli.OptionParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import Model.Message;
import Model.Vote;
import merrimackutil.cli.LongOption;
import merrimackutil.util.Tuple;

import static merrimackutil.json.JsonIO.readObject;

public class JondoDriver {
    private static Jondo jondo;
    private static String jondoAddr;
    private static int jondoPort;
    private static String blenderAddr;
    private static int blenderPort;
    private static String config;
    private static Vote currVote;
    private static HashMap<String, Vote> sentVotes = new HashMap<>(); // Store sent votes

    private static void usage() {
        System.out.println("Usage:");
        System.out.println("   Jondo --ip <Jondo IP> --port <Jondo Port> --threads <# of threads> " +
                "--blenderip <Blender IP> --blenderport ");
        System.out.println("   Jondo --help (displays the usage)");
        System.out.println("   Jondo --config <config file>");
        System.out.println("   Jondo (use default config)");
        System.out.println("Options:");
        System.out.println("  -h, --help\t\t Displays the usage");
        System.out.println("  -i, --ip\t\t Jondo's IP address");
        System.out.println("  -p, --port\t\tPort that Jondo will listen on");
        System.out.println("  -t, --threads\t\tNumber of threads Jondo can use");
        System.out.println("  -b, --blenderip\t\t IP address of the Blender server");
        System.out.println("  -r, --blenderport\t\t Port of the Blender server");
        System.out.println("  -c, --config\t\tConfig file to use.");
        System.out.println(" No options specified will use default config");
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
                new LongOption("blenderport", true, 'r'),
                new LongOption("config", true,'c')
        };
        parser.setLongOpts(opts);
        parser.setOptString("i:p:t:b:r:c:");

        jondoAddr = null;
        jondoPort = 0;
        int threads = 0;
        blenderAddr = null;
        blenderPort = 0;
        config = null;

        boolean doConfig = false;

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
                case 'c':
                    config = currOpt.getSecond();
                    doConfig = true;
                    break;
                case 'h':
                    usage();
                    break;
                case '?':
                    usage();
                    break;
            }
        }

        if (args.length == 0) {
            System.out.println("Using the default config file");
            config = "src/Jondo/config.json";
            doConfig = true;
        }

        if ((jondoAddr == null || jondoPort == 0 || threads == 0 || blenderAddr == null || blenderPort == 0) && (!doConfig)) {
            usage();
        }

        if (doConfig && (!(jondoAddr == null && jondoPort == 0 && threads == 0 && blenderAddr == null && blenderPort == 0))) {
            usage();
        }

        if (doConfig) {
            try {
                Configuration configFile = new Configuration(readObject(new File(config)));
                jondoAddr = configFile.getAddr();
                jondoPort = configFile.getPort();
                threads = configFile.getThreads();
                blenderAddr = configFile.getBlenderAddr();
                blenderPort = configFile.getBlenderPort();

            } catch (FileNotFoundException e) {
                System.err.println("Config file not found! Please check path: " + config);
                e.printStackTrace();
                return;
            } catch (InvalidObjectException e) {
                System.err.println("Error parsing config file");
                e.printStackTrace();
                return;
            }
        }

        System.out.println("Starting Jondo on " + jondoAddr + "/" + jondoPort + " with " +threads + " threads " +
                " with blender on " + blenderAddr + "/" +blenderPort);

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

        System.out.println("Type '.vote' to cast vote, .results to get results, .quit' to exit, .help to see this menu.");

        while (running) {

            System.out.print("> ");
            // if the next line is an integer then it is a vote choice
            command = scanner.nextLine().trim();

            switch (command.toLowerCase()) {
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
                case ".help":
                    System.out.println("Type '.vote' to cast vote, .results to get results, .quit' to exit, .help to see this menu.");
                    break;
                case ".quit":
                    running = false;
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown command.");
                    break;
            }
        }
        scanner.close();
    }
}
