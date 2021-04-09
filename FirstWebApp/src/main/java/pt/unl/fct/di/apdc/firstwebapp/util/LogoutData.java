package pt.unl.fct.di.apdc.firstwebapp.util;

public class LogoutData {

	public String username;
	public String token_id;
	
	public LogoutData() {
		//nada
	}
	
	public LogoutData(String username, String token_id) {
		this.username = username;
		this.token_id = token_id;
	}
	
}
