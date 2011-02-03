/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.identity.oauth.service.persistence.Consumer;
import com.sun.jersey.oauth.signature.HMAC_SHA1;
import com.sun.jersey.oauth.signature.RSA_SHA1;
import java.net.URLEncoder;
import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Service Consumer resource handling.
 *
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */

@Path(PathDefs.ConsumersPath + "/{cid}")
public class ConsumerResource {
    @Context
    private UriInfo context;

    /**
     * GET method for retrieving a specific Service Consumer instance
     * and obtaining corresponding metadata (consumer name, URI, secret).
     *
     * @param signature_method {@link String} to choose the signature algorithm
     * of interest (e.g. <PRE>?signature_method=RSA-SHA1</PRE> will return
     * the RSA public key of the service consumer).
     *
     * @return an HTTP response with URL encoded value of the service metadata.
     */
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    public Response getRegistration(@PathParam("cid") String consID,
            @QueryParam("signature_method") String sigmethod) {
        PersistenceService service = PersistenceService.getInstance();
        try {
            String name =null;
            String icon = null;
            service.beginTx();
            String ckey = context.getAbsolutePath().toString();
            Consumer cons = getConsumerByKey(Consumer.class, ckey);
            if (cons == null)
                return Response.noContent().build();

            String cs=null;
            if (sigmethod != null) {
                if (sigmethod.equalsIgnoreCase(RSA_SHA1.NAME))
                    cs = URLEncoder.encode(cons.getConsRsakey());
                else
                    if (sigmethod.equalsIgnoreCase(HMAC_SHA1.NAME))
                        cs = URLEncoder.encode(cons.getConsSecret());
            }
            if (cons.getConsName() != null)
                name = URLEncoder.encode(cons.getConsName());

            String resp = "cons_key=" + URLEncoder.encode(ckey);
            if (name != null)
                resp += "&name=" + name;
            if (cs != null)
                resp += "&secret=" + cs;

            service.close();
            return Response.ok(resp, MediaType.TEXT_PLAIN).build();
        } finally {
            service.close();
        }
    }

    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deleteRegistration() {
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();
            String consKey = context.getAbsolutePath().toString();
            Consumer cons = getConsumerByKey(Consumer.class, consKey);
            if (cons == null)
                return Response.status(401).build();
            service.removeEntity(cons);
            service.commitTx();
            service.close();
            return Response.ok().build();
        } finally {
            service.close();
        }
    }

    protected <T> T getConsumerByKey(Class<T> type, String key) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM " + type.getSimpleName()+" e where e.consKey = :key").setParameter("key", key).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

}
