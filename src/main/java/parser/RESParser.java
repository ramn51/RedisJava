package main.java.parser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RESParser {
//    private final BufferedInputStream in;
    public RESParser(){
//        this.in = new BufferedInputStream(inputStream);
    }
    public List<String> parse(BufferedReader input){
        List<String> args = new ArrayList<>();
        try {
            String line = input.readLine();
            if(line == null || !line.startsWith("*")) return null;

            int argC = Integer.parseInt(line.substring(1));
            int currLen = 0;
            for(int i=0; i<argC; i++){
                String lengthLine = input.readLine();
                int len = Integer.parseInt(lengthLine.substring(1));

                char [] data = new char[len];
                int bytesRead = 0;

                while(bytesRead < len){
                    int count = input.read(data, bytesRead, len - bytesRead);
                    bytesRead += count;
                }

                args.add(new String(data));
                input.read();
                input.read();

            }

            return args;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testStandardCommand() {
        System.out.print("Test 1: Standard SET command... ");

        // Simulating: SET key val
        // *3\r\n $3\r\n SET\r\n $3\r\n key\r\n $3\r\n val\r\n
        String input = "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$3\r\nval\r\n";

        List<String> result = runParser(input);

        if (result.size() == 3 &&
                result.get(0).equals("SET") &&
                result.get(2).equals("val")) {
            System.out.println("PASSED ✅");
        } else {
            System.out.println("FAILED ❌");
            System.out.println("Expected: [SET, key, val]");
            System.out.println("Got: " + result);
        }
    }

    private static void testComplexValue() {
        System.out.print("Test 2: Value containing \\r\\n (Binary Safety)... ");

        // Simulating: SET story "Hello\r\nWorld"
        // Notice the payload has a newline inside it!
        String input = "*3\r\n$3\r\nSET\r\n$5\r\nstory\r\n$12\r\nHello\r\nWorld\r\n";

        List<String> result = runParser(input);

        if (result.size() == 3 &&
                result.get(2).equals("Hello\r\nWorld")) {
            System.out.println("PASSED ✅");
        } else {
            System.out.println("FAILED ❌");
            System.out.println("Expected value: 'Hello\\r\\nWorld'");
            System.out.println("Got: '" + result.get(2).replace("\r", "\\r").replace("\n", "\\n") + "'");
        }
    }

    private static List<String> runParser(String rawData) {
        try {
            // Convert String -> InputStream -> BufferedReader
            ByteArrayInputStream byteStream = new ByteArrayInputStream(rawData.getBytes());
            BufferedReader reader = new BufferedReader(new InputStreamReader(byteStream));

            // Assuming your main.java.parser class is named RespParser
            // You might need to make the parse method 'static' or instantiate the class
            return new RESParser().parse(reader);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


//    public parse(BufferedInputStream )
    public static void main(String args[]){
            testComplexValue();
            testStandardCommand();
    }

}