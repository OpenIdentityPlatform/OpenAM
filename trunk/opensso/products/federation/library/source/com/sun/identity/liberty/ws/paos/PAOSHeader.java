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
 * $Id: PAOSHeader.java,v 1.4 2008/06/25 05:47:20 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.paos; 

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

/**
 * The <code>PAOSHeader</code> class is used by a web application
 * on HTTP server side to parse a <code>PAOS</code> header in an HTTP request
 * from the user agent side. This header is used by the User Agent
 * as a <code>PAOS</code> server to publish which services are available, which
 * <code>PAOS</code> versions are supported, etc..
 *
 * An example <code>PAOS</code> header looks like the following:
 * <pre>
 * PAOS: ver="ver1","ver2",ext="ext1","ext2";"service1","opt11",
 *           "opt12";"service2","opt21","opt22"
 * </pre>
 *
 * This class has methods for obtaining all the parts inside such
 * a header.
 *
 * @supported.all.api
 */
public class PAOSHeader {

    private String paosHeaderStr = null;

    private ArrayList versionList = new ArrayList();
    private ArrayList extensionList = new ArrayList();
    
    private static final String PAOS_HEADER = "PAOS";

    // keys are strings and value are sets (set elements are
    // Strings)
    private HashMap servicesAndOptions = new HashMap();

    /**
     * This constructor accepts an <code>HttpServletRequest</code>
     * and tries to parse the <code>PAOS</code> header string if there is one
     * inside.
     *
     * @param req the incoming HTTP request which is supposed
     *            to contain the <code>PAOS</code> header.
     * @throws PAOSException if there are any parsing errors because
     *            the <code>PAOS</code> header is not there at all or
     *            because its content is not compliant to the
     *            <code>PAOS</code> specifications.
     */
    public PAOSHeader(HttpServletRequest req) throws PAOSException {
        paosHeaderStr = req.getHeader(PAOS_HEADER);
        if (PAOSUtils.debug.messageEnabled()) {
            PAOSUtils.debug.message("PAOSHeader.PAOSHeader: PAOS Header = " +
                paosHeaderStr);
        }
        if (paosHeaderStr != null) {
            paosHeaderStr = paosHeaderStr.trim();
        } else {
            PAOSUtils.debug.error(
                    "PAOSHeader:PAOSHeader: No POAS header.");
            throw new PAOSException(
                    "There is no PAOS header.");
        }
        parse();
    }
    
    /**
     * This constructor accepts a <code>PAOS</code> header string and tries to
     * parse it. 
     *
     * @param paosHeaderString the <code>PAOS</code> header string which
     *        supposedly contains information on available services, etc..
     * @throws PAOSException if there are any parsing error because the
     *            <code>PAOS</code> header is invalid.
     */
    public PAOSHeader(String paosHeaderString) throws PAOSException {
        if (paosHeaderStr != null) {
            paosHeaderStr = paosHeaderStr.trim();
        } else {
            PAOSUtils.debug.error(
                    "PAOSHeader:PAOSHeader: No POAS header.");
            throw new PAOSException(
                    "There is no PAOS header.");
        }
        parse();
    }
    
    /**
     * Parses the <code>PAOS</code> Header.
     */
    private void parse() throws PAOSException {
	
	if (paosHeaderStr.length() == 0) {
	    PAOSUtils.debug.error(
		"PAOSHeader:PAOSHeader: Null POAS header.");
	    throw new PAOSException(
		"PAOS header value is empty"); 
	}
	
	StringTokenizer st1 = new 
	    StringTokenizer(paosHeaderStr, ";");
	
	int n = st1.countTokens();
	
	String versExts = st1.nextToken().trim();
	
	int indexOfExt = versExts.indexOf("ext=");
	
	String vers = null;
	
	if (indexOfExt < 0) {
	    vers = versExts;
	} else {
	    vers = versExts.substring(0, indexOfExt).trim();
	}
	String versions = null;
	
	if (vers.startsWith("ver=")) {
	    versions = vers.substring(4).trim();
	} else {
	    versions = vers;
	}

	StringTokenizer st3 = new StringTokenizer(versions, ",");
	
	while (st3.hasMoreTokens()) {
	    versionList.add(trimQuotes(st3.nextToken().trim()));
	}
	
	if (indexOfExt >= 0) {
	    
	    String extensions = versExts.substring(indexOfExt+4).trim();
	    
	    StringTokenizer st4 = new StringTokenizer(extensions, ",");
	    while (st4.hasMoreTokens()) {
		extensionList.add(trimQuotes(st4.nextToken().trim()));
	    }
	}
	String servAndOpt = null;
	StringTokenizer st5 = null;
	String serv = null;
	HashSet optSet = null;
	while (st1.hasMoreTokens()) {
	    
	    servAndOpt = st1.nextToken();
	    
	    st5 = new StringTokenizer(servAndOpt, ",");
	    
	    if (st5.hasMoreTokens()) {
		serv = trimQuotes(st5.nextToken().trim());
	    }
	    if (st5.hasMoreTokens()) {
		optSet = new HashSet();
		while (st5.hasMoreTokens()) {
		    optSet.add(trimQuotes(st5.nextToken().trim()));
		}
	    }
	    servicesAndOptions.put(serv, optSet);
	    optSet = null;
	}
    }
    
    private String trimQuotes(String inStr) {
	
	if ((inStr.startsWith("\"") && inStr.endsWith("\"")) ||
            (inStr.startsWith("'") && inStr.endsWith("'"))) {
	    return inStr.substring(1, inStr.length()-1);
	}
	return inStr;
    }
    
    /**
     * Returns the list of versions as <code>String</code>s.
     *
     * @return the list of versions as <code>String</code>s.
     */
    public Iterator getVersions() {
	return versionList.iterator();
    }
    
    /**
     * Returns the list of extensions as <code>String</code>s.
     *
     * @return the list of extensions as <code>String</code>s.
     */
    public Iterator getExtensions() {
	return extensionList.iterator();
    }

    /**
     * Returns a <code>HashMap</code> containing the services and
     * corresponding options.
     *
     * @return a <code>HashMap</code> with each key being a service represented
     *         as a <code>String</code>, and with each value being a
     *         <code>Set</code> of the corresponding options represented
     *         as <code>String</code>s
     */
     public HashMap getServicesAndOptions() { 
	return servicesAndOptions;
    }
}
