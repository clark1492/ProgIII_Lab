module it.unito.servermail {
  requires javafx.controls;
  requires javafx.fxml;


  opens it.unito.servermail.application to javafx.fxml;
  exports it.unito.servermail.application;

  opens it.unito.servermail.controller to javafx.fxml;
  exports it.unito.servermail.controller;

  opens it.unito.servermail.model to javafx.fxml;
  exports it.unito.servermail.model;

  opens it.unito.servermail.utils to javafx.fxml;
  exports it.unito.servermail.utils;
}