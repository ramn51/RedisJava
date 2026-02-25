package test.java;

import main.java.server.AofHandler;
import main.java.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class AofHandlerTest {

    private static final String TEST_FILE = "test_database.aof";

    public static void main(String[] args) {
        System.out.println("--- Running AOF Persistence Tests ---");

        // 1. Clean up old test files
        deleteTestFile();

        try {
            testAppendAndLoad();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("TESTS FAILED ❌");
        }

        // Cleanup after test
        deleteTestFile();
    }

    private static void testAppendAndLoad() throws IOException {
        System.out.print("Test 1: Write to Disk and Recover... ");

        // --- PHASE 1: WRITE (Simulating Runtime) ---
        AofHandler writer = new AofHandler(TEST_FILE);

        // Simulate: SET user "JK"
        writer.append(Arrays.asList("SET", "user", "JK"));

        // Simulate: SET role "Engineer"
        writer.append(Arrays.asList("SET", "role", "Engineer"));

        // Simulate: PING (Should generally NOT be appended, but if you did, let's see if it breaks load)
        // writer.append(Arrays.asList("PING"));

        writer.close(); // Important: Closes file, flushes buffers.

        // --- PHASE 2: RECOVER (Simulating Restart) ---

        // Create a FRESH, empty main.java.storage
        Storage newStorage = new Storage();

        // Create a new Handler (mimicking main.java.server restart)
        AofHandler reader = new AofHandler(TEST_FILE);

        // Load data from disk
        reader.load(newStorage);

        // --- PHASE 3: VERIFY ---
        // Note: I assume your Storage class has a .get() method.
        // If not, you might need to check internal map or add a get() for testing.
        String user = newStorage.get("user");
        String role = newStorage.get("role");

        if ("JK".equals(user) && "Engineer".equals(role)) {
            System.out.println("PASSED ✅");
        } else {
            System.out.println("FAILED ❌");
            System.out.println("Expected: user=JK, role=Engineer");
            System.out.println("Got: user=" + user + ", role=" + role);
        }

        reader.close();
    }

    private static void deleteTestFile() {
        File f = new File(TEST_FILE);
        if (f.exists()) {
            f.delete();
        }
    }
}
