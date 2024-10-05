package it.unito.servermail.model;

public enum Folder {
  INBOX,
  BIN,
  SENT,
  WRITE;


  @Override
  public String toString() {
    switch (this){
      case SENT -> {return "Sent";}
      case BIN -> {return  "Bin";}
      case WRITE -> {return "Write";}
      case INBOX -> {return "Inbox";}
      default -> {return null;}
    }
  }
}


