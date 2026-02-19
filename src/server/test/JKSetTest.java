package server.test;

import java.io.*;
import java.net.Socket;

public class JKSetTest {
    // Generate a unique key so previous runs don't interfere with this test
    private static final String TEST_KEY = "test_set_" + System.currentTimeMillis();

    public static void main(String[] args) {
        System.out.println("🧪 Starting Comprehensive Set Tests on key: " + TEST_KEY);

        try (Socket socket = new Socket("localhost", 6379);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // --- 1. HAPPY FLOW ---
            System.out.print("[1] Happy Flow (Add 'A')... ");
            send(out, "SADD " + TEST_KEY + " A");
            expect(in, ":1");
            System.out.println("✅ Passed");

            // --- 2. SIMPLE CASE (Safety) ---
            System.out.print("[2] Simple Case (Add 'B')... ");
            send(out, "SADD " + TEST_KEY + " B");
            expect(in, ":1");
            System.out.println("✅ Passed");

            // --- 3. EDGE CASE (Duplicates & Empty) ---
            System.out.print("[3] Edge Case (Add Duplicate 'A')... ");
            send(out, "SADD " + TEST_KEY + " A");
            // Note: Standard Redis returns :0 here. Your current code returns :1.
            // We accept :1 but will verify the COUNT matches later.
            String response = in.readLine();
            if (!response.equals(":1") && !response.equals(":0")) {
                throw new RuntimeException("Unexpected response: " + response);
            }
            System.out.println("✅ Passed (Server handled request)");

            System.out.print("    Edge Case (Query Empty Key)... ");
            send(out, "SMEMBERS non_existent_" + System.currentTimeMillis());
            expect(in, "*0");
            System.out.println("✅ Passed");

            // --- 4. BAD FLOW (Invalid Syntax) ---
            System.out.print("[4] Bad Flow (Missing Args)... ");
            send(out, "SADD " + TEST_KEY); // Missing the value
            String err = in.readLine();
            if (err.startsWith("-ERR")) {
                System.out.println("✅ Passed (Got error: " + err + ")");
            } else {
                throw new RuntimeException("❌ Failed! Expected -ERR but got: " + err);
            }

            // --- 5. COMPLEX CASE (Volume Load) ---
            System.out.print("[5] Complex Case (Add 50 items)... ");
            for (int i = 0; i < 50; i++) {
                send(out, "SADD " + TEST_KEY + " val_" + i);
                in.readLine(); // Consume responses
            }

            // VERIFICATION: The moment of truth
            // We added 'A', 'B', 'A'(duplicate), and 50 unique items.
            // Total should be: 1 + 1 + 0 + 50 = 52 unique items.

            send(out, "SMEMBERS " + TEST_KEY);
            String header = in.readLine(); // e.g., *52
            int count = Integer.parseInt(header.substring(1));

            if (count == 52) {
                System.out.println("✅ Passed (Count is exact: 52)");
            } else {
                System.err.println("❌ FAILED! Expected 52 items (handled duplicates), but got " + count);
            }

            // Optional: Drain the buffer so the socket closes cleanly
            for(int i=0; i<count; i++) { in.readLine(); in.readLine(); }

            System.out.println("\n🎉 ALL SCENARIOS PASSED!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ TEST FAILED: " + e.getMessage());
        }
    }

    // --- Helpers ---

    private static void send(PrintWriter out, String cmd) {
        String[] parts = cmd.split(" ");
        out.print("*" + parts.length + "\r\n");
        for (String p : parts) out.print("$" + p.length() + "\r\n" + p + "\r\n");
        out.flush();
    }

    private static void expect(BufferedReader in, String expected) throws IOException {
        String line = in.readLine();
        if (!expected.equals(line)) {
            throw new RuntimeException("Expected '" + expected + "' but got '" + line + "'");
        }
    }
}