/*
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
 * $Id: SMSJAXRPCObjectImpl.java,v 1.22 2009/10/28 04:24:27 hengming Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems, LLC
 */

package com.sun.identity.sm.jaxrpc;

import static org.forgerock.openam.utils.Time.*;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.server.PLLServer;
import com.iplanet.services.comm.server.SendNotificationException;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.jaxrpc.JAXRPCRequestFilter;
import com.sun.identity.jaxrpc.JAXRPCUtil;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.CachedSMSEntry;
import com.sun.identity.sm.CachedSubEntries;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSNotificationManager;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SMSUtils;
import com.sun.identity.sm.ServiceAttributeValidator;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class SMSJAXRPCObjectImpl implements SMSObjectIF, SMSObjectListener {

    static Debug debug = Debug.getInstance("amSMSServerImpl");

    // Everything forLog() replaces: the ASCII control range, plus the three
    // line terminators outside it. Compiled once, since a caller decides how
    // often this runs.
    private static final Pattern UNSAFE_FOR_LOG =
        Pattern.compile("[\\p{Cntrl}\\u0085\\u2028\\u2029]");

    // What validateServiceAttributes() accepts for validation. The caller of
    // that method names the validator and supplies the values, so the work a
    // validator is asked to do is the caller's to choose, and the validators
    // are not written for input of a size a service schema never produces:
    // ResourceComparatorValidator's overlap check used to cost O(n^3) in the
    // length of one value, and the map key pattern the agent property
    // validators share O(n^2). Both are bounded now; these limits are what
    // keeps the next one from being reachable before it is found.
    //
    // They are far above anything the shipped schemas declare a validator for:
    // the largest such value is a 256 bit key in base64, 44 characters, and
    // the rest are key aliases, server URLs, port numbers and single map
    // entries.
    static final int MAX_VALUE_LENGTH = 2048;

    static final int MAX_VALUES = 1000;

    static final int MAX_TOTAL_LENGTH = 65536;

    static Map<String, URL> notificationURLs = new HashMap<String, URL>();

    static SSOTokenManager tokenMgr;

    static SSOException initializationError;

    static String baseDN;

    static String amsdkbaseDN;

    static boolean initialized;

    static String serverURL;

    // Cache of modified DNs for the last 30 minutes
    static int cacheSize = 30;

    static LinkedList cacheIndices = new LinkedList();

    static HashMap cache = new HashMap(cacheSize);

    // Default constructor
    public SMSJAXRPCObjectImpl() {
        // Empty constructor to avoid bootstraping when JAX-RPC
        // initialized this object on the Serve
    }

    // Initialization to register the callback handler
    private void initialize() {
        if (!initialized) {
            try {
                tokenMgr = SSOTokenManager.getInstance();
            } catch (SSOException ssoe) {
                debug.error("SMSJAXRPCObject: "
                        + "Unable to get SSO Token Manager");
                initializationError = ssoe;
            }
            
            // Register for notifications & polling cache
            if (SMSNotificationManager.isCacheEnabled()) {
                SMSNotificationManager.getInstance()
                    .registerCallbackHandler(this);
                if (debug.messageEnabled()) {
                    debug.message("SMSJAXRPCObjectImpl.init " +
                        "Registered for notifications");
                }
                
                // Obtain the cache size, if configured
                String cacheSizeStr = SystemProperties.get(
                    Constants.EVENT_LISTENER_REMOTE_CLIENT_BACKLOG_CACHE);
                try {
                    cacheSize = Integer.parseInt(cacheSizeStr);
                    if (cacheSize < 0) {
                        cacheSize = 10;
                    }
                } catch (NumberFormatException e) {
                    //do nothing
                }
                if (debug.messageEnabled()) {
                    debug.message("SMSJAXRPCObjectImpl.init  " +
                        "EventNotification cache size is set to " + cacheSize);
                }
            }
            
            // Construct server URL
            String namingURL = SystemProperties.get(Constants.AM_NAMING_URL);
            if (namingURL != null) {
                int index = namingURL.toLowerCase().indexOf("/namingservice");
                if (index != -1) {
                    serverURL = namingURL.substring(0, index);
                } else {
                    serverURL = "";
                }
            } else {
                serverURL = SystemProperties.getServerInstanceName();
                if (serverURL == null) {
                    serverURL = "";
                }
            }
            if ((serverURL == null) || (serverURL.length() == 0)) {
                debug.error("SMSJAXRPCObjectImpl.init Server URL IS NULL");
            } else if (debug.messageEnabled()) {
                debug.message("SMSJAXRPCObjectImpl.init ServerURL: " +
                    serverURL);
            }
            initialized = true;
        }
    }

    // Method to check if service is local and also to
    // test if the server is down
    public void checkForLocal() {
        SMSJAXRPCObject.isLocal = true;
    }

    /**
     * Returns the attribute names and values of the provided object using the
     * identity of the provided SSO Token
     */
    public Map read(String tokenID, String objName)
        throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::read dn: " + objName);
        }

        Map returnAttributes = null;
        if (objName.equals("o=" + SMSJAXRPCObject.AMJAXRPCVERSIONSTR)) {

            returnAttributes = new HashMap();
            returnAttributes.put(SMSJAXRPCObject.AMJAXRPCVERSIONSTR,
                   SMSJAXRPCObject.AMJAXRPCVERSION);
        } else {
            CachedSMSEntry ce = CachedSMSEntry.getInstance(getToken(tokenID),
                objName);
            if (ce.isDirty()) {
                ce.refresh();
            }
            Map attrs = ce.getSMSEntry().getAttributes();
            if ((attrs != null) && (attrs instanceof CaseInsensitiveHashMap)) {
                returnAttributes = new HashMap();
                for (Iterator items = attrs.keySet().iterator(); 
                    items.hasNext();) {
                    String attrName = items.next().toString();
                    Object o = attrs.get(attrName);
                    returnAttributes.put(attrName, o);
                }            
            } else { // could be null or instance of HashMap - return as it is.
                returnAttributes = attrs;
            }                             
        }
        return returnAttributes;

    }

    /**
     * Creates an entry in the persistent store. Throws an exception if the
     * entry already exists
     */
    public void create(String tokenID, String objName, Map attributes)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::create dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.setAttributes(attributes);
        entry.save();
    }

    /**
     * Modifies the attributes to the object.
     */
    public void modify(String tokenID, String objName, String mods)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::modify dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.modifyAttributes(getModItems(mods));
        entry.save();
    }

    /**
     * Delete the entry in the datastore. This should delete sub-entries also
     */
    public void delete(String tokenID, String objName) throws SMSException,
            SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::delete dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.delete();
    }

    /**
     * Returns the suborganization names. Returns a set of SMSEntry objects that
     * are suborganization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public Set searchSubOrgNames(String tokenID, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            boolean recursive) throws SMSException, SSOException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::searchSubOrgNames dn: " + dn);
        }
        CachedSubEntries ce = CachedSubEntries.getInstance(
            getToken(tokenID), dn);
        return (ce.searchSubOrgNames(getToken(tokenID), filter, recursive));
    }

    /**
     * Returns the organization names. Returns a set of SMSEntry objects that
     * are organization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public Set searchOrganizationNames(String tokenID, String dn,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            String serviceName, String attrName, Set values)
            throws SMSException, SSOException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::searchOrganizationNames dn: "
                    + dn);
        }

        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.searchOrgNames(getToken(tokenID), serviceName,
                attrName, values));
    }

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public Set subEntries(String tokenID, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::subentries dn: " + dn);
        }
        
        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.getSubEntries(getToken(tokenID), filter));        
    }

    /**
     * Returns the sub-entries matching the schema id. Returns a set of SMSEntry
     * objects that are sub-entries for the provided schema id. The paramter
     * <code>numOfEntries</code> identifies the number of entries to return,
     * if <code>0</code> returns all the entries.
     */
    public Set schemaSubEntries(String tokenID, String dn, String filter,
            String sidFilter, int numOfEntries, boolean sortResults, boolean ao)
            throws SMSException, SSOException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::subentries dn: " + dn);
        }
        
        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.getSchemaSubEntries(getToken(tokenID), filter, sidFilter));
    }

      /**
       * Searchs the data store for objects that match the filter
       */
    public Set search(String tokenID, String startDN, String filter)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::search dn: " + startDN
                    + " filter: " + filter);
        }
        return (SMSEntry.search(getToken(tokenID), startDN, filter, 0, 0,
            false, false));
    }     

    /**
     * Searchs the data store for objects that match the filter
     */
    public Set search2(String tokenID, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::search dn: " + startDN
                    + " filter: " + filter);
        }
        return (SMSEntry.search(getToken(tokenID), startDN, filter,
            numOfEntries, timeLimit, sortResults, ascendingOrder));
    }

    /**
     * Searches the data store for objects that match the filter with an exclude set
     */
    public Set search3(String tokenID, String startDN, String filter,
        int numOfEntries, int timeLimit, boolean sortResults,
        boolean ascendingOrder, Set excludes)
            throws SMSException, SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::search dn: " + startDN
                    + " filter: " + filter + " excludes: " + excludes);
        }
        
        Iterator i = SMSEntry.search(getToken(tokenID), startDN, filter,
            numOfEntries, timeLimit, sortResults, ascendingOrder, excludes);
        
        Set<String> result = new HashSet<String>();
        
        while (i.hasNext()) {
            SMSDataEntry e = (SMSDataEntry)i.next();
            try {
                result.add(e.toJSONString());
            } catch (JSONException ex) {
                debug.error("SMSJAXRPCObjectImpl::problem performing search dn: " + startDN
                    + " filter: " + filter + " excludes: " + excludes, ex);
            }
        }
        
        return result;
    }

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */
    public boolean entryExists(String tokenID, String objName)
            throws SSOException, RemoteException {
        initialize();
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::exists dn: " + objName);
        }
        boolean entryExists = false;
        try {
            CachedSMSEntry ce = CachedSMSEntry.getInstance(getToken(tokenID),
                objName);
            if (ce.isDirty()) {
                ce.refresh();
            }
            entryExists = !(ce.getSMSEntry().isNewEntry());
        } catch (SMSException smse) {
            // Ignore the exception
        }
        return (entryExists);
    }

    /**
     * Returns the root suffix (i.e., base DN) for the SMS objects. All
     * SMSEntries will end with this root suffix.
     */
    public String getRootSuffix() throws RemoteException {
        if (baseDN == null) {
            baseDN = SMSEntry.getRootSuffix();
        }
        return (baseDN);
    }

    /**
     * Returns the root suffix (i.e., amsdkbase DN) for the UMS objects.
     * All UMSEntries will end with this root suffix.
     */
    public String getAMSdkBaseDN() throws RemoteException {
        if (amsdkbaseDN == null) {
            amsdkbaseDN = SMSEntry.getAMSdkBaseDN();
        }
        return (amsdkbaseDN);
    }

    /**
     * Validates service configuration attributes.
     *
     * @param token Single Sign On token.
     * @param validatorClass validator class name.
     * @param values Values to be validated.
     * @return <code>true</code> of values are valid.
     * @throws SMSException if the values are not valid, if the token is not
     *         usable, if the server failed to bootstrap, if the values are
     *         larger than this server validates, if the validator class cannot
     *         be used or if the validator itself fails on the values it was
     *         given.
     * @throws SSOException declared by <code>SMSObjectIF</code>; this method
     *         reports every failure as an <code>SMSException</code> instead.
     * @throws RemoteException if remote method cannot be invoked.
     */
    public boolean validateServiceAttributes(
        String token,
        String validatorClass,
        Set values
    ) throws SMSException, SSOException, RemoteException {
        initialize();
        // The /jaxrpc/* endpoints are not covered by a container
        // <security-constraint> and this method used to ignore the token it is
        // handed, so an anonymous caller could name any class on the server
        // classpath and have it loaded and instantiated. The caller now has to
        // present a usable session.
        //
        // Deliberately no role check on top of that. The arbitrary
        // instantiation primitive is gone because the class is type checked
        // before it is touched at all (see loadValidator), and neither of the
        // two gates this servant already knows about fits here:
        //   - a server/agent gate (isServerOrAgentAuthorized() in
        //     JAXRPCRequestFilter, used by the notification methods) accepts
        //     only the serverconfig.xml super user or a
        //     SessionType.APPLICATION session, and ssoadm, the remote SDK and
        //     delegated realm administrators all hold plain user sessions. It
        //     also carries a SKIP_AUTH_CHECK system property that turns it off
        //     wholesale, which is not what an advisory fix should rest on;
        //   - a super user gate would work for the first two.
        //     AuthD.isSuperUser compares the session principal against the
        //     super user's universal id and never looks at the session type,
        //     so it does admit ssoadm and the remote SDK - but it locks out
        //     the delegated realm administrators that
        //     ServiceSchemaImpl.validatePlugin sends here for ordinary
        //     attribute validation whenever the local validation throws in
        //     client mode.
        //
        // What an authenticated non administrator keeps is the ability to run
        // one of the ServiceAttributeValidator implementations and read the
        // boolean it answers. Most of them only parse their argument, but
        // three do more, and they are the residual risk of this choice:
        //   - SiteIDValidator and ServerIDValidator read the platform server
        //     and site lists under AdminTokenAction rather than under the
        //     caller's token, so their answer is an oracle for whether a given
        //     server URL or (two byte) server id is already configured;
        //   - KeyAliasValidator builds an AMKeyProvider per call, which reads
        //     the key store and both password files from disk and answers
        //     whether a given key alias exists.
        // The configuration write this call is a pre-check for stays enforced
        // by the directory ACIs on the caller's own token; the pre-check
        // itself is not, which is what those three expose.
        //
        // The fourth thing it keeps is not an oracle but a CPU bill: every one
        // of those implementations is run on values the caller supplies, and
        // none of them was written for input of a size a service schema never
        // produces. ResourceComparatorValidator's overlap check cost O(n^3) in
        // the length of one value and the key pattern the agent property map
        // validators share cost O(n^2), so tens of kilobytes of value bought
        // minutes of a request thread, repeatable until the pool is gone. Both
        // are bounded now, and refuseOversizedValues() below bounds what any of
        // them is handed, so the next one is not reachable before it is found.
        //
        // Every failure leaves as an SMSException, the server's own bootstrap
        // failure included: RemoteServiceAttributeValidator turns an
        // SSOException into a "false" answer, which reaches the administrator
        // as invalid attribute values or as unknown property names rather than
        // as the failure it is. Only the error code survives the JAXRPC round
        // trip, so it has to be set on each of them.
        //
        // The validator's own execution is inside that guarantee too. The
        // accepted validators are not written for hostile input - the tokens
        // ResourceComparatorValidator splits are indexed without checking that
        // the separator was there - and an unwrapped RuntimeException would
        // leave this servant as a raw SOAP fault carrying a stack trace, which
        // is the very thing the uniform refusal exists to avoid.
        //
        // Unlike its siblings, which resolve the token with getToken() alone,
        // this method also validates it: it is the one method of this servant
        // that runs code the caller named, so a session the two disagree on is
        // refused here on purpose. validateToken() resets the session idle
        // time, but so does the getToken() it is given - createSSOToken(String)
        // reaches getSession(id, false, true) - so the check adds no keep alive
        // that resolving the token had not already performed, here or in any
        // other method of this servant.
        if (initializationError != null) {
            // Reachable without a session, since there is no token manager to
            // check one with, so it is logged at warning rather than at error:
            // initialize() already reported the failure once, and a caller
            // must not be able to grow the debug file one stack trace per
            // request on a server that is already down.
            if (debug.warningEnabled()) {
                debug.warning("SMSJAXRPCObjectImpl::validateServiceAttributes:"
                    + " the servant failed to bootstrap", initializationError);
            }
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                IUMSConstants.SMS_SERVER_DOWN, null);
        }
        try {
            tokenMgr.validateToken(getToken(token));
        } catch (SSOException ssoe) {
            // At warning for the same reason as the branch above: this one
            // is reachable without a session too, and the shipped default
            // level is error, so a caller cannot grow the debug file until an
            // operator has deliberately turned warnings on.
            if (debug.warningEnabled()) {
                debug.warning("SMSJAXRPCObjectImpl::validateServiceAttributes:"
                    + " refused a caller without a usable session", ssoe);
            }
            // The cause is deliberately not attached: SMSException(Throwable,
            // errorCode) runs exceptionMapper(), which overwrites the code
            // with sms-AUTHENTICATION_ERROR for an SSOException cause anyway.
            // It is set here directly so that the code the client sees is the
            // code this line names.
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                IUMSConstants.SMS_AUTHENTICATION_ERROR, null);
        }
        if (debug.messageEnabled()) {
            // The values are deliberately not logged, they can carry the clear
            // text of a password type attribute.
            debug.message("SMSJAXRPCObjectImpl::validateServiceAttributes: " +
                forLog(validatorClass));
        }
        // A missing <Set_3> element decodes to null, which the validators do
        // not expect; an absent value is the same as no value at all.
        Set attrValues = (values == null) ? Collections.emptySet() : values;
        // Before the class name is looked at, so that the answer to an
        // oversized request does not depend on the class at all - telling the
        // caller "too large" only for a class that turned out to be a real
        // validator would be the classpath oracle the uniform refusal closes.
        refuseOversizedValues(validatorClass, attrValues);
        // An SMSException from loadValidator() is the uniform refusal already
        // and is logged there; being checked, it passes both clauses below
        // untouched and needs no clause of its own.
        try {
            return loadValidator(validatorClass).validate(attrValues);
        } catch (RuntimeException ex) {
            throw validatorFailed(validatorClass, ex);
        } catch (LinkageError ex) {
            throw validatorFailed(validatorClass, ex);
        }
    }

    /**
     * Resolves and instantiates the given service attribute validator class.
     * <p>
     * The class is resolved without running its static initializer and is
     * rejected unless it is a <code>ServiceAttributeValidator</code>, so that
     * a caller supplied class name cannot be used to instantiate arbitrary
     * classes from the OpenAM classpath. Whatever the reason, the failure is
     * reported with the same error code and the same message, so that the
     * caller cannot tell whether a given class is present on the classpath.
     *
     * @param validatorClass name of the validator class.
     * @return the validator instance.
     * @throws SMSException if the class cannot be resolved, is not a service
     *         attribute validator or cannot be instantiated.
     */
    static ServiceAttributeValidator loadValidator(String validatorClass)
        throws SMSException {
        if ((validatorClass == null) || (validatorClass.trim().length() == 0)) {
            throw validatorRefused(validatorClass, null);
        }
        try {
            Class<?> clazz = Class.forName(validatorClass, false,
                SMSJAXRPCObjectImpl.class.getClassLoader());
            if (!ServiceAttributeValidator.class.isAssignableFrom(clazz)) {
                throw validatorRefused(validatorClass, null);
            }
            // The static initializer and the constructor only run here, once
            // the class is known to be a validator. Constructor failures are
            // wrapped in an InvocationTargetException instead of being thrown
            // as they are by Class.newInstance().
            return clazz.asSubclass(ServiceAttributeValidator.class)
                .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw validatorRefused(validatorClass, ex);
        } catch (SecurityException ex) {
            // getDeclaredConstructor() under a security manager; refused like
            // everything else so that it stays indistinguishable.
            throw validatorRefused(validatorClass, ex);
        } catch (LinkageError ex) {
            throw validatorRefused(validatorClass, ex);
        }
    }

    /**
     * Refuses a request whose values are larger than this server validates.
     * <p>
     * The caller of <code>validateServiceAttributes</code> names the validator
     * and supplies the values, so how much work a validator is asked to do is
     * the caller's to choose, and the validators are not written for input of a
     * size a service schema never produces. The two that were super linear in
     * it are bounded now, but the reachability is what makes the next one
     * matter, so the input is bounded here as well.
     * <p>
     * The limits are not a substitute for a validator that behaves: an
     * O(n<sup>3</sup>) check still costs seconds at
     * <code>MAX_VALUE_LENGTH</code>. What they remove is the tail, where tens
     * of kilobytes of value turn into minutes of CPU per request.
     * <p>
     * The reason is logged and is not reported to the caller, which is told
     * only that its request was too large: the limits are the server's, the
     * same for everyone, and nothing about them depends on the values.
     *
     * @param validatorClass name of the class named by the caller, for the log
     *        line only - it has not been looked at yet.
     * @param values the values the caller wants validated.
     * @throws SMSException if the values are larger than the server accepts.
     */
    private static void refuseOversizedValues(String validatorClass, Set values)
        throws SMSException {
        String exceeded = null;
        if (values.size() > MAX_VALUES) {
            exceeded = "value count " + values.size();
        } else {
            long total = 0;
            for (Object value : values) {
                // Anything that is not a String cannot be measured and cannot
                // be large either: the JAXRPC decoder builds this set out of
                // strings. It still counts towards the value count above.
                int length = (value instanceof String)
                    ? ((String) value).length() : 0;
                if (length > MAX_VALUE_LENGTH) {
                    exceeded = "value length " + length;
                    break;
                }
                total += length;
                if (total > MAX_TOTAL_LENGTH) {
                    exceeded = "total length " + total;
                    break;
                }
            }
        }
        if (exceeded == null) {
            return;
        }
        debug.error("SMSJAXRPCObjectImpl::validateServiceAttributes: " +
            "refused an oversized request: " + exceeded + ", for validator " +
            "class: " + forLog(validatorClass));
        throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
            IUMSConstants.SMS_VALIDATOR_VALUES_TOO_LARGE, null);
    }

    /**
     * Builds the one exception reported for every refused validator class,
     * whatever the reason. The error code is what survives the JAXRPC round
     * trip, so it has to be set for the caller to see anything at all.
     * <p>
     * The reason itself only goes to the server log: the caller is told the
     * same thing every time so that the endpoint does not report whether a
     * class is present on the classpath, but a validator that fails for a real
     * reason - a throwing constructor, a missing transitive dependency - has to
     * stay diagnosable on the server, the way the local twin
     * <code>ServiceSchemaImpl.serverEndAttrValidation</code> does it.
     *
     * @param validatorClass name of the refused class.
     * @param cause what made it fail, or <code>null</code> if the name was
     *        refused before anything was attempted with it.
     * @return the exception to throw.
     */
    private static SMSException validatorRefused(String validatorClass,
        Throwable cause) {
        // Debug.error() ignores a null throwable, so the reason being absent
        // needs no branch of its own. At error, not at warning: this branch is
        // past the token check, the shipped default level is error
        // (com.iplanet.services.debug.level in AMConfig.properties), and a
        // refusal that only the caller can see is a refusal nobody can
        // diagnose - the local twin logs it at error for the same reason.
        debug.error("SMSJAXRPCObjectImpl::validateServiceAttributes: " +
            "refused validator class: " + forLog(validatorClass), cause);
        return validatorCannotBeUsed(validatorClass);
    }

    /**
     * The same answer for a validator that was accepted and then failed on the
     * values it was given. Only the log line differs: the caller is told what
     * every other refused class is told, so that a validator which throws does
     * not become a way of telling a validator apart from a class that is not
     * one.
     *
     * @param validatorClass name of the validator class.
     * @param cause what it failed with.
     * @return the exception to throw.
     */
    private static SMSException validatorFailed(String validatorClass,
        Throwable cause) {
        debug.error("SMSJAXRPCObjectImpl::validateServiceAttributes: " +
            "validator class failed: " + forLog(validatorClass), cause);
        return validatorCannotBeUsed(validatorClass);
    }

    /**
     * Builds the one answer both of the above report. The bundle name, the
     * error code and the message argument are all part of it: the code is what
     * survives the JAXRPC round trip, and the argument is what fills the
     * <code>{0}</code> the bundle text carries.
     * <p>
     * The argument goes through <code>forLog()</code> as well. It is the same
     * caller supplied name, and the message it lands in is serialized to the
     * client, where <code>ssoadm</code> prints it and writes it to a log of its
     * own - sanitizing only the server's copy would move the forged line to the
     * client rather than prevent it.
     *
     * @param validatorClass name of the class named by the caller.
     * @return the exception to throw.
     */
    private static SMSException validatorCannotBeUsed(String validatorClass) {
        return new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
            IUMSConstants.SMS_VALIDATOR_CANNOT_INSTANTIATE_CLASS,
            new Object[] { forLog(validatorClass) });
    }

    /**
     * Renders a caller supplied value for a log line or for a message that
     * carries it. The class name reaches both before anything has accepted it,
     * so line delimiters and other control characters are replaced - otherwise
     * a caller could forge whole log entries - and the length is capped.
     * <p>
     * <code>\p{Cntrl}</code> alone is the ASCII control range: NEL, LINE
     * SEPARATOR and PARAGRAPH SEPARATOR are outside it and are line terminators
     * to plenty of readers, so they are named as well.
     *
     * @param value the value to render.
     * @return the value, safe to concatenate into a log line.
     */
    static String forLog(String value) {
        if (value == null) {
            return "null";
        }
        String capped = value;
        if (capped.length() > 200) {
            // Not in the middle of a surrogate pair, which would leave a lone
            // surrogate behind.
            int end = Character.isHighSurrogate(capped.charAt(199)) ? 199 : 200;
            capped = capped.substring(0, end) + "...";
        }
        return UNSAFE_FOR_LOG.matcher(capped).replaceAll("_");
    }

    // Implementation to receive requests from clients
    // Returns changes in the past <i>time</i> minutes
    public synchronized Set objectsChanged(int time) throws RemoteException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::objectsChanged: " + time);
        }
        Set answer = new HashSet();
        // Get the cache index for times upto time+2
        Calendar calendar = getCalendarInstance();
        calendar.setTime(newDate());
        // Add 1 minute to offset, the initial lookup
        calendar.add(Calendar.MINUTE, 1);
        for (int i = 0; i < time + 3; i++) {
            calendar.add(Calendar.MINUTE, -1);
            String cacheIndex = calendarToString(calendar);
            Set modDNs = (Set) cache.get(cacheIndex);
            if (modDNs != null)
                answer.addAll(modDNs);
        }
        return (answer);
    }

    // Implementation for SMSObjectListener
    public synchronized void objectChanged(String name, int type) {
        Calendar calendar = getCalendarInstance();
        calendar.setTime(newDate());
        String cacheIndex = calendarToString(calendar);
        Set modDNs = (Set) cache.get(cacheIndex);
        if (modDNs == null) {
            modDNs = new HashSet();
            cache.put(cacheIndex, modDNs);
            // Maintain cacheIndex
            cacheIndices.addFirst(cacheIndex);
            if (cacheIndices.size() > cacheSize) {
                String index = (String) cacheIndices.removeLast();
                cache.remove(index);
            }
        }
        String modItem = null;
        switch (type) {
        case ADD:
            modItem = "ADD:" + name;
            break;
        case DELETE:
            modItem = "DEL:" + name;
            break;
        default:
            modItem = "MOD:" + name;
        }
        modDNs.add(modItem);

        // If notification URLs are present, send notifications
        synchronized (notificationURLs) {
            for (Map.Entry<String, URL> entry : notificationURLs.entrySet()) {
                String id = entry.getKey();
                URL url = entry.getValue();

                // Construct NotificationSet
                Notification notification = new Notification(modItem);
                NotificationSet ns =
                    new NotificationSet(JAXRPCUtil.SMS_SERVICE);
                ns.addNotification(notification);
                try {
                    PLLServer.send(url, ns);
                    if (debug.messageEnabled()) {
                        debug.message("SMSJAXRPCObjectImpl:objectChanged sent notification to " +
                            "URL: " + url + " Data: " + ns);
                    }
                } catch (SendNotificationException ne) {
                    if (debug.warningEnabled()) {
                        debug.warning("SMSJAXRPCObjectImpl:objectChanged failed sending " +
                            "notification to: " + url + "\nRemoving " +
                            "URL from notification list.", ne);
                    }
                    // Remove the URL from Notification List
                    notificationURLs.remove(id);
                }
            }
        }
    }

    public void allObjectsChanged() {
        // do nothing. Not sending to remote clients
    }

    // Methods to register notification URLs
    public String registerNotificationURL(String url) throws RemoteException {
        initialize();
        // Default value if there are any issues with the registration process.
        String id = "0";
        // Only a server or an agent may register a notification URL. This prevents
        // unauthenticated callers from turning this endpoint into a stored SSRF
        // (GHSA-w858-46wv-v45w).
        if (!JAXRPCRequestFilter.isServerOrAgentAuthorized()) {
            if (debug.warningEnabled()) {
                debug.warning("SMSJAXRPCObjectImpl.registerNotificationURL: rejecting "
                        + "unauthorized registration for URL: " + url);
            }
            return id;
        }
        try {
            // Check URL is not the local server
            if (!url.toLowerCase().startsWith(serverURL)) {
                synchronized (notificationURLs) {
                    URL notificationUrl = new URL(url);
                    // Don't add the URL again if we already have it registered
                    boolean alreadyRegistered = false;
                    for (Map.Entry<String, URL> entry : notificationURLs.entrySet()) {
                        if (notificationUrl.equals(entry.getValue())) {
                            // This allows us to return the existing entry ID to support clients being able to
                            // de-register the correct entry.
                            id = entry.getKey();
                            alreadyRegistered = true;
                            if (debug.messageEnabled()) {
                                debug.message("SMSJAXRPCObjectImpl:registerNotificationURL() - URL "
                                        + url + " already registered, returning existing ID " + id);
                            }
                            break;
                        }
                    }
                    // If we didn't find the url in our list, add it
                    if (!alreadyRegistered) {
                        String serverID = "";
                        try {
                            serverID = WebtopNaming.getAMServerID();
                        } catch (ServerEntryNotFoundException e) {
                            if (debug.messageEnabled()) {
                                debug.message("SMSJAXRPCObjectImpl:registerNotificationURL - " +
                                        "had a problem getting our serverID ", e);
                            }
                        }
                        // Generate a unique value that includes the serverID to have a better chance of being unique
                        // in a cluster should a de-register request end up on the wrong server.
                        id = SMSUtils.getUniqueID() + "_" + serverID;
                        notificationURLs.put(id, notificationUrl);
                        if (debug.messageEnabled()) {
                            debug.message("SMSJAXRPCObjectImpl:registerNotificationURL - " +
                                    "registered notification URL: " + url + " with ID " + id);
                        }
                    }
                }
            } else {
                // Cannot add this server for notifications
                if (debug.warningEnabled()) {
                    debug.warning("SMSJAXRPCObjectImpl:registerNotificationURL "
                            + "cannot add local server: " + url);
                }
            }
        } catch (MalformedURLException e) {
            if (debug.warningEnabled()) {
                debug.warning("SMSJAXRPCObjectImpl:registerNotificationURL "
                        + " invalid URL: " + url, e);
            }
        }
        return id;
    }

    public void deRegisterNotificationURL(String id) throws RemoteException {
        if (!JAXRPCRequestFilter.isServerOrAgentAuthorized()) {
            if (debug.warningEnabled()) {
                debug.warning("SMSJAXRPCObjectImpl.deRegisterNotificationURL: rejecting "
                        + "unauthorized deregistration for ID: " + id);
            }
            return;
        }
        synchronized (notificationURLs) {
            URL url = notificationURLs.remove(id);
            if (url != null && debug.messageEnabled()) {
                debug.message("SMSJAXRPCObjectImpl.deRegisterNotificationURL() - URL "
                        + url + " de-registered for ID " + id);
            }
        }
    }

    /**
     * Processes object changed events from other severs
     * 
     * @param name DN of the object changed
     * @param type change type
     * @throws java.rmi.RemoteException if a remote communication error occurs
     */
    public void notifyObjectChanged(String name, int type)
            throws RemoteException {
        SMSNotificationManager.getInstance().objectChanged(name, type);
    }

    private static String calendarToString(Calendar calendar) {
        // Get year, month, date, hour and minute
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        StringBuilder sb = new StringBuilder(200);
        sb.append(serverURL);
        sb.append(":").append(year).append(month).append(date);
        sb.append(hour).append(minute);
        return (sb.toString());
    }

    /**
     * Returns SSOToken from token ID
     */
    private static SSOToken getToken(String tokenID) throws SSOException {
        if (initializationError != null)
            throw (initializationError);
        return (tokenMgr.createSSOToken(tokenID));
    }

    /**
     * Returns an array of ModificationItems converted from string
     * representation of mods. The string representation is of the format:
     * <pre>
     * <Modifications size="xx"> <AttributeValuePair event="ADD | REPLACE |
     * DELETE"> <Attribute name="attrName" /> <Value>...</Value>
     * </AttributeValuePair> </Modifications>
     * </pre>
     */
    static ModificationItem[] getModItems(String mods) throws SMSException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObject::StringToMods: " + mods);
        }
        ModificationItem[] answer = null;
        try {
            if (mods != null) {
                mods = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + mods;
                Document doc = XMLUtils.toDOMDocument(mods, debug);
                Node root = XMLUtils.getRootNode(doc, "Modifications");
                int modsSize = Integer.parseInt(XMLUtils.getNodeAttributeValue(
                        root, "size"));
                answer = new ModificationItem[modsSize];
                NodeList nl = root.getChildNodes();
                for (int i = 0; i < modsSize; i++) {
                    Node node = nl.item(i);
                    if (node.getNodeName().equals("AttributeValuePair")) {
                        String eventS = XMLUtils.getNodeAttributeValue(node,
                                "event");
                        int event = DirContext.ADD_ATTRIBUTE;
                        if (eventS.equals("REPLACE"))
                            event = DirContext.REPLACE_ATTRIBUTE;
                        else if (eventS.equals("DELETE"))
                            event = DirContext.REMOVE_ATTRIBUTE;
                        Node attrNode = XMLUtils
                                .getChildNode(node, "Attribute");
                        String attrName = XMLUtils.getNodeAttributeValue(
                                attrNode, "name");
                        Set vals = XMLUtils.getAttributeValuePair(node, false);
                        // Construct ModificationItem
                        BasicAttribute attr = new BasicAttribute(attrName);
                        for (Iterator it = vals.iterator(); it.hasNext();)
                            attr.add(it.next());
                        answer[i] = new ModificationItem(event, attr);
                    }
                }
            }
        } catch (Exception e) {
            throw (new SMSException(e,
                    "sms-JAXRPC-cannot-copy-fromModStringToModItem"));
        }
        return (answer);
    }
}
