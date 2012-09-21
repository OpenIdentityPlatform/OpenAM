/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: URLPatternMatcher.java,v 1.3 2009/11/09 19:40:48 leiming Exp $
 *
 */

package com.sun.identity.agents.common;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URL;

import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.ResourceMatch;
import com.sun.identity.policy.plugins.HttpURLResourceName;

/**
 * The class provides pattern matching for notenforced URIs/URLs.
 */
public class URLPatternMatcher extends SurrogateBase implements
        IPatternMatcher {
    
    /**
     * Constructor
     */
    public URLPatternMatcher(Module module) {
        super(module);
    }
    
    /**
     * initialize this class.
     * @param patterns the array of notenforced URLs/URIs.
     */
    public void initialize(String[] patterns) {
        List patternList = null;
        
        if((patterns != null) && (patterns.length > 0)) {
            patternList = Arrays.asList(patterns);
        } else {
            patternList = new ArrayList();
        }
        setPatternList(patternList);
        
        resourceName = new HttpURLResourceName();
        resourceName.initialize(new HashMap());
    }
    
    /**
     * match against notenforced URLs/URIs
     * @param requestedURL the URL to be matched
     * @return true if matched, false otherwise
     */
    public boolean match(String requestedURL) {
        
        boolean result = false;
        List  patterns  = getPatternList();
        String pattern = null;
        String patternLower = null;
        int size = patterns.size();
        
        for(int i = 0; i < size; i++) {
            pattern = (String) patterns.get(i);
            patternLower = pattern.toLowerCase();
            
            try {
                requestedURL = resourceName.canonicalize(requestedURL);
                
                // convert URI to URL if any.
                if (pattern.startsWith("/")) {
                    pattern = convertToURL(pattern, requestedURL);
                } else if (!patternLower.startsWith("http")) {
                    pattern = convertToURL("/" + pattern, requestedURL);
                }

                pattern = resourceName.canonicalize(pattern);
                if(isLogMessageEnabled()) {
                    logMessage("URLPatternMatcher.match(" +
                            requestedURL + "): matching by pattern: " +
                            pattern);
                }
                
                ResourceMatch res = resourceName.compare(requestedURL, pattern,
                        true);
                
                if (res == ResourceMatch.WILDCARD_MATCH ||
                        res == ResourceMatch.EXACT_MATCH ) {
                    if(isLogMessageEnabled()) {
                        logMessage("URLPatternMatcher.match(" +
                                requestedURL + "): matched by pattern: " +
                                pattern + " result = " + res);
                    }
                    result = true;
                    break;
                }
                
            } catch (PolicyException ex) {
                if(isLogWarningEnabled()) {
                    logWarning("URLPatternMatcher.match(" +
                            requestedURL + ") - Exception:", ex);
                }
            }
        }
        
        return result;
    }
    
    /*
     * convert pattern's URI to URL format.
     */
    private String convertToURL(String pattern, String requestedURL) {
        
        StringBuffer buffer = new StringBuffer();
        
        try {
            URL url = new URL(requestedURL);
            buffer.append(url.getProtocol()).append("://");
            buffer.append(url.getHost()).append(":");
            buffer.append(url.getPort());
            buffer.append(pattern);
            pattern = buffer.toString();
            
        } catch (Exception ex) {
            if(isLogWarningEnabled()) {
                logWarning("URLPatternMatcher.convertToURL()- pattern:" + 
                        pattern + " requestedURL:" + requestedURL + 
                        " Exception:", ex);
            }
        }
        
        return pattern;
    }
    
    private List getPatternList() {
        return _patternList;
    }
    
    private void setPatternList(List patternList) {
        _patternList = patternList;
    }
    
    private List _patternList;
    
    private HttpURLResourceName resourceName = null;
}
