package com.wormsgroup.worms_re;

import java.io.IOException;
import java.net.*;
import java.util.function.Consumer;

public class NetworkManager {

    private DatagramSocket socket;
    private InetAddress peerAddress;
    private int peerPort;
    private boolean isRunning;
    private Thread listenerThread;
    
    // Callback to pass received data to the main game
    private Consumer<String> onMessageReceived;

    /**
     * Initialize the NetworkManager with a local port to listen on.
     * @param localPort The port this peer will listen on.
     * @param onMessageReceived Callback for when a message is received.
     * @throws SocketException If the socket could not be opened.
     */
    public NetworkManager(int localPort, Consumer<String> onMessageReceived) throws SocketException {
        this.socket = new DatagramSocket(localPort);
        this.onMessageReceived = onMessageReceived;
    }

    /**
     * Set the destination peer IP and port for sending packets.
     * @param peerIp The IP address of the other peer.
     * @param peerPort The port the other peer is listening on.
     * @throws UnknownHostException If the IP address is invalid.
     */
    public void setPeer(String peerIp, int peerPort) throws UnknownHostException {
        this.peerAddress = InetAddress.getByName(peerIp);
        this.peerPort = peerPort;
    }

    /**
     * Start listening for incoming UDP packets on a separate thread.
     */
    public void startListening() {
        isRunning = true;
        listenerThread = new Thread(() -> {
            byte[] buffer = new byte[1024]; // Adjust buffer size as needed
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    // Parse the packet data into a String (or handle binary data for your game)
                    String message = new String(packet.getData(), 0, packet.getLength());
                    
                    // Pass the received data to the callback
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(message);
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error receiving packet: " + e.getMessage());
                    }
                }
            }
        });
        
        // Daemon thread ensures it doesn't block application shutdown
        listenerThread.setDaemon(true); 
        listenerThread.start();
    }

    /**
     * Send string data to the configured peer.
     * @param data The data to send.
     */
    public void sendData(String data) {
        if (peerAddress == null || peerPort == 0) {
            System.err.println("Peer address or port not set. Cannot send data.");
            return;
        }

        try {
            byte[] buffer = data.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, peerAddress, peerPort);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending packet: " + e.getMessage());
        }
    }

    /**
     * Stop listening and close the socket.
     */
    public void close() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }
}
