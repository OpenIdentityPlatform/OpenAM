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
 * Portions Copyrighted 2011-2014 ForgeRock AS.
 */

package org.forgerock.openam.shared.resourcename;

import com.sun.identity.shared.debug.Debug;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A generic 'prefix' implementation of BaseResourceName.
 * @param <T> The type that the compare method is going to return instances of.
 * @param <E> The exception type thrown by the canonicalize method.
 * @see org.forgerock.openam.shared.resourcename.BaseResourceName
 */
public abstract class BasePrefixResourceName<T, E extends Exception> implements BaseResourceName<T, E> {

    protected final T wildcardMatch;
    protected final T superResourceMatch;
    protected final T subResourceMatch;
    protected final T noMatch;
    protected final T exactMatch;

    protected String delimiter = "/";
    protected boolean caseSensitive = false;
    /**
     * String indicating default wild card pattern.
     */
    protected String wildcard = "*";
    protected int wildcardLength = 1;
    /**
     * String indicating default one level wild card pattern.
     */
    protected String oneLevelWildcard = "-*-";
    protected int oneLevelWildcardLength = 3;
    /**
     * boolean indicating if the wild card pattern is embedded
     * in the one level wild card pattern eg. wildcard is "*"
     * while one level wild card pattern is "-*-".
     */
    protected boolean wildcardEmbedded = true;
    /**
     * boolean indicating if the one level wild card pattern is embedded
     * in the wild card pattern eg. one level wildcard is "*"
     * while wild card pattern is "-*-".
     */
    protected boolean oneLevelWildcardEmbedded = false;

    protected Debug debug;
    private static String PROTO_DELIMITER = "://";
    private static int PROTO_DELIMITER_SIZE = PROTO_DELIMITER.length();
    private static final String CURRENT_PATH = ".";
    private static final String PARENT_PATH = "..";

    protected BasePrefixResourceName(Debug debug, T exactMatch, T noMatch, T subResourceMatch, T superResourceMatch,
            T wildcardMatch) {
        this.debug = debug;
        this.exactMatch = exactMatch;
        this.noMatch = noMatch;
        this.subResourceMatch = subResourceMatch;
        this.superResourceMatch = superResourceMatch;
        this.wildcardMatch = wildcardMatch;
    }

    /**
     * Initializes the resource name with configuration information,
     * usally set by the administrators. The main configration information
     * retrived is mainly like wild card pattern used, one level wild card
     * pattern used, case sensitivity etc.
     *
     * Subclasses may want to populate the following protected fields:
     * <ul>
     *     <li>{@code delimiter}</li>
     *     <li>{@code caseSensitive}</li>
     *     <li>{@code wildcard}</li>
     *     <li>{@code oneLevelWildcard}</li>
     *     <li>{@code oneLevelWildcardLength}</li>
     *     <li>{@code wildcardLength}</li>
     *     <li>{@code wildcardEmbedded}</li>
     *     <li>{@code oneLevelWildcardEmbedded}</li>
     * </ul>
     *
     * @param configParams configuration parameters as a map.
     * The keys of the map are the configuration paramaters.
     * Each key is corresponding to one <code>String</code> value
     * which specifies the configuration paramater value.
     */
    public void initialize(Map configParams) {
        // use default values
    }

    public Set<String> getServiceTypeNames() {
        return null;
    }

    /**
     * Compares two resources.
     *
     * Description: The targetResource may contain wildcard or
     *     one level wildcard pattern but not both  which
     *     matches zero or more characters. The wildcard character
     *     can show up anywhere within the string. The targetResource
     *     can contain any number of one type of wildcard characters.
     *     Based on the type of wild card in the resource either
     *     regular wildcard comparison is done or one level wild card
     *     comparison is done.
     *     One of the five possible match types is returned based
     *     on how the two resource strings are related. The comparison
     *     starts from the beginning of the resource strings.
     *
     *     ResourceMatch.NO_MATCH means two resources don't match.
     *     ResourceMatch.EXACT_MATCH means two resources match.
     *     ResourceMatch.SUB_RESOURCE_MATCH means targetResource is
     *         the sub resource of the requestResource.
     *     ResourceMatch.SUPER_RESOURCE_MATCH means targetResource
     *         is the super resource of the requestResource.
     *     ResourceMatch.WILDCARD_MATCH means two resources match
     *         with respect to the wildcard.
     *
     * @param requestResource name of the resource which will be compared
     * @param targetResource name of the resource which will be compared with
     * @param wildcardCompare flag for wildcard comparison
     *
     * @return returns <code>ResourceMatch</code> that
     * specifies if the resources are exact match, or
     * otherwise.
     */
    public T compare(String requestResource, String targetResource, boolean wildcardCompare) {

        // if both strings are null, we consider them exact match
        if (requestResource == null && targetResource == null) {
            return exactMatch;
        }

        // if only one of the strings is null, they are not match
        if (requestResource == null || targetResource == null) {
            return noMatch;
        }

        /** make them all in lower cases before comapring if we
         * want to do case insensitive comparison
         */
        if (!caseSensitive) {
            requestResource = requestResource.toLowerCase();
            targetResource = targetResource.toLowerCase();
        }

        requestResource = normalizeRequestResource(requestResource);
        targetResource = normalizeTargetResource(targetResource);

        /**
         * checks if one level wild card pattern is embedded in wildcard pattern
         * and wild card pattern is not in the resource, then implies that
         * resource contains only one level wild card and hence call the
         * oneLevelWildcardCompare() method.
         */
        if (oneLevelWildcardEmbedded && targetResource.indexOf(wildcard) == -1) {
            if (targetResource.indexOf(oneLevelWildcard) != -1 && wildcardCompare) {
                debug.message("PrefixResourceName:compare():invoking one level compare");
                return oneLevelWildcardCompare(requestResource, targetResource, wildcardCompare);
            }
        }
        /** if wild card is embedded or
         * neither of the wild card patterns are embedded in each other,
         * different patterns like "abc" and "def".
         * Then checks if one level wild card pattern exists in resource
         * if yes, call oneLevelWildcardCompare() method.
         */
        if (wildcardEmbedded || !(wildcardEmbedded || oneLevelWildcardEmbedded)) {
            if (targetResource.indexOf(oneLevelWildcard) != -1 && wildcardCompare) {
                debug.message("PrefixResourceName:compare():invoking one level compare");
                return oneLevelWildcardCompare(requestResource, targetResource, wildcardCompare);
            }
        }

        int reqBegin = 0; // index pointer for requestResource
        int reqEnd = 0;   // index pointer for requestResource
        int tarBegin = 0; // index pointer for targetResource
        int tarEnd = 0;   // index pointer for targetResource
        int reqLen = 0;
        int tarLen = 0;
        String subString = null;

        reqLen = requestResource.length();
        tarLen = targetResource.length();
        tarEnd = targetResource.indexOf(wildcard, tarBegin);

        if (!wildcardCompare || tarEnd == -1) {
            // non-wildcard comparison
            // Compare for equality
            if (requestResource.equals(targetResource)) {
                return exactMatch;
            }

            if (targetResource.startsWith(
                    (requestResource.endsWith(delimiter)) ? requestResource : requestResource + delimiter)) {
                return subResourceMatch;
            }

            if (requestResource.startsWith(
                    (targetResource.endsWith(delimiter)) ? targetResource : targetResource + delimiter)) {
                return superResourceMatch;
            }

            return noMatch;
        }

        // now we have to do wildcard comparison
        // get the sub string prior to the first wildcard char
        subString = targetResource.substring(tarBegin, tarEnd);

        // checks if the first char in targetResource is the wildcard, i.e.
        // the substr is null
        if (tarEnd > tarBegin) {
            // check if requestResource starts with the substr
            if (!(requestResource.startsWith(subString))) {
                if (subString.startsWith(
                        (requestResource.endsWith(delimiter)) ? requestResource : requestResource + delimiter)) {
                    return subResourceMatch;
                }
                return noMatch;
            }
        }

        // yes, requestResource does start with substr
        // move the pointers to the next char after the substring
        // which is already matched
        reqBegin = tarEnd;

        if (tarEnd >= tarLen - 1) {
            return wildcardMatch;
        }

        tarBegin = tarEnd + 1;

        // if there are more wildcards in the targetResource
        while ((tarEnd = targetResource.indexOf(wildcard, tarBegin)) != -1) {

            subString = targetResource.substring(tarBegin, tarEnd);

            if (tarEnd > tarBegin) {
                if ((reqBegin = requestResource.indexOf(subString, reqBegin)) == -1) {
                    return subResourceMatch;
                }
            }

            if (tarEnd >= tarLen - 1) {
                return wildcardMatch;
            }

            reqBegin = reqBegin + (tarEnd - tarBegin);
            tarBegin = tarEnd + 1;
        }

        // we just pass the last wildcard in targetResource
        subString = targetResource.substring(tarBegin, tarLen);

        if ((reqEnd = requestResource.lastIndexOf(subString, reqLen - 1)) == -1) {
            return subResourceMatch;
        }

        if (reqBegin > reqEnd) {
            return subResourceMatch;
        }

        reqBegin = reqEnd;

        if ((reqLen - reqBegin) == (tarLen - tarBegin)) {
            return wildcardMatch;
        }

        reqBegin = reqBegin + (tarLen - tarBegin);
        subString = requestResource.substring(reqBegin, reqBegin + 1);

        if (subString.equals(delimiter)) {
            return superResourceMatch;
        }

        return subResourceMatch;
    }

    /**
     * If further normalization of {@code targetResource} is required during comparison, it can be added here.
     * @param targetResource The target resource from the compare method.
     * @return The normalized target resource (unmodified by default).
     */
    protected String normalizeTargetResource(String targetResource) {
        return targetResource;
    }

    /**
     * If further normalization of {@code requestResource} is required during comparison, it can be added here.
     * @param requestResource The target resource from the compare method.
     * @return The normalized target resource (unmodified by default).
     */
    protected String normalizeRequestResource(String requestResource) {
        return requestResource;
    }

    /**
     * Compares two resources containing one level wild card(s).
     *
     * Description: The targetResource may contain one level wildcard '-*-'
     * ( configurable) which matches zero or more characters, at the same level.
     * So a/b/-*-/d will WILDCARD_MATCH a/b/df/d where -*- matches 'df'
     * but not a/b/c/f/d since -*- does not match "c/f". So in other words
     * the matching pattern for the one level wildcard cannot go across
     * delimiter boundary. The wildcard character can show up anywhere within
     * the string. The targetResource can contain any number of wildcard
     * characters. One of the five possible match types is returned based
     * on how the two resource strings are related. The comparison
     * starts from the beginning of the reource strings.
     *
     *     ResourceMatch.NO_MATCH means two resources don't match.
     *     ResourceMatch.EXACT_MATCH means two resources match.
     *     ResourceMatch.SUB_RESOURCE_MATCH means targetResource is
     *         the sub resource of the requestResource.
     *     ResourceMatch.SUPER_RESOURCE_MATCH means targetResource
     *         is the super resource of the requestResource.
     *     ResourceMatch.WILDCARD_MATCH means two resources match
     *         with respect to the wildcard.
     *
     * @param requestResource name of the resource which will be compared
     * @param targetResource name of the resource which will be compared with
     * @param wildcardCompare flag for wildcard comparison
     *
     * @return returns <code>ResourceMatch</code> that
     * specifies if the resources are exact match, or
     * otherwise.
     */

    public T oneLevelWildcardCompare(String requestResource,
            String targetResource,
            boolean wildcardCompare)
    {
        // requestResource & targetResource are not null,
        // have no ending delimiters and if case insensitive
        // compare is defined, are already lowercase.

        StringTokenizer st1 = new StringTokenizer(requestResource, delimiter);
        StringTokenizer st2 = new StringTokenizer(targetResource, delimiter);
        String[] requestTokens = new String[st1.countTokens()];
        String[] targetTokens = new String[st2.countTokens()];
        int i = 0;
        int j = 0;
        while (st1.hasMoreTokens()) {
            requestTokens[i++] = (String)st1.nextToken();
        }
        while (st2.hasMoreTokens()) {
            targetTokens[j++] = (String)st2.nextToken();
        }
        boolean wildcardMatch = false;
        j = 0;
        for (i = 0; i < requestTokens.length; ) {
            String requestToken = requestTokens[i++];
            if (j < targetTokens.length) {
                T matchTokensResult = null;
                String targetToken = aggregateWildcard( targetTokens[j++]);
                matchTokensResult = matchTokens( targetToken, requestToken);
                if (matchTokensResult.equals(noMatch)) {
                    return matchTokensResult;
                }
                if (matchTokensResult.equals(this.wildcardMatch)) {
                    wildcardMatch = true;
                }
            } else {
                if ( i  <= requestTokens.length )  {
                    return superResourceMatch;
                } else {
                    return noMatch;
                }
            }
        }
        // request tokens are over
        // check if target tokens still remain
        if ( j <  targetTokens.length) {
            if ( j == targetTokens.length-1  && aggregateWildcard(targetTokens[j]).equals(oneLevelWildcard)) {
                // last token is a wildcard
                return this.wildcardMatch;
            }
            return subResourceMatch;
        }
        if (wildcardMatch) {
            return this.wildcardMatch;
        }
        return exactMatch;
    }

    /**
     * This method is used to reduce mutiple oocurrences of
     * one level wildcard immediately following a previous one
     * to just one.  So "a[*][*][*]b" where one level wild pattern is
     * "[*]" reduces to "a[*]b".
     */
    private String aggregateWildcard ( String targetToken) {
        int len = 0;
        if (targetToken == null || (len = targetToken.length()) == 0) {
            return targetToken;
        }
        char[] oldchars = targetToken.toCharArray();
        char[] newchars = new char[len];
        int i = 0;
        int j = 0;
        int k = 0;

        boolean foundWildcard = false;

        while (i < len) {
            if (targetToken.startsWith(oneLevelWildcard, i)) {
                if (!foundWildcard) {
                    k = i;
                    while (i < k + oneLevelWildcardLength) {
                        newchars[j++] = oldchars[i++];
                    }
                    foundWildcard = true;
                } else { // ignore the wildcard
                    i = i + oneLevelWildcardLength;
                }
            } else {
                foundWildcard = false;
                newchars[j++] = oldchars[i++];
            }
        }
        return String.valueOf(newchars, 0, j);
    }


    /**
     * matches individual request and target tokens. This method is
     * used to compare tokens in one level wild card compare to compare
     * tokens between delimiter boundaries.
     */

    private T matchTokens(String targetToken,
            String requestToken)
    {
        int wildcardIndex = 0;
        if (targetToken == null && requestToken == null) {
            return exactMatch;
        }
        if (targetToken == null || requestToken == null) {
            return noMatch;
        }
        int beginTargetIndex = 0;
        int beginRequestIndex = 0;
        String substr = null;
        int targetTokenLength = targetToken.length();
        int requestTokenLength = requestToken.length();

        if (targetToken.indexOf(oneLevelWildcard, beginTargetIndex) != -1 ) {
            while ((wildcardIndex = targetToken.indexOf(oneLevelWildcard,
                    beginTargetIndex)) != -1 ) {
                if (wildcardIndex > beginTargetIndex) {
                    substr = targetToken.substring(beginTargetIndex,
                            wildcardIndex);
                    if ((beginRequestIndex = requestToken.indexOf(
                            substr,beginRequestIndex)) == -1)
                    {
                        return noMatch;
                    }
                    beginTargetIndex = beginTargetIndex + substr.length()
                            + oneLevelWildcardLength;
                    beginRequestIndex = beginRequestIndex + substr.length();
                } else {
                    if (wildcardIndex == beginTargetIndex) {
                        // check if only wild card
                        if (targetTokenLength == oneLevelWildcardLength) {
                            return wildcardMatch;
                        } else {// has more string after wild card
                            // advance over the wildcard and go back to while
                            beginTargetIndex = beginTargetIndex +
                                    oneLevelWildcardLength;
                            continue;
                        }
                    }
                }
            }
            //check if wildcard was last in the target string
            if (beginTargetIndex >= targetTokenLength) {
                return wildcardMatch;
            }
            // match the target string after last wildcard
            // against the remaining request going backwards
            String targetStr = targetToken.substring(beginTargetIndex, targetTokenLength);
            // get the remaining request
            String remRequest = requestToken.substring(beginRequestIndex, requestTokenLength);
            int remRequestIndex = -1;
            if ((remRequestIndex = remRequest.lastIndexOf(targetStr, remRequest.length() - 1)) == -1 ) {
                return noMatch;
            } else {
                beginRequestIndex = beginRequestIndex + remRequestIndex;
                // check if we are at the end of request or still
                // chars remain
                if (beginRequestIndex + targetStr.length() >= requestTokenLength) {
                    return wildcardMatch;
                } else { // some non matching chars remain in request
                    return noMatch;
                }
            }
        } else {
            if (targetToken.equals(requestToken)) {
                return exactMatch;
            }
        }
        return noMatch;
    }

    /**
     * Appends sub-resource to super-resource.
     *
     * @param superResource name of the super-resource to be appended to
     * @param subResource name of the sub-resource to be appended
     *
     * @return returns the combination resource.
     */
    public String append(String superResource, String subResource) {
        // Remove duplicate /
        if (superResource.endsWith(delimiter) && subResource.startsWith(delimiter)) {
            superResource = superResource.substring(0,
                    superResource.length() -1);
        }

        if (!superResource.endsWith(delimiter) && !subResource.startsWith(delimiter)) {
            subResource = delimiter + subResource;
        }

        return superResource+subResource;
    }

    /**
     * Gets sub-resource from an original resource minus
     * a super resource. This is the complementary method of
     * append().
     *
     * @param resource name of the original resource consisting of
     * the second parameter superResource and the returned value
     * @param superResource name of the super-resource which the first
     * parameter begins with.
     *
     * @return returns the sub-resource which the first paramter
     * ends with. If the first parameter doesn't begin with the
     * the first parameter, then the return value is null.
     */
    public String getSubResource(String resource, String superResource) {
        String subResource = null;
        if ( !superResource.endsWith(delimiter) ) {
            superResource = superResource + delimiter;
        }
        if (resource.startsWith(superResource)) {
            subResource = resource.substring(superResource.length());
        }
        return subResource;
    }


    /* Splits the given resource name
     * @param res the resource name to be split
     * @return an array of (String) split resource names
     */
    public String[] split(String res) {
        String protocol = "";
        int doubleD = res.indexOf(delimiter+delimiter);
        if (doubleD != -1) {
            protocol = res.substring(0, doubleD + 2);
            res = res.substring(doubleD+2);
        }
        StringTokenizer st = new StringTokenizer(res, delimiter);
        int n = st.countTokens();
        String[] retVal = new String[n];
        for (int i=0; i<n; i++) {
            retVal[i] = st.nextToken();
        }
        retVal[0] =  protocol + retVal[0];
        if (res.startsWith(delimiter) ) {
            retVal[0] = retVal[0] + delimiter;
        }
        if (res.endsWith(delimiter) ) {
            retVal[n-1] = retVal[n-1] + delimiter;
        }
        return retVal;
    }

    /**
     * This method is used to canonicalize a prefix resource
     * It throws an Exception if both regular multi wildcard and
     * one level wild card appears is same resource String.
     * Also removes ( purges) mutiple consecutive delimiters to 1,
     * in the URI.
     * <li>So http://xyz.com///abc///d becomes http://xyz.com/abc/d
     * where "/" is the delimiter.</li>
     *
     * @param res the prefix resource string to be canonicalized
     * @return the prefix resource string in its canonicalized form.
     * @throws E if resource is invalid
     */
    public String canonicalize(String res) throws E
    {
        boolean errorCondition = false;
        int startOneLevelIndex = res.indexOf(oneLevelWildcard);
        int endOneLevelIndex = (startOneLevelIndex != -1) ? startOneLevelIndex + oneLevelWildcardLength - 1 : -1;
        int startWildcardIndex = res.indexOf(wildcard);
        int endWildcardIndex = (startWildcardIndex != -1) ? startWildcardIndex + wildcardLength - 1 : -1;

        int resLength = res.length();

        boolean oneLevelFound = false;
        if (wildcardEmbedded) {
            while (startWildcardIndex != -1 && (startWildcardIndex + wildcardLength <= resLength )) {
                if (startOneLevelIndex != -1 ) {
                    if ( startWildcardIndex >=startOneLevelIndex && endOneLevelIndex >= endWildcardIndex) {
                        oneLevelFound = true;
                    } else {
                        errorCondition = true;
                        break;
                    }
                } else { // wildcard encountered but not one level wildcard
                    // if previous occurence of one level was found
                    if (oneLevelFound) {
                        errorCondition = true;
                        break;
                    }
                }
                startOneLevelIndex = res.indexOf(oneLevelWildcard, endOneLevelIndex +1);
                endOneLevelIndex = (startOneLevelIndex != -1) ? startOneLevelIndex + oneLevelWildcardLength - 1 : -1;
                startWildcardIndex = res.indexOf(wildcard, endWildcardIndex + 1);
                endWildcardIndex = (startWildcardIndex != -1) ? startWildcardIndex + wildcardLength - 1 : -1;
            }
        }
        boolean wildcardFound = false;
        if (oneLevelWildcardEmbedded) {
            while (startOneLevelIndex != -1 && (startOneLevelIndex + oneLevelWildcardLength <= resLength )) {

                if (startWildcardIndex != -1 ) {
                    if ( startOneLevelIndex >=startWildcardIndex && endWildcardIndex >= endOneLevelIndex) {
                        wildcardFound = true;
                    } else {
                        errorCondition = true;
                        break;
                    }
                } else { // one level found, but not wildcard
                    // if previous occurence of wildcard was found
                    if (wildcardFound) {
                        errorCondition = true;
                        break;
                    }
                }
                startOneLevelIndex = res.indexOf(oneLevelWildcard,
                        endOneLevelIndex +1);
                endOneLevelIndex = (startOneLevelIndex != -1) ?
                        startOneLevelIndex + oneLevelWildcardLength - 1 :-1;
                startWildcardIndex =
                        res.indexOf(wildcard, endWildcardIndex + 1);
                endWildcardIndex = (startWildcardIndex != -1) ?
                        startWildcardIndex + wildcardLength - 1 :-1;
            }
        }
        if (!oneLevelWildcardEmbedded && !wildcardEmbedded) {
            if (res.indexOf(oneLevelWildcard) != -1 && res.indexOf(wildcard) != -1) {
                errorCondition = true;
            }
        }

        if (errorCondition) {
            throw constructResourceInvalidException(null);
        }

        int idx = res.indexOf(PROTO_DELIMITER);
        if (idx >= 0) {
            String protoString = res.substring(0, idx + PROTO_DELIMITER_SIZE);
            String remainingResource = res.substring(idx + PROTO_DELIMITER_SIZE);
            String hostString = "";
            String queryString = "";
            int slashIdx = remainingResource.indexOf('/');
            if (slashIdx != -1) {
                hostString = remainingResource.substring(0, slashIdx);
                remainingResource = remainingResource.substring(slashIdx);
            }
            int queryIdx = remainingResource.indexOf('?');
            if (queryIdx != -1) {
                queryString = remainingResource.substring(queryIdx);
                remainingResource = remainingResource.substring(0, queryIdx);
            }
            return protoString + hostString + normalizePath(remainingResource) + queryString;
        } else {
            return normalizePath(res);
        }
    }

    /**
     * Cleanses the URL path, by doing the followings:
     * <ul>
     *  <li>Eliminates the null path (consecutive delimiters).</li>
     *  <li>Removes noop ./ segments from the path.</li>
     *  <li>Removes ../ segments along with the parent directory from the path.</li>
     * </ul>
     *
     * @param resource The resource that needs to be normalized.
     * @return The normalized resource URI.
     */
    private String normalizePath(String resource) {
        if (resource == null || resource.isEmpty()) {
            return "";
        }

        int len = resource.length();

        char[] oldchars = resource.toCharArray();
        Deque<String> segments = new ArrayDeque<String>();
        char delimiterChar = delimiter.charAt(0);

        int j = 0;
        for (int i = 0; i < len; i++) {
            if (oldchars[i] == delimiterChar) {
                if (i != j) {
                    String segment = new String(oldchars, j, i - j);
                    if (segment.equals(PARENT_PATH)) {
                        if (segments.size() > 0) {
                            segments.removeLast();
                        }
                    } else if (!segment.equals(CURRENT_PATH)) {
                        segments.add(segment);
                    }
                    j = i + 1;
                } else {
                    j++;
                }
            }
        }
        if (oldchars[len - 1] != delimiterChar) {
            segments.add(new String(oldchars, j, len - j));
        }
        StringBuilder result = new StringBuilder(resource.length());

        for (String segment : segments) {
            result.append(delimiterChar).append(segment);
        }
        if (!resource.startsWith(delimiter)) {
            result.deleteCharAt(0);
        }
        if (resource.endsWith(delimiter)) {
            result.append(delimiterChar);
        }

        return result.toString();
    }

    /**
     * Construct the exception that will be thrown if the resource is invalid during canonicalize.
     * @return
     */
    protected abstract E constructResourceInvalidException(Object[] args);
}
