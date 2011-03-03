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
 * $Id: IDPPContainer.java,v 1.2 2008/06/25 05:47:17 qcheng Exp $
 *
 */

package com.sun.identity.liberty.ws.idpp.plugin;

import com.sun.identity.liberty.ws.idpp.jaxb.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import com.sun.identity.liberty.ws.idpp.common.IDPPException;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * The interface <code>IDPPContainer</code> provides a plugin for the
 * IDPP containers. Each IDPP container needs to implement this interface
 * for those containers that the Personal profile supports.
 */

public interface IDPPContainer {


     /**
      * Converts all the supported container values into an XML document.
      * @param userMap user data map
      * @return Document XML representation of container.
      * @exception IDPPException. 
      */
     public Document toXMLDocument(Map userMap) throws IDPPException;

     /**
      * Gets the supported container attributes.
      * @return Set set of supported container attributes.
      */
     public Set getContainerAttributes();

     /**
      * Gets the container attributes for a select expression.
      * @param select select string.
      * @return Set set of container attributes for the given select.
      */
     public Set getContainerAttributesForSelect(String select); 
 
     /**
      * Gets the data map for a given select and the data.
      * @param select select expression.
      * @param data list of new data objects.
      * @return Map Attribute value pair for the given select and data.
      * @exception IDPPException.
      */
     public Map getDataMapForSelect(String select, List data)
     throws IDPPException;

     /**
      * Checks if the container has any binary attributes.
      * @return true if any of the container attributes are binary.
      */
     public boolean hasBinaryAttributes();


     /**
      * Sets the UserDN for the container that needs to be built by using
      * this user attributes.
      * @param userDN User's DN. 
      */
     public void setUserDN(String userDN);

}
