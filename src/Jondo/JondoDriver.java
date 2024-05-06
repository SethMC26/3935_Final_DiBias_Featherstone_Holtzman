package Jondo;

import merrimackutil.cli.OptionParser;

import java.util.Scanner;

import merrimackutil.cli.LongOption;
import merrimackutil.util.Tuple;

public class JondoDriver {
    private static void usage() {
        System.out.println("Usage: java JondoDriver --ip <Jondo IP> --port <Jondo Port> --threads <Threads> --blenderip <Blender IP> --blenderport <Blender Port>");
        System.exit(1);
    }

    public static void main(String[] args) {
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

        String jondoAddr = null;
        int jondoPort = 0;
        int threads = 0;
        String blenderAddr = null;
        int blenderPort = 0;

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

        Jondo jondo = new Jondo(jondoAddr, jondoPort, threads, blenderAddr, blenderPort);
        runCLI(jondo);
    }

    private static void runCLI(Jondo jondo) {
        Scanner scanner = new Scanner(System.in);
        String command;
        boolean running = true;

        System.out.println("Type 'send' to send data, 'quit' to exit.");

        while (running) {
            System.out.print("> ");
            command = scanner.nextLine().trim();

            switch (command.toLowerCase()) {
                case "send":
                    System.out.print("Enter destination IP: ");
                    String dstAddr = scanner.nextLine().trim();
                    System.out.print("Enter destination port: ");
                    int dstPort = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("Enter message: ");
                    String message = scanner.nextLine().trim();
                    System.out.println(jondo.send(message, dstAddr, dstPort));
                    break;
                case "quit":
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