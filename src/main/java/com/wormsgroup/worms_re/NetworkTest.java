package com.wormsgroup.worms_re;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class NetworkTest {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int myPort = 5000;
        String friendIp = "127.0.0.1";
        int friendPort = 5000;
        // 1. Parse CLI arguments or prompt user interactively
        if (args.length >= 3) {
            try {
                myPort = Integer.parseInt(args[0]);
                friendIp = args[1];
                friendPort = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid CLI arguments. Falling back to interactive prompts.");
            }
        } else {
            System.out.println("================================================");
            System.out.println("         P2P Network Diagnostic Test            ");
            System.out.println("================================================");

            System.out.print("Enter your LOCAL port to listen on (default 5000): ");
            String inputMyPort = scanner.nextLine().trim();
            if (!inputMyPort.isEmpty()) {
                try {
                    myPort = Integer.parseInt(inputMyPort);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port, using 5000.");
                }
            }
            System.out.print("Enter the PEER's IP address (default 127.0.0.1): ");
            String inputFriendIp = scanner.nextLine().trim();
            if (!inputFriendIp.isEmpty()) {
                friendIp = inputFriendIp;
            }
            System.out.print("Enter the PEER's port to send to (default 5000): ");
            String inputFriendPort = scanner.nextLine().trim();
            if (!inputFriendPort.isEmpty()) {
                try {
                    friendPort = Integer.parseInt(inputFriendPort);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port, using 5000.");
                }
            }
        }
        System.out
                .println("\nConfigured: Listening on port " + myPort + " | Sending to " + friendIp + ":" + friendPort);
        System.out.println("Initializing network sockets...");
        try {
            // 2. Start listening on your chosen port
            NetworkManager networkManager = new NetworkManager(myPort, message -> {
                System.out.println("\n[RECEIVED] " + message);
                System.out.print("> "); // Keep the input prompt visible
            });
            networkManager.startListening();
            System.out.println("Socket successfully opened and listening!");

            // 3. Set the destination peer
            networkManager.setPeer(friendIp, friendPort);
            // 4. Chat loop to send messages
            System.out.println("Type a message and press Enter to send (or 'exit' to quit):");
            System.out.print("> ");
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit"))
                    break;
                networkManager.sendData(input);
                System.out.print("> ");
            }
            networkManager.close();
            System.out.println("Sockets closed. Test complete.");
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Network initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}