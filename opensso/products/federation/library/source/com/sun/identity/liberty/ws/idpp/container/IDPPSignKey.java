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
 * $Id: IDPPSignKey.java,v 1.2 2008/06/25 05:47:16 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.idpp.container;

import javax.xml.bind.JAXBException;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import org.w3c.dom.Document;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.liberty.ws.idpp.jaxb.*;
import com.sun.identity.liberty.ws.idpp.plugin.*;


/**
 * This class <code>IDPPSignKey</code> is an implementation of 
 * <code>IDPPContainer</code>.
 */ 

public class IDPPSignKey extends IDPPBaseContainer {

     /**
      * Constructor
      */
     public IDPPSignKey() { 
        IDPPUtils.debug.message("IDPPSignKey:constructor:init.");
     }

     /**
      * Gets the common name jaxb element 
      * @param userMap user map
      * @return InformalNameElement JAXB Object.
      * @exception IDPPException.
      */
     public Object getContainerObject(Map userMap) throws IDPPException {
        IDPPUtils.debug.message("IDPPSignKey:getContainerObject:Init");
        try {
            PPType ppType = IDPPUtils.getIDPPFactory().createPPElement();
            SignKeyElement signKey = 
                IDPPUtils.getIDPPFactory().createSignKeyElement();
            byte[][] certBytes = (byte[][]) userMap.get(
            getAttributeMapper().getDSAttribute(
            IDPPConstants.SIGN_KEY_ELEMENT).toLowerCase());

            if(IDPPUtils.debug.messageEnabled()) {
               IDPPUtils.debug.message("IDPPSignKey.getContainerObject: " +
                "SignKey Value" + certBytes);
            }
            com.sun.identity.liberty.ws.common.jaxb.xmlsig.ObjectFactory of =
            new com.sun.identity.liberty.ws.common.jaxb.xmlsig.ObjectFactory();

            com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataType 
            x509DataType = of.createX509DataElement();

            com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataType.
            X509Certificate cert = of.createX509DataTypeX509Certificate(
              certBytes[0]);

            x509DataType.
            getX509IssuerSerialOrX509SKIOrX509SubjectName().add(cert);

            signKey.getContent().add(x509DataType);

            ppType.setSignKey(signKey);
            return ppType;
        } catch (JAXBException je) {
            IDPPUtils.debug.error(
            "IDPPContainers:getInformalName: JAXB failure", je); 
            throw new IDPPException(
            IDPPUtils.bundle.getString("jaxbFailure"));
        }
     }

     /**
      * Gets required common name container attributes.
      * @return Set set of required container attributes 
      */
     public Set getContainerAttributes() {
        IDPPUtils.debug.message("IDPPSignKey:getContainerAttributes:Init");
        Set set = new HashSet();
        set.add(IDPPConstants.SIGN_KEY_ELEMENT);
        return getMapperAttributeSet(set);
     }

     /**
      * Gets the container attributes for a given select expression.
      * @param  select Select expression.
      * @return Set set of required user attributes.
      */
     public Set getContainerAttributesForSelect(String select) {
         return getContainerAttributes();
     }

     /**
      * Processes modify container values and returns key value/pair to 
      * be modified.
      * @param select Select expression.
      * @param data list of new data objects.
      * @return Attribute key value pair for the given select.
      * @exception IDPPException.
      */
     public Map getDataMapForSelect(String select, List data) 
     throws IDPPException {

        IDPPUtils.debug.message("IDPPSignKey:getDataMapForSelect:Init");
        Object dataElement = null;
        if(data != null && !data.isEmpty()) {
           dataElement = data.get(0);
        }

        HashMap map = new HashMap();

        if(dataElement == null) { 
           map.put(getAttributeMapper().getDSAttribute(
               IDPPConstants.SIGN_KEY_ELEMENT), null);
           return map;
        }

        if(dataElement instanceof SignKeyElement) {
           byte[] certBytes = null; 
           SignKeyElement signKey = (SignKeyElement)dataElement;
           List contents = signKey.getContent();

           if(contents == null || contents.size() == 0) {
              return getAttributeMap(
              IDPPConstants.SIGN_KEY_ELEMENT, null, map); 
           }

           Iterator iter = contents.iterator();

           while(iter.hasNext()) {
             Object obj = iter.next();
             if(obj instanceof 
                com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataElement){

                com.sun.identity.liberty.ws.common.jaxb.xmlsig.X509DataElement
                x509Data = (com.sun.identity.liberty.ws.common.jaxb.
                            xmlsig.X509DataElement)obj;
                List certs = 
                     x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName(); 

                if(certs == null || certs.size() == 0) {
                   IDPPUtils.debug.error("IDPPSignKey.getDataMapForSelect:" +
                   "Unsupported data. certs are null");
                   return null;
                }

                Object certObj = certs.get(0);
                if(certObj instanceof 
                   com.sun.identity.liberty.ws.common.jaxb.xmlsig.
                   X509DataType.X509Certificate) {
                   com.sun.identity.liberty.ws.common.jaxb.xmlsig.
                   X509DataType.X509Certificate cert = 
                   (com.sun.identity.liberty.ws.common.jaxb.xmlsig.
                    X509DataType.X509Certificate)certObj;
                    certBytes =  cert.getValue();
                } else {
                   IDPPUtils.debug.error("IDPPSignKey.getDataMapForSelect:" +
                   "Unsupported data. not x509 certificate");
                   return null;
                }
                break;
             } else {
                IDPPUtils.debug.error("IDPPSignKey.getDataMapForSelect:" +
                "not x509data element");
                continue;
             }
          }

          byte[][] attributeByte = new byte[1][certBytes.length];
          for(int i=0; i < certBytes.length; i++) {
              attributeByte[0][i] = certBytes[i];
          }

          map.put(getAttributeMapper().getDSAttribute(
          IDPPConstants.SIGN_KEY_ELEMENT), attributeByte); 
          return map;

        } else {
           throw new IDPPException(
           IDPPUtils.bundle.getString("invalid Element"));
        }

     }

     /**
      * Checks if there are any binary attributes.
      */
     public boolean hasBinaryAttributes() {
         return true;
     }
}
