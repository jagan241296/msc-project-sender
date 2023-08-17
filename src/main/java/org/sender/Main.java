package org.sender;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

public class Main {
    public static void main(String[] args) throws IOException {
        loadLogger();

        // args: 0-DirectoryPath for Thread 1, 1-QueueName, 2-BatchSize, 3 - Sleep time, 4 - Iterations, 5 - Threads
        if (args.length < 7) {
            int sleepTimeinMs = 100;
            if (args.length > 3) {
                sleepTimeinMs = Integer.parseInt(args[3]);
            }

            int finalSleepTimeInMs = sleepTimeinMs;

            Runnable task = () -> {

                var streamer = new VideoStreamer();
                streamer.sendDataToQueue(args[0], args[1], Integer.parseInt(args[2]), finalSleepTimeInMs, Integer.parseInt(args[4]));
            };

            // Create Executor Service
            var threadCount = Integer.parseInt(args[5]);
            var executorService = Executors.newFixedThreadPool(threadCount);
            try {
                for (var i = 0; i < threadCount; i++) {
                    executorService.submit(task);
                }
            } finally {
                executorService.shutdown();
            }

        } else {
            System.out.println("Check args...They are wrong.");
        }
    }

    private static void loadLogger() throws IOException {
        // Load logging configuration from the logging.properties file
        LogManager.getLogManager().readConfiguration(
                Main.class.getResourceAsStream("/logging.properties")
        );
    }
}
