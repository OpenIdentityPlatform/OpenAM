/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MaskingClassLoader.java,v 1.9 2009/01/09 22:45:51 mrudul_uchil Exp $
 *
 */

package com.sun.identity.classloader;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import sun.misc.CompoundEnumeration;

/**
 * {@link ClassLoader} that masks a specified set of classes
 * from its parent class loader.
 *
 * <p>
 * This code is used to create an isolated environment.
 *
 */
public class MaskingClassLoader extends ClassLoader {

    private final ClassLoader parent;
    private final String[] masks;
    private final String[] maskResources;
    private final URL[] urls;
    private final String resource = 
        "META-INF/services/com.sun.xml.ws.api.pipe.TransportPipeFactory";
    private final String resource2 = 
        "META-INF/services/com.sun.xml.ws.policy.spi.PolicyAssertionValidator";
    private final String resourceAuthConfigProvider = 
        "META-INF/services/javax.security.auth.message.config.AuthConfigProvider";
    private final String resourceTransformerFactory =
        "META-INF/services/javax.xml.transform.TransformerFactory";
    private final String resourceSAXParserFactory =
        "META-INF/services/javax.xml.parsers.SAXParserFactory";
    private final String resourceDocumentBuilderFactory =
        "META-INF/services/javax.xml.parsers.DocumentBuilderFactory";
    private final String resourceTubelineAssembler = 
        "META-INF/services/com.sun.xml.ws.api.pipe.TubelineAssemblerFactory";

    /*public MaskingClassLoader(String[] masks) {
        this.masks = masks;
    }

    public MaskingClassLoader(Collection<String> masks) {
        this(masks.toArray(new String[masks.size()]));
    }*/

    public MaskingClassLoader(ClassLoader parent, String[] masks, 
        String[] maskResources,URL[] urls) {
        super(parent);
        this.parent = parent;
        this.masks = masks;
        this.maskResources = maskResources;
        this.urls = urls;
    }

    public MaskingClassLoader(ClassLoader parent, 
                              Collection<String> masks,
                              Collection<String> maskResources,
                              URL[] urls) {
        //this(parent, masks.toArray(new String[masks.size()]) , 
        //    maskResources.toArray(new String[maskResources.size()]), urls);
        super(parent);
        this.parent = parent;
        this.masks = masks.toArray(new String[masks.size()]);
        if (maskResources != null) {
            this.maskResources = 
                maskResources.toArray(new String[maskResources.size()]);
        } else {
            this.maskResources = null;
        }
        this.urls = urls;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) 
    throws ClassNotFoundException {
        for (String mask : masks) {
            if(name.startsWith(mask)) {
                throw new ClassNotFoundException();
            }
        }
        return super.loadClass(name, resolve);
    }
    
    @Override
    public synchronized URL getResource(String name) {
        if (maskResources == null) {
            return super.getResource(name);
        }
        
        if(name.startsWith(resourceAuthConfigProvider)) {
            // Read the "resourceAuthConfigProvider" from openssowssproviders.jar
            try {
                URL jarURL =
                    new URL("jar:" + (urls[6]).toString() + "!/" + 
                    resourceAuthConfigProvider);
                return jarURL;
            } catch (MalformedURLException mue) {
                // Continue
            }
        }
        if(name.startsWith(resourceTransformerFactory)) {
            // Read the "resourceTransformerFactory" from xalan.jar
            try {
                URL jarURL =
                    new URL("jar:" + (urls[7]).toString() + "!/" + 
                    resourceTransformerFactory);
                return jarURL;
            } catch (MalformedURLException mue) {
                // Continue
            }
        }
        if(name.startsWith(resourceSAXParserFactory)) {
            // Read the "resourceSAXParserFactory" from xercesImpl.jar
            try {
                URL jarURL =
                    new URL("jar:" + (urls[8]).toString() + "!/" + 
                    resourceSAXParserFactory);
                return jarURL;
            } catch (MalformedURLException mue) {
                // Continue
            }
        }
        if(name.startsWith(resourceDocumentBuilderFactory)) {
            // Read the "resourceDocumentBuilderFactory" from xercesImpl.jar
            try {
                URL jarURL =
                    new URL("jar:" + (urls[8]).toString() + "!/" + 
                    resourceDocumentBuilderFactory);
                return jarURL;
            } catch (MalformedURLException mue) {
                // Continue
            }
        }
        if(name.startsWith(resourceTubelineAssembler)) {
            // Read the "resourceTubelineAssembler" from webservices-rt.jar
            try {
                URL jarURL =
                    new URL("jar:" + (urls[1]).toString() + "!/" + 
                    resourceTubelineAssembler);
                return jarURL;
            } catch (MalformedURLException mue) {
                // Continue
            }
        }
        for (String mask : maskResources) {
            if(name.startsWith(mask)) {
                return null;
            }
        }
        return super.getResource(name);
    }
    
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (maskResources == null) {
            return super.getResources(name);
        }
        
        Enumeration[] tmp = new Enumeration[1];
        if(name.startsWith(resource)) {
            Vector vec = new Vector(1);
            // Read the "resource" from amserver.jar OR openssoclientsdk.jar
            URL jarURL = 
                new URL("jar:" + (urls[5]).toString() + "!/" + resource);
            vec.add(jarURL);
            tmp[0] = vec.elements();
            return new CompoundEnumeration(tmp);
        }
        if(name.startsWith(resource2)) {
            Vector vec = new Vector(1);
            // Read the "resource2" from webservices-rt.jar
            URL jarURL = 
                new URL("jar:" + (urls[1]).toString() + "!/" + resource2);
            vec.add(jarURL);
            tmp[0] = vec.elements();
            return new CompoundEnumeration(tmp);
        }
        if(name.startsWith(resourceTubelineAssembler)) {
            Vector vec = new Vector(1);
            // Read the "resourceTubelineAssembler" from webservices-rt.jar
            URL jarURL = 
                new URL("jar:" + (urls[1]).toString() + "!/" + resourceTubelineAssembler);
            vec.add(jarURL);
            tmp[0] = vec.elements();
            return new CompoundEnumeration(tmp);
        }
        if(name.startsWith(resourceTransformerFactory)) {
            Vector vec = new Vector(1);
            // Read the "resourceTransformerFactory" from xalan.jar
            URL jarURL = 
                new URL("jar:" + (urls[7]).toString() + "!/" + 
                resourceTransformerFactory);
            vec.add(jarURL);
            tmp[0] = vec.elements();
            return new CompoundEnumeration(tmp);
        }
        if(name.startsWith(resourceSAXParserFactory)) {
            Vector vec = new Vector(1);
            // Read the "resourceSAXParserFactory" from xercesImpl.jar
            URL jarURL = 
                new URL("jar:" + (urls[8]).toString() + "!/" + 
                resourceSAXParserFactory);
            vec.add(jarURL);
            tmp[0] = vec.elements();
            return new CompoundEnumeration(tmp);
        }
        if(name.startsWith(resourceDocumentBuilderFactory)) {
            Vector vec = new Vector(1);
            // Read the "resourceDocumentBuilderFactory" from xercesImpl.jar
            URL jarURL = 
                new URL("jar:" + (urls[8]).toString() + "!/" + 
                resourceDocumentBuilderFactory);
            vec.add(jarURL);
            tmp[0] = vec.elements();
            return new CompoundEnumeration(tmp);
        }
        return super.getResources(name);
    }
    
    @Override
    public synchronized String toString() {
        return "com.sun.identity.classloader.MaskingClassLoader : Super is : " 
            + super.toString();
    }
}
