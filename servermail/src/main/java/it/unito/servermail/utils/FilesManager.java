package it.unito.servermail.utils;

import it.unito.servermail.model.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;

public class FilesManager {

  public static final String FILES_PATH = "servermail/files/";
  public static final String SERVER_PATH = "servermail/files/server";
  public static final String USERS_FILE = "servermail/files/server/users.dat";
  public static final String ID_COUNTER_FILE = "servermail/files/server/id_counter.dat";
  public static final String INBOX_FILENAME = "/inbox.dat";
  public static final String SENT_FILENAME = "/sent.dat";
  public static final String BIN_FILENAME = "/bin.dat";

  public static void createFiles(User username) throws IOException {

    if (username == null)
      throw new IllegalArgumentException("[FilesManager.createFiles]: username to be created cannot be null");

    Files.createDirectories(Paths.get(FILES_PATH + username.getEmail()));

    File inbox = new File(FILES_PATH + username.getEmail() + INBOX_FILENAME);
    File sent = new File(FILES_PATH + username.getEmail() + SENT_FILENAME);
    File bin = new File(FILES_PATH + username.getEmail() + BIN_FILENAME);

    ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(inbox));
    outputStream.writeObject(new LinkedList<Email>());
    outputStream = new ObjectOutputStream(new FileOutputStream(sent));
    outputStream.writeObject(new LinkedList<Email>());
    outputStream = new ObjectOutputStream(new FileOutputStream(bin));
    outputStream.writeObject(new LinkedList<Email>());
    outputStream.close();
  }

  public static void serverFiles() throws IOException {

    Files.createDirectories(Paths.get(FILES_PATH));
    Files.createDirectories(Paths.get(SERVER_PATH));

    ArrayList<User> userList = new ArrayList<>();
    ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(USERS_FILE));
    outputStream.writeObject(userList);
    DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(ID_COUNTER_FILE));
    dataOutputStream.writeInt(0);
    outputStream.close();
    dataOutputStream.close();

  }

  public static boolean addUserToFile(User user) throws IllegalArgumentException, IOException, ClassNotFoundException {

    if (user == null )
      throw new IllegalArgumentException("[FilesManager.addUserToFile] : arguments cannot be null");

    ArrayList<User> accounts = getUserList();

    if(accounts.contains(user)){
      return false;
    }

    accounts.add(user);
    ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(USERS_FILE));
    outputStream.writeObject(accounts);
    outputStream.close();

    createFiles(user);
    return true;
  }



  public static ArrayList<User> getUserList() {

    ArrayList<User> userList = new ArrayList<>();

    try {
      ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(USERS_FILE));
      userList = (ArrayList<User>) inputStream.readObject();
      inputStream.close();
    } catch (IOException | ClassNotFoundException e) {
      System.out.println(e.getMessage());
    }

    return userList;
  }

  private static File getFile(String user, Folder folder) throws IllegalArgumentException{

    if (user == null || folder == null)
      throw new IllegalArgumentException("[FilesManager.getFile] : arguments cannot be null");

    String path = FILES_PATH + user;
    switch (folder) {
      case INBOX -> {
        path += INBOX_FILENAME;}
      case BIN -> {
        path += BIN_FILENAME;}
      case SENT -> {
        path += SENT_FILENAME;}
    }

    return new File(path);
  }

  public static LinkedList<Email> getMailBox(String user, Folder folder) throws IllegalArgumentException{

    if (user == null || folder == null)
      throw new IllegalArgumentException("[FilesManager.getMailBox] : arguments cannot be null");

    File mailBox = getFile(user, folder);
    LinkedList<Email> emailList = new LinkedList<>();

    try {
      ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(mailBox));
      emailList = (LinkedList<Email>) inputStream.readObject();
      inputStream.close();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    return emailList;
  }

  private static void updateMailBox(File mailBox, LinkedList<Email> updated) throws IllegalArgumentException{

    if(mailBox == null || updated == null)
      throw new IllegalArgumentException("[FilesManager.updateMailBox] : arguments cannot be null");

    try {
      ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(mailBox));
      out.writeObject(updated);
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void rmMailFromMailbox(Email toRemove, String email) throws IllegalArgumentException{

    if (toRemove == null || email == null )
      throw new IllegalArgumentException("[FilesManager.rmMailFromMailbox] : arguments cannot be null");

    File fileSource = new File(FILES_PATH + email + "/" + toRemove.getBelonging().toString() + ".dat");

    synchronized (fileSource) {

      LinkedList<Email> mailList = getMailBox(email, toRemove.getBelonging());

      int idToRemove = toRemove.getId();
      boolean removed = false;
      for (int i = 0; i < mailList.size() && !removed; i++) {
        if (mailList.get(i).getId() == idToRemove) {
          mailList.remove(i);
          removed = true;
        }
      }
      if(removed)
        updateMailBox(fileSource,mailList);
    }
  }

  public static void moveMail(Email toMove, Folder destination, String user)
          throws Exception {
    if (toMove == null || destination == null || user == null)
      throw new IllegalArgumentException("[FilesManager.moveMail] : arguments cannot be null");

    // rimuovo la mail dalla cartella sorgente
    rmMailFromMailbox(toMove,user);
    // la inserisco nella cartella di destinazione
    insMailToMailbox(toMove, destination, user, false);
  }

  public static void insMailToMailbox(Email toInsert, Folder folder, String user, boolean inc) throws IllegalArgumentException {

    if (toInsert == null || folder == null || user == null)
      throw new IllegalArgumentException("[FilesManager.insMailToMailbox] : arguments cannot be null");

    LinkedList<Email> mailBox;

    boolean reply = (toInsert.getReply()!= null);

    switch(folder){
      case BIN -> {

        toInsert.setBelonging(Folder.BIN);
        toInsert.setRead(true);
        File file = new File(FILES_PATH + user + BIN_FILENAME);

        synchronized (file){
          mailBox = getMailBox(user, Folder.BIN);
          mailBox.addFirst(toInsert);
          updateMailBox(file,mailBox);
        }
      }
      case INBOX -> {

        toInsert.setBelonging(Folder.INBOX);

        if (toInsert.getSender() == user) // aggiorno l'inbox del sender con la conversazione aggiornata
          toInsert.setRead(true);
        else
          toInsert.setRead(false);  // inserisco l'email nell'inbox dei dest

        if (reply) {
          toInsert.setId(toInsert.getReply().getId()); //setto l'email con lo stesso id della mail risposta

          File fileInbox = new File(FILES_PATH + user + INBOX_FILENAME);

          synchronized (fileInbox) {
            mailBox = getMailBox(user, Folder.INBOX);
            for (int i = 0; i < mailBox.size(); i++) {
              if (mailBox.get(i).getId() == toInsert.getId())
                mailBox.remove(i);
            }
            mailBox.addFirst(toInsert);
            updateMailBox(fileInbox, mailBox);
          }
        } else {
          toInsert.setId(getID());

          File fileInbox = new File(FILES_PATH + user + INBOX_FILENAME);

          synchronized (fileInbox) {
            mailBox = getMailBox(user, Folder.INBOX);
            mailBox.addFirst(toInsert);
            updateMailBox(fileInbox, mailBox);
          }
          if(inc)
            incrementID();
        }
      }

      case SENT -> {

        String sender = toInsert.getSender();

        if (sender == null)
          throw new IllegalArgumentException();

        File file = getFile(sender, Folder.SENT);

        synchronized (file) {
          mailBox = getMailBox(sender, Folder.SENT);
          toInsert.setBelonging(Folder.SENT);
          toInsert.setRead(true);
          if (reply) {
            for (int i = 0; i < mailBox.size(); i++) {
              if (mailBox.get(i).getId() == toInsert.getId())
                mailBox.remove(i);
            }
          }
          mailBox.addFirst(toInsert);
          updateMailBox(file,mailBox);
        }
      }
    }
  }

  public static boolean setMailRead(User user, Email toFlag) {

    if (toFlag == null || toFlag.getBelonging() != Folder.INBOX || user == null)
      throw new IllegalArgumentException();

    File inbox = getFile(user.getEmail(), Folder.INBOX);

    LinkedList<Email> mailBox;

    boolean found = false;

    synchronized (inbox) {
      mailBox = getMailBox(user.getEmail(), Folder.INBOX);
      for (Email email : mailBox) {
        if (email.getId() == toFlag.getId()) {
          email.setRead(true);
          found = true;
          updateMailBox(inbox,mailBox);
          break;
        }
      }
    }

    return found;
  }

  // ID Counter
  private static int getID() {

    File idCounter = new File(ID_COUNTER_FILE);
    int id = 0;
    synchronized (idCounter) {
      try {
        DataInputStream inputStream = new DataInputStream(new FileInputStream(ID_COUNTER_FILE));
        id = inputStream.readInt();
        inputStream.close();
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
      return id;
    }
  }

  private static void incrementID() {

    File idCounter = new File(ID_COUNTER_FILE);

    synchronized (idCounter) {
      try {
        DataInputStream inputStream = new DataInputStream(new FileInputStream(idCounter));
        int id = inputStream.readInt();
        DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(idCounter));
        outputStream.writeInt(id + 1);
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }
  }
}

