/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Main.java,v 1.3 2009/06/11 06:02:37 superpat7 Exp $
 */

package c1demoharness;

import java.io.StringReader;
import java.io.StringWriter;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.oauth.signature.OAuthParameters;
import com.sun.jersey.oauth.signature.OAuthSecrets;
import com.sun.jersey.oauth.client.OAuthClientFilter;

public class Main
{
    private static final String TOKEN_SERVICE = "http://localhost:8080/TokenService/resources/oauth/v1/";
    private static final String CONSUMER_NAME = "javafx";
    private static final String CONSUMER_KEY = TOKEN_SERVICE + "consumer/" + CONSUMER_NAME;
    private static final String CONSUMER_SECRET = CONSUMER_NAME + "_secret";
    private static final String SIGNATURE_METHOD = "HMAC-SHA1";

    private static final Client client = Client.create();
    private static final OAuthParameters params = new OAuthParameters();
    private static final OAuthSecrets secrets = new OAuthSecrets();
    private static final OAuthClientFilter filter = new OAuthClientFilter(client.getProviders(), params, secrets);

    public static void main(String[] args) {
        if ( args.length < 2 ) {
            System.err.println("Usage: c1demoharness username password [protected_resource ...]");
        }

        newConsumer();
        getRequestToken();
        authorizeToken(args[0], args[1]);
        getAccessToken();

        client.addFilter(filter);

        for ( int i = 2; i < args.length; i++ ) {
            getResource(args[i]);
        }
    }

    private static void newConsumer()
    {
        System.out.println("Registering the OAuth consumer\n");

        WebResource resource = client.resource(TOKEN_SERVICE + "consumer_registration");

        MultivaluedMapImpl form = new MultivaluedMapImpl();
        form.add("cons_key", CONSUMER_KEY);
        form.add("secret", CONSUMER_SECRET);
        form.add("name", CONSUMER_NAME);
        form.add("signature_method", "HMAC-SHA1");

        MultivaluedMap response = POST(resource, form);

        System.out.println(response+"\n");
    }

    private static void getRequestToken() {
        System.out.println("Getting an OAuth request token\n");

        params.consumerKey(CONSUMER_KEY).signatureMethod(SIGNATURE_METHOD);
        secrets.consumerSecret(CONSUMER_SECRET);

        WebResource resource = client.resource(TOKEN_SERVICE + "get_request_token");

        resource.addFilter(filter);

        MultivaluedMap<String, String> response = POST(resource, new MultivaluedMapImpl());

        params.token(required(response.getFirst("oauth_token")));

        secrets.tokenSecret(required(response.getFirst("oauth_token_secret")));

        System.out.println(response+"\n");
    }

    // follow the detour
    private static void authorizeToken(String username, String password) {
        System.out.println("Authenticating to OAuth token service\n");

        WebResource resource = client.resource(TOKEN_SERVICE + "NoBrowserAuthorization");

        MultivaluedMapImpl query = new MultivaluedMapImpl();
        query.add("username", username);
        query.add("password", password);
        query.add("request_token", params.getToken());

        String s = resource.queryParams(query).get(String.class);

        System.out.println("Authenticated ok\n");
    }

    private static void getAccessToken() {
        System.out.println("Getting an OAuth access token\n");

        WebResource resource = client.resource(TOKEN_SERVICE + "get_access_token");

        resource.addFilter(filter);

        MultivaluedMap<String, String> response = POST(resource, new MultivaluedMapImpl());
        params.token(required(response.getFirst("oauth_token")));

        secrets.tokenSecret(required(response.getFirst("oauth_token_secret")));

        System.out.println(response+"\n");
    }

    private static void getResource(String protectedResource) {
        System.out.println("GETting resource: "+protectedResource+"\n");

        WebResource resource = client.resource(protectedResource);

        try {
            String response = resource.get(String.class);

            System.out.println(prettyPrint(response));
        } catch ( UniformInterfaceException uie ) {
            System.err.println("Server returned " +
                uie.getResponse().getStatus() + " " +
                uie.getResponse().getClientResponseStatus().getReasonPhrase() +
                "\n" );
        }
    }

    @SuppressWarnings("unchecked")
    private static MultivaluedMap<String, String> POST(WebResource resource, MultivaluedMap data) {
        return resource.type("application/x-www-form-urlencoded").post(MultivaluedMap.class, data);
    }

    private static String required(String value) {
        if (value == null || value.length() == 0) { throw new IllegalStateException("required!"); }
        return value;
    }

    public static String prettyPrint(String inString) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute("indent-number", new Integer(4));
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult result = new StreamResult(new StringWriter());
            StreamSource source = new StreamSource(new StringReader(inString));
            transformer.transform(source, result);
            String xmlString = result.getWriter().toString();
            return xmlString;
        } catch (TransformerException ex) {
            ex.printStackTrace();
        }
        return null;
    }
 }
