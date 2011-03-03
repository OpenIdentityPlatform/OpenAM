/* The contents of this file are subject to the terms
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
 * $Id: CookieFilter.java,v 1.4 2009/10/18 18:41:27 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.filter;

import com.sun.identity.proxy.handler.Filter;
import com.sun.identity.proxy.handler.HandlerException;
import com.sun.identity.proxy.http.Exchange;
import com.sun.identity.proxy.http.Request;
import com.sun.identity.proxy.http.Response;
import com.sun.identity.proxy.http.Session;
import com.sun.identity.proxy.util.CIStringSet;
import com.sun.identity.proxy.util.StringUtil;
import java.io.IOException;
import java.net.CookiePolicy;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

/**
 * Filters exchanges by suppressing, relaying and managing cookies. The names
 * of filtered cookies are stored in one of three action set variables:
 * <tt>suppressed</tt>, <tt>relayed</tt> and <tt>managed</tt>. If a cookie is
 * not found in any of the action sets, then a default action is selected.
 * <p>
 * The default action is controlled by setting the <tt>defaultAction</tt>
 * variable. The default action at initialization is to manage all cookies.
 * In the event a cookie appears in more than one action set, then it will
 * be selected in order of precedence: managed, suppressed, relayed.
 * <p>
 * Managed cookies are intercepted by the cookie filter itself and stored in
 * the request {@link Session} object. The default <tt>policy</tt> is to
 * accept all incoming cookies, but can be changed to others as appropriate.
 *
 * @author Paul C. Bryan
 */
public class CookieFilter extends Filter
{
    /** Action to be performed for a cookie. */
    public enum Action {
        /** Intercept and manage the cookie within the proxy. */ MANAGE,
        /** Remove the cookie from request and response. */ SUPPRESS,
        /** Relay the cookie between remote client and remote host. */ RELAY
    }

    /** Splits string using comma delimiter, outside of quotes. */
    private static final Pattern DELIM_COMMA = Pattern.compile(",(?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))");
    
    /** Splits string using equals sign delimiter, outside of quotes. */
    private static final Pattern DELIM_EQUALS = Pattern.compile("=(?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))");

    /** Splits string using semicolon delimiter, outside of quotes. */
    private static final Pattern DELIM_SEMICOLON = Pattern.compile(";(?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))");

    /** Response headers to parse. */
    private static final String[] RESPONSE_HEADERS = { "Set-Cookie", "Set-Cookie2" };

    /** Action to perform for cookies that do not match an action set. Default: manage. */
    public Action defaultAction = Action.MANAGE;

    /** The policy for managed cookies. Default: accept all cookies. */
    public CookiePolicy policy = CookiePolicy.ACCEPT_ALL;

    /** Action set for cookies to be suppressed. */
    public final CIStringSet suppressed = new CIStringSet();

    /** Action set for cookies to be relayed. */
    public final CIStringSet relayed = new CIStringSet();
    
    /** Action set for cookies that filter should intercept and manage. */
    public final CIStringSet managed = new CIStringSet();

    /**
     * Creates a new cookie filter.
     */
    public CookieFilter() {
    }

    /**
     * Filters the exchange by suppressing, relaying and managing cookies.
     */
    @Override
    public void handle(Exchange exchange) throws HandlerException, IOException {
        URI resolved = exchange.request.resolveHostURI(); // resolve to client-supplied host header
        CookieManager manager = getManager(exchange.request.session); // session cookie jar
        suppress(exchange.request); // remove cookies that are suppressed or managed
        exchange.request.headers.putAll(manager.get(resolved, exchange.request.headers)); // add managed cookies
        next.handle(exchange); // pass exchange to next handler in chain
        manager.put(resolved, exchange.response.headers); // manage cookie headers in response
        suppress(exchange.response); // remove cookies that are suppressed or managed
    }

    /**
     * Computes what action to perform for the specified cookie name.
     *
     * @param name the name of the cookie to compute action for.
     * @return the computed action to perform for the given cookie.
     */
    private Action action(String name) {
        if (managed.contains(name)) {
            return Action.MANAGE;
        }
        else if (suppressed.contains(name)) {
            return Action.SUPPRESS;
        }
        else if (relayed.contains(name)) {
            return Action.RELAY;
        }
        else {
            return defaultAction;
        }
    }

    /**
     * Returns the cookie manager for the session, creating one if it does not
     * already exist.
     *
     * @param session the session that contains the cookie manager.
     * @return the retrieved (or created) cookie manager.
     */
    private CookieManager getManager(Session session) {
        CookieManager manager = null;
        synchronized(session) { // prevent race for the cookie manager
            manager = (CookieManager)session.get(CookieManager.class.getName());
            if (manager == null) {
                manager = new CookieManager(null, new CookiePolicy() {
                    public boolean shouldAccept(URI uri, HttpCookie cookie) {
                        return (action(cookie.getName()) == Action.MANAGE && policy.shouldAccept(uri, cookie));
                    }
                });
                session.put(CookieManager.class.getName(), manager);
            }
        }
        return manager;
    }

    /**
     * Removes the cookies from the request that are suppressed or managed.
     *
     * @param request the request to suppress the cookies in.
     */
    private void suppress(Request request) {
        List<String> headers = request.headers.get("Cookie");
        if (headers != null) {
            for (ListIterator<String> hi = headers.listIterator(); hi.hasNext();) {
                String header = hi.next();
                ArrayList<String> parts = new ArrayList<String>(Arrays.asList(DELIM_SEMICOLON.split(header, 0)));
                int originalSize = parts.size();
                boolean remove = false;
                int intact = 0;
                for (ListIterator<String> pi = parts.listIterator(); pi.hasNext();) {
                    String part = pi.next().trim();
                    if (part.length() != 0 && part.charAt(0) == '$') {
                        if (remove) {
                            pi.remove();
                        }
                    }
                    else {
                        Action action = action((DELIM_EQUALS.split(part, 2))[0].trim());
                        if (action == Action.SUPPRESS || action == Action.MANAGE) {
                            pi.remove();
                            remove = true;
                        }
                        else {
                            intact++;
                            remove = false;
                        }
                    }
                }
                if (intact == 0) {
                    hi.remove();
                }
                else if (parts.size() != originalSize) {
                    hi.set(StringUtil.join(";", parts));
                }
            }
        }
    }

    /**
     * Removes the cookies from the response that are suppressed or managed.
     *
     * @param response the response to suppress the cookies in.
     */
    private void suppress(Response response) {
        for (String name : RESPONSE_HEADERS) {
            List<String> headers = response.headers.get(name);
            if (headers != null) {
                for (ListIterator<String> hi = headers.listIterator(); hi.hasNext();) {
                    String header = hi.next();
                    ArrayList<String> parts;
                    if (name.equals("Set-Cookie2")) {  // rfc 2965 cookie
                        parts = new ArrayList<String>(Arrays.asList(DELIM_COMMA.split(header, 0)));
                    }
                    else { // netscape cookie
                        parts = new ArrayList<String>();
                        parts.add(header);
                    }
                    int originalSize = parts.size();
                    for (ListIterator<String> pi = parts.listIterator(); pi.hasNext();) {
                        String part = pi.next();
                        Action action = action((DELIM_EQUALS.split(part, 2))[0].trim());
                        if (action == Action.SUPPRESS || action == Action.MANAGE) {
                            pi.remove();
                        }
                    }
                    if (parts.size() == 0) {
                        hi.remove();
                    }
                    else if (parts.size() != originalSize) {
                        hi.set(StringUtil.join(",", parts));
                    }
                }
            }
        }
    }
}

