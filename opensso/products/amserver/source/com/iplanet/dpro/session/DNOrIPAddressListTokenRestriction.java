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
 * $Id: DNOrIPAddressListTokenRestriction.java,v 1.7 2009/10/29 17:33:29 ericow Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.dpro.session;

import com.iplanet.am.util.Misc;
import com.iplanet.am.util.Debug;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map;

/**
 * <code>DNOrIPAddressListTokenRestriction</code> implements
 * <code>TokenRestriction</code> interface and handles the restriction of
 * the <code>DN</code> or <code>IPAddress</code>
 */

public class DNOrIPAddressListTokenRestriction implements TokenRestriction {

    static final long serialVersionUID = 8352965917649287133L;
    private String dn;
    private static Debug debug;

    private Set addressList = new HashSet();

    private String asString;
     /**
      * boolean to indicate if the restriction checking is strictly based on DN
      * or not during cookie hijacking mitigation mode.
      * By default if DN is absent or cannot be determined,restriction is 
      * set to IP address of the client. This property if not defined in
      * is assumed false.
      * If strict DN checking is desired this property needs to be defined
      * with value "true"
      */
    private static boolean dnRestrictionOnly;

    private static final String SESSION_DNRESTRICTIONONLY_ATTR_NAME =
        "iplanet-am-session-dnrestrictiononly";

    private static final String AM_SESSION_SERVICE = "iPlanetAMSessionService";
    private static SSOToken adminToken = null;

   static {
       debug = Debug.getInstance("amSession");
       dnRestrictionOnly = getDNRestrictionOnly();
       if (debug.messageEnabled()) {
           debug.message(
               "DNOrIPAddressListTokenRestriction"
             +": fetching value for dnRestrictionOnly:"+
              dnRestrictionOnly);
       }
   }

   /**
    * Constructs <code>DNOrIPAddressListTokenRestriction</code> object based on
    * the <code>DN</code> and list of host names to be restricted.
    * @param dn the <code>DN</code> of the user
    * @param hostNames list of host names.
    * @exception Exception if finding IP Address of host to be restricted or
    *            if something goes wrong.
    */
   public DNOrIPAddressListTokenRestriction(String dn, List hostNames)
        throws Exception
    {
        String val = ""; 
        boolean hostmatch = false;    
        for (Iterator i = hostNames.iterator(); i.hasNext();) {
            try { 
                val = (String)i.next();  
                addressList.add(InetAddress.getByName(val)); 
                hostmatch = true; 
            } catch (java.net.UnknownHostException e) { 
                if (SessionService.sessionDebug.warningEnabled()) { 
                    SessionService.sessionDebug.warning(
                    "DNOrIPAddressListTokenRestriction.constructor: " +
                    "failure resolving host " + val); 
                } 
                if (!i.hasNext() && !hostmatch) {
                    throw new java.net.UnknownHostException (val); 
                }
            } 
        }

        StringBuffer buf = null;
        if (dn.indexOf("|") > 0) {
            StringTokenizer st = new StringTokenizer(dn, "|");
            while(st.hasMoreTokens()) {
                if (buf == null) {
                    buf = new StringBuffer(Misc.canonicalize(st.nextToken()));
                } else {
                    buf.append("|").append(Misc.canonicalize(st.nextToken()));
                }
            }
        } else {
            buf = new StringBuffer(Misc.canonicalize(dn));
        }
        this.dn = buf.toString();
        buf.append("\n");
        Object[] sortedAddressList = addressList.toArray();
        Arrays.sort(sortedAddressList, addressComparator);
        for (int i = 0; i < sortedAddressList.length; i++) {
            buf.append(((InetAddress) sortedAddressList[i]).getHostAddress());
            buf.append("\n");
        }
        asString = buf.toString();
        if (debug.messageEnabled()) {
            debug.message("DNOrIPAddressListTokenRestriction.new " + asString);
        }
    }

    private static Comparator addressComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ((InetAddress) o1).getHostAddress().compareTo(
                    ((InetAddress) o2).getHostAddress());
        }
    };

    /**
     * This method returns the restriction as a string.
     * 
     * @return A concatenated string of DN and/or Host Name/IP Address.
     */
    public String toString() {
        return asString;
    }

    /**
     * Returns a hash code for this object.
     * 
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Returns a true if the restriction matches the context for which it was
     * set.
     * 
     * @param context The context from which the restriction needs to be
     *        checked. The context can be any from the following - the Single
     *        Sign on token of the Application against which the restriction
     *        is being compared - the IP Address/Host Name of the Application
     *        against which the restriction is being compared
     * @return true if the restriction is satisfied.
     * @throws Exception is thrown if the there was an error.
     */
    public boolean isSatisfied(Object context) throws Exception {
        if (context == null) {
            return false;
        } else if (context instanceof SSOToken) {
            if (SessionService.sessionDebug.messageEnabled()) {
                SessionService.sessionDebug.message(
                   "DNOrIPAddressListTokenRestriction"
                   +".isSatisfied(): context is instance of SSOToken");
            }
            SSOToken usedBy = (SSOToken) context;
            String udn = Misc.canonicalize(usedBy.getPrincipal().getName());
            StringTokenizer st = new StringTokenizer(dn, "|");
            while(st.hasMoreTokens()) {
                if (st.nextToken().equals(udn)) {
                    return true;
                }
            }

            if (debug.messageEnabled()) {
                debug.message("DNOrIPAddressListTokenRestriction:isSatisfied SSOToken of " + udn + " does not match with restriction " + dn);
            }
            return false;
        } else if (context instanceof InetAddress) {
            if (dnRestrictionOnly) {
                SessionService.sessionDebug.error(
                     "DNOrIPAddressListTokenRestriction"
                    +".isSatisfied():dnRestrictionOnly"
                    +" is true, hence cannot accept passed IP as"
                    +" restriction");
                return false;
            } else {
                if (SessionService.sessionDebug.messageEnabled()) {
                    SessionService.sessionDebug.message(
                         "DNOrIPAddressListTokenRestriction"
                        +".isSatisfied(): dnRestrictionOnly is false");
                    SessionService.sessionDebug.message(
                         "DNOrIPAddressListTokenRestriction"
                        +".isSatisfied(): IP based"
                        +" restriction received and accepted");
                }
                return addressList.contains(context);
            }
        } else {
            if (SessionService.sessionDebug.warningEnabled()) {
                SessionService.sessionDebug.warning("Unknown context type:"
                        + context);
            }
            return false;
        }
    }

    /**
     * Returns true of <code>other</code> meets these criteria.
     * <ol type="1">
     * <li>it is not null;
     * <li>it is an instance of <code>DNOrIPAddressListTokenRestriction</code>;
     * <li>it has the same distinguished name as this object; and
     * <li>it has the same set of IP addresses as this object.
     * </ol>
     * 
     * @param other the object to be used for comparison.
     * @return true if <code>other</code> meets the above criteria.
     */
    public boolean equals(Object other) {
        return other != null
                && (other instanceof DNOrIPAddressListTokenRestriction)
                && other.toString().equals(this.toString());
    }

   /*
    * Gets the admin token for checking the dn restriciton property
    * @return admin <code>SSOTken</code>
    */
    static SSOToken getAdminToken() {
        if (adminToken == null) {
            try {
                adminToken = (SSOToken) AccessController.doPrivileged(
                     AdminTokenAction.getInstance());
            } catch (Exception e) {
                SessionService.sessionDebug.error(
                   "Failed to get the admin token for "
                        + "dnRestrictionOnly property checking.", e);
            }
        }
        return adminToken;
    }

    static SessionService getSS() {
        SessionService ss = SessionService.getSessionService();
        if (ss == null) {
            SessionService.sessionDebug.error(
                "DNOrIPAddressListTokenRestriction: "
                    + " Failed to get the session service instance");
        }
        return ss;
    }

    /*
     * Gets the  value of the "iplanet-am-session-dnrestrictiononly"
     * session global attribute.
     */
    private static boolean getDNRestrictionOnly() {
        boolean dnRestrictionOnly = false;
        try {
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                    AM_SESSION_SERVICE, getAdminToken());
            ServiceSchema schema = ssm.getGlobalSchema();
            Map attrs = schema.getAttributeDefaults();
            dnRestrictionOnly = Boolean.valueOf(
                        CollectionHelper.getMapAttr(
                        attrs,  SESSION_DNRESTRICTIONONLY_ATTR_NAME, "false")
                        ).booleanValue();
        } catch (Exception e) {
            if (SessionService.sessionDebug.messageEnabled()) {
                SessionService.sessionDebug.message(
                        "Failed to get the default dnRestrictionOnly"
                        + "setting. => Set  dnRestrictionOnly to "
                        + "false", e);
            }
        }
        return dnRestrictionOnly;
    }
}
