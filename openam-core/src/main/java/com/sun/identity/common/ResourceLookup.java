/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ResourceLookup.java,v 1.7 2009/05/02 22:12:04 kevinserwin Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package com.sun.identity.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.search.FileLookup;
import com.sun.identity.shared.search.FileLookupException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletContext;

/**
 * ResourceLookup is a partial replacement for implementation of FileLookup. It
 * performs the equivalent of "fstat" using ServletContext.getResource(), thus
 * increasing web container independence.
 */
public class ResourceLookup {
	private static final Debug DEBUG = Debug.getInstance("amResourceLookup");

	final private static Map<String,String> resourceNameCache = new ConcurrentHashMap<String,String>();
    /**
     * Returns the first existing resource in the ordered search paths.
     * 
     * @param context Servlet Context Reference.
     * @param fileRoot
     * @param locale
     * @param orgFilePath
     * @param clientPath
     * @param filename
     * @param resourceDir absolute path of template base directory
     * @return <code>String</code> first existing resource in the ordered
     *         search paths.
     */
    public static String getFirstExisting(ServletContext context,
            String fileRoot, String locale, String orgFilePath,
            String clientPath, String filename, String resourceDir) {
        String resourceName = null;

        String cacheKey = new StringBuffer(fileRoot).append(":").append(locale)
                .append(":").append(orgFilePath).append(":").append(clientPath)
                .append(":").append(filename).append(":").append(resourceDir)
                .toString();

        resourceName = (String) resourceNameCache.get(cacheKey);
        if (resourceName != null) 
            return resourceName;

        if (resourceURLCacheNotExists.getIfPresent(cacheKey)!=null)
        		return null;
        
        URL resourceUrl = null;

        // calls FileLookup to get the file paths to locate file
        try {
            File[] orderedPaths = FileLookup.getOrderedPaths(fileRoot, locale,
                    null, orgFilePath, clientPath, filename);

            for (File orderedPath : orderedPaths) {
                resourceName = resourceDir + Constants.FILE_SEPARATOR + orderedPath.toString();
                resourceName = resourceName.replaceAll("\\\\", "/");
                if ((resourceUrl = getResourceURL(context, resourceName)) != null) {
                    break;
                }
            }
        } catch (FileLookupException fe) {
            DEBUG.message("ResourceLookup.getFirstExisting: ", fe);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("amResourceLookup: resourceURL: " + resourceUrl);
            DEBUG.message("amResourceLookup: resourceName: " + resourceName);
        }

        if (resourceUrl != null) 
            resourceNameCache.put(cacheKey, resourceName);
        else{
	        	resourceURLCacheNotExists.put(cacheKey, true);
	        	resourceName = null;
        }
        return resourceName;
    }

    final static Map<String, URL> resourceURLCache=new ConcurrentHashMap<String, URL>();
    final static Cache<String,Boolean> resourceURLCacheNotExists=
    		CacheBuilder.newBuilder()
    		.maximumSize(32000)
    		.expireAfterAccess(1, TimeUnit.HOURS)
    		.build();
    /* returns the resourceURL for the resource name for the request */
    private static URL getResourceURL(ServletContext context, String resourceName) {
        URL resourceURL = resourceURLCache.get(resourceName);
        if (resourceURL!=null)
        		return resourceURL;
        if (resourceURLCacheNotExists.getIfPresent(resourceName)!=null)
        		return null;
        try {
            if (context != null) {
                resourceURL = context.getResource(resourceName);
            }
            //Only try to lookup XMLs from the classpath as UI files from JAR files cannot be used by RequestDispatcher
            if (resourceURL == null && resourceName.endsWith(".xml")) {
                // remove leading '/' from resourceName
                resourceURL = Thread.currentThread().getContextClassLoader().getResource(resourceName.substring(1));
            }
        } catch (MalformedURLException murle) {
            DEBUG.message("Error getting resource: " + resourceURL + " cause: " + murle.getMessage());
        }
        if (resourceURL!=null)
	        	resourceURLCache.put(resourceName,resourceURL);
	    	else
	    		resourceURLCacheNotExists.put(resourceName,true);
        return resourceURL;
    }
}
