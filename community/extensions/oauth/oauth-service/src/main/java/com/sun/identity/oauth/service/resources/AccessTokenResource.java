/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.identity.oauth.service.persistence.AccessToken;
import java.net.URLEncoder;
import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */

@Path(PathDefs.AccessTokensPath + "/{id}")
public class AccessTokenResource {
    @Context
    private UriInfo context;

    /**
     * GET method for retrieving a specific Service Consumer instance
     * and obtaining corresponding metadata (consumer name, URI, secret).
     *
     * @param subject (@link int) to retrieve the principal's id. Expected
     * value is either 1 (yes) or 0 (no) (e.g <PRE>&subject=1</PRE>).
     * @param shared_secret (@link int) to retrieve the shared secret (same
     * value as subject parameter).
     *
     * @return an HTTP response with URL encoded value of the service metadata.
     */
    @GET
    //@Consumes(MediaType.TEXT_PLAIN)
    public Response getAccessToken(@QueryParam("subject") int sub,
            @QueryParam("shared_secret") int shsec) {
        PersistenceService service = PersistenceService.getInstance();
        try {
            String resp = null;
            String s = null;
            String p =  null;

            service.beginTx();
            String turi = context.getAbsolutePath().toString();
            AccessToken token = getAcctokenByUri(AccessToken.class, turi);
            if (token == null)
                return Response.noContent().build();
            if ((sub == 1) && (token.getAcctPpalid() != null)) {
                p = URLEncoder.encode(token.getAcctPpalid());
                resp = "subject=" + p;
            }
            if ((shsec == 1) && (token.getAcctSecret() != null)) {
                s = URLEncoder.encode(token.getAcctSecret());
                if (shsec == 1)
                    resp += "&";
                resp += "shared_secret=" + s;
            }

            service.close();
            return Response.ok(resp, MediaType.TEXT_PLAIN).build();
        } finally {
            service.close();
        }
    }


    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deleteAcctoken() {
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();
            String tokenuri = context.getAbsolutePath().toString();
            AccessToken token = getAcctokenByUri(AccessToken.class, tokenuri);
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

    protected <T> T getAcctokenByUri(Class<T> type, String tokenuri) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM " + type.getSimpleName()+" e where e.acctUri = :tokenuri").setParameter("tokenuri", tokenuri).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

}