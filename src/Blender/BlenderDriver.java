package Blender;

import Model.Configuration;
import merrimackutil.cli.OptionParser;
import merrimackutil.cli.LongOption;
import merrimackutil.json.types.JSONObject;
import merrimackutil.util.Tuple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidObjectException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

import Model.Vote;

import static merrimackutil.json.JsonIO.readObject;

public class BlenderDriver {
    private static void usage() {
        System.out.println("Usage:");
        System.out.println("   Blender --ip <Blender IP> --port <Blender Port> --threads <# of threads>");
        System.out.println("   Blender --help ");
        System.out.println("   Blender --config <config file>");
        System.out.println("   Blender (use default config)");
        System.out.println("Options:");
        System.out.println("  -h, --help\t\t Displays the usage");
        System.out.println("  -i, --ip\t\tBlender's IP address");
        System.out.println("  -p, --port\t\tPort that Blender will listen on");
        System.out.println("  -t, --threads\t\tNumber of threads Blender can use");
        System.out.println("  -c, --config\t\tConfig file to use.");
        System.out.println(" No options specified will use default config");
        System.exit(1);
    }

    public static void main(String[] args) {
        OptionParser parser = new OptionParser(args);
        LongOption[] opts = {
                new LongOption("help", true,'h'),
                new LongOption("config",true,'c'),
                new LongOption("ip", true, 'i'),
                new LongOption("port", true, 'p'),
                new LongOption("threads", true, 't'),
        };

        parser.setLongOpts(opts);
        parser.setOptString("hc:i:p:t:");

        String blenderAddr = null;
        int blenderPort = 0;
        int threads = 0;

        String config = null;
        boolean doConfig = false;

        Tuple<Character, String> currOpt;
        while (parser.getOptIdx() != args.length) {
            currOpt = parser.getLongOpt(false);
            switch (currOpt.getFirst()) {
                case 'i':
                    blenderAddr = currOpt.getSecond();
                    break;
                case 'p':
                    blenderPort = Integer.parseInt(currOpt.getSecond());
                    break;
                case 't':
                    threads = Integer.parseInt(currOpt.getSecond());
                    break;
                case 'c':
                    config = currOpt.getSecond();
                    doConfig = true;
                    break;
                case 'h':
                    usage();
                    break;
                case '?':
                    System.err.println("Unknown option: " + currOpt );
                    usage();
                    break;
            }
        }

        //no arguments specified use the default config file
        if (args.length == 0) {
            System.out.println("Using default config file");
            config = "src/Blender/config.json";
            doConfig = true;
        }

        if ((blenderAddr == null || blenderPort == 0 || threads == 0) && (!doConfig)) {
            usage();
            return;
        }

        if (doConfig && (!(blenderAddr == null && blenderPort == 0 && threads == 0))) {
            usage();
            return;
        }

        if (doConfig) {
            try {
                //create config from file
                Configuration configFile = new Configuration(readObject(new File(config)));
                blenderAddr = configFile.getAddr();
                blenderPort = configFile.getPort();
                threads = configFile.getThreads();

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

        System.out.println("Starting Blender on " + blenderAddr + "/" + blenderPort + " with threads " + threads);
        Blender blender = new Blender(blenderAddr, blenderPort, threads);
        runCLI(blender);
    }

    private static void runCLI(Blender blender) {
        Scanner scanner = new Scanner(System.in);
        String command;
        boolean running = true;

        System.out.println("Type '.castvote' to create and broadcast a vote, '.quit' to exit, '.help' to see this menu");

        while (running) {
            System.out.print("> ");
            command = scanner.nextLine().trim();

            switch (command.toLowerCase()) {
                case ".castvote":
                    System.out.print("Enter vote description: ");
                    String voteDetails = scanner.nextLine().trim();
                    System.out.print("Enter vote options (comma separated): ");
                    String voteOptions = scanner.nextLine().trim();
                    List<String> voteOptionsList = Arrays.asList(voteOptions.split(","));
                    String voteId = generateVoteId(voteDetails, voteOptionsList);
                    Vote vote = new Vote.Builder(voteId)
                            .setQuestion(voteDetails)
                            .setOptions(voteOptionsList)
                            .build();
                    blender.broadcastVote(vote);
                    System.out.println("Vote broadcasted: " + vote.serialize());
                    break;
                case ".quit":
                    System.exit(0);
                    break;
                case ".help":
                    System.out.println("Type '.castvote' to create and broadcast a vote, '.quit' to exit, '.help' to see this menu");
                    break;
                default:
                    System.out.println("Unknown command.");
                    break;
            }
        }
        scanner.close();
    }

    private static String generateVoteId(String question, List<String> options) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = question + String.join("", options);
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}