package server;

import parser.RESParser;
import storage.Storage;

import java.util.List;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;

public class ClientHandler implements Runnable{
    private int port;
    private String host;
    private Socket clientSocket;
    private final Storage storage;
    private final AofHandler aof;
    private boolean isReplica;
    private BufferedReader preCreatedReader;

    public ClientHandler(Socket clientSocket,  Storage storage, AofHandler aofHandler, boolean isReplica, BufferedReader reader){
        this.clientSocket = clientSocket;
        this.storage = storage;
        this.aof = aofHandler;
        this.isReplica = isReplica;
        this.preCreatedReader = reader;
    }

    public ClientHandler(Socket clientSocket, Storage storage, AofHandler aofHandler){
        this(clientSocket, storage, aofHandler, false, null);
    }

    @Override
    public void run() {
        RESParser parser = new RESParser();

        try{
            BufferedReader in;
            if(preCreatedReader != null){
                in = preCreatedReader;
            } else{
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            }

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String inputLine;

            while(true){
                List<String> parts = parser.parse(in);
                if (parts == null) break;

                System.out.println("Received: " + parts);
                String command = parts.get(0).toUpperCase();
                String key = parts.size() > 1 ? parts.get(1) : "";
                String val = parts.size() > 2 ? parts.get(2) : "";

                switch(command){
                    case "PING":
                        out.print("+PONG\r\n");
                        break;
                    case "SET":
                        if (parts.size() >= 3) {
                            long ttl = -1;

                            for (int i = 3; i < parts.size(); i++) {
                                if (parts.get(i).equalsIgnoreCase("PX")) {
                                    // The NEXT argument should be the number
                                    if (i + 1 < parts.size()) {
                                        try {
                                            ttl = Long.parseLong(parts.get(i + 1));
                                        } catch (NumberFormatException e) {
                                            out.print("-ERR Invalid expiration time\r\n");
                                            out.flush();
                                            return; // Stop processing
                                        }
                                    }
                                }
                            }

                            storage.put(key, val, ttl);
                            aof.append(parts);

                            ReplicaManager.propagate(parts.toArray(new String[0]));
                            if(!isReplica){
                                // Append file
                                out.print("+OK\r\n");
                                out.flush();
                            }
                        } else {
                            out.print("-ERR Usage: SET key value\r\n");
                        }
                        break;

                    case "GET":
                        if (parts.size() >= 2) {
                            String value = storage.get(key);
                            if(value == null){
                                out.print("$-1\r\n");
                            } else{
                                out.print("$" + value.length() + "\r\n" + value + "\r\n");
                            }

                        } else {
                            out.print("-ERR Usage: GET key\r\n");
                        }
                        break;

                    case "REPLCONF":
                        out.println("+OK\r\n");
                        out.flush();
                        break;
                    case "PSYNC":
                        System.out.println("Replica requesting synchronization...");
                        String masterReplId = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
                        out.print("+FULLRESYNC " + masterReplId + " 0\r\n");
                        out.flush();

                        String emptyRdbBase64 = "UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZWXCUIACAPoIYW9mLWJhc2XAZgAAFEfJbbqO337A8nMsw75sLv7i";
                        byte[] emptyRdb = java.util.Base64.getDecoder().decode(emptyRdbBase64);

                        out.print("$" + emptyRdb.length + "\r\n");
                        out.flush();

                        clientSocket.getOutputStream().write(emptyRdb);
                        clientSocket.getOutputStream().flush();
                        ReplicaManager.addReplica(clientSocket.getOutputStream());
                        System.out.println("Snapshotting data to new replica...");

                        for(String k: storage.keySet()){
                            String v = storage.get(k);

                            StringBuilder sb = new StringBuilder();
                            sb.append("*3\r\n");
                            sb.append("$3\r\nSET\r\n");
                            sb.append("$").append(k.length()).append("\r\n").append(k).append("\r\n");
                            sb.append("$").append(v.length()).append("\r\n").append(v).append("\r\n");

                            clientSocket.getOutputStream().write(sb.toString().getBytes());
                        }

                        clientSocket.getOutputStream().flush();

                        System.out.println("Replica fully synced and registered.");
                        return;

                    case "PUBLISH":
                        if(parts.size() < 3) out.print("-ERR wrong number of arguments\\r\\n");
                        String channel = parts.get(1);
                        String message = parts.get(2);

                        System.out.println("Publishing to " + channel + ": " + message);

                        PubSubManager.publish(channel, message);
                        out.print(":1\r\n");
//                        out.flush();
                        break;

                    case "SUBSCRIBE":
                        if(parts.size() < 2 ) out.print("-ERR wrong number of args");
                        String subChannel = parts.get(1);

                        PubSubManager.subscribe(subChannel, clientSocket.getOutputStream());
                        out.print("*3\r\n$9\r\nsubscribe\r\n$" + subChannel.length() + "\r\n" + subChannel + "\r\n:1\r\n");
                        out.flush();
                        break;
//                        return ":1\r\n";

                    default:
                        out.print("-ERR Unknown command\r\n");
                }

                out.flush();

            }
        }catch (IOException e) {
            System.err.println("Client disconnected abruptly: " + e.getMessage());
        } finally {
            // Good practice to ensure cleanup
            try { clientSocket.close(); } catch (IOException e) { /* ignore */ }
        }
    }
}
