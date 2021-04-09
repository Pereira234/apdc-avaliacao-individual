package pt.unl.fct.di.apdc.firstwebapp.util;

public class EditUserData {

	public String username;
	public String token_id;
	public String target_username;
	public String profile;
	public String telephone;
	public String mobile;
	public String address;
	public String addicional_address;
	public String local;
	
	public EditUserData() {
		//nada
	}
	
	public EditUserData(String username, String token_id, String target_username, String profile, String telephone,
			String mobile, String address, String addicional_address, String local) {
		this.username = username;
		this.token_id = token_id;
		this.target_username = target_username;
		this.profile = profile;
		this.telephone = telephone;
		this.mobile = mobile;
		this.address = address;
		this.addicional_address = addicional_address;
		this.local = local;
	}
	
}
