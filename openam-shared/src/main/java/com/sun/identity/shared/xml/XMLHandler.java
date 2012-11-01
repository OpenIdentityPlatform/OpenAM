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
 * $Id: XMLHandler.java,v 1.2 2008/06/25 05:53:08 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.shared.xml;

import java.io.InputStream;
import java.io.StringReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is a custom XML handler to load the dtds from the classpath This should
 * be used by all the xml parsing document builders to set the default entity
 * resolvers. This will avoid to have the dtds having specified in a fixed
 * directory that will get replaced during installation This will need to
 * specify the dtds as follows jar://com/sun/identity/sm/sms.dtd Bundle all the
 * dtds along with the jar files and
 */
public class XMLHandler extends DefaultHandler {

    public InputSource resolveEntity(String aPublicID, String aSystemID) {
        String sysid = aSystemID.trim();

        if (sysid.toLowerCase().startsWith("jar://")) {
            String dtdname = sysid.substring(5);
            InputStream is = getClass().getResourceAsStream(dtdname);
            if (is != null) {
                return new InputSource(is);
            }
        }

        //make sure that we do NOT return null here, as xerces would
        //fall back to the default entity resolver and try to resolve the
        //entity with that
        return new InputSource(new StringReader(""));
    }
}
