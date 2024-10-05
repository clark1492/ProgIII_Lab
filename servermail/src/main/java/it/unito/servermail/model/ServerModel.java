package it.unito.servermail.model;

import it.unito.servermail.handler.HandleClient;
import it.unito.servermail.utils.FilesManager;

import javafx.beans.property.SimpleStringProperty;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ServerModel implements Runnable {

  private final ServerSocket client;

  private SimpleStringProperty logText;

  private ArrayList<User> usersList;

  public ServerModel() throws IOException {

    client = new ServerSocket(8189);
    usersList = new ArrayList<>();
    logText = new SimpleStringProperty("");

  }

  public SimpleStringProperty logTextProperty() {
    return logText;
  }

  public void setLogText(String string) {
    this.logText.setValue(string);
  }

  public boolean addUser(User user) {
    if (user == null)
      throw new IllegalArgumentException("User cannot be null");

    try {
      if (FilesManager.addUserToFile(user)) {
        loadUsersList();
        return true;
      }
    } catch (IOException | ClassNotFoundException e) {
      System.out.println(e.getMessage());
    }
    return false;
  }

  private void loadUsersList() {

    ArrayList<User> list = FilesManager.getUserList();

    for (int i = 0; i < list.size(); i++) {
      usersList.add(list.get(i));
    }
  }

  public boolean userExist(String value) {

    if (value == null || value.isEmpty())
      throw new IllegalArgumentException("User is null");

    for (User user : usersList) {
      if (user.getEmail().equals(value))
        return true;
    }

    return false;

  }

  public boolean passwordIsCorrect(String email, String password) {

    if (email == null || password == null)
      throw new IllegalArgumentException();

    User user = getUser(email);

    if (user.getPassword().equals(password))
      return true;
    return false;

  }

  public User getUser(String email) {

    for (User user : usersList) {
      if (user.getEmail().equals(email)) {
        return user;
      }
    }

    return null;
  }

  private void initialize() {
    try {
      if (!Files.exists(Paths.get(FilesManager.FILES_PATH)))
        FilesManager.serverFiles();

      loadUsersList();


      for (User user : usersList) {
        if (!Files.exists(Paths.get(FilesManager.FILES_PATH + user.getEmail())))
          FilesManager.createFiles(user);
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  @Override
  public void run() {
    Thread.currentThread().setName("Server");
    initialize();
    setLogText("[SERVER] : Waiting clients connection...\n");
    Socket incoming = null;
    try {
      while (true) {
        incoming = client.accept();
        Thread client = new Thread(new HandleClient(this, incoming));
        client.start();
      }
    } catch (IOException | ClassNotFoundException e) {
      System.out.println(e.getMessage());
    } finally {
      try {
        if (incoming != null)
          incoming.close();
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }
  }
}

  ///////////////////////////////////////////
