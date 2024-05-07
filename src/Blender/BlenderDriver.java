package Blender;

import merrimackutil.cli.OptionParser;
import merrimackutil.cli.LongOption;
import merrimackutil.util.Tuple;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

import Model.Vote;

public class BlenderDriver {
    private static void usage() {
        System.out.println("Usage: java BlenderDriver --ip <Blender IP> --port <Blender Port> --threads <Threads>");
        System.exit(1);
    }

    public static void main(String[] args) {
        OptionParser parser = new OptionParser(args);
        LongOption[] opts = {
                new LongOption("ip", true, 'i'),
                new LongOption("port", true, 'p'),
                new LongOption("threads", true, 't')
        };
        parser.setLongOpts(opts);
        parser.setOptString("i:p:t:");

        String blenderAddr = null;
        int blenderPort = 0;
        int threads = 0;

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
                case '?':
                    usage();
                    break;
            }
        }

        if (blenderAddr == null || blenderPort == 0 || threads == 0) {
            usage();
        }

        Blender blender = new Blender(blenderAddr, blenderPort, threads);
        runCLI(blender);
    }

    private static void runCLI(Blender blender) {
        Scanner scanner = new Scanner(System.in);
        String command;
        boolean running = true;

        System.out.println("Type '.castvote' to create and broadcast a vote, '.quit' to exit.");

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
                    running = false;
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