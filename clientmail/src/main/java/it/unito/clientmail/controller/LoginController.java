package it.unito.clientmail.controller;

import it.unito.clientmail.model.LoginModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
  // loginPane
  @FXML
  private AnchorPane loginPane;
  @FXML
  private Label messageLabel;
  @FXML
  private TextField emailField;
  @FXML
  private PasswordField passwordField;

  // signUpPane
  @FXML
  private AnchorPane signInPane;
  @FXML
  private TextField newEmailField;
  @FXML
  private PasswordField newPasswordField;
  @FXML
  private PasswordField newPassConf;
  @FXML
  private ImageView attentionImg;

  private LoginModel model;


  @FXML
  protected void loginRequest() {
    new Thread(new LoginThread()).start();
  }

  @FXML
  protected void newUserOnAction() {
    emailField.clear();
    passwordField.clear();
    messageLabel.setText("");
    loginPane.setVisible(false);
    signInPane.setVisible(true);
  }

  @FXML
  protected void signInOnAction(){
    new Thread(new SignInThread()).start();
  }
  @FXML
  protected void backOnAction(){
    signInPane.setVisible(false);
    loginPane.setVisible(true);
    messageLabel.setText("");
    newEmailField.clear();
    newPasswordField.clear();
    newPassConf.clear();
  }
  private void noService(){
    if(loginPane.isVisible())
      loginPane.setVisible(false);
    if(signInPane.isVisible())
      signInPane.setVisible(false);
    messageLabel.setText("SERVER UNREACHABLE");
    attentionImg.setVisible(true);
  }

  private void onService(){
    messageLabel.setText("");
    attentionImg.setVisible(false);
    loginPane.setVisible(true);
    signInPane.setVisible(false);
  }

  private void errorNotification() {
    noService();
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText("Server unreacheable");
    ButtonType updateButton = new ButtonType("Update");

    alert.getButtonTypes().setAll(updateButton);
    Optional<ButtonType> result =  alert.showAndWait();

    if(result.isPresent() &&  result.get() == updateButton) {
      try {
        model = new LoginModel();
        model.messageProperty().bindBidirectional(messageLabel.textProperty());
        onService();
      } catch (IOException | ClassNotFoundException e) {
        errorNotification();
      }
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    if (this.model != null)
      throw new IllegalStateException("Model can only be initialized once");

    try {
      model = new LoginModel();
      model.messageProperty().bindBidirectional(messageLabel.textProperty());
      onService();

    } catch (IOException | ClassNotFoundException e) {
      errorNotification();
      System.out.println(e.getMessage());
    }
  }

  private class LoginThread implements Runnable{

    public LoginThread() {}

    @Override
    public void run() {
      Platform.runLater(() -> {
        try {
          model.loginRequest(emailField.getText(),passwordField.getText());
        } catch (IOException | ClassNotFoundException e) {
          errorNotification();
          System.out.println(e.getMessage());
        }
      });
    }
  }

  private class SignInThread implements Runnable{

    public SignInThread() {}

    @Override
    public void run() {
      Platform.runLater(() -> {
        String email = newEmailField.getText();
        String password = newPasswordField.getText();
        String confermation = newPassConf.getText();
        try {
          model.signUpRequest(email, password, confermation);
          newEmailField.clear();
          newPasswordField.clear();
          newPassConf.clear();
        } catch (IOException | ClassNotFoundException e) {
          errorNotification();
          System.out.println(e.getMessage());
        }
      });
    }
  }
}