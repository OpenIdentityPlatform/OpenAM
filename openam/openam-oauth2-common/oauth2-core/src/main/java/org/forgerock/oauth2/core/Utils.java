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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.forgerock.oauth2.core.OAuth2Constants.AuthorizationEndpoint.ID_TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.AuthorizationEndpoint.TOKEN;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.OPENID;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.RESPONSE_TYPE;
import org.forgerock.oauth2.core.OAuth2Constants.UrlLocation;
import static org.forgerock.oauth2.core.OAuth2Constants.UrlLocation.FRAGMENT;
import static org.forgerock.oauth2.core.OAuth2Constants.UrlLocation.QUERY;

/**
 * Utility class containing common utility functions.
 *
 * @since 12.0.0
 */
public final class Utils {

    /**
     * Determines whether the specified String is {@code null} or empty.
     *
     * @param s The String to check.
     * @return {@code true} if the String is {@code null} or empty.
     */
    public static boolean isEmpty(final String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Determines whether the specified Collection is {@code null} or empty.
     *
     * @param c The Collection to check.
     * @return {@code true} if the Collection is {@code null} or empty.
     */
    public static boolean isEmpty(final Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
     * Determines whether the specified Map is {@code null} or empty.
     *
     * @param m The Map to check.
     * @return {@code true} if the Map is {@code null} or empty.
     */
    public static boolean isEmpty(final Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    /**
     * Splits the specified String of response types into a {@code Set} of response types.
     * <br/>
     * If the String of response types is {@code null} an empty {@code Set} is returned.
     *
     * @param responseType The String of response types.
     * @return A {@code Set} of response types.
     */
    public static Set<String> splitResponseType(final String responseType) {
        return stringToSet(responseType);
    }

    /**
     * Splits the specified String of scopes into a {@code Set} of scopes.
     * <br/>
     * If the String of scopes is {@code null} an empty {@code Set} is returned.
     *
     * @param scope The String of scopes.
     * @return A {@code Set} of scopes.
     */
    public static Set<String> splitScope(final String scope) {
        return stringToSet(scope);
    }

    /**
     * Joins the specified {@code Set} of scopes into a space delimited String.
     * <br/>
     * If the specified {@code Set} of scopes is null, an empty String is returned.
     *
     * @param scope The scopes to join.
     * @return A String of the joined scopes.
     */
    public static String joinScope(final Set<String> scope) {

        if (scope == null) {
            return "";
        }

        final Iterator<String> iterator = scope.iterator();

        final StringBuilder sb = new StringBuilder();
        if (iterator.hasNext()) {
            sb.append(iterator.next());
        }
        while (iterator.hasNext()) {
            sb.append(" ").append(iterator.next());
        }
        return sb.toString();
    }

    /**
     * Splits the string on ' ' character and returns a {@code Set<String>} of the contents.
     *
     * @param string The string.
     * @return A {@code Set<String>}.
     */
    public static Set<String> stringToSet(String string) {
        if (string == null || string.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<String>(Arrays.asList(string.split(" ")));
    }

    /**
     * When using the OpenId Connect authorization Implicit Flow the response_type value is
     * "id_token token" or "id_token". When using the Hybrid Flow, this value is "code id_token",
     * "code token", or "code id_token" token.
     *
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#ImplicitAuthRequest">
     *     3.2.2.1. Authentication Request</a>
     * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#HybridAuthRequest">
     *     3.3.2.1. Authentication Request</a>
     *
     * @return True if the request is part of an OpenId Connect authorization Implicit Flow or Hybrid Flow.
     */
    public static boolean isOpenIdConnectFragmentErrorType(Set<String> requestedResponseTypes) {
        return requestedResponseTypes.contains(ID_TOKEN) || requestedResponseTypes.contains(TOKEN);
    }

    /**
     * When using the OAuth2 Implicit Grant the response_type value is "token".
     *
     * @see <a href="http://tools.ietf.org/html/rfc6749#section-4.2.2.1">4.2.2.1. Error Response</a>
     *
     * @return True if the request is part of an OAuth2 Implicit Grant.
     */
    public static boolean isOAuth2FragmentErrorType(Set<String> requestedResponseTypes) {
        if (requestedResponseTypes == null) {
            return false;
        }

        return requestedResponseTypes.size() == 1 && requestedResponseTypes.contains(TOKEN);
    }

    /**
     * Check if the OAuth2 Client is configured to be an OpenId Connect Client.
     *
     * @param clientRegistration The registered client.
     * @return True if the client is configured as an OpenId Connect client.
     */
    public static boolean isOpenIdConnectClient(ClientRegistration clientRegistration) {
        return clientRegistration.getAllowedScopes().contains(OPENID);
    }

    /**
     * Determines if the UrlLocation is fragment or query based on the response types read from the request
     * and the type of client.
     *
     * @param request The OAuth2 request.
     * @param clientRegistration The ClientRegistration.
     * @return UrlLocation.FRAGMENT or UrlLocation.QUERY
     */
    public static UrlLocation getRequiredUrlLocation(OAuth2Request request, ClientRegistration clientRegistration) {
        final Set<String> responseTypes = splitResponseType(request.<String>getParameter(RESPONSE_TYPE));
        return getRequiredUrlLocation(responseTypes, clientRegistration);
    }

    /**
     * Determines if the UrlLocation is fragment or query based on the given response types and the type of client.
     *
     * @param responseTypes The requested response types.
     * @param clientRegistration The registered client.
     * @return UrlLocation.FRAGMENT or UrlLocation.QUERY
     */
    public static UrlLocation getRequiredUrlLocation(Set<String> responseTypes, ClientRegistration clientRegistration) {
        return (isOpenIdConnectClient(clientRegistration) && isOpenIdConnectFragmentErrorType(responseTypes))
                || isOAuth2FragmentErrorType(responseTypes) ? FRAGMENT : QUERY;
    }

    /**
     * Converts a collection of comparable items into a list, using the given comparator to order the
     * items.
     *
     * @param collection The collection to sort
     * @param comp The comparator to use
     * @param <T> The type of the collection.
     * @return A sorted list including all elements from the collection.
     */
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> collection,
                                                                         Comparator<? super T> comp) {
        List<T> list = new ArrayList<T>(collection);
        Collections.sort(list, comp);
        return list;
    }

}
