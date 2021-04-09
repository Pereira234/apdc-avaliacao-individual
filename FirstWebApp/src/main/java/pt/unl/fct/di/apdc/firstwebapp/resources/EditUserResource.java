package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.firstwebapp.util.EditUserData;

@Path("/edit/user")
public class EditUserResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public EditUserResource() {
		//nada
	}
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doEdit(EditUserData data) {
		LOG.fine("Edit attempt by username: " + data.username);

		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(data.target_username);		

		Key userTokenKey = datastore.newKeyFactory().addAncestors(PathElement.of("User", data.username))
				.setKind("UserToken").newKey("token");

		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			if(user == null) {
				LOG.warning("Failed user edit attempt by username: " + data.username + " , for username: " + data.target_username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("User: " + data.username + ", does not exist!").build();
			}

			Entity targetUser = txn.get(targetUserKey);
			if(targetUser == null) {
				LOG.warning("Failed user edit attempt by username: " + data.username + " , for username: " + data.target_username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("User: " + data.target_username + ", does not exist!").build();
			}
			
			Entity userToken = txn.get(userTokenKey);
			if(userToken == null || !userToken.getString("token_id").equals(data.token_id) || userToken.getLong("expirationData") < System.currentTimeMillis()) {
				LOG.warning("Failed user edit attempt by username: " + data.username + " , for username: " + data.target_username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("Token is invalid").build();
			}
			
			/*
			if(user.getString("role").equals(targetUser.getString("role"))) {
				if (!user.getKey().getName().equals(targetUser.getKey().getName())) {
					LOG.warning("Failed user edit attempt for username: " + data.username + " , by username: " + data.target_username);
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
				}
				
			}
			*/
			Entity uTargetUser = Entity.newBuilder(targetUserKey, targetUser)
					.set("profile", data.profile)
					.set("telephone", data.telephone)
					.set("mobile", data.mobile)
					.set("address", data.address)
					.set("addicional_address", data.addicional_address)
					.set("local", data.local)
					.build();
			
			txn.put(uTargetUser);
			txn.commit();
			LOG.info("User " + data.target_username + " updated by " + data.username);
			
			return Response.ok("Utilizador atualizado com sucesso").build();
		}
		catch (Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		
	}
	
}
