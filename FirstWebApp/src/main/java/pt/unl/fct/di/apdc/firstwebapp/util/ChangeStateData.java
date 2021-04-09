package pt.unl.fct.di.apdc.firstwebapp.util;

public class ChangeStateData {

	public String username;
	public String token_id;
	public String target_username;
	public String state;
	
	public ChangeStateData() {
		
	}
	
	public ChangeStateData(String username, String token_id, String target_username, String state) {
		this.username = username;
		this.token_id = token_id;
		this.target_username = target_username;
		this.state = state;
	}
	
	
	
}
