package test.java;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JKClientLoadTester {
    public static void main(String args[]){
        int numClients = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numClients);
        String host = "localhost";
        int port = 6379;

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executorService.execute(() -> {
                try (Socket socket = new Socket(host, port)) {
                    OutputStream out = socket.getOutputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // --- THE FIX: Send raw RESP bytes, not println ---

                    // We want to send: SET key-X value-X
                    String key = "key-" + clientId;
                    String val = "value-" + clientId;

                    // RESP Format:
                    // *3\r\n $3\r\nSET\r\n $len\r\nKey\r\n $len\r\nVal\r\n
                    String respCommand =
                            "*3\r\n" +
                                    "$3\r\nSET\r\n" +
                                    "$" + key.length() + "\r\n" + key + "\r\n" +
                                    "$" + val.length() + "\r\n" + val + "\r\n";

                    // Send bytes directly
                    out.write(respCommand.getBytes(StandardCharsets.UTF_8));
                    out.flush();

                    // Read response (Expect: +OK)
                    String response = in.readLine();
                    System.out.println("Client " + clientId + " (" + key + ") -> " + response);

                } catch (Exception e) {
                    System.err.println("Client " + clientId + " error: " + e.getMessage());
                }
            });
        }

        executorService.shutdown();



    }
}
