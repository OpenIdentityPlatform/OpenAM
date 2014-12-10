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
 * Copyright 2006-2009 Sun Microsystems Inc.
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */

package org.forgerock.openam.shared.resourcename;

import com.sun.identity.shared.debug.Debug;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * This is a base implementation of a URLResourceName, extending the functionality found in
 * <code>BasePrefixResourceName</code> to provide special handling to URL type prefix resource
 * names in <code>canonicalize</code> method like validating port, assigning default port of
 * 80, if port absent etc.
 * @param <T> The type that the compare method is going to return instances of.
 * @param <E> The exception type thrown by the canonicalize method.
 * @see org.forgerock.openam.shared.resourcename.BaseResourceName
 * @see org.forgerock.openam.shared.resourcename.BasePrefixResourceName
 */
public abstract class BaseURLResourceName<T, E extends Exception> extends BasePrefixResourceName<T, E> {

    protected BaseURLResourceName(Debug debug, T exactMatch, T noMatch, T subResourceMatch, T superResourceMatch,
            T wildcardMatch) {
        super(debug, exactMatch, noMatch, subResourceMatch, superResourceMatch, wildcardMatch);
    }

    protected static Comparator<String> comparator = new QueryParameterComparator();

    private static final String QUERY_PARAMETER_DELIMITER = "&";
    private static final String QUERY_PARAMETER_VALUE_DELIMITER = "=";
    private static final String COLON = ":";
    private static final String SCHEME_DELIMITER = "://";
    private static final String SLASH = "/";
    private static final String DEFAULT_WEB_PROTOCOL = "http";
    private static final String SECURE_WEB_PROTOCOL = "https";
    private static final String DEFAULT_PORT = "80";
    private static final String SECURE_PORT = "443";
    private static final Pattern ACCEPTABLE_URLS = Pattern.compile("^(http|https)\\**://.*$");

    /**
     * Specific comparison for URLs, where a wildcard in the host/port should not match any of the path.
     * Strings should be canonicalized prior to entering this comparison, else they will be
     * compared by the super class' comparison function.
     *
     * @param requestResource name of the resource which will be compared
     * @param targetResource name of the resource which will be compared with
     * @param wildcardCompare flag for wildcard comparison
     * @return If a wildcard is in the host/port, separately compares the path/query and scheme/host/port, returning
     * NO_MATCH if any don't match. Otherwise returns the match of the port/path/query.
     */
    @Override
    public T compare(String requestResource, String targetResource, boolean wildcardCompare) {
        if (!wildcardCompare) {
            return super.compare(requestResource, targetResource, wildcardCompare);
        }

        String schemelessTarget = targetResource;
        String schemelessRequest = requestResource;

        if (schemelessTarget.contains(SCHEME_DELIMITER) && schemelessRequest.contains(SCHEME_DELIMITER)) {
            schemelessTarget = removeSchemeEnsureSlash(schemelessTarget);
            schemelessRequest = removeSchemeEnsureSlash(schemelessRequest);
        } else {//urls should be canonicalised before reaching here
            return super.compare(requestResource, targetResource, wildcardCompare);
        }

        final int firstColon = schemelessTarget.indexOf(COLON);
        final int firstSlash = schemelessTarget.indexOf(SLASH);

        if (firstColon == -1) {
            return super.compare(requestResource, targetResource, wildcardCompare);
        } else {

            T schemeMatch = compareBeforeBreakpoint(requestResource, targetResource, SCHEME_DELIMITER);

            if (noMatch.equals(schemeMatch) || subResourceMatch.equals(schemeMatch)) {
                return noMatch; //subResource of scheme/host/port isn't appropriate
            } else if (firstSlash >= 0 && firstSlash < firstColon) { //no port, or : is part of the path
                return super.compare(schemelessRequest, schemelessTarget, wildcardCompare);
            } else if (firstSlash >= 0 && firstSlash > firstColon) { //port, wildcard & path

                T hostMatch = compareBeforeBreakpoint(schemelessRequest, schemelessTarget, COLON);

                if (noMatch.equals(hostMatch) || subResourceMatch.equals(hostMatch)) {
                    return noMatch;
                }

                final String postColonRequest = schemelessRequest.substring(schemelessRequest.indexOf(COLON));
                final String postColonTarget = schemelessTarget.substring(schemelessTarget.indexOf(COLON));

                T portMatch = compareBeforeBreakpoint(postColonRequest, postColonTarget, SLASH);

                if (noMatch.equals(portMatch) || subResourceMatch.equals(portMatch)) {
                    return noMatch;
                }

                final String postSlashRequest = postColonRequest.substring(postColonRequest.indexOf(SLASH));
                final String postSlashTarget = postColonTarget.substring(postColonTarget.indexOf(SLASH));

                T pathMatch = super.compare(postSlashRequest, postSlashTarget, wildcardCompare); //for multilevel

                return wildcardResponseCombiner(hostMatch, portMatch, pathMatch);
            } else { //case where firstColon >= 0; port wildcard
                return compareSplit(schemelessRequest, schemelessTarget, COLON);
            }
        }
    }

    /**
     * Initial recursive component. Calls down to compareBeforeBreakpoint and
     * compareAfterBreakpoint, which subsequently call back up to compareSplit.
     */
    private T compareSplit(String resource, String target, String breakPoint) {

        if (!resource.contains(breakPoint)) {
            return super.compare(resource, target, true);
        }

        T firstMatch = compareBeforeBreakpoint(resource, target, breakPoint);

        if (noMatch.equals(firstMatch) || subResourceMatch.equals(firstMatch)) {
            return firstMatch;
        }

        T secondMatch = compareAfterBreakpoint(resource, target, breakPoint);

        return wildcardResponseCombiner(firstMatch, secondMatch);
    }

    /**
     * Ensures that e.g. if a wildcard match was made on the half of a URL, the overall
     * result reflects this rather than the match-type of the latter half of the URL only.
     */
    private T wildcardResponseCombiner(T... matches) {
        boolean wildcard = false;

        for(T match : matches) {
            if (wildcardMatch.equals(match)) {
                wildcard = true;
            } else if (!exactMatch.equals(match)) {
                return match;
            }
        }

        if (wildcard) {
            return wildcardMatch;
        } else {
            return exactMatch;
        }
    }

    /**
     * Recursive-component step
     */
    private T compareBeforeBreakpoint(String resource, String target, String breakPoint) {

        int firstResourceBreakPoint = resource.indexOf(breakPoint);
        int firstTargetBreakPoint = target.indexOf(breakPoint);

        if (firstResourceBreakPoint == -1 || firstTargetBreakPoint == -1) {
            return super.compare(resource, target, true);
        }

        String resourceSub = resource.substring(0, firstResourceBreakPoint);
        String targetSub = target.substring(0, firstTargetBreakPoint);

        //for situation such as /asdf/*
        if (targetSub.endsWith(wildcard)) {
            return super.compare(resourceSub, targetSub, true);
        } else {
            return compareSplit(resourceSub, targetSub, breakPoint);
        }
    }

    /**
     * Recursive-component step
     */
    private T compareAfterBreakpoint(String resource, String target, String breakPoint) {

        int firstResourceBreakPoint = resource.indexOf(breakPoint);
        int firstTargetBreakPoint = target.indexOf(breakPoint);

        if (firstResourceBreakPoint == -1 || firstTargetBreakPoint == -1) {
            return super.compare(resource, target, true);
        }

        String resourceSub = resource.substring(firstResourceBreakPoint + breakPoint.length());
        String targetSub = target.substring(firstTargetBreakPoint + breakPoint.length());

        //for situation such as /asdf/*
        if (targetSub.endsWith(wildcard)) {
            return super.compare(resourceSub, targetSub, true);
        } else {
            return compareSplit(resourceSub, targetSub, breakPoint);
        }

    }

    /**
     * Removes the scheme, appends a slash if the resource contains a port.
     */
    private String removeSchemeEnsureSlash(String url) {
        String part = url.substring(url.indexOf(SCHEME_DELIMITER) + SCHEME_DELIMITER.length());
        if (!part.contains(SLASH)) {
            return part + SLASH;
        }

        return part;
    }

    /**
     * This method is used to canonicalize a url string. It removes leading delimiters after the protocol
     * http:////abc becomes http://abc. If port number is provided validates it to be either wildcard
     * a valid integer, if not provided, adds default port 80. Makes sure URL is not malformed, also if query parameters
     * are present sorts them based on the comparator's sort method.
     *
     * @param urlStr the url string to be canonicalized
     * @return the url string in its canonicalized form.
     * @throws E if the url string is invalid
     */

    public String canonicalize(String urlStr) throws E {

         /* if no http or https protocol resources
         * only call super.canonicalize()
         * to validate the wildcard usage and
         * remove extra delimiters.
         */
        if (!ACCEPTABLE_URLS.matcher(urlStr).matches()) {
            return super.canonicalize(urlStr);
        }

        int index = urlStr.indexOf("://");
        String proto = urlStr.substring(0, index);
        String resource = urlStr.substring(index + 3); // host.sample.com...

        String hostAndPort = resource;
        String urlPath = "";
        if (resource.startsWith(delimiter)) {
            int len = resource.length();
            char[] oldchars = resource.toCharArray();
            char[] newchars = new char[len];
            int j = 0;
            // charAt(0) assuming delimiter is only one character.
            while (j < len && oldchars[j] == delimiter.charAt(0)) {
                j++; // skip leading '/'
            }

            int i = 0;
            while (j < len) {
                newchars[i++] = oldchars[j++];
            }
            resource =  String.valueOf(newchars, 0, i);
        }
        String hostName = "";
        String port = "";
        String query = null;
        if (resource != null && resource.length() != 0) {
            index = resource.indexOf('/');
            if (index == -1) {
                index = resource.indexOf('?');
            }
            if (index != -1) {
                hostAndPort = resource.substring(0, index);
                urlPath = resource.substring(index);
            }
            hostName = hostAndPort;
            index = hostAndPort.indexOf(':');
            if (index != -1) {
                hostName = hostAndPort.substring(0, index);
                port = hostAndPort.substring(index + 1);
                validatePort(port);
            }
            index = urlPath.indexOf('?');
            if (index != -1) {
                query = urlPath.substring(index + 1);
            }

            // If no port has been specified set a default port value based on the protocol.
            // Should the protocol contain a wildcard, set the port to be a wildcard also.
            if ( port.length() == 0) {
                if (proto.equals(DEFAULT_WEB_PROTOCOL)) {
                    port = DEFAULT_PORT;
                } else if (proto.equals(SECURE_WEB_PROTOCOL)) {
                    port = SECURE_PORT;
                } else {
                    port = wildcard;
                }
            }
        }

        StringBuilder sb = new StringBuilder(100);
        sb.append(proto);
        sb.append("://");
        sb.append(hostName);
        if (hostName.length() != 0) {
            sb.append(":");
            sb.append(port);

            if (hostName.equals(wildcard) && hostAndPort.equals(wildcard) && resource.equals(wildcard)) {
                sb.append(wildcard);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("URLResourceName: url query=" + query);
        }

        if (query != null) {
            int indexQuery = urlPath.lastIndexOf(query);
            String prefix = super.canonicalize(urlPath.substring(0, indexQuery - 1));
            sb.append(prefix);
            sb.append('?');
            // check if there are more than one query parameters
            int indexAmp = query.indexOf(QUERY_PARAMETER_DELIMITER);
            if (indexAmp != -1) {
                // there are more than query parameters in the url
                String suffix= urlPath.substring(indexQuery + query.length());
                ArrayList al = new ArrayList();
                StringTokenizer st = new StringTokenizer(query, QUERY_PARAMETER_DELIMITER);
                while (st.hasMoreTokens()) {
                    al.add(st.nextToken());
                }
                // sort the query parameters based on rules of
                // the comparator
                Collections.sort(al, comparator);
                int size = al.size();
                // reconstruct the url in canonicalized form
                for (int i = 0; i < size; i++) {
                    if (i < (size-1)) {
                        sb.append((String) al.get(i)).append(QUERY_PARAMETER_DELIMITER);
                    } else {
                        sb.append((String)al.get(i));
                    }
                }
                sb.append(suffix);
            } else {
                // there is only one query parameter in the url
                sb.append(query);
            }
        } else {
            // there is no query string in the url
            sb.append(super.canonicalize(urlPath));
        }

        return sb.toString();
    }

    /* Validate the port number to make sure there is no invalid
     * character in the port part.
     * Valid characters are digits and wildcard.
     */
    private void validatePort(String port) throws E {
        String portString = port;
        int idx = port.indexOf(wildcard);
        if (idx != -1) {
            int begin = 0;
            int wildcardLen = wildcard.length();
            StringBuilder sb = new StringBuilder(100);
            while (idx != -1) {
                sb.append(port.substring(begin, idx));
                begin = idx + wildcardLen;
                idx = port.indexOf(wildcard, begin);
            }
            sb.append(port.substring(begin));
            portString = sb.toString();
        }
        /* wildcards have been extracted, the remainder should
         * be an integer or an empty string.
         */
        if ((portString != null) && (portString.length() != 0)) {
            try {
                Integer.parseInt(portString);
            } catch (Exception e) {
                String objs[] = { port };
                throw constructResourceInvalidException(objs);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("URLResourceName: portString = " + portString);
        }
    }

    /**
     * This class is used to compare two url query parameter
     * strings. A query parameter string is in the form of
     * variablename=value.
     * @see java.util.Comparator
     */
    private static final class QueryParameterComparator implements Comparator<String> {
        /**
         * @param s1 a url query parameter to be compared
         * @param s2 a url query parameter to be compared
         * @return -1 if s1 < s2; 0 if s1 = s2; 1 if s1 > s2
         */
        public int compare(String s1, String s2) {

            if (s1 == null) {
                if (s2 != null) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                if (s2 == null) {
                    return 1;
                }
            }

            // neither s1 nor s2 is null
            String var1 = s1;
            String value1 = null;
            String var2 = s2;
            String value2 = null;
            int index1 = s1.indexOf(QUERY_PARAMETER_VALUE_DELIMITER);
            int index2 = s2.indexOf(QUERY_PARAMETER_VALUE_DELIMITER);

            if (index1 != -1) {
                var1 = s1.substring(0, index1);
                value1 = s1.substring(index1);
            }
            if (index2 != -1) {
                var2 = s2.substring(0, index2);
                value2 = s2.substring(index2);
            }

            int result = var1.compareTo(var2);
            if (result == 0) {
                // variable names are the same, we need to further
                // compare the values
                if (value1 == null) {
                    if (value2 != null) {
                        result = -1;
                    } else {
                        result = 0;
                    }
                } else {
                    if (value2 == null) {
                        result = 1;
                    } else {
                        result = value1.compareTo(value2);
                    }
                }
            }
            return result;
        }
    }

}
