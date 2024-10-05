package it.unito.clientmail.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection {
  private Socket server;
  private ObjectOutputStream outputStream;
  private ObjectInputStream inputStream;

  public Connection(String host, int port) throws IOException {
    server = new Socket(host, port);
    openStreams();
  }

  public void openStreams() throws IOException {
    outputStream = new ObjectOutputStream(server.getOutputStream());
    outputStream.flush();
    inputStream = new ObjectInputStream(server.getInputStream());
  }

  public void write(Object obj) throws IOException {
    outputStream.writeObject(obj);
    outputStream.flush();
  }

  public Object read() throws IOException, ClassNotFoundException {
    return inputStream.readObject();
  }

  public void closeConnection() throws IOException {
    if (server != null ) {
      try{
        outputStream.close();
        inputStream.close();
        server.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
