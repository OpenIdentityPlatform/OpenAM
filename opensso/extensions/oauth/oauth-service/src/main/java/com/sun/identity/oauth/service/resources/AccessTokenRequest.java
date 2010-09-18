/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.jersey.oauth.server.OAuthServerRequest;
import com.sun.jersey.api.core.HttpContext;
import com.sun.identity.oauth.service.persistence.AccessToken;
import com.sun.identity.oauth.service.persistence.Consumer;
import com.sun.identity.oauth.service.persistence.RequestToken;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.NoResultException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;
import com.sun.jersey.oauth.signature.OAuthSignature;
import com.sun.jersey.oauth.signature.OAuthSignatureException;
import com.sun.identity.oauth.service.util.UniqueRandomString;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

/**
 * REST Web Service
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */

@Path(PathDefs.AccessTokenRequestPath)
public class AccessTokenRequest {
    @Context
    private UriInfo context;

    /** Creates a new instance of ReqTokenRequestResource */
    public AccessTokenRequest() {
    }


    /**
     * POST method for creating a request for Rquest Token
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response postAccessTokenRequest(
            @Context HttpContext hc,
            @Context Request req,
            String content) {
        boolean sigIsOk = false;
        PersistenceService service = PersistenceService.getInstance();
        try {
            Consumer cons = null;
            service.beginTx();
            OAuthServerRequest request = new OAuthServerRequest(hc.getRequest());
            OAuthParameters params = new OAuthParameters();
            params.readRequest(request);
            String callbackURI;

            if (params.getToken() == null)
                throw new WebApplicationException(new Throwable("oauth_token MUST be present."), 400);
            RequestToken rt = getReqTokenByURI(RequestToken.class, params.getToken());
            if (rt == null)
                throw new WebApplicationException(new Throwable("Token invalid."));

            String conskey = params.getConsumerKey();
            if (conskey == null)
                throw new WebApplicationException(new Throwable("Consumer key is missing."), 400);

            cons = rt.getConsumerId();
            if (cons == null)
                throw new WebApplicationException(new Throwable("Consumer key invalid or service not registered"), 400);

            OAuthSecrets secrets = new OAuthSecrets().consumerSecret(cons.getConsSecret()).tokenSecret(rt.getReqtSecret());
            try {
                sigIsOk = OAuthSignature.verify(request, params, secrets);
            } catch (OAuthSignatureException ex) {
                Logger.getLogger(AccessTokenRequest.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (!sigIsOk)
                throw new WebApplicationException(new Throwable("Signature invalid."), 400);



            // We're good to go.
            AccessToken newtok = new AccessToken();
            newtok.setAcctOnetime((short)1);
            newtok.setAcctPpalid(rt.getReqtPpalid());

            String baseUri = context.getBaseUri().toString();
            if (baseUri.endsWith("/"))
                    baseUri = baseUri.substring(0, baseUri.length() - 1);
            URI loc = URI.create(baseUri + PathDefs.AccessTokensPath + "/" + new UniqueRandomString().getString());
            newtok.setAcctUri(loc.toString());
            newtok.setAcctSecret(new UniqueRandomString().getString());

            newtok.setConsumerId(rt.getConsumerId());
            // for now val = uri
            newtok.setAcctVal(newtok.getAcctUri());

            service.persistEntity(newtok);
            service.removeEntity(rt);
            service.commitTx();

            // Preparing the response.
            String resp = "oauth_token=" + newtok.getAcctVal() + "&oauth_token_secret=" + newtok.getAcctSecret();
            return Response.created(loc).entity(resp).type(MediaType.APPLICATION_FORM_URLENCODED).build();
        } finally {
            service.close();
        }
    }



    protected <T> T getConsumerByKey(Class<T> type, String consKey) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM "+type.getSimpleName()+" e where e.consKey = :key").setParameter("key", consKey).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    protected <T> T getReqTokenByURI(Class<T> type, String uri) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM " + type.getSimpleName() + " e where e.reqtUri = :uri").setParameter("uri", uri).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

}
