/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.view.Viewable;
import com.sun.identity.oauth.service.persistence.Consumer;
import com.sun.identity.oauth.service.persistence.RequestToken;
import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */
/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */

@Path(PathDefs.ResourceOwnerAuthorizationPath)
public class ResourceOwnerAuthorization {
    @Context
    private UriInfo context;


    /** Creates a new instance of ResourceOwnerAuthorization */
    public ResourceOwnerAuthorization() {
    }

    /**
     * GET method for obtaining user's consent
     * @param content representation for the resource
     * @return an HTTP form with content of the updated or created resource.
     */
    @GET
    @Consumes("application/xml")
    public Viewable getResourceOwnerAuthorization(
            @Context HttpContext hc,
            @QueryParam("oauth_callback") String cbk,
            @QueryParam("oauth_token") String token) {

        if (cbk == null) {
                throw new WebApplicationException(new Throwable("No callback provided."));
            }
            if (token == null) {
                throw new WebApplicationException(new Throwable("No token provided."));
            }
            PersistenceService service = PersistenceService.getInstance();
            RequestToken reqtk = getReqTokenByURI(RequestToken.class, token);
            if (reqtk == null) {
                throw new WebApplicationException(new Throwable("Request token invalid."));
            }
            Consumer cons = reqtk.getConsumerId();
            if (cons == null)
                throw new WebApplicationException(new Throwable("Could not find corresponding Consumer."));
            String svcname = cons.getConsName();

            String retour = PathDefs.ResourceOwnerAuthorizationNextPath;
            return new Viewable(retour, null);
    }



    protected <T> T getReqTokenByURI(Class<T> type, String uri) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM " + type.getSimpleName() + " e where e.reqtUri = :uri").setParameter("uri", uri).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
}