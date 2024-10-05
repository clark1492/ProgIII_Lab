package it.unito.servermail.controller;

import it.unito.servermail.model.ServerModel;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.IOException;

public class ServerController {

  @FXML
  private TextArea logsArea;
  private ServerModel model;

  @FXML
  public void initialize() {

    if(this.model != null)
      throw new IllegalStateException();

    try {
      model = new ServerModel();
    } catch (IOException e) {
      e.printStackTrace();
    }

    model.logTextProperty().addListener((obs,oldString,newString) -> {
      if(newString != null && oldString != newString)
        appendLog(newString);
    });
    Thread server = new Thread(model);
    server.setDaemon(true);
    server.start();
  }

  private void appendLog(String log) {
    logsArea.appendText (log);
  }
}
