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
 * $Id: OfflineResolver.java,v 1.2 2008/06/25 05:47:38 qcheng Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.saml.xmlsig;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import org.apache.xml.utils.URI;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;

/**
 * This class helps us home users to resolve http URIs without a network
 * connection
 *
 * @author $Author: qcheng $
 */
public class OfflineResolver extends ResourceResolverSpi {

   /**
    * Method engineResolve
    *
    * @param uri
    * @param BaseURI
    * @throws ResourceResolverException
    */
   public XMLSignatureInput engineResolve(Attr uri, String BaseURI)
           throws ResourceResolverException {

      try {
         String URI = uri.getNodeValue();

         String newURI = (String) this._uriMap.get(URI);
         if (newURI != null) {

            InputStream is = new FileInputStream(newURI);


            XMLSignatureInput result = new XMLSignatureInput(is);

            // XMLSignatureInput result = new XMLSignatureInput(inputStream);
            result.setSourceURI(URI);
            result.setMIMEType((String) this._mimeMap.get(URI));

            return result;
         } else {
            Object exArgs[] = {
               "The URI " + URI + " is not configured for offline work" };

            throw new ResourceResolverException("generic.EmptyMessage", exArgs,
                                                uri, BaseURI);
         }
      } catch (IOException ex) {
         throw new ResourceResolverException("generic.EmptyMessage", ex, uri,
                                             BaseURI);
      }
   }

   /**
    * We resolve http URIs <I>without</I> fragment.
    *
    * @param uri
    * @param BaseURI
    */
   public boolean engineCanResolve(Attr uri, String BaseURI) {

      String uriNodeValue = uri.getNodeValue();

      if (uriNodeValue.length() == 0 || uriNodeValue.startsWith("#")) {
         return false;
      }

      try {
         URI uriNew = new URI(new URI(BaseURI), uri.getNodeValue());

         if (uriNew.getScheme().equals("http")) {

            return true;
         }

      } catch (URI.MalformedURIException ex) {}

      return false;
   }

   /** Field _uriMap */
   static Map _uriMap = null;

   /** Field _mimeMap */
   static Map _mimeMap = null;

   /**
    * Method register
    *
    * @param URI
    * @param filename
    * @param MIME
    */
   private static void register(String URI, String filename, String MIME) {
      OfflineResolver._uriMap.put(URI, filename);
      OfflineResolver._mimeMap.put(URI, MIME);
   }

   static {
      org.apache.xml.security.Init.init();

      OfflineResolver._uriMap = new HashMap();
      OfflineResolver._mimeMap = new HashMap();

      OfflineResolver.register("http://www.w3.org/TR/xml-stylesheet",
                               "data/org/w3c/www/TR/xml-stylesheet.html",
                               "text/html");
      OfflineResolver.register("http://www.w3.org/TR/2000/REC-xml-20001006",
                               "data/org/w3c/www/TR/2000/REC-xml-20001006",
                               "text/xml");
      OfflineResolver.register("http://www.nue.et-inf.uni-siegen.de/index.html",
                               "data/org/apache/xml/security/temp/nuehomepage",
                               "text/html");
      OfflineResolver.register(
         "http://www.nue.et-inf.uni-siegen.de/~geuer-pollmann/id2.xml",
         "data/org/apache/xml/security/temp/id2.xml", "text/xml");
      OfflineResolver.register(
         "http://xmldsig.pothole.com/xml-stylesheet.txt",
         "data/com/pothole/xmldsig/xml-stylesheet.txt", "text/xml");
   }
}
