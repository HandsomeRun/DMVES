package org.example;

import com.rabbitmq.interfaces.ISender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExploreLog {
    private static final Logger commandALogger = LoggerFactory.getLogger("command.a.logger");
    private static final Logger commandBLogger = LoggerFactory.getLogger("command.b.logger");

    public static void main(String[] args) {
        //ISender sender;
        commandALogger.info("This is a log message for command A.");
        commandBLogger.warn("Warning from command B.");
    }
}
