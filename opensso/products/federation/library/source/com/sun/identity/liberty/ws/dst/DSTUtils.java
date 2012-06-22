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
 * $Id: DSTUtils.java,v 1.3 2008/08/06 17:28:09 exu Exp $
 *
 */


package com.sun.identity.liberty.ws.dst;

import java.util.ResourceBundle;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import javax.xml.namespace.QName;
import java.util.StringTokenizer;
import com.sun.identity.liberty.ws.common.Status;

/**
 * This class <code>DSTUtils</code> is an utility class used by the DST layer
 * and also can be used by the any dependent services.
 */ 

public class DSTUtils {

    public static Debug debug = Debug.getInstance("libIDWSF");
    public static ResourceBundle bundle =
            Locale.getInstallResourceBundle("libDSTService");
    public DSTUtils() {}

    /**
     * Parses an XML input string.
     * @param xml xmlString.
     * @return Element the root element of a parse xml string.
     */ 
    public static Element parseXML(String xml) throws DSTException {
        try {
            Document doc = XMLUtils.toDOMDocument(xml, debug);
            return doc.getDocumentElement();
        } catch (Exception ex) {
            debug.error("DSTUtils.parseXML: Parsing error.", ex);
            throw new DSTException(ex);
        }
    }

    public static Status parseStatus(Element element) throws DSTException {
        if(element == null) {
           debug.error("DSTUtils.parseStatus: nullInputParams");
           throw new DSTException(bundle.getString("nullInputParams"));
        }
        
        String nameSpaceURI = element.getNamespaceURI();
        String prefix = element.getPrefix();
        Status status = new Status(nameSpaceURI, prefix);
        String code = element.getAttribute("code");
        if(code != null && code.length() != 0) {
            String localPart = null;
            String codePrefix = "";
            if(code.indexOf(":") != -1) {

               StringTokenizer st = new StringTokenizer(code, ":");
               if(st.countTokens() != 2) {
                  throw new DSTException(bundle.getString("invalidStatus"));
               }
               codePrefix = st.nextToken();
               localPart = st.nextToken();
            } else {
               localPart = code;
            }

            QName qName = new QName(nameSpaceURI, localPart, codePrefix);
            status.setCode(qName);

        } else {
            throw new DSTException(bundle.getString("invalidStatus"));
        }
        status.setComment(element.getAttribute("comment"));
        status.setRef(element.getAttribute("ref"));
        return status;
    }
}
