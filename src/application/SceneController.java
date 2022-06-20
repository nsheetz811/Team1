package application;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

public class SceneController {

	private Stage stage;
	private Scene scene;
	private Parent root;
	private static Scanner kb = new Scanner(System.in);
	private static Socket client;
	private static ObjectOutputStream out;
	private static ObjectInputStream in;
	
	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
			Pattern.CASE_INSENSITIVE);
	Alert invalidEmail = new Alert(AlertType.NONE);
	Alert notMatchingPassword = new Alert(AlertType.NONE);
	Alert passwordtooShort = new Alert(AlertType.NONE);
	Alert invalidLoginAlert = new Alert(AlertType.NONE);
	Alert userExistsAlert = new Alert(AlertType.NONE);
	Alert userCreatedAlert = new Alert(AlertType.NONE);

	// LoginPageFXML
	@FXML
	TextField username = null;
	@FXML
	PasswordField password = null;

	// NewUserFXML
	@FXML
	TextField newuserEmail = null;
	@FXML
	PasswordField newuserPassword = null;
	@FXML
	PasswordField newuserConfirmPassword = null;

	// Logic to grab info off of the login page and then send it to the datebase
	public void submitLoginData(ActionEvent event) {
		String user_name;
		String tempPassword;

		// Will be sending these strings to the database to check for login
		user_name = username.getText();
		tempPassword = password.getText();

		// Check for valid email alert
		if (validate(user_name) && tempPassword.length() >= 6) {
			// Replace this with logic to check database for successful login
			// **********************
			try {
				sendData("getUser", new String[] {user_name, tempPassword});
				Boolean result = (Boolean) in.readObject();
				if (result)
					switchToScene3(event, user_name);
				else
					invalidLoginAlert();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
			// Send user_name to database
			// Send tempPassword to database

			// *********************************************************************************************
			System.out.println("Sending to database...: \n");
			System.out.println("Username: " + user_name + "\n" + "password: " + tempPassword);
		}
		if (!validate(user_name)) {
			// if the email entered is not valid, throw an alert and do not add to data base
			invalidEmailAlert();
			// if the password is too short
		}
		if (tempPassword.length() < 6) {
			passwordTooShortAlert();

		}
	}

	// Logic to submit new users to the database
	public void submitNewUserData() {

		String user_name;
		String tempPassword;
		String tempconfirmpassword;

		// These will be the strings we send to the database
		user_name = newuserEmail.getText();
		tempPassword = newuserPassword.getText();
		tempconfirmpassword = newuserConfirmPassword.getText();

		// if the passwords match, and it is a valid email, and the password is atleast 6 characters add to database
		if (tempPassword.equals(tempconfirmpassword) && validate(user_name) && tempPassword.length() >= 6) {

			String[] info = new String[] {user_name,tempPassword};
			try {
				sendData("addUser", info);
				Boolean result = (Boolean) in.readObject();
				if (!result) userExistsAlert();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}

			// print the new user
			System.out.println("Sucess! \n New user being added to database");
			System.out.println("" + "       Username: " + user_name + "\n" + "       password: " + tempPassword + "\n"
					+ "confirmpassword: " + tempconfirmpassword);
			//Alert
			userCreatedAlert();
		}
		///Alerts////////////
		
		// Throw alert if passwords arent the same
		if (!tempPassword.equals(tempconfirmpassword)) {
			notMatchingPasswordsAlert();
		}

		// Throw alert if invalid email
		if (!validate(user_name)) {
			invalidEmailAlert();

		}
		//Throw alert if password is less than 6 characters
		if(tempPassword.length() < 6) {
			passwordTooShortAlert();
		}

	}
	public void establishConnection()
	{
		try
		{
			client = new Socket("54.89.249.120", 6789);
			out = new ObjectOutputStream(client.getOutputStream());
			in = new ObjectInputStream(client.getInputStream());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void setConnection (ObjectOutputStream out, ObjectInputStream in) {
		this.out = out;
		this.in = in;
	}
	
	private void sendData(String command, Object obj) throws IOException
	{
		out.writeObject(new Request(command, obj));
	}


	// Logic to switch scenes//////////////////////////////////////////////////////////////////////////////////////////
	public void switchToScene1(ActionEvent event) throws IOException {
		root = FXMLLoader.load(getClass().getResource("LoginPage.fxml"));
		stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();

	}

	public void switchToScene2(ActionEvent event) throws IOException {
		root = FXMLLoader.load(getClass().getResource("CreateNewUser.fxml"));
		stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
	public void switchToScene3(ActionEvent event, String username) throws IOException {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("ShoppingCart.fxml"));
		root = loader.load();

		ShoppingCartController controller = loader.getController();
		controller.initializeCart(username, out, in);

		stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	

	// Email Verification Regex////////////////////////////////////////////////////////////////////////////////////////
	public static boolean validate(String emailStr) {
		Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
		return matcher.find();
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	
	//Alerts///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Password Too short alert
	public void passwordTooShortAlert() {
		passwordtooShort.setAlertType(AlertType.INFORMATION);
		passwordtooShort.setTitle("Password too short");
		passwordtooShort.setHeaderText("Password too short");
		passwordtooShort.setContentText("Password too short");
		passwordtooShort.show();
		System.out.println("Password too short");

	}

	// Invalid Email Alert
	public void invalidEmailAlert() {
		// if the email entered is not valid, throw an alert and do not add to data base
		invalidEmail.setAlertType(AlertType.INFORMATION);
		invalidEmail.setTitle("Not a valid email");
		invalidEmail.setHeaderText("Something went wrong");
		invalidEmail.setContentText("Please enter a valid email");
		invalidEmail.show();
		System.out.println("Invalid email");
	}

	// Not matching Password alert
	public void notMatchingPasswordsAlert() {
		notMatchingPassword.setAlertType(AlertType.INFORMATION);
		notMatchingPassword.setTitle("Passwords do not match");
		notMatchingPassword.setContentText("Something went wrong");
		notMatchingPassword.setHeaderText("Passwords do not match");
		notMatchingPassword.show();
		System.out.println("Passwords do not match");

	}

	public void invalidLoginAlert() {
		invalidLoginAlert.setAlertType(AlertType.INFORMATION);
		invalidLoginAlert.setTitle("Invalid Login");
		invalidLoginAlert.setContentText("Username and Password are invalid. Please try again.");
		invalidLoginAlert.setHeaderText("Invalid Login");
		invalidLoginAlert.show();
		System.out.println("Invalid Login");

	}

	public void userExistsAlert() {
		userExistsAlert.setAlertType(AlertType.INFORMATION);
		userExistsAlert.setTitle("User Exists");
		userExistsAlert.setContentText("A user with this email already exists");
		userExistsAlert.setHeaderText("User Already Exists");
		userExistsAlert.show();
		System.out.println("User Exists");
	}
	public void userCreatedAlert() {
		userExistsAlert.setAlertType(AlertType.INFORMATION);
		userExistsAlert.setTitle("New User Created!");
		userExistsAlert.setContentText("Please log in");
		userExistsAlert.setHeaderText("New User Created!");
		userExistsAlert.show();
		System.out.println("User Exists");
	}
}
