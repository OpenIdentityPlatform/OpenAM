/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.identity.oauth.service.persistence.RequestToken;
import javax.persistence.NoResultException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 * @author Paul C. Bryan <pbryan@sun.com>
 */
@Path(PathDefs.NoBrowserAuthorizationPath)
public class NoBrowserAuthorization {

    private Client client = Client.create();
    
    // FIXME: make this configurable
    private WebResource authenticateResource =
     client.resource("http://localhost:8080/entitlement/identity/authenticate");

    /** Creates a new instance of AuthorizationFactory */
    public NoBrowserAuthorization() {
    }

    /**
     * GET method to authenticate & obtain user's consent.
     * This endpoint does not use callback and does not rely on
     * browser-based authorization but rather submits the credentials
     * to a predefined OpenSSO endpoint.
     *
     * @param username (@String) is the user name to authenticate at the OpenSSO
     * instance
     * @param password (@String) is the user's password
     * @param reqtoken (@String) is the request token to authorize
     * @return 200 in case of success, 403 if authentications fails, 400 otherwise.
     */
    @GET
    public Response NoBrowserAuthorization(
    @QueryParam("username") String username,
    @QueryParam("password") String password,
    @QueryParam("request_token") String requestToken) {

        if (username == null || password == null || requestToken == null) {
            throw new WebApplicationException(new Throwable("Request invalid."));
        }

        MultivaluedMap params = new MultivaluedMapImpl();
        params.add("username", username);
        params.add("password", password);
        
        String response;

        try {
            response = authenticateResource.queryParams(params).get(String.class);
        }
        catch (UniformInterfaceException uie) {
            return Response.status(403).build();
        }

        // ensure response is in expected format
        if (!response.startsWith("token.id=")) {
            return Response.status(400).build();
        }

        // FIXME: get fully-qualified subject universal id from opensso
        String subject = "id=" + username + ",ou=user,dc=opensso,dc=java,dc=net";

        PersistenceService service = PersistenceService.getInstance();

        try {
            service.beginTx();
            RequestToken rt = getReqtokenByURI(RequestToken.class, requestToken);
            if (rt == null) {
                throw new WebApplicationException(new Throwable("Request token invalid."));
            }
            rt.setReqtPpalid(subject);
            service.persistEntity(rt);
            service.commitTx();
            return Response.ok().build();
        }
        finally {
            service.close();
        }
    }

    protected <T> T getReqtokenByURI(Class<T> type, String uri) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM " + type.getSimpleName() + " e where e.reqtUri = :uri").setParameter("uri", uri).getSingleResult();
        }
        catch (NoResultException ex) {
            return null;
        }
    }

}
