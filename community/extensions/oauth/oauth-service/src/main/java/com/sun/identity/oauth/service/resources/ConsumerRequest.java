/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

import com.sun.identity.oauth.service.persistence.Consumer;
import com.sun.identity.oauth.service.util.UniqueRandomString;
import com.sun.jersey.oauth.signature.HMAC_SHA1;
import com.sun.jersey.oauth.signature.RSA_SHA1;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST Web Service
 *
 * Endpoint for Service Consumer Registration
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */
@Path(PathDefs.ConsumerRegistrationPath)
public class ConsumerRequest {
    @Context
    private UriInfo context;

    /** Creates a new instance of ConsumersRegistration */
    public ConsumerRequest() {
    }



    /**
     * POST method for registering a Service Consumer
     * and obtaining corresponding consumer key & secret.
     *
     * @param content {@link String} containing the service consumer's description.
     * This description takes the form of name=value pairs separated by &.
     * The following parameters are supported:
     * <OL>
     * <LI>name - the service consumer's name.</LI>
     * <LI>icon - the service consumer's URI for its icon (MUST be unique).</LI>
     * <LI>service - the service consumer's URI for its service</LI>
     * <LI>rsapublickey - (optional) the RSA public key of the Service Consumer.</LI>
     * </OL>
     * <p>
     *
     * Example of string:
     * <pre>
     *  name=Service XYZ&icon=http://www.example.com/icon.jpg&service=http://www.example.com
     * </pre>
     *
     *
     * @return an HTTP response with content of the created resource.
     * The location URI is set to the newly created OAuth consumer key.
     * The body of the response is of the form:
     * <pre>
     * consumer_key=http://serviceprovider/0123456762121
     * consumer_secret=12345633
     * </pre>
     * Both values are URL encoded.
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response postConsumerRegistrations(MultivaluedMap<String, String> formParams) {
        PersistenceService service = PersistenceService.getInstance();
        try {
            service.beginTx();

            // We might want to check whether this Consumer already exists,
            // but for now we let consumers register ad nauseum...
            Consumer cons = new Consumer();
            String sigmeth = null;
            String tmpsecret = null;
            Boolean keyed = false;

            Set<String> pnames = formParams.keySet();
            Iterator iter = pnames.iterator();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                String val = formParams.getFirst(key);
                if (key.equalsIgnoreCase("name"))
                    cons.setConsName(URLDecoder.decode(val));
                else
                    if (key.equalsIgnoreCase("signature_method"))
                        sigmeth = URLDecoder.decode(val);
                    else
                        if (key.equalsIgnoreCase("secret"))
                            tmpsecret = URLDecoder.decode(val);
                        else
                            if (key.equalsIgnoreCase("cons_key")) {
                                keyed = true;
                                cons.setConsKey(URLDecoder.decode(val));
                                Consumer tmpcons = getConsumerByKey(Consumer.class, URLDecoder.decode(val));
                                if (tmpcons != null) {
                                    service.removeEntity(tmpcons);
                                }
                            }
            }

            if (tmpsecret != null) {
                if (sigmeth.equalsIgnoreCase(RSA_SHA1.NAME)) {
                    cons.setConsRsakey(tmpsecret);
                    cons.setConsSecret(new UniqueRandomString().getString());
                }
                else
                    if (sigmeth.equalsIgnoreCase(HMAC_SHA1.NAME))
                        cons.setConsSecret(tmpsecret);
            } else {
                cons.setConsSecret(new UniqueRandomString().getString());
            }
            
            //URI consKeyURI = URI.create(endOfConsKey.toString());
            if (!keyed) {
                String baseUri = context.getBaseUri().toString();
                if (baseUri.endsWith("/"))
                        baseUri = baseUri.substring(0, baseUri.length() - 1);
                URI loc = URI.create(baseUri + PathDefs.ConsumersPath + "/" + new UniqueRandomString().getString());
                String consKey =  loc.toString();
                cons.setConsKey(consKey);
            }
            service.persistEntity(cons);
            service.commitTx();

            String resp = "consumer_key=" + URLEncoder.encode(cons.getConsKey()) + "&consumer_secret=" + URLEncoder.encode(cons.getConsSecret());
            return Response.created(URI.create(cons.getConsKey())).entity(resp).type(MediaType.APPLICATION_FORM_URLENCODED).build();
        } finally {
            service.close();
        }
    }

    protected <T> T getConsumerByKey(Class<T> type, String uri) {
        try {
            return (T) PersistenceService.getInstance().createQuery("SELECT e FROM " + type.getSimpleName()+" e where e.consKey = :uri").setParameter("uri", uri).getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

}