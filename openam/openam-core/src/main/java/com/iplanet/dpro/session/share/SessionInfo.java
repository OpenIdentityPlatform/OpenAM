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
 * $Id: SessionInfo.java,v 1.3 2008/06/25 05:41:31 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session.share;

import java.util.Enumeration;
import java.util.Hashtable;

import com.sun.identity.shared.xml.XMLUtils;

/**
 * <code>SessionInfo</code> class holds all the information about the 
 * <code>Session</code>
 *
 */
public class SessionInfo {

    /** <code>Session</code> id */
     public String sid;

    /** <code>Session</code> type */
     public String stype;

    /** <code>Cookie</code> id */
     public String cid;

    /** <code> Cookie</code> domain */
     public String cdomain;

    /** Max <code>Session</code> Time */
     public String maxtime;

    /** Max <code>Session</code> Idle time */
     public String maxidle;

    /** Max <code>Session</code> Cache */
     public String maxcaching;

    /** <code>Session</code> idle time */
     public String timeidle;

    /** Time left for <code>Session</code> to become inactive */
     public String timeleft;

    /** <code>Session</code> state */
     public String state;

    public Hashtable properties = new Hashtable();

    static final String QUOTE = "\"";

    static final String NL = "\n";

   /**
    *
    * Constructs <code> SessionInfo </code>
    */
   public SessionInfo() {
    }

   /**
    * translates the <code>Session</code> Information to an XML document 
    * String based
    * @return An XML String representing the information
    */
   public String toXMLString() {
        StringBuilder xml = new StringBuilder(200);
        xml.append("<Session sid=").append(QUOTE).append(sid).append(QUOTE)
                .append(" stype=").append(QUOTE).append(stype).append(QUOTE)
                .append(" cid=").append(QUOTE).append(
                        XMLUtils.escapeSpecialCharacters(cid)).append(QUOTE)
                .append(" cdomain=").append(QUOTE).append(
                        XMLUtils.escapeSpecialCharacters(cdomain))
                .append(QUOTE).append(" maxtime=").append(QUOTE)
                .append(maxtime).append(QUOTE).append(" maxidle=")
                .append(QUOTE).append(maxidle).append(QUOTE).append(
                        " maxcaching=").append(QUOTE).append(maxcaching)
                .append(QUOTE).append(" timeidle=").append(QUOTE).append(
                        timeidle).append(QUOTE).append(" timeleft=").append(
                        QUOTE).append(timeleft).append(QUOTE).append(" state=")
                .append(QUOTE).append(state).append(QUOTE).append(">").append(
                        NL);

        if (properties != null) {
            Enumeration enumerator = properties.keys();
            while (enumerator.hasMoreElements()) {
                String name = (String) enumerator.nextElement();
                String value = (String) properties.get(name);
                xml.append("<Property name=").append(QUOTE).append(
                        XMLUtils.escapeSpecialCharacters(name)).append(QUOTE)
                        .append(" value=").append(QUOTE).append(
                                XMLUtils.escapeSpecialCharacters(value))
                        .append(QUOTE).append(">").append("</Property>")
                        .append(NL);
            }
        }
        xml.append("</Session>");
        return xml.toString();
    }
}
