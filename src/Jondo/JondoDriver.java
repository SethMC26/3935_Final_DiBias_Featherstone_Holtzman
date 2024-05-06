package Jondo;

public class JondoDriver {
    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: java JondoDriver <Jondo IP> <Jondo Port> <Threads> <Blender IP> <Blender Port>");
            System.exit(1);
        }
        String jondoAddr = args[0];
        int jondoPort = Integer.parseInt(args[1]);
        int threads = Integer.parseInt(args[2]);
        String blenderAddr = args[3];
        int blenderPort = Integer.parseInt(args[4]);

        Jondo jondo = new Jondo(jondoAddr, jondoPort, threads, blenderAddr, blenderPort);
    }
}
