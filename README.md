# Arctic Wolf Assignment

This project was done as a demonstration for the technical team at Arctic Wolf - Scanning Engines. There are two branches for the project, the main branch is a demonstration without encryped connections and the encrypted branch demonstrates the same functionality using a TLS encrypted connection between the Client and Server.

## Table of Contents

- [Compilation](#compilation)
- [Usage & Examples](#usage)
- [Features](#features)

  ## Compilation

  There are two Java Prgrams included to run: the AWClient and the AWServer. To run these programs you must have a jdk installed. These have been run previously on openjdk 21.0.2

  You must pass in a valid config for the program you are running. See Usage for more info.

  For example if the jdk bin is on your PATH:
  
```bash
  java -classpath .\ AWServer .\AWServer.config
  java -classpath .\ AWClient .\AWClient.config
```

  
  ## Usage & Examples

  A few assumptions were made for the purposes of this demo. 

  -That properties files would only contain valid key value pairs.
  -That all the fields of the config file would be properly filled out.
  -That the Server will always be running when a client is run.

  See the Encrypted subsection on the set up required to run the encrypted version of the Client and Server.

  ### AWClient

  The AWClient should always be run after the AWServer. The purpose of the AWClient is to watch for new files in a specified directory and then filter the keys of the file based on a passed regex pattern and then send the new file contents to a specified AWServer.

  The AWClient config must have the following fields

  directoryPath - must be a valid directory where you want to watch for new file creation.<br />
  filteringPattern - this must be a valid java regex pattern to use for deciding what keys should be valid to send to the server<br />
  serverIP - the IP of the AWServer<br />
  serverPort - the port of the AWserver<br />
  truststorePath - is the location and name of the truststore if using encryption<br />
  truststorePass - is the password for the provided truststore (it's plaintext as this is for demo purposes only!)<br />

  Example:
  ```
  directoryPath=./input
  filteringPattern=\(cve\)
  serverIP=localhost
  serverPort=8413
    //Only used for encrypted branch runs
  truststorePath=./clienttruststore.jks
  truststorePass=password
  ```

  ### AWServer

  The AWServer should always be run before the AWClient. The purpose of the AWServer is to listen for connections from the AWClient and write a new file to disk at the location specifed in it's config with the name and contents specified by the AWClient.

  The AWServer config must have the following fields

  directoryPath - must be a valid directory where you want the files to be writen to.<br />
  serverPort - is the port the Server will listen on<br />
  keystorePath - is the location and name of the keystore if using encryption<br />
  keyPass - is the password for the provided keystore (it's plaintext as this is for demo purposes only!)<br />

  Example:
  ```
    directoryPath=./output
    serverPort=8413
    //Only used for encrypted branch runs
    keystorePath=./serverkeystore.jks
    keystorePass=password
  ```

  ### Encypted

  If you are running the encrypted version of the programs the following setup must happen first to generate a valid keystore and valid truststore

  #### Server Side:

```bash
# Generate a self-signed certificate and private key
keytool -genkey -keyalg RSA -keystore serverkeystore.jks -keysize 2048

# Export the server's public certificate
keytool -export -keystore serverkeystore.jks -file server.crt

# Import the server's public certificate into the truststore
keytool -import -file server.crt -keystore servertruststore.jks
```

#### Client Side:

```bash

# Generate a self-signed certificate and private key
keytool -genkey -keyalg RSA -keystore clientkeystore.jks -keysize 2048

# Export the client's public certificate
keytool -export -keystore clientkeystore.jks -file client.crt

# Import the client's public certificate into the truststore
keytool -import -file client.crt -keystore clienttruststore.jks

# Import the server's public certificate into the truststore
keytool -import -file server.crt -keystore clienttruststore.jks
```

#### Server Side:

```bash
# Import the clients's public certificate into the truststore
keytool -import -file client.crt -keystore servertruststore.jks
```

These last steps need to ahppen because we are using self signed certs.

### Running the Program

Now all you need to do is create a new file with appropriate name value pair format in the folder monitored by the AWClient. I have included an example in the input folder of the project already that you can use the cp command to trigger a new file event with.<br />

The new file will be read and deleted by AWClient and then you will see the new filtered file in the output folder as specified by AWServer.<br />

## Features

The basic functionality of the programs as specified in the assignment is met with this code. However Id ecided to go a bit further and add in an encrypted communication demonstration as well, but kept it to it's own feature branch. 
Since this is a demonstration I chose to not implement everything I could think of, but there are other ways I believe this could be imporved or expanded upon such as the following<br />

### More Error Handling & Schema Validation

The programs are built with assumptions that the messages will be | delimited and that all the config properties are properly formated and valid. There could certainly be more checking against these, especially with something like schema validation in the case that the properties files we are reading are always well formatted key-value pairs.

### Configurable Delimiters

Currently the data is formatted into a | delimited String when sent to the Server. I like the one shot nature of the single string being sent, which requires delimiters, but not necessarily |, making the delimiter character configurable could be done through the config.

### Regex?

The properties files are always key-value pairs. Using a regex to filter the key names is powerful, but if the files had a rigid key naming system it may be worth using something such as specifying the specific fields we want to pull (this pairs nicely with schema validation.)

### Configuration Management

The curent way the server and client are set up is reminiscent of other collecter-server software I ahve written in the past, often it is much more useful for people deploying clients to make them configurable from the server in some way. This requires much more infrastructure to generate and deploy out configurations to clients, but allows you to do things such as change what fields you are filtering out from a central config to potentially hundreds of distributed clients.


  
