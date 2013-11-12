/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.identity.oauth.service.persistence.RequestToken;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */

@Path(PathDefs.createAuthorizationPath)
public class AuthorizationFactory {
    @Context
    private UriInfo context;


    /** Creates a new instance of AuthorizationFactory */
    public AuthorizationFactory() {
    }

    /**
     * GET method for obtaining user's consent
     * @param content representation for the resource
     * @return an HTTP form with content of the updated or created resource.
     */
    @GET
    @Consumes("application/xml")
    public Response createAuthorization(
            @QueryParam("oauth_token") String token,
            @QueryParam("oauth_callback") String cbk,
            @QueryParam("id") String uid) {
        if (token == null)
            throw new WebApplicationException(new Throwable("No OAuth token."));
        if (cbk == null)
            throw new WebApplicationException(new Throwable("No callback URI."));
        if (uid == null)
            throw new WebApplicationException(new Throwable("No User iD."));

        // From here, we're good to go.
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();

            RequestToken rt = getReqtokenByURI(RequestToken.class, token);
            if (rt == null)
                throw new WebApplicationException(new Throwable("Request token invalid."));
            rt.setReqtPpalid(uid);

            service.persistEntity(rt);
            service.commitTx();

            // Preparing the response.
            String resp = cbk;
            if (cbk.contains("?"))
                resp += "&" + token;
            else
                resp += "?" + token;
            URI respURI = new URI(resp);
            return Response.seeOther(respURI).build();
        } catch (URISyntaxException ex) {
            Logger.getLogger(AuthorizationFactory.class.getName()).log(Level.SEVERE, null, ex);
            return Response.serverError().build();
        } finally {
            service.close();
        }
    }



    protected <T> T getReqtokenByURI(Class<T> type, String uri) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM " + type.getSimpleName() + " e where e.reqtUri = :uri").setParameter("uri", uri).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

}