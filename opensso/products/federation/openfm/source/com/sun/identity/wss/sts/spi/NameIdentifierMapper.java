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
 * $Id: NameIdentifierMapper.java,v 1.3 2008/06/25 05:50:15 qcheng Exp $
 *
 */
package com.sun.identity.wss.sts.spi;

/**
 * The interface <code>NameIdentifierMapper</code> is used to map the
 * real user identity to the psuedo name and vice versa.
 */
public interface NameIdentifierMapper {
     /**
      * Returns the user psuedo name for a given user ID.
      * @param userid user ID or name for which psuedo name to be retrieved.
      * @return the user psuedo name for a given user ID. 
      */
     String getUserPsuedoName(String userid);

     /**
      * Returns the user ID for a given psuedo name.
      * @param psuedoName the user psuedo name. 
      * @return the user ID for a given psuedo name.
      */
     String getUserID(String psuedoName);
}
