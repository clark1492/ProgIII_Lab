package it.unito.servermail.handler;

import it.unito.servermail.utils.FilesManager;
import it.unito.servermail.model.*;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class HandleClient implements Runnable {

  private final ServerModel server;
  private final Socket incoming;
  private User client;
  private ObjectInputStream inStream;
  private ObjectOutputStream outStream;
  private boolean exit;

  public HandleClient(ServerModel server, Socket incoming) throws IllegalArgumentException, IOException, ClassNotFoundException {

    if (server == null || incoming == null)
      throw new IllegalArgumentException();

    this.server = server;
    this.incoming = incoming;
    exit = false;

    openStreams();
  }

  private void openStreams() throws IOException {
    outStream = new ObjectOutputStream(incoming.getOutputStream());
    outStream.flush();
    inStream = new ObjectInputStream(incoming.getInputStream());
  }

  private void closeStreams(){
    try {
      if(inStream != null) {
        inStream.close();
      }

      if(outStream != null) {
        outStream.close();
      }

      if(incoming != null){
        incoming.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void sendResponse(String resp) throws IllegalArgumentException, IOException {

    if (resp == null)
      throw new IllegalArgumentException();

    outStream.writeObject(resp);
  }

  private void loginData() throws IOException, ClassNotFoundException {

    Object data = inStream.readObject();
    if (!(data.getClass() == String.class))
      throw new InvalidObjectException("[Invalid Object]: You need an user to login\n");

    String email = (String) data;
    if (!server.userExist(email)) {
      sendResponse("USER_NOT_EXIST");
      server.setLogText("Authentication problem: user " + email + " does not exist\n");
      return;
    }
    String password = (String) inStream.readObject();

    if (!server.passwordIsCorrect(email,password)) {
      sendResponse("UNCORRECT_PASSSWORD");
      server.setLogText("Authentication problem: password is not correct\n");
      return;
    }

    sendResponse("SERVER_SUCCESS");
    client = server.getUser(email);
    outStream.writeObject(client);
    server.setLogText("["+ client.getEmail() +"] " + " authenticated\n");
  }

  private void keepUpdate() throws IOException, ClassNotFoundException {

    Object data = inStream.readObject();

    if (!(data.getClass() == User.class))
      throw new InvalidObjectException("[Invalid Object]: You need an user to login\n");

    if(client == null) {
      client = (User) data;
      server.setLogText("["+ client.getEmail() +"] " + " connection re-established\n");
    }
    LinkedList<Email> news = new LinkedList<>();
    LinkedList<Email> inbox = FilesManager.getMailBox(client.getEmail(), Folder.INBOX);
    for (Email email : inbox) {
      if (!(email.isRead())) {
        news.add(email);
      }
    }
    outStream.writeObject(news);
  }

  private void signInRequest() throws IOException, ClassNotFoundException {

    Object email = inStream.readObject();
    Object password = inStream.readObject();


    if ((email.getClass() != String.class) || (password.getClass() != String.class)) {
      throw new InvalidObjectException("[Invalid Object]: You need an user to login\n");
    }

    if (server.userExist((String)email)) {
      sendResponse("USER_ALREADY_REGISTERED");
      server.setLogText("User " + email + " already registered\n");
      return;
    }
    if (server.addUser(new User((String)email,(String)password))) {
      //sendResponse(Protocol.SERVER_SUCCESS);
      sendResponse("SERVER_SUCCESS");
      server.setLogText("User " + email + " registered\n");
      return;
    }

    //sendResponse(Protocol.SERVER_UNSUCCESS);
    sendResponse("SERVER_UNSUCCESS");
    server.setLogText("Sign Up problem: Client " + email + " not registered\n");
  }

  private void mailBoxRequest() throws IOException, ClassNotFoundException {

    LinkedList<Email> mailList = null;

    Object data = inStream.readObject();

    if (!(data instanceof Folder))
      throw new InvalidObjectException("[Invalid Object]: You need to specify the folder\n");

    Folder fold = (Folder) data ;
    mailList = FilesManager.getMailBox(client.getEmail(), fold);

    outStream.writeObject(mailList);
  }

  private void sendEmailRequest() throws IOException, ClassNotFoundException {

    Object data = inStream.readObject();

    if (!(data instanceof Email))
      throw new InvalidObjectException("[Invalid Object]: You need to specify the email\n");

    Email toSend = (Email) data;

    boolean reply = (toSend.getReply() != null);
    List<String> listDests = toSend.getDests();
    String sender = toSend.getSender();

    for (String user : listDests)
      if (!server.userExist(user)) {
        sendResponse("USER_NOT_EXIST");
        server.setLogText("[" + client.getEmail() + "] : Impossible to sent email, user " + user + " does not exist\n");
        return;
      }

    boolean multiDest = (listDests.size() > 1);

    for(int i = 0; i < listDests.size(); i++){

      if( (i != (listDests.size() - 1) && (multiDest)))
        FilesManager.insMailToMailbox(toSend, Folder.INBOX, listDests.get(i),false);
      else
        FilesManager.insMailToMailbox(toSend, Folder.INBOX, listDests.get(i),true);

      if(reply) //se è una risposta modifico pure la mail nella cartella SENT dei destinatari
        FilesManager.insMailToMailbox(toSend,Folder.SENT,listDests.get(i),false);

    }

    FilesManager.insMailToMailbox(toSend,Folder.SENT,sender,false);

    if (reply)//se è una risposta modifico pure la mail nella cartella INBOX del mittente
      FilesManager.insMailToMailbox(toSend,Folder.INBOX,sender,false);


    sendResponse("SERVER_SUCCESS");
    server.setLogText("[" + client.getEmail() + "] : Mail " + toSend.getId() + " sent correctly to " + listDests + "\n");

  }

  private void deleteEmailRequest() throws Exception {
    Object data = inStream.readObject();

    if (!(data instanceof Email))
      throw new InvalidObjectException("[Invalid Object]: You need to specify the email\n");

    Email toRemove = (Email) data;


    if(toRemove.getBelonging() == Folder.BIN) {
      FilesManager.rmMailFromMailbox(toRemove,client.getEmail());
      sendResponse("SERVER_SUCCESS");
      server.setLogText("[" + client.getEmail() + "] : Mail " + toRemove.getId() + " removed correctly\n");
    }
    else {
      FilesManager.moveMail(toRemove,Folder.BIN,client.getEmail());
      sendResponse("MOVED_BIN");
      server.setLogText("[" + client.getEmail() + "] : Mail " + toRemove.getId() + " moved to bin\n");
    }

  }

  private void readFlagRequest() throws IOException, ClassNotFoundException {

    Object data = inStream.readObject();

    if (!(data instanceof Email))
      throw new InvalidObjectException("[Invalid Object]: You need to specify the email\n");

    Email mail = (Email) data;

    FilesManager.setMailRead(client, mail);
    sendResponse("SERVER_SUCCESS");
    server.setLogText("[" + client.getEmail() + "] : Mail " + mail.getId() + " set read-flag\n");
  }


  @Override
  public void run() {

    while (!exit) {
      Object read ;
      try {
        String intro = (client != null)? ("["+ client.getEmail() +"] ") : ("");
        System.out.println("Waiting for request...");
        if ((read = inStream.readObject()) != null) {
          if (read instanceof String) {

            switch ((String) read) {

              case "LOGIN_REQUEST" -> {
                System.out.println("LOGIN_REQUEST");
                loginData();
              }
              case "SIGNIN_REQUEST" -> {

                System.out.println("SIGNIN_REQUEST");
                signInRequest();
              }
              case "MAILBOX_REQUEST" -> {
                System.out.println( intro + "MAILBOX_REQUEST");
                mailBoxRequest();
              }
              case "LOG_OUT" -> {

                System.out.println( intro + "LOG_OUT");
                exit = true;
                server.setLogText( intro + " logged out\n");
              }
              case "SEND_EMAIL" -> {
                System.out.println( intro + "SEND_EMAIL");
                sendEmailRequest();
              }
              case "DEL_EMAIL" -> {
                System.out.println( intro + "DEL_EMAIL");
                deleteEmailRequest();
              }
              case "READ_FLAG" -> {
                System.out.println( intro + "READ_FLAG");
                readFlagRequest();
              }

              case "KEEP_UPDATE" -> {
                System.out.println( intro + "KEEP_UPDATE");
                keepUpdate();
              }
            }
          } else
            throw new InvalidObjectException("Must be instance of Protocol\n");
        } else
          throw new IllegalArgumentException("Argument should be not null\n");
      } catch (IOException e) {
        e.printStackTrace();
        exit = true;
        server.setLogText("Client " + client.getEmail() + " connection lost\n");
      } catch (ClassNotFoundException e) {
        System.out.println("Illegal class sent");
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    closeStreams();
  }
}
