package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class JKClient {
    public static void main(String args[]){

        try(Socket socket = new Socket("localhost", 6379))
        {
            System.out.println("Connected to Redis Clone.");
            System.out.println("Type commands like: SET key value");

            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Scanner scanner = new Scanner(System.in);
            while(true){
                System.out.print("> "); // Prompt
                String input = scanner.nextLine();

                if ("exit".equalsIgnoreCase(input)) break;

                String respCommand = toRESP(input);

                // 2. Send Bytes
                out.write(respCommand.getBytes(StandardCharsets.UTF_8));
                out.flush();

                // 3. Read Response
                // Note: Real Redis might return multiple lines.
                // For simple testing, we just read the first line.
                String response = in.readLine();

                if (response == null) {
                    System.out.println("Server closed connection.");
                    break;
                }
                System.out.println("Server says: " + response);

                // If response is Bulk String ($5), read the next line (the data)
                if (response.startsWith("$")) {
                    // Check if it is a Null Bulk String ($-1)
                    if (response.equals("$-1")) {
                        System.out.println("(nil)");
                    } else {
                        // Only read the next line if there is actual data (e.g., $5)
                        String data = in.readLine();
                        System.out.println(data);
                    }
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String toRESP(String input){
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
