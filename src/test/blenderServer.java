package test;

import Blender.Blender;

public class blenderServer {
    public static void main(String[] args) {
        //create blender on 127.0.0.1 and on port 5000
        Blender blender = new Blender("127.0.0.1",5000, 5);
    }
}
