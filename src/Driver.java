/*
 * free (adj.): unencumbered; not under the control of others
 * Written by Seth Holtzman in 2024 and released into the public domain
 * with no warranty of any kind, either expressed or implied.
 * It probably won't make your computer catch on fire, or eat
 * your children, but it might.  Use at your own risk.
 *
 * Oh but what's a constant among friends?
 */

import Blender.Blender;
import Model.Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Driver {
    public static void main(String[] args) {


        try {
            Socket sock;
            PrintWriter send;

            Message hello1 = new Message.Builder("HELLO").setHello("127.0.0.1",6000).build();
            Message hello2 = new Message.Builder("HELLO").setHello("127.0.0.1",6001).build();
            Message hello3 = new Message.Builder("HELLO").setHello("127.0.0.1",6002).build();
            Message helloBad = new Message.Builder("HELLO").setHello("127.0.0.1",6000).build();

            sock = new Socket("127.0.0.1", 5000);
            send = new PrintWriter(sock.getOutputStream(),true);
            send.println(hello1.serialize());

            sock = new Socket("127.0.0.1", 5000);
            send = new PrintWriter(sock.getOutputStream(),true);
            send.println(hello2.serialize());

            sock = new Socket("127.0.0.1", 5000);
            send = new PrintWriter(sock.getOutputStream(),true);
            send.println(hello3.serialize());

            sock = new Socket("127.0.0.1", 5000);
            send = new PrintWriter(sock.getOutputStream(),true);
            send.println(helloBad.serialize());

            sock.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        System.out.println("done sending messages");

    }
}
