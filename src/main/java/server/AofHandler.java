package main.java.server;

import main.java.parser.RESParser;
import main.java.storage.Storage;

import java.io.*;
import java.util.List;

public class AofHandler {
    private File file;
    private FileOutputStream fileOutputStream;
    public AofHandler(String fileName) throws FileNotFoundException {
        this.file = new File(fileName);
        System.out.println(">>> AOF FILE IS HERE: " + file.getAbsolutePath());
        this.fileOutputStream = new FileOutputStream(file, true);
    }

    public synchronized void append(List<String> parts){
        System.out.println(">>> APPENDING TO FILE...");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("*").append(parts.size()).append("\r\n");

            for(String part: parts){
                sb.append("$").append(part.length()).append("\r\n");
                sb.append(part).append("\r\n");
            }

            fileOutputStream.write(sb.toString().getBytes());
            fileOutputStream.flush();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void load(Storage storage){
        if(!file.exists()) return;

        System.out.println("Performing data recovery with aof file");

        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(file))){

            RESParser parser = new RESParser();

            while(true){
                List<String> parts = parser.parse(bufferedReader);
                if(parts == null) return;

                if(parts.get(0).equalsIgnoreCase("SET")){
                    storage.put(parts.get(1), parts.get(2));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        if(fileOutputStream != null)
            fileOutputStream.close();
    }

}
