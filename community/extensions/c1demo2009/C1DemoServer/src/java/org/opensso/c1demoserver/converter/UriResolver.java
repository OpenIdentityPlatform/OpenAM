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
 * $Id: UriResolver.java,v 1.2 2009/06/11 05:29:42 superpat7 Exp $
 */

package org.opensso.c1demoserver.converter;

import javax.ws.rs.WebApplicationException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import javax.xml.bind.JAXBContext;

/**
 * Utility class for resolving an uri into an entity.
 */
public class UriResolver {
    
    private static ThreadLocal<UriResolver> instance = new ThreadLocal<UriResolver>() {
        protected UriResolver initialValue() {
            return new UriResolver();
        }
    };
    
    private boolean inProgress = false;
    
    private UriResolver() {
    }
    
    /**
     * Returns an instance of UriResolver.
     *
     * @return an instance of UriResolver.
     */
    public static UriResolver getInstance() {
        return instance.get();
    }
    
    private static void removeInstance() {
        instance.remove();
    }
    
    /**
     * Returns the entity associated with the given uri.
     *
     * @param type the converter class used to unmarshal the entity from XML
     * @param uri the uri identifying the entity
     * @return the entity associated with the given uri
     */
    public <T> T resolve(Class<T> type, URI uri) {
        if (inProgress) return null;
        
        inProgress = true;
        
        try {
            if (uri == null) {
                throw new RuntimeException("No uri specified in a reference.");
            }
            
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == 200) {
                JAXBContext context = JAXBContext.newInstance(type);

                return (T) context.createUnmarshaller().unmarshal(conn.getInputStream());
            } else {
                throw new WebApplicationException(new Throwable("Resource for " + uri + " does not exist."), 404);
            }
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        } finally {
            removeInstance();
        }
    }
}