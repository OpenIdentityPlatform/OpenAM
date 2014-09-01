/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.jersey.oauth.server.OAuthServerRequest;
import com.sun.jersey.api.core.HttpContext;
import com.sun.identity.oauth.service.persistence.Consumer;
import com.sun.identity.oauth.service.persistence.RequestToken;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;
import com.sun.jersey.oauth.signature.OAuthSignature;
import com.sun.jersey.oauth.signature.OAuthSignatureException;
import com.sun.identity.oauth.service.util.UniqueRandomString;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.NoResultException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */

@Path(PathDefs.RequestTokenRequestPath)
public class RequestTokenRequest {
    @Context
    private UriInfo context;
    private Consumer cons = null;

    /** Creates a new instance of ReqtokenRequest */
    public RequestTokenRequest() {
    }


    /**
     * POST method for creating a request for a Request Token
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/x-www-form-urlencoded")
    public Response postReqTokenRequest(@Context HttpContext hc, String content) {
        boolean sigIsOk = false;
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();
            OAuthServerRequest request = new OAuthServerRequest(hc.getRequest());
            OAuthParameters params = new OAuthParameters();
            params.readRequest(request);

            String tok = params.getToken();
            if ((tok != null) && (!tok.contentEquals("")))
                throw new WebApplicationException(new Throwable("oauth_token MUST not be present."), 400);

            String conskey = params.getConsumerKey();
            if (conskey == null)
                throw new WebApplicationException(new Throwable("Consumer key is missing."), 400);

            cons = getConsumerByKey(Consumer.class, conskey);
            if (cons == null)
                throw new WebApplicationException(new Throwable("Consumer key invalid or service not registered"), 400);
            OAuthSecrets secrets = new OAuthSecrets().consumerSecret(cons.getConsSecret()).tokenSecret("");

            try {
                sigIsOk = OAuthSignature.verify(request, params, secrets);
            } catch (OAuthSignatureException ex) {
                Logger.getLogger(RequestTokenRequest.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (!sigIsOk)
                throw new WebApplicationException(new Throwable("Signature invalid."), 400);

            // We're good to go.
            RequestToken rt = new RequestToken();

            rt.setConsumerId(cons);
            String baseUri = context.getBaseUri().toString();
            if (baseUri.endsWith("/"))
                    baseUri = baseUri.substring(0, baseUri.length() - 1);
            URI loc = URI.create(baseUri + PathDefs.RequestTokensPath +"/" + new UniqueRandomString().getString());
            rt.setReqtUri(loc.toString());
            rt.setReqtSecret(new UniqueRandomString().getString());
            // Same value for now
            rt.setReqtVal(loc.toString());

            service.persistEntity(this.cons);
            service.persistEntity(rt);
            service.commitTx();

            String resp = "oauth_token=" + rt.getReqtVal() + "&oauth_token_secret=" + rt.getReqtSecret();
            return Response.created(loc).entity(resp).type(MediaType.APPLICATION_FORM_URLENCODED).build();
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
