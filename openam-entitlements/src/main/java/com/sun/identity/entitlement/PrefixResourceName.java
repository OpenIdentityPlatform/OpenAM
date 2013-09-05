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
 * $Id: PrefixResourceName.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This is a plugin impelmentation of the <code>ResourceName</code> interface
 * it provides methods to do resource comparisons and resource
 * handling based on prefix based string match going left to right.
 * This kind of pattern matching would be used by URL kind of resources.
 */

public class PrefixResourceName implements ResourceName {

    protected String delimiter = "/";
    private boolean caseSensitive = false; 
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
    
    private static String PROTO_DELIMITER = "://";
    private static int PROTO_DELIMITER_SIZE = PROTO_DELIMITER.length();

    private final static String TRUE = "true";
    private final static String FALSE = "false";
    private final static String DUMMY_URI = "dummy.html";
    
    //parameter wildcard is no longer required in the entitlement framework
    private final static String PARAM_WILDCARD = "*?*";

    /**
     * empty no argument constructor.
     */
    public PrefixResourceName() {
        // do nothing
    }

    /**
     * Initializes the resource name with configuration information,
     * usally set by the administrators. The main configration information
     * retrived is mainly like wild card pattern used, one level wild card
     * pattern used, case sensitivity etc.
     *
     * @param configParams configuration parameters as a map.
     * The keys of the map are the configuration paramaters. 
     * Each key is corresponding to one <code>String</code> value
     * which specifies the configuration paramater value.
     */
    public void initialize(Map configParams) {
        this.delimiter = "/";
        this.caseSensitive = false;
        this.wildcard = "*";
        this.oneLevelWildcard = "-*-";
        this.oneLevelWildcardLength = oneLevelWildcard.length();
        this.wildcardLength = wildcard.length();
        this.wildcardEmbedded = (oneLevelWildcard.indexOf(wildcard) != -1);
        this.oneLevelWildcardEmbedded =
            (wildcard.indexOf(oneLevelWildcard) != -1);
     }
       

    public Set getServiceTypeNames() 
    {
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
     *     starts from the beginning of the reource strings.
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
    public ResourceMatch compare(String requestResource, 
                                 String targetResource, 
                                 boolean wildcardCompare) 
    {
        // if both strings are null, we consider them exact match
        if ((requestResource == null) && (targetResource == null)) {
            return (ResourceMatch.EXACT_MATCH);
        }

        // if only one of the strings is null, they are not match
        if (((requestResource == null) || (targetResource == null))) {
            return (ResourceMatch.NO_MATCH);
        }

        /** make them all in lower cases before comapring if we 
         * want to do case insensitive comparison
         */
        if (!caseSensitive) {
            requestResource = requestResource.toLowerCase();
            targetResource = targetResource.toLowerCase();
        }
        
        // if the strings are identical then we have an exact match
        if (requestResource.equals(targetResource)) {
            return ResourceMatch.EXACT_MATCH;
        }

        String leftPrecedence =
                SystemPropertiesManager.get(Constants.DELIMITER_PREF_LEFT, FALSE);

        // end delimiter means we treat this resource as a directory
        if (leftPrecedence.equalsIgnoreCase(TRUE)) {
            if (requestResource.endsWith(delimiter)) {
                requestResource = requestResource + DUMMY_URI;
            } else if (requestResource.endsWith(delimiter + wildcard)) {
                requestResource = requestResource.substring(0, requestResource.length() - 1) + DUMMY_URI;
            }
        }

      
        // get rid of ending delimiters if any from requestResource
        while (requestResource.endsWith(delimiter)) {
            int len = requestResource.length();
            requestResource = requestResource.substring(0, len - 1);
        }

        // get rid of ending delimiters if any from targetResource
        while (targetResource.endsWith(delimiter)) {
            int len = targetResource.length();
            targetResource = targetResource.substring(0, len - 1);
        }
        
        // get rid of ending '?*' if any from requestResource 
        // new entitlement engine no longer evaluates parameter wildcard
        while (requestResource.endsWith(PARAM_WILDCARD)) {
            int len = requestResource.length();
            requestResource = requestResource.substring(0, len - 2);
        }

        // get rid of ending '?*' if any from targetResource
        // new entitlement engine no longer evaluates parameter wildcard
        while (targetResource.endsWith(PARAM_WILDCARD)) {
            int len = targetResource.length();
            targetResource = targetResource.substring(0, len - 2);
        }
        
        /**
         * checks if one level wild card pattern is embedded in wildcard pattern
         * and wild card pattern is not in the resource, then implies that
         * resource contains only one level wild card and hence call the
         * oneLevelWildcardCompare() method.
         */
        if (oneLevelWildcardEmbedded && targetResource.indexOf(wildcard) 
            == -1) 
        {
            if (targetResource.indexOf(oneLevelWildcard) != -1 
                && wildcardCompare) 
            {
                return oneLevelWildcardCompare(requestResource,
                    targetResource, wildcardCompare);
            }
        }
        /** if wild card is embedded or
         * neither of the wild card patterns are embedded in each other, 
         * different patterns like "abc" and "def".
         * Then checks if one level wild card pattern exists in resource
         * if yes, call oneLevelWildcardCompare() method.
         */
        if (wildcardEmbedded || 
            !(wildcardEmbedded || oneLevelWildcardEmbedded))
        {
            if (targetResource.indexOf(oneLevelWildcard) != -1 
                && wildcardCompare) 
            {
                return oneLevelWildcardCompare(requestResource,
                    targetResource, wildcardCompare);
            }
        }
            

        int beginIndex1 = 0; // index pointer for requestResource
        int endIndex1 = 0;   // index pointer for requestResource
        int beginIndex2 = 0; // index pointer for targetResource
        int endIndex2 = 0;   // index pointer for targetResource
        int strlen1 = 0;
        int strlen2 = 0;
        String substr = null;

        strlen1 = requestResource.length();
        strlen2 = targetResource.length();
        endIndex2 = targetResource.indexOf(wildcard, beginIndex2);

        if ((!wildcardCompare) || (endIndex2 == -1)) {
            // non-wildcard comparison
            // Compare for equality
            if (requestResource.equals(targetResource)) {
                return (ResourceMatch.EXACT_MATCH);
            }

            if (targetResource.startsWith(requestResource + delimiter)) {
                 return (ResourceMatch.SUB_RESOURCE_MATCH);
            }

                if (requestResource.startsWith(targetResource + delimiter)) {
                return (ResourceMatch.SUPER_RESOURCE_MATCH);
            }
            return (ResourceMatch.NO_MATCH);
        }

        // now we have to do wildcard comparison
        // get the sub string prior to the first wildcard char 
        substr = targetResource.substring(beginIndex2, endIndex2);
        
        // checks if the first char in targetResource is the wildcard, i.e. 
        // the substr is null
        if (endIndex2 > beginIndex2) {
            // check if requestResource starts with the substr
            if (!(requestResource.startsWith(substr))) {
                if (substr.startsWith(requestResource + delimiter)) {
                    return (ResourceMatch.SUB_RESOURCE_MATCH);
                }
                return (ResourceMatch.NO_MATCH);
            }

        } 
        // yes, requestResource does start with substr
        // move the pointers to the next char after the substring
        // which is already matched
        beginIndex1 = beginIndex1 + (endIndex2 - beginIndex2);
        if (endIndex2 >= strlen2 - 1) {
            return (ResourceMatch.WILDCARD_MATCH);
        }
        beginIndex2 = endIndex2 + 1;

        // if there are more wildcards in the targetResource
        while ((endIndex2 = 
             targetResource.indexOf(wildcard, beginIndex2)) != -1) {
            substr = targetResource.substring(beginIndex2, endIndex2);
            if (endIndex2 > beginIndex2) {
                if ((beginIndex1 = 
               requestResource.indexOf(substr, beginIndex1)) == -1) {
                    return (ResourceMatch.SUB_RESOURCE_MATCH);
                }
            }   

            beginIndex1 = beginIndex1 + (endIndex2 - beginIndex2);
            if (endIndex2 >= strlen2 - 1) {
                return (ResourceMatch.WILDCARD_MATCH);
            }
            beginIndex2 = endIndex2 + 1;
        }

        /**
         * targetArray and requestArray will contain the targetResource and requestResource tokenized by
         * delimiter.  in the case of a string like: http://example.forgerock.com/hello/world
         * we'll see an array like [0] = http://example.forgerock.com, [1] = hello, [2] = world
         *
         * If our requestArray has more than one element, then let's find out where
         * the last element of requestArray exists in the targetResource.  If that element does not
         * exist in the targetResource then let's find the last position of the wildcard and use that
         * Once we have that location we can create a substring and make it regex compatible.  If that string
         * matches our requestResource then our targetResource is a SUB_RESOURCE of
         * our requestResource and we can return that, if not, then let's see if the last element
         * of the requestArray exists again in our targetResource and check it again.  If we do have
         * a match, then we need to check that we aren't matching against the entire targetResource string
         * if that is the case, then we have a wildcard match and not a subresource match.
         *
         * If we only have a single element in our requestArray then we make a regex compatible string out
         * of the first element in the targetArray and compare that to requestResource.
         *
         * We'll only be concerned with SUB_RESOURCE matches in this logic, any other types of matches are
         * left for the pre-existing code.
         */

        String[] targetArray = split(targetResource);
        String[] requestArray = split(requestResource);
        String targetRegex;
        int truncateIndex = 0;

        if (requestArray.length > 1) {
            String lastRequestToken = requestArray[requestArray.length - 1];
            if (!targetResource.contains(lastRequestToken)) {
                 lastRequestToken = "*";
            }
            while ((truncateIndex = targetResource.indexOf(lastRequestToken, truncateIndex)) != - 1) {
                truncateIndex += lastRequestToken.length();
                targetRegex = targetResource.substring(0, truncateIndex).replace("*", ".*");
                if (requestResource.matches(targetRegex) && (targetResource.length()
                        != targetResource.substring(0, truncateIndex).length())) {
                    return (ResourceMatch.SUB_RESOURCE_MATCH);
                }
            }
        } else {
            targetRegex = targetArray[0].replace("*", ".*");
            if (requestResource.matches(targetRegex) && targetArray.length > 1) {
                return (ResourceMatch.SUB_RESOURCE_MATCH);
            }
        }

        // we just pass the last wildcard in targetResource
        substr = targetResource.substring(beginIndex2, strlen2);

        if ((endIndex1 = requestResource.lastIndexOf(substr, strlen1 - 1))
            == -1)
        {
            return (ResourceMatch.NO_MATCH);
        }
        
        if (beginIndex1 > endIndex1) {
            return (ResourceMatch.SUB_RESOURCE_MATCH);
        }

        beginIndex1 = endIndex1;
        if ((strlen1 - beginIndex1) == (strlen2 - beginIndex2)) {
            return (ResourceMatch.WILDCARD_MATCH);
        }

        beginIndex1 = beginIndex1 + (strlen2 - beginIndex2);
        substr = requestResource.substring(beginIndex1, beginIndex1 + 1);
        if (substr.equals(delimiter)) { 
            return (ResourceMatch.SUPER_RESOURCE_MATCH);
        }
        
        return (ResourceMatch.SUB_RESOURCE_MATCH);
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

    public ResourceMatch oneLevelWildcardCompare(String requestResource, 
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
        for (i = 0; i < requestTokens.length;) 
        {
            String requestToken = requestTokens[i++];
            if (j < targetTokens.length) {
                ResourceMatch  matchTokensResult = null;
                String targetToken = aggregateWildcard( targetTokens[j++]);
                matchTokensResult = matchTokens( targetToken, requestToken);
                if (matchTokensResult == ResourceMatch.NO_MATCH ) {
                    return matchTokensResult;
                }
                if (matchTokensResult == 
                    ResourceMatch.WILDCARD_MATCH) 
                {
                    wildcardMatch = true;
                }
            } else {
                if ( i  <= requestTokens.length )  {        
                    return ResourceMatch.SUPER_RESOURCE_MATCH;
                } else { 
                    return ResourceMatch.NO_MATCH;
                }
            }
        }
        // request tokens are over
        // check if target tokens still remain
        if ( j <  targetTokens.length) {
            if ( j == targetTokens.length-1  && 
                aggregateWildcard(targetTokens[j]).equals(oneLevelWildcard)) 
            // last token is a wildcard
            {
                return ResourceMatch.WILDCARD_MATCH;
            }
            return ResourceMatch.SUB_RESOURCE_MATCH;
        }
        if (wildcardMatch) {
            return ResourceMatch.WILDCARD_MATCH;
        }
        return ResourceMatch.EXACT_MATCH;
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
 
    private ResourceMatch  matchTokens(String targetToken, 
        String requestToken) 
    {
        int wildcardIndex = 0;
        if (targetToken == null && requestToken == null) {
            return ResourceMatch.EXACT_MATCH;
        }
        if (targetToken == null || requestToken == null) {
            return ResourceMatch.NO_MATCH;
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
                        return ResourceMatch.NO_MATCH;
                    }
                    beginTargetIndex = beginTargetIndex + substr.length() 
                        + oneLevelWildcardLength;
                    beginRequestIndex = beginRequestIndex + substr.length();
                } else { 
                    if (wildcardIndex == beginTargetIndex) {
                    // check if only wild card
                        if (targetTokenLength == oneLevelWildcardLength) {
                                return ResourceMatch.WILDCARD_MATCH;
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
                return ResourceMatch.WILDCARD_MATCH;
            }
            // match the target string after last wildcard
            // against the remaining request going backwards
            String targetStr = targetToken.substring(beginTargetIndex,
                targetTokenLength);
            // get the remaining request
            String remRequest = requestToken.substring(beginRequestIndex, 
                requestTokenLength);
            int remRequestIndex = -1;
            if ((remRequestIndex = remRequest.lastIndexOf(
                targetStr, remRequest.length() - 1)) == -1 )
            {
                    return ResourceMatch.NO_MATCH;
            } else {
                beginRequestIndex = beginRequestIndex + remRequestIndex;
                // check if we are at the end of request or still
                // chars remain
                if (beginRequestIndex + targetStr.length() 
                    >= requestTokenLength) 
                {
                        return ResourceMatch.WILDCARD_MATCH;
                } else { // some non matching chars remain in request
                    return ResourceMatch.NO_MATCH;
                }
            }
        } else {
            if (targetToken.equals(requestToken)) {
                return ResourceMatch.EXACT_MATCH;
            }
        }
        return ResourceMatch.NO_MATCH;
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
        if (superResource.endsWith(delimiter) && 
            subResource.startsWith(delimiter)) 
        {
            superResource = superResource.substring(0, 
                superResource.length() -1);
        }

        if (!superResource.endsWith(delimiter) && 
            !subResource.startsWith(delimiter)) 
        {
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
     * @throws PolicyException if resource is invalid
     */
    public String canonicalize(String res) throws EntitlementException
    {
        boolean errorCondition = false;
        int startOneLevelIndex = res.indexOf(oneLevelWildcard);
        int endOneLevelIndex = (startOneLevelIndex != -1) ?
            startOneLevelIndex + oneLevelWildcardLength - 1 :-1;
        int startWildcardIndex = res.indexOf(wildcard);
        int endWildcardIndex = (startWildcardIndex != -1) ?
            startWildcardIndex + wildcardLength - 1 :-1;

        int resLength = res.length();

        boolean oneLevelFound = false;
        if (wildcardEmbedded) {
            while (startWildcardIndex != -1 && 
                (startWildcardIndex + wildcardLength <= resLength ))
            {
                if (startOneLevelIndex != -1 ) {
                    if ( startWildcardIndex >=startOneLevelIndex &&
                        endOneLevelIndex >= endWildcardIndex)
                    {
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
        boolean wildcardFound = false;
        if (oneLevelWildcardEmbedded) {
            while (startOneLevelIndex != -1 &&
                (startOneLevelIndex + oneLevelWildcardLength <= resLength ))
            {
        
                if (startWildcardIndex != -1 ) {        
                    if ( startOneLevelIndex >=startWildcardIndex
                        && endWildcardIndex >= endOneLevelIndex) 
                    {
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
            if (res.indexOf(oneLevelWildcard) != -1 && 
                res.indexOf(wildcard) != -1) 
            {
                errorCondition = true;
            }
        }

        if (errorCondition) {
            //throw new PolicyException(ResBundleUtils.rbName,
                //"both_type_wildcards_unsupported", null, null);
            throw new EntitlementException(300);
        }

        int idx = res.indexOf(PROTO_DELIMITER);
        if (idx >= 0) {
            String protoString = 
                res.substring(0, idx + PROTO_DELIMITER_SIZE); 
            String remainingRes = res.substring(idx + PROTO_DELIMITER_SIZE);
            return protoString + purgeNullPath(remainingRes);
        } else {
            return purgeNullPath(res);
        }
    }

    /** 
     * eliminates the null path (consecutive delimiters) from the resource 
     */
    private String purgeNullPath(String res) {
        if ((res == null) || (res.length() == 0)) {
            return "";
        }

        boolean preceedingDelimiter = false;
        int len = res.length();
        
        char[] oldchars = res.toCharArray();
        char[] newchars = new char[len];

        int i = 0;
        int j = 0;
        while (i < len) {
            if (oldchars[i] == delimiter.charAt(0)) {
                if (!preceedingDelimiter) {
                    newchars[j++] = oldchars[i++];
                    preceedingDelimiter = true;
                } else {
                    i++;
                }
            } else {
                newchars[j++] = oldchars[i++];
                preceedingDelimiter = false;
            }
        }

        if (preceedingDelimiter) {
            // Remove any trailing forward slash on the end.
            j--;
        }

        return String.valueOf(newchars, 0, j);         
    } 
}
            
            

            
