import javax.net.ssl.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
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
            //SSLSocket setup
            char[] keystorePassword = "qwerty".toCharArray();
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream("C:\\Users\\Drew\\IdeaProjects\\AWTest\\src\\serverkeystore.jks"), keystorePassword);

            // Create and initialize SSLContext
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, keystorePassword);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keystore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // Create SSLServerSocketFactory
            SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

            SSLServerSocket serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(serverPort);
            while (true) {
                // Accept a new client connection
                SSLSocket clientSocket = (SSLSocket)serverSocket.accept();

                // Create a new thread to handle communication with the client
                Thread clientThread = new Thread(() -> {
                    try {
                        clientHandler(clientSocket,directoryPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                clientThread.start();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void clientHandler(SSLSocket clientSocket, String writeDirectory) throws IOException {
        try (
                InputStream inputStream = clientSocket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader)
        ) {
            String receivedData = reader.readLine();
            System.out.println(receivedData);
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

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            clientSocket.close();
            System.out.println("Closing handler thread");
        }
    }
}
