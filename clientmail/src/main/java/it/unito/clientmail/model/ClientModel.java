package it.unito.clientmail.model;

import it.unito.servermail.model.Email;
import it.unito.servermail.model.Folder;
import it.unito.servermail.model.User;
import it.unito.clientmail.application.Login;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.LinkedList;

public class ClientModel {

  private User user;
  private Connection connection;
  private ListProperty<Email> emailListProperty;
  private ObservableList<Email> currentEmailList;
  private ObjectProperty<Folder> currentFolder;
  private ObjectProperty<Email> currentMail;

  private boolean available = true;


  public ClientModel(User user, Connection connection) {
    this.user = user;
    this.connection = connection;
    currentFolder = new SimpleObjectProperty<>();
    currentMail = new SimpleObjectProperty<>();
    currentEmailList = FXCollections.observableList(new LinkedList<>());
    emailListProperty = new SimpleListProperty<>();
    emailListProperty.set(currentEmailList);
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public void setCurrentEmailList(LinkedList<Email> currentEmailList) {
    if(currentEmailList == null){
      this.currentEmailList.clear();
      return;
    }
    this.currentEmailList.setAll(currentEmailList);
  }

  public User getUser() {
    return user;
  }

  public Email getCurrentMail() {
    return currentMail.get();
  }

  public ObjectProperty<Email> currentMailProperty() {
    return currentMail;
  }

  public void setCurrentMail(Email currentMail) {
    this.currentMail.set(currentMail);
  }
  public void setCurrentFolder(Folder currentFolder) {
    this.currentFolder.set(currentFolder);
  }

  public ListProperty<Email> emailListProperty() {
    return emailListProperty;
  }

  public LinkedList<Email> getNews() throws IOException, ClassNotFoundException{

    LinkedList<Email> news ;

      Object data;

      synchronized (connection) {

        available = false;
        connection.write("KEEP_UPDATE");
        System.out.println("KEEP_UPDATE");
        connection.write(user);
        data = connection.read();
        available = true;

      }

      if (!(data instanceof LinkedList<?>))
        throw new InvalidObjectException("ClientModel.mailboxrequest[Invalid Object]: object read is not the right type");

      news = (LinkedList<Email>) data;

    return news;
  }
  public LinkedList<Email> mailboxRequest(Folder folder) throws IOException, ClassNotFoundException{

    LinkedList<Email> mailBox = new LinkedList<>();
    if (folder != Folder.WRITE) {

      Object data;
      synchronized (connection) {

        connection.write("MAILBOX_REQUEST");
        System.out.print("MAILBOX_REQUEST: ");
        connection.write(folder);
        System.out.println(folder.toString().toUpperCase());
        data = connection.read();

      }

      if (!(data instanceof LinkedList<?>))
        throw new InvalidObjectException("ClientModel.mailboxrequest[Invalid Object]: object read is not the right type");

      mailBox = (LinkedList<Email>) data;

    }
    return mailBox;
  }

  public boolean sendEmail(Email toSend) throws IOException, ClassNotFoundException{

    if(toSend == null)
      throw new IllegalArgumentException("[sendEmail]: toSend is null");

    String resp;

    synchronized (connection) {

      connection.write("SEND_EMAIL");
      System.out.print("SEND_EMAIL -> ");
      connection.write(toSend);
      resp = serverResponse();
      System.out.println(resp);

    }

    switch (resp) {
      case "SERVER_SUCCESS" -> {
        infoNotification("Email just sent");
        return true;
      }
      case "USER_NOT_EXIST" -> {
        infoNotification("User not exist");
        return false;
      }
      default -> {
        return false;
      }
    }
  }

  public boolean deleteEmail(Email toDelete) throws IOException, ClassNotFoundException{

    if(toDelete == null)
      throw new IllegalArgumentException("[deleteEmail]: toDelete is null");

    String resp;

    synchronized (connection) {

      connection.write("DEL_EMAIL");
      System.out.print("DEL_EMAIL -> ");
      connection.write(toDelete);
      resp = serverResponse();
      System.out.println(resp);

    }

    switch (resp) {
      case "SERVER_SUCCESS" -> {
        infoNotification("Email deleted");
        return true;
      }
      case "MOVED_BIN" -> {
        infoNotification("Email moved in bin");
        return true;
      }
      default -> {return false;}
    }
  }

  public boolean notifyReadEmail(Email toNotify) throws IOException, ClassNotFoundException {

    if(toNotify == null)
      throw new IllegalArgumentException("[notifyReadEmail]: toNotify is null");

    String resp;
    synchronized (connection) {

      connection.write("READ_FLAG");
      System.out.print("READ_FLAG -> ");
      connection.write(toNotify);
      resp = serverResponse();
      System.out.println(resp);

    }
    return resp.equals("SERVER_SUCCESS");
  }

  public void logoutRequest() {
    Stage stage = Login.getStage();
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.setTitle("Logout");
      alert.setHeaderText("You're about to logout.");
      if ( alert.showAndWait().get() == ButtonType.OK) {

        try {
          synchronized (connection) {

            connection.write("LOG_OUT");
            System.out.println("LOG_OUT");
            connection.closeConnection();
          }
          stage.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private String serverResponse() throws IOException, ClassNotFoundException {
    return (String) connection.read();
  }
  public void infoNotification(String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Info");
      alert.setHeaderText(message);
      alert.showAndWait();
    });
  }
}
