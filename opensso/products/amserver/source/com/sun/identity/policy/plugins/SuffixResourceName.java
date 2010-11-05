/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SuffixResourceName.java,v 1.2 2008/06/25 05:43:52 qcheng Exp $
 *
 */



package com.sun.identity.policy.plugins;

import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.policy.ResourceMatch;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.interfaces.ResourceName;

/**
 * This is a plugin impelmentation of the <code>ResourceName</code> interface
 * it provides methods to do resource comparisons and resource
 * handling based on suffix based string match going right to left.
 */
public class SuffixResourceName implements ResourceName {

    private String delimiter = ",";
    private boolean caseSensitive = false; 
    protected String wildcard = "*";
    
    /**
     * empty no argument constructor.
     */
    public SuffixResourceName() {
        // do nothing
    }

    /**
     * Initializes the resource name with configuration information,
     * usally set by the administrators. The main configration information
     * retrived is mainly like wild card pattern used, delimiter,
     * case sensitivity etc.
     *
     * @param configParams configuration parameters as a map.
     * The keys of the map are the configuration paramaters. 
     * Each key is corresponding to one <code>String</code> value
     * which specifies the configuration paramater value.
     */
    public void initialize(Map configParams) {
        String delimiterConfig = (String)configParams.get(
                        PolicyConfig.RESOURCE_COMPARATOR_DELIMITER);

        if (delimiterConfig != null) {
            this.delimiter = delimiterConfig;
        }

        String caseConfig = (String)configParams.get(
                        PolicyConfig.RESOURCE_COMPARATOR_CASE_SENSITIVE);
        if (caseConfig != null) {
            if (caseConfig.equals("true")) {
                this.caseSensitive = true;
            }
            else if (caseConfig.equals("false")) {
                this.caseSensitive = false;
            } else {
                this.caseSensitive = false;
            }        
        }   
        String wildcardConfig = (String)configParams.get(
                        PolicyConfig.RESOURCE_COMPARATOR_WILDCARD);
        if (wildcardConfig != null) {
            this.wildcard = wildcardConfig;
        }
        return;
    }


    public Set getServiceTypeNames() 
    {
        return null;
    }


    /**
     * Compares two resources.
     *
     * Description: The targetResource may contain wildcard '*' which
     *     matches zero or more characters. The wildcard character 
     *     can show up anywhere within the string. The targetResource
     *     can contain any number of wildcard characters.
     *     One of the five possible match types is returned based 
     *     on how the two resource strings are related. The comparison
     *     starts from the end of the reource strings.
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
        int beginIndex1 = 0; // index pointer for requestResource
        int endIndex1 = 0;   // index pointer for requestResource
        int beginIndex2 = 0; // index pointer for targetResource
        int endIndex2 = 0;   // index pointer for targetResource
        int strlen1 = 0;
        int strlen2 = 0;
        String substr = null;

        // if both strings are null, we consider them exact match
        if ((requestResource == null) && (targetResource == null)) {
            return (ResourceMatch.EXACT_MATCH);
        }

        // if only one of the strings is null, they are not match
        if (((requestResource == null) || (targetResource == null))) {
            return (ResourceMatch.NO_MATCH);
        }

        // make them all in lower cases before comapring if we 
        // want to do case insensitive comparison
        if (!caseSensitive) {
            requestResource = requestResource.toLowerCase();
            targetResource = targetResource.toLowerCase();
        }

        requestResource = reverseString(requestResource);
        targetResource = reverseString(targetResource);

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
        
        // check if the first char in targetResource is the wildcard, i.e. 
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
                
        // we just pass the last wildcard in targetResource
        substr = targetResource.substring(beginIndex2, strlen2);
        if ((endIndex1 = requestResource.lastIndexOf(substr, strlen1 - 1)) 
            == -1) 
        {
            return (ResourceMatch.SUB_RESOURCE_MATCH);
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
     * Reverse the string
     */
    private String reverseString(String str)
    {
        int strlen = str.length();
        if (strlen < 2) {
            return str;
        }
        char[] chars = new char[strlen];
        for (int i = 0; i < strlen; i++) {
            chars[i] = str.charAt(strlen - i -1);
        }
        return (new String(chars));
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
        
        return subResource + delimiter + superResource;
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
        if ( !superResource.startsWith(delimiter) ) {
            superResource = delimiter + superResource;
        }
        if (resource.endsWith(superResource)) {
            int endIndex = resource.length() - superResource.length();
            subResource = resource.substring(0, endIndex);
        }
        return subResource;
    }


    /* Splits the given resource name
     * @param res the resource name to be split
     * @return an array of (String) split resource names
     */
    public String[] split(String res) {
        StringTokenizer st = new StringTokenizer(res, delimiter);
        int n = st.countTokens();
        String[] retVal = new String[n];
        for (int i = 0; i < n; i++) {
            retVal[n-i-1] = st.nextToken();
        }
        return retVal;
    }

    /**
     * This method is used to canonicalize a suffix resource
     * It returns the string back as is.
     * 
     * @param res the suffix resource string to be canonicalized
     * @return the suffix resource string in its canonicalized form.
     * @throws PolicyException if resource is invalid
     */
    public String canonicalize(String res) throws PolicyException
    {
        return res;
    }
}
            
            

            
