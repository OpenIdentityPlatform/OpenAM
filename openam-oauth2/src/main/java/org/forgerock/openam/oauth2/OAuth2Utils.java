/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import org.forgerock.oauth2.core.OAuth2Constants;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class OAuth2Utils {

    public static final Logger DEBUG = LoggerFactory.getLogger("OAuth2Provider");

    public static final String SCOPE_DELIMITER = " ";

    /**
     * Gets the deployment URI of the OAuth2 authorization server
     * @param request the request to get the deployment uri of
     * @return
     */
    public static String getDeploymentURL(HttpServletRequest request){
        String uri = request.getRequestURI();
        String deploymentURI = uri;
        int firstSlashIndex = uri.indexOf("/");
        int secondSlashIndex = uri.indexOf("/", firstSlashIndex + 1);
        if (secondSlashIndex != -1) {
            deploymentURI = uri.substring(0, secondSlashIndex);
        }
        StringBuffer sb = new StringBuffer(100);
        sb.append(request.getScheme()).append("://")
                .append(request.getServerName()).append(":")
                .append(request.getServerPort())
                .append(deploymentURI);
        return sb.toString();
    }

    /**
     * Determines if a string is empty. Empty is defined as null or empty
     * string.
     * <p/>
     *
     * <pre>
     *  OAuth2Utils.isEmpty(null)               = true
     *  OAuth2Utils.isEmpty(&quot;&quot;)       = true
     *  OAuth2Utils.isEmpty(&quot; &quot;)      = false
     *  OAuth2Utils.isEmpty(&quot;bob&quot;)    = false
     *  OAuth2Utils.isEmpty(&quot; bob &quot;)  = false
     * </pre>
     *
     * @param val
     *            string to evaluate as empty.
     * @return true if the string is empty else false.
     */
    public static boolean isEmpty(String val) {
        return (val == null) ? true : "".equals(val) ? true : false;
    }

    /**
     * <pre>
     *      OAuth2Utils.isBlank(null)                = true
     *      OAuth2Utils.isBlank(&quot;&quot;)        = true
     *      OAuth2Utils.isBlank(&quot; &quot;)       = true
     *      OAuth2Utils.isBlank(&quot;bob&quot;)     = false
     *      OAuth2Utils.isBlank(&quot;  bob  &quot;) = false
     * </pre>
     */
    public static boolean isBlank(String val) {
        return (val == null) ? true : isEmpty(val.trim());
    }

    public static boolean isNotBlank(String val) {
        return !isBlank(val);
    }

    public static String join(Iterable<? extends Object> iterable, String delimiter) {
        if (null != iterable) {
            Iterator<? extends Object> iterator = iterable.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            StringBuilder buffer = new StringBuilder();
            buffer.append(iterator.next());
            String d = null != delimiter ? delimiter : SCOPE_DELIMITER;
            while (iterator.hasNext()) {
                buffer.append(d).append(iterator.next());
            }
            return buffer.toString();
        }
        return null;
    }

    public static Set<String> split(String string, String delimiter) {
        if (isNotBlank(string)) {
            StringTokenizer tokenizer =
                    new StringTokenizer(string, null != delimiter ? delimiter : SCOPE_DELIMITER);
            Set<String> result = new TreeSet<String>();
            while (tokenizer.hasMoreTokens()) {
                result.add(tokenizer.nextToken());
            }
            return Collections.unmodifiableSet(result);
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Get the realm from the Attributes first and then look for the realm in
     * the request.
     * <p/>
     * Example: Restlet Template populates the realm into the
     * {@link Request#attributes} {@code TemplateRoute route =
     * router.attach("/oauth2/ realm}/authorize", (Restlet)authorization);}
     * <p/>
     * Example: Custom code fetches it from the query, the body or more secure
     * from the User Session
     *
     * @param request
     * @return
     */
    public static String getRealm(Request request) {
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);
        return getRealm(httpRequest);
    }

    public static String getRealm(HttpServletRequest request) {
        Object realm = request.getParameter(OAuth2Constants.Custom.REALM);
        if (realm instanceof String) {
            return (String) realm;
        }
        return "/";
    }

    public static String getLocale(Request request) {
        Object locale = request.getAttributes().get(OAuth2Constants.Custom.LOCALE);
        if (locale instanceof String) {
            return (String) locale;
        }
        return getRequestParameter(request, OAuth2Constants.Custom.LOCALE, String.class);
    }

    public static <T> T getRequestParameter(Request request, String parameterName, Class<T> clazz) {
        Object value = getRequestParameters(request).get(parameterName);
        if (null != value && clazz.isAssignableFrom(value.getClass())) {
            return clazz.cast(value);
        }
        return null;
    }

    /**
     * It copies the given parameters only once!!!
     * way the CallResolver can use it and the FreeMarker can list and add all
     * into the generated form
     *
     * @param request
     *            incoming request object
     * @return The modifiable attributes map.
     */
    public static Map<String, Object> getRequestParameters(Request request) {
        Map<String, String> parameters = null;
        if (request.getAttributes().get(OAuth2Constants.Params.class.getName()) instanceof Map == false) {
            parameters = getParameters(request);
            if (null != parameters) {
                // Copy the parameter for CallResolver
                request.getAttributes().putAll(parameters);
            }
            // Avoid reprocess the request next time.
            request.getAttributes().put(OAuth2Constants.Params.class.getName(), parameters);
        }
        return request.getAttributes();
    }

    /**
     * Get the parameters from the request.
     * <p/>
     * If the method is GET then the parameters are fetched from the query If
     * the request has no body/payload then the parameters are fetched from the
     * query If the content type is "application/x-www-form-urlencoded" then the
     * parameters are fetched from the body
     *
     * @param request
     *            incoming request object
     * @return null if the request does not contains any parameter
     */
    public static Map<String, String> getParameters(Request request) {
        if (Method.GET.equals(request.getMethod())
                || request.getEntity() instanceof EmptyRepresentation) {
            return OAuth2Utils.ParameterLocation.HTTP_QUERY.getParameters(request);
        } else {
            return OAuth2Utils.ParameterLocation.HTTP_QUERY.getParameters(request);
        }
    }

    public static enum ParameterLocation {
        HTTP_QUERY, HTTP_HEADER, HTTP_FRAGMENT, HTTP_BODY;

        @SuppressWarnings(value = "unchecked")
        public Map<String, String> getParameters(Request request) {
            Map<String, String> result = null;
            switch (this) {
                case HTTP_FRAGMENT:
                    if (request.getReferrerRef() == null || request.getReferrerRef().getFragment() == null){
                        return null;
                    }
                    return new Form(request.getReferrerRef().getFragment()).getValuesMap();
                case HTTP_HEADER:
                    if (null != request.getChallengeResponse()
                            && !request.getChallengeResponse().getParameters().isEmpty()) {
                        return new Form(request.getChallengeResponse().getParameters()).getValuesMap();
                    }
                    return null;
                case HTTP_QUERY:
                    // Merge the parameterd from query and body
                    result = request.getResourceRef().getQueryAsForm().getValuesMap();
                case HTTP_BODY:
                    if (null == result) {
                        result = new LinkedHashMap<String, String>();
                    }
                    if (null != request.getEntity()) {
                        if (MediaType.APPLICATION_WWW_FORM.equals(request.getEntity().getMediaType())) {
                            Form form = new Form(request.getEntity());
                            // restore the entity body
                            request.setEntity(form.getWebRepresentation());
                            result.putAll(form.getValuesMap());
                        } else if (MediaType.APPLICATION_JSON
                                .equals(request.getEntity().getMediaType())) {
                            JacksonRepresentation<Map> representation =
                                    new JacksonRepresentation<Map>(request.getEntity(), Map.class);
                            try {
                                result.putAll(representation.getObject());
                            } catch (IOException e) {
                                throw new ResourceException(e);
                            }
                            request.setEntity(representation);
                        }
                    }
                    return result;
                default:
                    return null;
            }
        }

        /**
         *
         * @param context
         * @return
         */
        public Redirector getRedirector(Context context, OAuthProblemException exception) {
            /*
             * 3.1.2.4. Invalid Endpoint
             *
             * If an authorization request fails validation due to a missing,
             * invalid, or mismatching redirection URI, the authorization server
             * SHOULD inform the resource owner of the error, and MUST NOT
             * automatically redirect the user-agent to the invalid redirection
             * URI.
             */
            if (null != exception.getRedirectUri()) {
                Reference cb = new Reference(exception.getRedirectUri());
                switch (this) {
                    case HTTP_FRAGMENT: {
                        // Redirect URI can not contain Fragment so we can set it
                        cb.setFragment(exception.getErrorForm().getQueryString());
                        break;
                    }
                    case HTTP_QUERY: {
                        cb.addQueryParameters(exception.getErrorForm());
                        break;
                    }
                    default:
                        return null;
                }
                return new Redirector(context, cb.toString(), Redirector.MODE_CLIENT_FOUND);
            }
            return null;
        }
    }
}
