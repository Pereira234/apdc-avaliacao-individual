package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public RegisterResource() {
		//nada
	}
	
	@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegistrationV1(RegisterData data) {
		LOG.fine("Registration attempt by user: " + data.username);
		
		if(!data.validData()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Entity user = Entity.newBuilder(userKey)
				.set("password", DigestUtils.sha512Hex(data.password))
				.set("email", data.email)
				.set("role", "USER")
				.set("created_at", Timestamp.now())
				.build();
		
		datastore.put(user);
		LOG.info("User registered " + data.username );
		
		return Response.ok("{}").build();
	}

	@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegistrationV2(RegisterData data) {
		LOG.fine("Registration attempt by user: " + data.username);
		
		if(!data.validData()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		
		Transaction txn = datastore.newTransaction();
		
		try {
			Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
			Entity user = txn.get(userKey);
			if(user != null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists!").build();
			}
			else {
				user = Entity.newBuilder(userKey)
						.set("password", DigestUtils.sha512Hex(data.password))
						.set("email", data.email)
						.set("role", "USER")
						.set("state", "ENABLED")
						.set("created_at", Timestamp.now())
						.build();
				txn.add(user);
				LOG.info("User registered " + data.username );
				txn.commit();
			}
			
		}
		finally {
			if(txn.isActive()) {
				txn.rollback();
			}
		}
		
		
		
		
		return Response.ok("{}").build();
	}
	
}
