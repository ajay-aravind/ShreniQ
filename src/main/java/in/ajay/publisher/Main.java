package in.ajay.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    static void main() throws InterruptedException {
        Logger logger =
                LoggerFactory.getLogger(Main.class);
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        logger.info(String.format("Starting publisher application"));
    }
}

