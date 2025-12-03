package server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class ReplicaManager {
    private static final List<OutputStream> replicas = new CopyOnWriteArrayList<>();

    public static void addReplica(OutputStream out){
        replicas.add(out);
        System.out.println("New Replica registered " + replicas.size());
    }

    public static void propagate(String[] commandParts){
        if(replicas.isEmpty()) return;

        byte [] data = toRespBytes(commandParts);

        for(OutputStream replicaOut : replicas){
            try{
                replicaOut.write(data);
                replicaOut.flush();
            } catch (IOException e){
                System.out.println("Replica disconnected");
                replicas.remove(replicaOut);
            }
        }
    }

    private static byte[] toRespBytes(String[] parts) {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(parts.length).append("\r\n");
        for (String part : parts) {
            sb.append("$").append(part.length()).append("\r\n");
            sb.append(part).append("\r\n");
        }
        return sb.toString().getBytes();
    }

}
