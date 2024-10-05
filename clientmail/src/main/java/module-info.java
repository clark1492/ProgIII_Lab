module it.unito.clientmail {
  requires javafx.controls;
  requires javafx.fxml;
  requires it.unito.servermail;


  opens it.unito.clientmail.application to javafx.fxml;
  exports it.unito.clientmail.application;

  opens it.unito.clientmail.controller to javafx.fxml;
  exports it.unito.clientmail.controller;

  opens it.unito.clientmail.model to javafx.fxml;
  exports it.unito.clientmail.model;

}