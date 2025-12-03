package server;
import storage.EvictionPolicy;
import storage.Storage;

import java.util.concurrent.*;
import java.net.*;
import java.io.*;

public class JKServer {
    private int port = 6789;
    private ServerSocket serverSocket;
    private String masterHost;
    private int masterPort;

    private final ExecutorService threadPool;

    private final Storage storage = new Storage();
    private AofHandler aof;

    public JKServer(int port, int n_threads, String [] serverProps){
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(n_threads);
        String mode = serverProps[0];
        if(mode.equalsIgnoreCase("REPLICA")){
            masterHost = serverProps[1];
            masterPort = Integer.parseInt(serverProps[2]);
        }

        try {
            this.aof = new AofHandler("database"+this.port+".aof");
        } catch (IOException e) {
            System.err.println("CRITICAL: Could not create database file.");
            e.printStackTrace();
        }
    }

    public void connectToMaster(){
        System.out.println("--- 🔗 Connecting to Master at " + masterHost + ":" + masterPort + " ---");
        if(this.masterHost != null){
            try {
                // Create client socket as well to communicate iwht the master node
                Socket masterSocket = new Socket(masterHost, masterPort);
                OutputStream out = masterSocket.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(masterSocket.getInputStream()));

                System.out.println("SENDING PING....");
                sendResp(out, "PING");
                String response = in.readLine();
                System.out.println("Received: " + response);

                System.out.print("Sending REPLCONF port... ");
                sendResp(out, "REPLCONF", "listening-port", String.valueOf(this.port));
                response = in.readLine();
                System.out.println("Received: " + response);

                System.out.print("Sending REPLCONF capa... ");
                sendResp(out, "REPLCONF", "capa", "psync2");
                response = in.readLine();
                System.out.println("Received: " + response);

                System.out.print("Sending PSYNC... ");
                sendResp(out, "PSYNC", "?", "-1");
                response = in.readLine();
                System.out.println("Received: " + response);

                String lengthLine = in.readLine();

                if(lengthLine.startsWith("$")){
                    int len = Integer.parseInt(lengthLine.substring(1));
                    System.out.println("Master is sending RDB file of size: " + len + " bytes. Discarding...");

                    char [] buffer = new char[len];

                    int bytesRead = 0;
                    while(bytesRead < len){
                        bytesRead += in.read(buffer, bytesRead, len - bytesRead);
                    }

                    System.out.println("RDB File consumed, Sync completed");
                }

                System.out.println("✅ Connected to Master. Listening for updates...");
                boolean isReplica = true;
                new Thread(new ClientHandler(masterSocket, storage, aof, isReplica, in)).start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
//            socket replicaClientConnection =
        }
    }

    private void sendResp(OutputStream out, String... args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(args.length).append("\r\n");
        for (String arg : args) {
            sb.append("$").append(arg.length()).append("\r\n");
            sb.append(arg).append("\r\n");
        }
        out.write(sb.toString().getBytes());
        out.flush();
    }

    public void start(){
        try {
            System.out.println("Recovering data...");
            aof.load(storage);
            System.out.println("Data loaded.");

            if(this.masterHost != null){
                connectToMaster();
            }

            System.out.println("Starting Background Eviction Policy...");
            Thread evictionThread = new Thread(new EvictionPolicy(storage));
            evictionThread.setDaemon(true); // Daemon means it dies when the main app dies
            evictionThread.start();

            this.serverSocket = new ServerSocket(this.port);
            System.out.println("Created Socket");

            while(true){
                Socket clientConnection = serverSocket.accept();
                System.out.println("client connected " + clientConnection.getInetAddress());

                threadPool.execute(new ClientHandler(clientConnection, storage, aof));
            }

        } catch (Exception e){
            System.err.println("Unable to connect or create socket");
            e.printStackTrace();
            shutdown();
        }
    }

    public void shutdown(){
        threadPool.shutdown();
        try {
            if(serverSocket != null)
                serverSocket.close();

            if(aof != null) aof.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String mode = "SERVE";
        String replicaofHost = null;
        int port = 6379;
        int replicaOfPort = -1;

        for(int i=0; i< args.length; i++){
            String arg = args[i];
            if(arg.equalsIgnoreCase("--port") && i + 1 < args.length){
                port = Integer.parseInt(args[i+1]);
            }

            else if(arg.equalsIgnoreCase("--replicaof") && i + 2 < args.length){
                mode = "REPLICA";
                replicaofHost = args[i+1];
                replicaOfPort = Integer.parseInt(args[i+2]);
            }
        }

        String [] serverProps;

        if(replicaofHost != null){
            System.out.println("Starting REPLICA OF " + replicaofHost);
            serverProps = new String[]{mode, replicaofHost, String.valueOf(replicaOfPort)};
        } else {
            System.out.println("STARTING as MASTER");
            serverProps = new String[] {"MASTER"};
        }

        JKServer server = new JKServer(port, 3, serverProps); // 6379 is the standard Redis port
        server.start();
    }
}
