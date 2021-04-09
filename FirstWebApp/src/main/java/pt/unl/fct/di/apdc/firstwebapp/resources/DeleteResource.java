package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;

@Path("/delete")
public class DeleteResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public DeleteResource() {
		//nada
	}
	
	@DELETE
	@Path("/{username}/{token_id}/{target_username}")
	public Response doDelete(@PathParam("username") String username, @PathParam("token_id") String token_id,
			@PathParam("target_username") String target_username) {
		
		LOG.fine("Delete attempt by user: " + username + " target user : " + target_username);
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
		Key targetUserKey = datastore.newKeyFactory().setKind("User").newKey(target_username);		

		Key userTokenKey = datastore.newKeyFactory().addAncestors(PathElement.of("User", username))
				.setKind("UserToken").newKey("token");

		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			if(user == null) {
				LOG.warning("Failed user edit attempt by username: " + username + " , for username: " + target_username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("User: " + username + ", does not exist!").build();
			}

			Entity targetUser = txn.get(targetUserKey);
			if(targetUser == null) {
				LOG.warning("Failed user edit attempt by username: " + username + " , for username: " + target_username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("User: " + target_username + ", does not exist!").build();
			}
			
			Entity userToken = txn.get(userTokenKey);
			if(userToken == null || !userToken.getString("token_id").equals(token_id) || userToken.getLong("expirationData") < System.currentTimeMillis()) {
				LOG.warning("Failed user edit attempt by username: " + username + " , for username: " + target_username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("Token is invalid!").build();
			}
			
			//verificacao se o user tem o role necessario
			String userRole = user.getString("role");
			String targetRole = targetUser.getString("role");
			
			if(userRole.equals(targetRole)) {
				if (!user.getKey().getName().equals(targetUser.getKey().getName())) {
					LOG.warning("Failed user delete attempt by username: " + username + " , for username: " + target_username);
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
				}
				
			}
			
			if(targetRole.equals("GBO")) {
				if (userRole.equals("USER")) {
					LOG.warning("Failed user delete attempt by username: " + username + " , for username: " + target_username);
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
				}
			}

			if(targetRole.equals("GA")) {
				if (userRole.equals("USER") || userRole.equals("GBO") ) {
					LOG.warning("Failed user delete attempt by username: " + username + " , for username: " + target_username);
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
				}
			}

			if(targetRole.equals("SU")) {
				if (userRole.equals("USER") || userRole.equals("GBO") || userRole.equals("GA") ) {
					LOG.warning("Failed user delete attempt by username: " + username + " , for username: " + target_username);
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity("You dont have the right permissions").build();
				}
			}
			
			txn.delete(targetUserKey);
			txn.commit();
			LOG.info("User " + target_username + " deleted by " + username);
			return Response.ok().entity("Utilizador eliminado com sucesso").build();
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
