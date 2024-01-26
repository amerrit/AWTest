import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class AWServer {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide the path to the server config file as a command-line argument.");
            return;
        }
        //Config Parameters
        final String DIRECTORY_PATH_KEY = "directoryPath";
        final String SERVER_PORT_KEY = "serverPort";

        String configFilePath = args[0];

        Properties configProperties = new Properties();

        try (FileInputStream fileInputStream = new FileInputStream(configFilePath)) {
            configProperties.load(fileInputStream);
        }
        catch (IOException e) {
            System.out.println("An error occurred while loading the properties file: " + e.getMessage());
            return;
        }

        String directoryPath = configProperties.getProperty(DIRECTORY_PATH_KEY);
        int serverPort = Integer.parseInt(configProperties.getProperty(SERVER_PORT_KEY));

        try {
            ServerSocket serverSocket = new ServerSocket(serverPort);
            while (true) {
                // Accept a new client connection
                Socket clientSocket = serverSocket.accept();

                // Create a new thread to handle communication with the client
                Thread clientThread = new Thread(() -> clientHandler(clientSocket,directoryPath));
                clientThread.start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }
    private static void clientHandler(Socket clientSocket, String writeDirectory) {
        try (
                InputStream inputStream = clientSocket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader)
        ) {
            String receivedData = reader.readLine();
            String[] properties = receivedData.split("\\|");
            String fileName = properties[0];

            File outputFile = new File(writeDirectory,fileName);

            try (FileWriter writer = new FileWriter(outputFile)) {
                // Write the received data to the new file
                for(int i = 1; i < properties.length; i++) {
                    writer.write(properties[i] + "\n");
                }
            }

            System.out.println("Wrote filtered properties file: " + writeDirectory + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("Closing handler thread");
        }
    }
}
