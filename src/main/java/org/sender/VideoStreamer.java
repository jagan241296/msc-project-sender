package org.sender;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class VideoStreamer {

    private final static String RABBITMQ_EXCHANGE_NAME = "streamData.topic";

    public void sendDataToQueue(String directoryPath, String queue, int batchSize, int sleepTime, int iterations) {
        System.out.printf("Input queue name: %s and batch size: %s\n", queue, batchSize);
        if (QueueEnum.RABBITMQ.name().equals(queue)) {
            sendStreamToRabbitMq(directoryPath, batchSize, sleepTime, iterations);
        } else if (QueueEnum.KAFKA.name().equals(queue)) {
            sendStreamToKafka(directoryPath, batchSize, sleepTime, iterations);
        } else if (queue.equals(QueueEnum.SERVICE_BUS.name())) {
            sendStreamToServiceBus(directoryPath, batchSize, sleepTime, iterations);
        }
    }

    private void sendStreamToServiceBus(String directoryPath, int batchSize, int sleepTime, int iterations) {

        try (var senderClient = constructServiceBusClient()) {
            var files = getFilesFromDirectory(directoryPath);
            System.out.println("START TIME: " + System.currentTimeMillis());
            var random = new Random();
            for (int i = 0; i < iterations; i++) {
                for (var file : files) {
                    //System.out.println("Reading file: " + file.getName());
                    try (var streamReader = new BufferedInputStream(new FileInputStream(file))) {
                        //batchSize = (random.nextInt(240) + 1) * 1024;
                        byte[] buffer = new byte[batchSize];
                        int bytesRead;
                        while ((bytesRead = streamReader.read(buffer)) != -1) {
                            //System.out.println(Thread.currentThread().getName() + ": Bytes read = " + bytesRead);

                            // send to queue
                            var message = new ServiceBusMessage(buffer);
                            message.getApplicationProperties().put("serviceBus.start-time", System.currentTimeMillis());
                            senderClient.sendMessage(message);

                            // sleep thread for given time
                            Thread.sleep(sleepTime);
                        }
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Error reading file: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("END TIME: " + System.currentTimeMillis());
        }
    }

    private void sendStreamToKafka(String directoryPath, int batchSize, int sleepTime, int iterations) {

        try (var producer = createKafkaProducer()) {

            // Send a message to the topic as byte array
            var topic = "kafkaStreamData";
            String key = "kafkaStreamData.key";

            var files = getFilesFromDirectory(directoryPath);
            System.out.println("START TIME: " + System.currentTimeMillis());
            var random = new Random();
            for (int i = 0; i < iterations; i++) {
                for (var file : files) {
                    //System.out.println("Reading file: " + file.getName());
                    try (var streamReader = new BufferedInputStream(new FileInputStream(file))) {
                        //batchSize = (random.nextInt(1024) + 1) * 1024;
                        byte[] buffer = new byte[batchSize];
                        int bytesRead;
                        while ((bytesRead = streamReader.read(buffer)) != -1) {
                            //System.out.println(Thread.currentThread().getName() + ": Bytes read = " + bytesRead);

                            // send to queue
                            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, buffer);
                            record.headers().add("kafka.start-time", longToBytes(System.currentTimeMillis()));
                            producer.send(record).get();

                            // sleep thread for given time
                            Thread.sleep(sleepTime);
                        }
                    } catch (IOException | ExecutionException | InterruptedException e) {
                        System.err.println("Error reading file: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("END TIME: " + System.currentTimeMillis());
            producer.flush();
        }
    }

    public static byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

    private void sendStreamToRabbitMq(String directoryPath, int batchSize, int sleepTime, int iterations) {

        //********************** Connect to RabbitMQ **************************//
        var factory = createRabbitMqConnection();
        System.out.println("Connecting to RabbitMQ Server");
        try (var connection = factory.newConnection()) {
            var channel = connection.createChannel();

            // Declare the topic exchange
            channel.exchangeDeclare(RABBITMQ_EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true);

            // Message properties (optional, you can customize these as needed)
            String routingKey = "streamData.key";

            // Iterate through files and send messages to the exchange
            var files = getFilesFromDirectory(directoryPath);
            System.out.println("START TIME: " + System.currentTimeMillis());
            var random = new Random();
            for (int i = 0; i < iterations; i++) {
                System.out.println("Iteration: " + i);
                for (var file : files) {
                    try (var streamReader = new BufferedInputStream(new FileInputStream(file));) {
                        //batchSize = (random.nextInt(1024) + 1) * 1024;
                        byte[] buffer = new byte[batchSize];
                        int bytesRead;
                        while ((bytesRead = streamReader.read(buffer)) != -1) {
                            // System.out.printf("%s Bytes sent %s\n", Thread.currentThread(), bytesRead);

                            // send to queue
                            var props = new AMQP.BasicProperties.Builder()
                                    .headers(Map.of("rabbitmq.start-time", System.currentTimeMillis()))
                                    .build();

                            channel.basicPublish(RABBITMQ_EXCHANGE_NAME, routingKey, props, buffer);

                            // sleep thread for given time
                            Thread.sleep(sleepTime);
                        }
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Error reading file: " + file.getName());
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("END TIME: " + System.currentTimeMillis());
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private ConnectionFactory createRabbitMqConnection() {
        var host = "20.25.42.181"; // Replace with the IP address or hostname of your remote RabbitMQ server
        int port = 5672; // Default RabbitMQ port for non-TLS connections
        String username = "rabbitmq";
        String password = "rabbitmq";
        String virtualHost = "/";

        var connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        return connectionFactory;
    }

    private Producer<String, byte[]> createKafkaProducer() {
        // Kafka broker connection properties
        var bootstrapServers = "20.97.171.226:9092";

        // Create producer properties
        var properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        return new KafkaProducer<>(properties);
    }

    private ServiceBusSenderClient constructServiceBusClient() {
        var connectionString
                = "Endpoint=sb://msc-stream-data.servicebus.windows.net/;SharedAccessKeyName=ClientSender;SharedAccessKey=GD+ToPA+GRfQh9pROLFfC8rlugaO8s5nf+ASbP2pKlo=";
        var topicName = "servicebus_streamdata_topic";

        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(topicName)
                .buildClient();
    }

    private List<File> getFilesFromDirectory(String directoryPath) {
        var directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {

            // read all files from the directory
            return Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                    .filter(File::isFile)
                    .collect(Collectors.toList());
        } else {
            System.out.println("No files found in the directory.");
            return Collections.emptyList();
        }
    }

}
