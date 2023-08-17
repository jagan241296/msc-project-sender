# msc-project-sender

Clone the repository and run **gradle clean shadowJar** command at the root level of the project to create an Executable Jar for the application.

To run the Jar:
The Jar file takes command line args:
args: 0-DirectoryPath for Thread 1, 1-QueueName, 2-BatchSize, 3 - Message Delay, 4 - Iterations, 5 - Threads

Example command to run application and send messages to RabbitMQ.

**java -jar SenderApp.jar "/home/azureuser/videos" RABBITMQ 10240 0 500 20
**
