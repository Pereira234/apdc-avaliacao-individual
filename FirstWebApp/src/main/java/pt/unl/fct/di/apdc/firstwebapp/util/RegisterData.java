package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterData {

	public String username;
	public String email;
	public String password;
	public String password_confirm;
	
	private static final String EMAIL_REGEX = "^(.+)@(.+)$";
	
	public RegisterData() {
		//nada
	}
	
	public RegisterData(String username, String email, String password, String password_confirm) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.password_confirm = password_confirm;
	}
	
	public boolean validData() {
		return username != null &&
				password != null &&
				password.length() > 5 &&
				password.equals(password_confirm) &&
				email != null &&
				isValidEmail(email);
	}
	
	private boolean isValidEmail(String email) {
		Pattern pattern = Pattern.compile(EMAIL_REGEX);
		Matcher matcher = pattern.matcher(email);
		
		return matcher.matches();
	}
	
}
