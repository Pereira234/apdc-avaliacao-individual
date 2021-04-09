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

import pt.unl.fct.di.apdc.firstwebapp.util.LogoutData;

@Path("/logout")
public class LogoutResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public LogoutResource() {
		//nada
	}
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogout(LogoutData data) {
		LOG.fine("Logout attempt by username: " + data.username);
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		
		Key userTokenKey = datastore.newKeyFactory().addAncestors(PathElement.of("User", data.username))
				.setKind("UserToken").newKey("token");
		
		Transaction txn = datastore.newTransaction();

		try {
			
			Entity user = txn.get(userKey);
			if(user == null) {
				LOG.warning("Failed logout attempt for username: " + data.username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("User: " + data.username + ", does not exist!").build();
			}

			Entity userToken = txn.get(userTokenKey);
			if(userToken == null) {
				LOG.warning("Failed logout attempt for username: " + data.username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("Token is invalid").build();
			}
			if(!userToken.getString("token_id").equals(data.token_id)) {
				LOG.warning("Failed logout attempt for username: " + data.username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("Token is invalid").build();
			}
			if(userToken.getLong("expirationData") < System.currentTimeMillis()) {
				LOG.warning("Failed logout attempt for username: " + data.username);
				txn.rollback();
				return Response.status(Status.FORBIDDEN).entity("Token is invalid").build();
			}

			Entity uToken = Entity.newBuilder(userTokenKey)
					.set("token_id", userToken.getString("token_id"))
					.set("user_role", userToken.getString("user_role"))
					.set("creationData", userToken.getLong("creationData"))
					.set("expirationData", System.currentTimeMillis())
					.build();

			txn.put(uToken);
			txn.commit();
			LOG.info("User " + data.username + " logged out successfully");
			return Response.ok("Fez Logout da sua sessao").build();
			
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
