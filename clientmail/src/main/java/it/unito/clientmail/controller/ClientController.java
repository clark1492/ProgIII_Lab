package it.unito.clientmail.controller;

import it.unito.clientmail.model.*;
import it.unito.servermail.model.Email;
import it.unito.servermail.model.Folder;
import it.unito.servermail.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientController {

  private ClientModel model;
  private SimpleDateFormat dateForm = new SimpleDateFormat("dd/MM/yyyy");

  //folders_box
  @FXML
  private Label lblUsername;


  //lstEmail
  @FXML
  private Label lblFolder;
  @FXML
  private ListView<Email> lstEmails;


  //writeBox
  @FXML
  private VBox writeBox;
  @FXML
  private TextField subjectField;
  @FXML
  private TextField toField;
  @FXML
  private Label fromLabelW;
  @FXML
  private TextArea emailAreaW;


  //readBox
  @FXML
  private VBox readBox;
  @FXML
  private Label subjectLabel;
  @FXML
  private Label fromLabelR;
  @FXML
  private Label toLabelR;
  @FXML
  private Label dateLabel;
  @FXML
  private TextArea emailAreaR;
  @FXML
  private Button replyButton;
  @FXML
  private Button replyAllButton;
  @FXML
  private Button forwardButton;
  @FXML
  private Button deleteButton;
  @FXML
  private Label errorLabel;
  @FXML
  private ImageView errorImg;
  private boolean readMode = true;

  // Cambio la modalità da scrittura a lettura e viceversa
  private void readView(boolean read) {
    if (!read) {
      readMode = false;
      readBox.setVisible(false);
      writeBox.setVisible(true);
      subjectLabel.setText("");
      fromLabelR.setText("");
      dateLabel.setText("");
      emailAreaR.clear();
      toLabelR.setText("");
    } else {
      readMode = true;
      readBox.setVisible(true);
      writeBox.setVisible(false);
      subjectField.setEditable(true);
      toField.setEditable(true);
      fromLabelW.setText(model.getUser().getEmail());
      subjectField.clear();
      toField.clear();
      emailAreaW.clear();
    }
  }

  @FXML
  protected void newMailOnAction() {
    new Thread(new Refresh(Folder.WRITE, null)).start();
  }

  @FXML
  protected void inboxOnAction() {
    new Thread(new Refresh(Folder.INBOX, null)).start();
  }

  @FXML
  protected void sentMailOnAction() {
    new Thread(new Refresh(Folder.SENT, null)).start();
  }

  @FXML
  protected void trashOnAction() {
    new Thread(new Refresh(Folder.BIN, null)).start();
  }

  @FXML
  protected void logoutOnAction() {
    new Thread((new LogOutThread())).start();
  }

  @FXML
  protected void replyOnAction() {
    reply(false);
  }

  @FXML
  protected void replyAllOnAction() {
    reply(true);
  }

  @FXML
  protected void forwardOnAction() {
    Email toForward = new Email();
    Email original = model.getCurrentMail();
    toForward.setSender(model.getUser().getEmail());
    if (original.getSubject().startsWith("FWD: "))
      toForward.setSubject(original.getSubject());
    else
      toForward.setSubject("FWD: " + original.getSubject());
    String s = "";
    for (int i = 0; i < 75; i++)
      s += "*";
    String intro = "\n\n" + s + "\n\nBeing forwarded:\n\nFROM: " + original.getSender() + "\nSUBJECT: " + original.getSubject() + "\nDATE: " +
            original.getDate().toString() + "\nTO: " + getDestsString(original.getDests()) + "\n\n";
    toForward.setContent(intro + original.getContent());
    toForward.setReply(null);
    toForward.setBelonging(Folder.WRITE);
    new Thread(new Refresh(Folder.WRITE, toForward)).start();
  }

  @FXML
  protected void deleteOnAction() {
    Email toDelete = model.getCurrentMail();
    new Thread(new DeleteThread(toDelete)).start();
  }

  @FXML
  protected void sendOnAction() {

    Email toSend = composeMail(model.getCurrentMail());
    if (toSend != null) {
      new Thread(new SendThread(toSend)).start();
    }
  }

  @FXML
  protected void undoOnAction() {
    subjectField.clear();
    toField.clear();
    emailAreaW.clear();
    subjectField.setEditable(true);
    toField.setEditable(true);
    model.setCurrentMail(null);
  }

  private void reply(boolean all) {
    Email reply = new Email();

    Email toReply = model.getCurrentMail();
    List<String> destsToReply = toReply.getDests();
    System.out.println("i destinatri sono: " + destsToReply );
    String senderToReply = toReply.getSender();
    reply.setReply(toReply);
    reply.setSender(model.getUser().getEmail());
    reply.setId(toReply.getId());
    List<String> destsReply = new ArrayList<>();

    if (!(senderToReply.equals(model.getUser().getEmail()))) {
      if (all) {
        for (int i = 0 ; i < destsToReply.size(); i++) {
          if (!(destsToReply.get(i).equals(model.getUser().getEmail())))
            destsReply.add(destsToReply.get(i)); // aggiungo tutti gli user tranne me stesso ai destinatari
        }
        destsReply.add(senderToReply);
        reply.setDests(destsReply);
      } else {
        reply.setDests(List.of(senderToReply));
      }
    } else {  //se la mail è la continuazione di una reply che abbiamo già inviato
      if (all) {
        reply.setDests(destsToReply);
      } else {
        reply.setDests(List.of(destsToReply.get(0)));
      }
    }

    if (toReply.getSubject().startsWith("RE: "))
      reply.setSubject(toReply.getSubject());
    else
      reply.setSubject("RE: " + toReply.getSubject());
    String s = "";
    for (int i = 0; i < 75; i++)
      s += "*";
    String intro = "\n\n" + s + "\n\nOn " + toReply.getDate().toString() + " " + toReply.getSender() + " wrote:\n\n";
    reply.setContent(intro + toReply.getContent());
    reply.setBelonging(Folder.WRITE);
    new Thread(new Refresh(Folder.WRITE, reply)).start();
  }

  private Email composeMail(Email reply) {

    if (subjectField.getText().isEmpty()) {
      model.infoNotification("[Email not sent] add a subject to the email");
      return null;
    }
    if (toField.getText().isEmpty()) {
      model.infoNotification("[Email not sent] add a recipient to the email");
      return null;
    }

    List<String> dests = getDestsList(toField.getText());

    for (String dest : dests) {
      if (!User.validateEmail(dest)) {
        model.infoNotification("[Email not sent] " + dest + " is not a valid email address");
        return null;
      }
      if (dest.equals(model.getUser().getEmail())) {
        model.infoNotification("You cannot send an email to you");
        return null;
      }
    }

    Email newMail = new Email();
    newMail.setSubject(subjectField.getText());
    newMail.setDests(dests);
    newMail.setSender(model.getUser().getEmail());
    newMail.setContent(emailAreaW.getText());
    newMail.setDate(Calendar.getInstance().getTime());
    if(reply != null)
      newMail.setReply(reply.getReply());
    else
      newMail.setReply(null);
    return newMail;
  }


  private List<String> getDestsList(String string) {

    String[] splitted = string.split("[ ,;-]+");

    for (String s : splitted) {
      s.trim();
    }
    return (new ArrayList<>(Arrays.asList(splitted)));
  }

  private String getDestsString(List<String> list) {
    String dest = "";
    if(list == null)
      return dest;
    return list.toString().replaceAll("[\\[\\]]", "");
  }

  public void initModel(ClientModel model) {
    if (this.model != null)
      throw new IllegalStateException("Model can only be initialized once");

    this.model = model;
    Thread update = new Thread(new Update());
    update.setDaemon(true);
    update.start();



    readBox.setVisible(true);
    writeBox.setVisible(false);
    serverUnreachable(false);

    lblUsername.setText(model.getUser().getEmail());
    lstEmails.itemsProperty().bind(model.emailListProperty());
    lstEmails.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) ->
            model.setCurrentMail(newSelection));

    new Thread(new Refresh(Folder.INBOX, null)).start();
    lstEmails.getSelectionModel().clearSelection();
    updateDetailView(null);

    model.currentMailProperty().addListener((obs, oldMail, newMail) -> {
      if (newMail == null ) {
        lstEmails.getSelectionModel().clearSelection();
        updateDetailView(null);
      } else {
        lstEmails.getSelectionModel().select(newMail);
        updateDetailView(newMail);

      }
    });
    lstEmails.setCellFactory(lv -> new ListCell<>() {
      @Override
      public void updateItem(Email email, boolean empty) {
        super.updateItem(email, empty);
        if (empty)
          setText("");
        else {
          if (email.getBelonging() != Folder.SENT) {
            setText(email.getSender() + "\t\t\t" + dateForm.format(email.getDate()) + "\n" + email.getSubject());
          } else
            setText(getDestsString(email.getDests()) + "\t\t\t" + dateForm.format(email.getDate()) + "\n" + email.getSubject());
        }
      }
    });

  }

  //Aggiorna la vista della mail corrente
  private void updateDetailView(Email email) {
    if (email != null) {
      if (!readMode) {
        fromLabelW.setText(model.getUser().getEmail());
        subjectField.setText(email.getSubject());
        subjectField.setEditable(false);
        if(email.getReply() != null) {
          toField.setText(getDestsString(email.getDests()));
          toField.setEditable(false);
        }
        else {
          toField.clear();
          toField.setEditable(true);
        }
        emailAreaW.setText(email.getContent());
      } else {
        replyButton.setDisable(false);
        replyAllButton.setDisable(false);
        forwardButton.setDisable(false);
        deleteButton.setDisable(false);
        subjectLabel.setText(email.getSubject());
        fromLabelR.setText(email.getSender());
        dateLabel.setText(dateForm.format(email.getDate()));
        emailAreaR.setText(email.getContent());
        toLabelR.setText(getDestsString(email.getDests()));
      }
    } else {
      if (!readMode) {
        subjectField.setEditable(true);
        toField.setEditable(true);
        fromLabelW.setText(model.getUser().getEmail());
        subjectField.clear();
        toField.clear();
        emailAreaW.clear();
      } else {
        replyButton.setDisable(true);
        replyAllButton.setDisable(true);
        forwardButton.setDisable(true);
        deleteButton.setDisable(true);
        subjectLabel.setText("");
        fromLabelR.setText("");
        dateLabel.setText("");
        emailAreaR.clear();
        toLabelR.setText("");
      }
    }
  }
  private synchronized void newEmailNotification(Email email) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("[" + model.getUser().getEmail() + "] "+ "New email");
      alert.setHeaderText("You have a new mail from: " + email.getSender() + "\nSubject: " + email.getSubject());
      ButtonType showButton = new ButtonType("Show", ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
      alert.getButtonTypes().setAll(showButton, cancelButton);

      if ( alert.showAndWait().get() == showButton)
        new Thread(new Refresh(Folder.INBOX, email)).start();
    });
  }
  private void serverUnreachable(boolean value) {
    Platform.runLater(() -> {
      errorImg.setVisible(value);
      errorLabel.setVisible(value);
    });
  }

  private class Reconnect implements Runnable{
    @Override
    public void run(){
      try {
        model.setConnection(new Connection("127.0.0.1", 8189));
        System.out.println("Connection re-established");
      } catch (IOException e){
        e.printStackTrace();
      }
    }
  }

  private class Update implements Runnable {

    @Override
    public void run() {

      while (true) {
        try {
          Thread.sleep(3000);
          keepUpdate();
        } catch (InterruptedException | IOException | ClassNotFoundException e) {
          serverUnreachable(true);
          new Thread(new Reconnect()).start();
        }
      }
    }

    private void keepUpdate() throws IOException, ClassNotFoundException, InterruptedException {

      Platform.runLater(() -> {
        errorImg.setVisible(false);
        errorLabel.setVisible(false);
      });

      LinkedList<Email> news = model.getNews();
      if (!(news.isEmpty())) {
        for (Email toNotify : news) {
          new Thread(new NotifyThread(toNotify)).start();

        }
      }
    }
  }

  private class Refresh implements Runnable {
    private Folder folder;
    private Email email;

    public Refresh(Folder folder, Email email) {
      this.folder = folder;
      this.email = email;
    }
    @Override
    public void run() {
      try {
        if (folder != null) {
          LinkedList<Email> mailbox = model.mailboxRequest(folder);
          Platform.runLater(() -> {
            model.setCurrentFolder(folder);
            model.setCurrentEmailList(mailbox);
            lblFolder.setText(folder.toString());
            if (folder != Folder.WRITE)
              readView(true);
            else
              readView(false);
            model.setCurrentMail(email);
          });
        }
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class SendThread implements Runnable{

    private Email toSend;

    public SendThread(Email toSend){
      this.toSend = toSend;
    }

    @Override
    public void run() {
      try {
        model.sendEmail(toSend);
        new Thread(new Refresh(Folder.SENT, null)).start();
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class DeleteThread implements Runnable{

    private Email toDelete;

    public DeleteThread(Email toDelete){
      this.toDelete = toDelete;
    }

    @Override
    public void run() {
      try {
        model.deleteEmail(toDelete);
        new Thread(new Refresh(Folder.BIN, null)).start();
      } catch (IOException | ClassNotFoundException  e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class NotifyThread implements Runnable{

    private Email toNotify;

    public NotifyThread(Email toNotify){
      this.toNotify = toNotify;
    }

    @Override
    public void run() {
      try {
        model.notifyReadEmail(toNotify);
        newEmailNotification(toNotify);
      } catch (IOException | ClassNotFoundException  e) {
        throw new RuntimeException(e);
      }
    }
  }
  private class LogOutThread implements Runnable{

    public LogOutThread() {}

    @Override
    public void run() {
      model.logoutRequest();
    }
  }
}
