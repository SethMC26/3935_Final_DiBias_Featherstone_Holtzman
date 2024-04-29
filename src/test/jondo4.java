package test;

import Jondo.Jondo;

public class jondo4 {
    public static void main(String[] args) {
        Jondo jondo4 = new Jondo("127.0.0.1",6004,4,"127.0.0.1",5000);

        System.out.println(jondo4.send("PING","127.0.0.1",7000));

    }
}
