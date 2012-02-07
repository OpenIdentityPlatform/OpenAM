/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.identity.oauth.service.persistence.RequestToken;
import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */

@Path(PathDefs.RequestTokensPath + "/{id}")
public class RequestTokenResource {
    @Context
    private UriInfo context;

    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deleteReqtoken() {
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();
            String tokenuri = context.getAbsolutePath().toString();
            RequestToken token = getReqtokenByUri(RequestToken.class, tokenuri);
            if (token == null)
                return Response.status(401).build();
            service.removeEntity(token);
            service.commitTx();
            service.close();
            return Response.ok().build();
        } finally {
            service.close();
        }
    }

    protected <T> T getReqtokenByUri(Class<T> type, String uri) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM " + type.getSimpleName()+" e where e.reqtUri = :uri").setParameter("uri", uri).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

}