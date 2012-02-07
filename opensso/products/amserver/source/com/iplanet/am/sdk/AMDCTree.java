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
 * $Id: AMDCTree.java,v 1.5 2008/06/25 05:41:19 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;


import com.iplanet.am.sdk.common.IDCTreeServices;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class <code>AMDCTree</code> contains the functionality to support a DC
 * Tree in the LDAP DIT. The methods of this class will be used by other classes
 * in com.iplanet.dpro.sdk package.
 * <p>
 * 
 * In order to determine if DC Tree support is required or not, the parameter
 * <code>com.iplanet.am.dctree</code> will be verified. A value of
 * <code>true</code> for this parameter, means DC Tree support is required
 * <p>
 * 
 * NOTE: An explicit check must be performed using AMDCTree.isRequired() method
 * before calling any other methods in this class.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
class AMDCTree implements AMConstants {

    protected static ServiceSchema gsc = null;

    public static final String IPLANET_DOMAIN_NAME_ATTR = "sunPreferredDomain";

    public static final String INET_CANONICAL_DOMAIN = 
        "inetcanonicaldomainname";

    public static final String DOMAIN_BASE_DN = "inetDomainBaseDN";

    private static Map domainMap;

    private static Map canonicalDomainMap;

    private static Debug debug = AMCommonUtils.debug;

    private static IDCTreeServices dcTreeServices = 
        AMDirectoryAccessFactory.getDCTreeServices();

    static {
        domainMap = new HashMap();
        canonicalDomainMap = new HashMap();
    }

    /**
     * Method to determine if DC Tree support is required or not.
     * 
     * @return true if DC Tree support required, false otherwise
     */
    protected static boolean isRequired() throws AMException {
        try {
            if (AMCompliance.gsc == null) {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        ADMINISTRATION_SERVICE, (SSOToken) AccessController
                                .doPrivileged(AdminTokenAction.getInstance()));
                AMCompliance.gsc = scm.getGlobalSchema();
            }
            Map attrMap = AMCompliance.gsc.getAttributeDefaults();
            Set values = (Set) attrMap.get(DCT_ENABLED_ATTR);
            boolean required = false;
            if (values == null || values.isEmpty()) {
                required = false;
            } else {
                String val = (String) values.iterator().next();
                required = (val.equalsIgnoreCase("true"));
            }
            return required;
        } catch (SMSException ex) {
            debug.error(AMSDKBundle.getString("354"), ex);
            throw new AMException(AMSDKBundle.getString("354"), "354");
        } catch (SSOException ex) {
            debug.error(AMSDKBundle.getString("345"), ex);
            throw new AMException(AMSDKBundle.getString("354"), "354");
        }

    }

    protected static String getOrganizationDN(SSOToken token, String domainName)
            throws AMException {
        return dcTreeServices.getOrganizationDN(token, domainName);
    }

    protected static AttrSet[] splitAttrSet(String orgDN, AttrSet attrSet)
            throws AMException {
        AttrSet attrArray[] = new AttrSet[2];
        attrArray[0] = new AttrSet();
        attrArray[1] = new AttrSet();
        if (attrSet == null) {
            return (attrArray);
        }
        Set dcNodeAttrs = dcNodeAttributes();
        Iterator it = dcNodeAttrs.iterator();
        while (it.hasNext()) {
            String aName = (String) it.next();
            if (aName.indexOf("objectclass=") > -1) {
                Attr attr = attrSet.getAttribute("objectclass");
                String oc = aName.substring("objectclass=".length());
                Attr dcAttr = new Attr("objectclass");
                if (attr != null && attr.contains(oc)) {
                    attr.removeValue(oc);
                    dcAttr.addValue(oc);
                    attrSet.replace(attr);
                    attrArray[1].add(dcAttr);
                }
            } else {
                Attr attr = attrSet.getAttribute(aName);
                if (attr != null) {
                    attrArray[1].add(attr);
                    attrSet.remove(aName);
                }
            }
        }
        attrArray[0] = attrSet;
        if (debug.messageEnabled()) {
            debug.message("AMCompliance.splitAttrSet: " + "domain attrset = "
                    + attrArray[1].toString());
            debug.message("AMCompliance.splitAttrSet: "
                    + "non-domain attrset = " + attrArray[0].toString());
        }
        return attrArray;
    }

    /**
     * This is a protected method used by the notification event listener thread
     * to clean the domain map, when organization entry is changed.
     * 
     * @param canonOrgDN
     *            organization DN
     */
    protected static void cleanDomainMap(String canonOrgDN) {
        synchronized (canonicalDomainMap) {
            canonicalDomainMap.remove(canonOrgDN);
        }
        synchronized (domainMap) {
            domainMap.remove(canonOrgDN);
        }
    }

    private static Set dcNodeAttributes() throws AMException {
        try {
            if (gsc == null) {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        ADMINISTRATION_SERVICE, (SSOToken) AccessController
                                .doPrivileged(AdminTokenAction.getInstance()));
                gsc = scm.getGlobalSchema();
            }
            Map attrMap = gsc.getAttributeDefaults();
            Set values = (Set) attrMap.get(DCT_ATTRIBUTE_LIST_ATTR);
            if (values == null) {
                if (debug.messageEnabled()) {
                    debug.message("DCTree.dcNodeAttributes = null");
                }
                return Collections.EMPTY_SET;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("DCTree.dcNodeAttributes = "
                            + values.toString());
                }
                return values;
            }
        } catch (SMSException ex) {
            debug.error(AMSDKBundle.getString("908"), ex);
            throw new AMException(AMSDKBundle.getString("908"), "908");
        } catch (SSOException ex) {
            debug.error(AMSDKBundle.getString("902"), ex);
            throw new AMException(AMSDKBundle.getString("902"), "902");
        }

    }

}
