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
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.iplanet.dpro.session;

import com.iplanet.am.util.Misc;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * <code>DNOrIPAddressListTokenRestriction</code> implements
 * {@link TokenRestriction} interface and handles the restriction of
 * the <code>DN</code> or <code>IPAddress</code>
 */

public class DNOrIPAddressListTokenRestriction implements TokenRestriction {

    static final long serialVersionUID = 8352965917649287133L;
    private String dn;
    private static Debug debug;

    private Set<InetAddress> addressList = new HashSet<InetAddress>();

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
    public DNOrIPAddressListTokenRestriction(String dn, List<String> hostNames) throws Exception {

        StringBuilder buf = null;
        if (dn.indexOf('|') > 0) {
            StringTokenizer st = new StringTokenizer(dn, "|");
            while (st.hasMoreTokens()) {
                if (buf == null) {
                    buf = new StringBuilder(Misc.canonicalize(st.nextToken()));
                } else {
                    buf.append('|').append(Misc.canonicalize(st.nextToken()));
                }
            }
        } else {
            buf = new StringBuilder(Misc.canonicalize(dn));
        }
        this.dn = buf.toString();

        if (!dnRestrictionOnly) {
            boolean hostmatch = false;
            Iterator<String> it = hostNames.iterator();
            while (it.hasNext()) {
                String val = it.next();
                try {
                    addressList.add(InetAddress.getByName(val));
                    hostmatch = true;
                } catch (UnknownHostException e) {
                    if (SessionService.sessionDebug.warningEnabled()) {
                        SessionService.sessionDebug.warning(
                                "DNOrIPAddressListTokenRestriction.constructor: "
                                + "failure resolving host " + val);
                    }
                    if (!it.hasNext() && !hostmatch) {
                        throw new UnknownHostException(val);
                    }
                }
            }
        }
        buf.append('\n');
        Collections.sort(hostNames);
        for (String hostName : hostNames) {
            buf.append(hostName).append('\n');
        }
        asString = buf.toString();
        if (debug.messageEnabled()) {
            debug.message("DNOrIPAddressListTokenRestriction.new " + asString);
        }
    }

    /**
     * This method returns the restriction as a string.
     * 
     * @return A concatenated string of DN and/or Host Name/IP Address.
     */
    @Override
    public String toString() {
        return asString;
    }

    /**
     * Returns a hash code for this object.
     * 
     * @return a hash code value for this object.
     */
    @Override
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
                //returning true here lessens the security, but truth to be told
                //sessionservice endpoint should not be accessible externally
                if (SessionService.sessionDebug.warningEnabled()) {
                    SessionService.sessionDebug.warning(
                            "DNOrIPAddressListTokenRestriction.isSatisfied():"
                            + "dnRestrictionOnly is true, but IP has been received "
                            + "as the restriction context, this could be a "
                            + "suspicious activity. Received InetAddress is: "
                            + ((InetAddress) context).toString());
                }
                return true;
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
                return addressList.contains((InetAddress) context);
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
     * <li>it is not null
     * <li>it is an instance of {@link DNOrIPAddressListTokenRestriction}
     * <li>it has the same distinguished name as this object and
     * <li>it has the same set of IP addresses as this object.
     * </ol>
     * 
     * @param other the object to be used for comparison.
     * @return true if <code>other</code> meets the above criteria.
     */
    @Override
    public boolean equals(Object other) {
        return other != null
                && (other instanceof DNOrIPAddressListTokenRestriction)
                && other.toString().equals(this.toString());
    }

   /**
    * Gets the admin token for checking the dn restriciton property
    * @return admin the admin {@link SSOToken}
    */
    static SSOToken getAdminToken() {
        try {
            return (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
        } catch (Exception e) {
            SessionService.sessionDebug.error(
                    "Failed to get the admin token for "
                    + "dnRestrictionOnly property checking.", e);
        }
        return null;
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

    /**
     * Gets the  value of the "iplanet-am-session-dnrestrictiononly"
     * session global attribute.
     *
     * @return whether the DN restriction only is enabled
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
                    attrs, SESSION_DNRESTRICTIONONLY_ATTR_NAME, "false")).booleanValue();
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
