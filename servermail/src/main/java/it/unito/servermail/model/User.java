package it.unito.servermail.model;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class User implements Serializable {

  private static final long serialVersionUID = 456L;
  private static final String EMAIL_PATTERN = "^\\w+@[a-zA-Z_]+?\\.[a-zA-Z]{2,3}$";
  private String email;
  private String password;

  public User(String email,String password) {
    this.email = email;
    this.password = password;
  }
  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public static boolean validateEmail(String email){

    Pattern emailPattern = Pattern.compile(EMAIL_PATTERN, Pattern.CASE_INSENSITIVE);
    Matcher emailMatcher = emailPattern.matcher(email);
    return emailMatcher.find();
  }

  @Override
  public String toString() {
    return "User{" +
            "email='" + email + '\'' +
            ", password='" + password + '\'' +
            '}';
  }
}
