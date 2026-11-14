package in.ajay.shreniQ;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Main {
    static void main() {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        IO.println(String.format("Starting shreniQ!"));

        ShreniQServer server = new ShreniQServer();
        try {
            server.start();
        } catch (GeneralSecurityException e) {
            IO.println("Failed to init application. Exception:"+ e.getMessage());
        } catch (IOException e) {
            IO.println("Failed to init application. Exception:"+ e.getMessage());
        }
        finally {
            // todo any cleanup operations
        }
    }
}

