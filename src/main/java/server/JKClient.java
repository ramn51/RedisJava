package main.java.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class JKClient {
    public static void main(String args[]) {
        try (Socket socket = new Socket("localhost", 6379)) {
            System.out.println("Connected to Redis Clone.");

            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // --- THREAD 1: THE LISTENER (Always reads) ---
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        // Just print everything the main.java.server sends instantly
                        System.out.println("<< " + line);
                        System.out.print("> "); // Repaint prompt
                    }
                } catch (Exception e) {
                    System.out.println("Server connection closed.");
                    System.exit(0);
                }
            }).start();

            // --- THREAD 2: THE WRITER (Waits for you) ---
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) break;

                String respCommand = toRESP(input);
                out.write(respCommand.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String toRESP(String input) {
        // Simple split by space
        // Note: This breaks if you use quotes like "Hello World" (it splits into 2 args)
        // For testing, just use single words like Hello_World
        String[] parts = input.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(parts.length).append("\r\n");
        for (String part : parts) {
            sb.append("$").append(part.length()).append("\r\n");
            sb.append(part).append("\r\n");
        }
        return sb.toString();
    }
}