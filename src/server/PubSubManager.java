package server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PubSubManager {
    private static final Map<String, List<OutputStream>> channels = new ConcurrentHashMap<>();

    public static void subscribe(String channel, OutputStream outputStream){
        channels.computeIfAbsent(channel, k-> new CopyOnWriteArrayList<>()).add(outputStream);
        System.out.println("Client subscribed to " + channel);
    }

    public static void publish(String channel, String message){
        List<OutputStream> subscribers = channels.get(channel);

        if(subscribers == null || subscribers.isEmpty()){
            System.out.println("No subscribers for channel: " + channel);
            return;
        }

        String resp = toRespPushMessage("message", channel, message);

        byte [] data = resp.getBytes();

        for(OutputStream out: subscribers){
            try{
                synchronized (out){
                    out.write(data);
                    out.flush();
                }
            }catch (IOException e){
                System.out.println("Client disconnected");
                subscribers.remove(out);
            }
        }
    }

    private static String toRespPushMessage(String type, String channel, String msg) {
        return "*3\r\n" +
                "$" + type.length() + "\r\n" + type + "\r\n" +
                "$" + channel.length() + "\r\n" + channel + "\r\n" +
                "$" + msg.length() + "\r\n" + msg + "\r\n";
    }

}

