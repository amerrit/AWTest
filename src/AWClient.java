import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AWClient {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide the path to the server config file as a command-line argument.");
            return;
        }

        //Config Parameters
        final String DIRECTORY_PATH_KEY = "directoryPath";
        final String FILTERING_PATTERN_KEY = "filteringPattern";
        final String SERVER_IP_KEY = "serverIP";
        final String SERVER_PORT_KEY = "serverPort";

        String configFilePath = args[0];

        Properties configProperties = new Properties();

        try (FileInputStream fileInputStream = new FileInputStream(configFilePath)) {
            configProperties.load(fileInputStream);
        } catch (IOException e) {
            System.out.println("An error occurred while loading the properties file: " + e.getMessage());
            return;
        }

        Path directoryPath = Paths.get(configProperties.getProperty(DIRECTORY_PATH_KEY));
        String filteringPattern = configProperties.getProperty(FILTERING_PATTERN_KEY);
        String serverIP = configProperties.getProperty(SERVER_IP_KEY);
        int serverPort = Integer.parseInt(configProperties.getProperty(SERVER_PORT_KEY));

        try {
            // Create a WatchService
            WatchService watchService = FileSystems.getDefault().newWatchService();

            // Register the directory for ENTRY_CREATE events
            directoryPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            System.out.println("Monitoring directory: " + directoryPath);

            // Load client truststore
            char[] truststorePassword = "qwerty".toCharArray();
            KeyStore truststore = KeyStore.getInstance("JKS");
            truststore.load(new FileInputStream("C:\\Users\\Drew\\IdeaProjects\\AWTest\\src\\clienttruststore.jks"), truststorePassword);

            // Create and initialize SSLContext
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(truststore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // Create SSLSocketFactory
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Start an infinite loop to process events
            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    // Handle ENTRY_CREATE event
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path filePath = (Path) event.context();
                        System.out.println("New file created: " + filePath);

                        String dataToSend = processFile(directoryPath.resolve(filePath), filteringPattern);

                        try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(serverIP, serverPort)) {
                            // Get the output stream from the socket
                            OutputStream outputStream = socket.getOutputStream();

                            // Convert the string to bytes and write to the output stream
                            byte[] messageBytes = dataToSend.getBytes();
                            outputStream.flush();
                            outputStream.write(messageBytes);

                            System.out.println("String sent to server: " + dataToSend);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Reset the key to receive further events
                key.reset();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String processFile(Path filePath, String filteringPattern) {
        StringBuilder stringBuilder = new StringBuilder();
        Pattern filter = Pattern.compile(filteringPattern);
        try {
            // Read the content of the file using BufferedReader
            BufferedReader reader = Files.newBufferedReader(filePath);

            stringBuilder.append(filePath.getFileName().toString() + "|");
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = filter.matcher(line);
                if (matcher.find()) {
                    stringBuilder.append(line + "|");
                }
            }

            reader.close();
            Files.delete(filePath);



        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}
