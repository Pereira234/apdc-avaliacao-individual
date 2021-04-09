package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
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

import pt.unl.fct.di.apdc.firstwebapp.util.ChangeStateData;


@Path("/change/state")
public class ChangeStateResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public ChangeStateResource() {
		//nada
	}
	
	@PUT
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doStateChange(ChangeStateData data) {
		
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
			
			if(data.state == null) {
				LOG.warning("Failed user edit attempt by username: " + data.username + " , for username: " + data.target_username);
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
			}
			
			//ver se o user tem permissao correta
			String userRole = user.getString("role");
			String targetRole = targetUser.getString("role");
			
			if(userRole.equals(targetRole)) {
				LOG.warning("Failed user delete attempt by username: " + data.username + " , for username: " + data.target_username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
			}
			
			if(targetRole.equals("GBO")) {
				if (userRole.equals("USER")) {
					LOG.warning("Failed user delete attempt by username: " + data.username + " , for username: " + data.target_username);
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
				}
			}

			if(targetRole.equals("GA")) {
				if (userRole.equals("USER") || userRole.equals("GBO") ) {
					LOG.warning("Failed user delete attempt by username: " + data.username + " , for username: " + data.target_username);
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
				}
			}

			if(targetRole.equals("SU") || userRole.equals("USER")) {
				LOG.warning("Failed user delete attempt by username: " + data.username + " , for username: " + data.target_username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
			}
			
			Entity uTargetUser = Entity.newBuilder(targetUserKey, targetUser)
					.set("state", data.state)
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
