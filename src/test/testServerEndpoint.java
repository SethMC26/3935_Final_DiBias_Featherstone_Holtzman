package test;

import Jondo.Jondo;
import Model.Message;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import static merrimackutil.json.JsonIO.readObject;

public class testServerEndpoint {
    public static void main(String[] args) {
        try {
            ServerSocket server = new ServerSocket(7000);

            while(true) {
                Socket sock = server.accept();
                System.out.println("Connected to " + sock.getRemoteSocketAddress());

                Scanner in = new Scanner(sock.getInputStream());
                PrintWriter send = new PrintWriter(sock.getOutputStream(),true);

                Message recvMsg = new Message(readObject(in.nextLine()));
                System.out.println("Got message " + recvMsg);

                send.println(recvMsg.serialize());
                System.out.println("Sending message back to " + sock.getRemoteSocketAddress());
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
