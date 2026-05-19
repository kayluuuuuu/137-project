package com.wormsgroup.worms_re;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class NetworkTest {
    public static void main(String[] args) {
        int myPort = 5000; 
        
        try {
            // 1. Start listening on your chosen port
            NetworkManager networkManager = new NetworkManager(myPort, message -> {
                System.out.println("\n[RECEIVED] " + message);
            });
            networkManager.startListening();
            System.out.println("Listening on port " + myPort);

            // 2. Set the destination (Replace with your friend's PUBLIC IP and Port)
            String friendIp = "203.0.113.45"; // Example Public IP
            int friendPort = 5000;
            networkManager.setPeer(friendIp, friendPort);

            // 3. Chat loop to send messages
            Scanner scanner = new Scanner(System.in);
            System.out.println("Type a message and press Enter to send:");
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) break;
                networkManager.sendData(input);
            }

            networkManager.close();
            
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
