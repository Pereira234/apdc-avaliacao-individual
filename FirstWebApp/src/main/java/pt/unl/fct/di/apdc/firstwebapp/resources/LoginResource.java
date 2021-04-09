package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	private final Gson g = new Gson();
	
	Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	public LoginResource() {
		//nada
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLogin(LoginData data) {
		LOG.fine("Login attempt by user: " + data.username);
		if (data.username.equals("pedro") && data.password.equals("secret")) {
			AuthToken at = new AuthToken(data.username);
			return Response.ok(g.toJson(at)).build();
		}
		return Response.status(Status.FORBIDDEN).entity("Incorrect username or password.").build();
	}
	
	
	@POST
	@Path("/final")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doLoginFinal(LoginData data, @Context HttpServletRequest request, @Context HttpHeaders headers) {
		LOG.fine("Login attempt by user: " + data.username);
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Key ctrsKey = datastore.newKeyFactory().addAncestors(PathElement.of("User", data.username))
				.setKind("UserStats").newKey("counters");
		
		Key logKey = datastore.allocateId(
				datastore.newKeyFactory()
				.addAncestors(PathElement.of("User", data.username))
				.setKind("UserLog")
				.newKey()
				);
		
		Key userToken = datastore.newKeyFactory().addAncestors(PathElement.of("User", data.username))
				.setKind("UserToken").newKey("token");
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Entity user = txn.get(userKey);
			if(user == null) {
				LOG.warning("Failed login attempt for username: " + data.username);
				txn.rollback();
				LOG.fine("Login attempt by user: " + data.username);
				return Response.status(Status.FORBIDDEN).entity("User: " + data.username + ", does not exist!").build();
			}
			
			Entity stats = txn.get(ctrsKey);
			if(stats == null) {
				stats = Entity.newBuilder(ctrsKey)
				.set("user_stats_logins", 0L)
				.set("user_stats_failed", 0L)
				.set("first_login_at", Timestamp.now())
				.set("last_login_at", Timestamp.now())
				.build();
			}
			
			String hashedPWD = user.getString("password");
			 
			  if(hashedPWD.equals(DigestUtils.sha512Hex(data.password))) {
				//pass correta
				//construir logs
				
				Entity log = Entity.newBuilder(logKey)
						.set("user_login_ip", request.getRemoteAddr())
						.set("user_login_host", request.getRemoteHost())
						.set("user_login_latlon",
								StringValue.newBuilder(headers.getHeaderString("X-AppEngine-CityLatLong"))
								.setExcludeFromIndexes(true).build()
								)
						.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
						.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
						.set("user_login_time", Timestamp.now())
						.build();
				
				
				Entity ustats = Entity.newBuilder(ctrsKey)
						.set("user_stats_logins", 1L + stats.getLong("user_stats_logins"))
						.set("user_stats_failed", stats.getLong("user_stats_failed"))
						.set("first_login_at", stats.getTimestamp("first_login_at"))
						.set("last_login_at", Timestamp.now())
						.build();
				
				AuthToken token = new AuthToken(user);
				
				Entity tokenEntity = Entity.newBuilder(userToken)
						.set("token_id", token.tokenID)
						.set("user_role", token.user_role)
						.set("creationData", token.creationData)
						.set("expirationData", token.expirationData)
						.build();
				
				
				txn.put(log, ustats, tokenEntity);
				txn.commit();
//				AuthToken token = new AuthToken(data.username);
				LOG.info("User " + data.username + " logged in successfully");
				return Response.ok(g.toJson(token)).build();
			}
			else {
				//pass incorreta
				Entity ustats = Entity.newBuilder(ctrsKey)
						.set("user_stats_logins", stats.getLong("user_stats_logins"))
						.set("user_stats_failed", 1L + stats.getLong("user_stats_failed"))
						.set("first_login_at", stats.getTimestamp("first_login_at"))
						.set("last_login_at", stats.getTimestamp("last_login_at"))
						.set("last_attempt_at", Timestamp.now())
						.build();
				
				txn.put(ustats);
				txn.commit();
				
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.FORBIDDEN).build();
			}
			  
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
	
	@POST
    @Path("/simple")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doLoginSimple(LoginData data) {
        LOG.fine("Login attempt by user: " + data.username);
        
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
        Entity user = datastore.get(userKey);
        if(user!=null) {
            String hashedPwd = user.getString("password");
            if(hashedPwd.equals(DigestUtils.sha512Hex(data.password))) {
                AuthToken token = new AuthToken(data.username);
                LOG.info("User " + data.username + "logged in sucessfully.");
                return Response.ok(g.toJson(token)).build();
            }
            else {
                LOG.warning("Wrong password.");
                return Response.status(Status.FORBIDDEN).build();
            }
        }
        else {
            LOG.warning("Failed to login.");
            return Response.status(Status.FORBIDDEN).build();
        }
    }


	@GET
	@Path("/{username}")
	public Response checkUsernameAvailable(@PathParam("username") String username) {
		if(!username.trim().equals("pedro")) {
			return Response.ok().entity(g.toJson(false)).build();
		} else {
			return Response.ok().entity(g.toJson(true)).build();
		}
	}

}
