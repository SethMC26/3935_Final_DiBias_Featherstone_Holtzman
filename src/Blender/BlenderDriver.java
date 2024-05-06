package Blender;

public class BlenderDriver {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java BlenderDriver <Blender IP> <Blender Port> <Threads>");
            System.exit(1);
        }
        String blenderAddr = args[0];
        int blenderPort = Integer.parseInt(args[1]);
        int threads = Integer.parseInt(args[2]);

        Blender blender = new Blender(blenderAddr, blenderPort, threads);
    }
}
