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
 * $Id: AMLoginModule.java,v 1.22 2009/11/21 01:11:56 222713 Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2012 ForgeRock AS
 */

package com.sun.identity.authentication.spi;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMUser;
import com.iplanet.am.sdk.AMUserPasswordValidation;
import com.iplanet.am.util.Misc;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionCount;
import com.iplanet.dpro.session.service.SessionConstraint;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AMAuthErrorCode;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.LoginStateCallback;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.util.ISValidation;
import com.sun.identity.common.AdministrationServiceListener;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.common.ISAccountLockout;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.AccountLockoutInfo;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.shared.ldap.util.DN;

/**
 * An abstract class which implements JAAS LoginModule, it provides
 * methods to access OpenSSO services and the module
 * xml configuration.
 * <p>
 * Because it is an abstract class, Login Module writers must subclass
 * and implement init(), process(), getPrincipal() methods.
 * <p>
 * The Callback[] for the Login Module is dynamically generated based
 * on the xml module configuration. The module configuration file name
 * must be the same as the name of the class (no package name) and have the
 * extension .xml.
 * <p>
 * Here is a sample module configuration file:
 * <pre>
 * &lt;ModuleProperties moduleClass="LDAP" version="1.0" &gt;
 *     &lt;Callbacks length="2" order="1" timeout="60" header="LDAP
 *     Authentication" &gt;
 *         &lt;NameCallback&gt;
 *             &lt;Prompt&gt; Enter UserId &lt;/Prompt&gt;
 *         &lt;/NameCallback&gt;
 *         &lt;PasswordCallback echoPassword="false" &gt;
 *             &lt;Prompt&gt; Enter Password &lt;/Prompt&gt;
 *         &lt;/PasswordCallback&gt;
 *     &lt;/Callbacks&gt;
 *     &lt;Callbacks length="3" order="2" timeout="120" header="Password
 *     Expiring Please Change" &gt;
 *         &lt;PasswordCallback echoPassword="false" &gt;
 *             &lt;Prompt&gt; Enter Current Password &lt;/Prompt&gt;
 *         &lt;/PasswordCallback&gt;
 *         &lt;PasswordCallback echoPassword="false" &gt;
 *             &lt;Prompt&gt; Enter New Password &lt;/Prompt&gt;
 *         &lt;/PasswordCallback&gt;
 *         &lt;PasswordCallback echoPassword="false" &gt;
 *             &lt;Prompt&gt; Confirm New Password &lt;/Prompt&gt;
 *         &lt;/PasswordCallback&gt;
 *     &lt;/Callbacks&gt;
 * &lt;/ModuleProperties&gt;
 * </pre>
 * Each Callbacks Element corresponds to one login state.
 * When an authentication process is invoked, there will be Callback[]
 * generated from user's Login Module for each state. All login state
 * starts with 1, then module controls the login process, and decides what's
 * the next state to go in the process() method.
 * <p>
 * In the sample module configuration shown above, state one has
 * three Callbacks, Callback[0] is for module information, Callback[1] is
 * for user ID, Callback[2] is for user password. When the user fills in the
 * Callbacks, those Callback[] will be sent to the process() method, where
 * the module writer gets the submitted Callbacks, validates them and returns.
 * If user's password is expiring, the module writer will set the next
 * state to 2. State two has four Callbacks to request user to change
 * password. The process() routine is again
 * called after user submits the Callback[]. If the module writer throws an
 * LoginException, an 'authentication failed' page will be sent to the user.
 * If no exception is thrown, the user will be redirected to their default
 * page.
 * <p>
 * The optional 'timeout' attribute in each state is used to ensure that the
 * user responds in a timely manner. If the time between sending the Callbacks
 * and getting response is greater than the timeout, a timeout page will be
 * sent.
 * <p>
 * There are also optional 'html' and 'image' attribute in each state. The
 * 'html' attribute allows the module writer to use a custom HTML
 * page for the Login UI. The 'image' attribute allows the writer to display
 * a custom background image on each page.
 * <p>
 * When multiple states are available to the user, the Callback array from a
 * previous state may be retrieved by using the <code>getCallbak(int)</code>
 * methods. The underlying login module keeps the Callback[] from the previous
 * states until the login process is completed.
 * <p>
 * If a module writer need to substitute dynamic text in next state, the writer
 * could use the <code>getCallback()</code> method to get the Callback[] for the
 * next state, modify the output text or prompt, then call
 * <code>replaceCallback()</code> to update the Callback array. This allows a
 * module writer to dynamically generate challenges, passwords or user IDs.
 * <p>
 * Each authentication session will create a new instance of your
 * Login Module Java class. The reference to the class will be
 * released once the authentication session has either succeeded
 * or failed. It is important to note that any static data or
 * reference to any static data in your Login module
 * must be thread-safe.
 * <p>
 *
 * For a complete sample, please refer to
 * &lt;install_root&gt;/SUNWam/samples/authentication/providers
 *
 * @supported.api
 */
public abstract class AMLoginModule implements LoginModule {
    // list which holds both presentation and credential callbacks
    List internal = null;
    // list which holds only credential callbacks
    List external = null;
    // list which contains the original Callback list from AMModuleProperties
    List origList = null;
    // class name
    private String fileName = null;
    // if true, means this module does not hava any Callbacks defined, this
    // is the case for anonymous/cert, which have a size 0 config file
    boolean noCallbacks = false;
    
    // constant for empty Callback array
    private static Callback[] EMPTY_CALLBACK = new Callback[0];
    
    // state length for this module
    private int stateLength = 0;
    
    // resource bundle
    private ResourceBundle bundle = null;
    
    // login state
    private LoginState loginState =null;
    
    /**
     * Holds callback handler object passed in through initialize method
     */
    private CallbackHandler handler = null;
    /**
     * Holds subject object passed in through initialize method
     */
    private Subject subject = null;
    /**
     * Holds shared state map passed in through initialize method
     */
    private Map sharedState = null;
    /**
     * Holds options map passed in through initialize method
     */
    private Map options = null;
    
    private static Debug debug = Debug.getInstance("amLoginModule");
    
    private int currentState = ISAuthConstants.LOGIN_START;
    
    private final String EMPTY_STRING = "";
    private String moduleName = null;
    private String moduleClass = null;
    private static final String bundleName = "amAuth";
    private static AuthD ad = AuthD.getAuth();
    private Principal principal = null;
    // the authentication status
    private boolean succeeded = false;

    private boolean forceCallbacksRead = false;
    
    //use Shared state by default disabled
    private boolean isSharedState = false;
    private boolean isStore = true;
    private String sharedStateBehaviorPattern = "";

    // variable used in replaceHeader()
    private String headerWithReplaceTag;
    private boolean alreadyReplaced = false;
    private int lastState = 0;
        
    /**
     * Holds handle to ResourceBundleCache to quickly get ResourceBundle for
     * any Locale.
     */
    protected static AMResourceBundleCache amCache =
        AMResourceBundleCache.getInstance();
    
    
    /**
     * Clone Callback[], and save it in the internal/external
     * callbacks list. External callback contains all user defined
     * Callbacks in the xml module configuration (property file),
     * internal callback contains the external callbacks plus the
     * PagePropertiesCallback. Note here, although
     * Callback[] in internal/external are different, the Callback
     * instance they pointed are actually same instance
     * @param index indicates state of callback
     * @param original original array of callback to be cloned
     * @return Callback[] returns cloned callback
     * @exception AuthLoginException if callback can not be cloned
     */
    private Callback[] cloneCallbacks(int index, Callback[] original)
    throws AuthLoginException {
        // check if there is any callbacks in original
        if (original == null || original.length == 0) {
            // this is the error case where there is no Callbacks
            // defined for a state
            debug.error("cloneCallbacks, no callbacks in state " + (index+1));
            throw new AuthLoginException(bundleName, "noCallbackState",
            new Object[]{new Integer(index + 1)});
        }
        
        int len = original.length;
        // Callback array which hold the cloned Callbacks
        Callback[] copy = new Callback[len];
        // List which contains the external callbacks only
        List extCallbacks = new ArrayList();
        
        // iterate through Callback array, and copy them one by one
        // if it is an external Callback, add to the extCallback list
        for (int i = 0; i < len; i++) {
            if (original[i] instanceof NameCallback) {
                String dftName = ((NameCallback)original[i]).getDefaultName();
                if (dftName != null && dftName.length() != 0) {
                    copy[i] = new NameCallback(
                            ((NameCallback)original[i]).getPrompt(), dftName);
                } else {
                    copy[i] = new NameCallback(
                            ((NameCallback)original[i]).getPrompt());
                }
                extCallbacks.add(copy[i]);
                if (debug.messageEnabled()) {
                    debug.message("clone #" + i + " is NameCallback");
                }
            } else if (original[i] instanceof PasswordCallback) {
                copy[i] = new PasswordCallback(
                ((PasswordCallback) original[i]).getPrompt(),
                ((PasswordCallback) original[i]).isEchoOn());
                extCallbacks.add(copy[i]);
                if (debug.messageEnabled()) {
                    debug.message("clone #" + i + " is PasswordCallback");
                }
            } else if (original[i] instanceof TextOutputCallback) {
                copy[i] = new TextOutputCallback(
                ((TextOutputCallback) original[i]).getMessageType(),
                ((TextOutputCallback) original[i]).getMessage());
                extCallbacks.add(copy[i]);
                if (debug.messageEnabled()) {
                    debug.message("clone #" + i + " is TextOutputCallback");
                }
            } else if (original[i] instanceof PagePropertiesCallback) {
                // PagePropertiesCallback, no need to add to external callbacks
                copy[i] = new PagePropertiesCallback(
                ((PagePropertiesCallback) original[i]).getModuleName(),
                ((PagePropertiesCallback) original[i]).getHeader(),
                ((PagePropertiesCallback) original[i]).getImage(),
                ((PagePropertiesCallback) original[i]).getTimeOutValue(),
                ((PagePropertiesCallback) original[i]).getTemplateName(),
                ((PagePropertiesCallback) original[i]).getErrorState(),
                ((PagePropertiesCallback) original[i]).getPageState());
                ((PagePropertiesCallback) copy[i]).setRequire(
                ((PagePropertiesCallback) original[i]).getRequire());
                ((PagePropertiesCallback) copy[i]).setAttribute(
                ((PagePropertiesCallback) original[i]).getAttribute());
                ((PagePropertiesCallback) copy[i]).setInfoText(
                ((PagePropertiesCallback) original[i]).getInfoText());
                if (debug.messageEnabled()) {
                    debug.message("clone #" + i + " is PagePropertiesCallback");
                }
            } else if (original[i] instanceof ChoiceCallback) {
                int selection =
                    ((ChoiceCallback)original[i]).getDefaultChoice();
                copy[i] = new ChoiceCallback(
                ((ChoiceCallback) original[i]).getPrompt(),
                ((ChoiceCallback) original[i]).getChoices(),
                selection,
                ((ChoiceCallback) original[i]).allowMultipleSelections());
                ((ChoiceCallback) copy[i]).setSelectedIndex(selection);
                extCallbacks.add(copy[i]);
                if (debug.messageEnabled()) {
                    debug.message("clone #" + i + " is ChoiceCallback");
                }
            } else if (original[i] instanceof ConfirmationCallback) {
                ConfirmationCallback temp = (ConfirmationCallback) original[i];
                String prompt = temp.getPrompt();
                String[] options = temp.getOptions();
                if (prompt == null) {
                    // no prompt
                    if (options == null) {
                        // no options
                        copy[i] = new ConfirmationCallback(
                        temp.getMessageType(),
                        temp.getOptionType(),
                        temp.getDefaultOption());
                    } else {
                        copy[i] = new ConfirmationCallback(
                        temp.getMessageType(),
                        options,
                        temp.getDefaultOption());
                    }
                } else {
                    // has prompt
                    if (options == null) {
                        // no options
                        copy[i] = new ConfirmationCallback(
                        prompt,
                        temp.getMessageType(),
                        temp.getOptionType(),
                        temp.getDefaultOption());
                    } else {
                        copy[i] = new ConfirmationCallback(
                        prompt,
                        temp.getMessageType(),
                        options,
                        temp.getDefaultOption());
                    }
                }
                extCallbacks.add(copy[i]);
                if (debug.messageEnabled()) {
                    debug.message("clone #" + i + " is ConfirmationCallback");
                }
            } else if (original[i] instanceof TextInputCallback) {
                copy[i] = new TextInputCallback(
                ((TextInputCallback) original[i]).getPrompt());
                extCallbacks.add(copy[i]);
                if (debug.messageEnabled()) {
                    debug.message("clone #" + i + " is TextInputCallback");
                }
            } else if (original[i] instanceof HttpCallback) {
                HttpCallback hc = (HttpCallback) original[i];
                copy[i] = new HttpCallback(hc.getAuthorizationHeader(),
                hc.getNegotiationHeaderName(),
                hc.getNegotiationHeaderValue(),
                hc.getNegotiationCode());
                extCallbacks.add(copy[i]);
            } else if (original[i] instanceof RedirectCallback) {
                RedirectCallback rc = (RedirectCallback) original[i];
                copy[i] = new RedirectCallback(rc.getRedirectUrl(),
                rc.getRedirectData(),
                rc.getMethod(),
                rc.getStatusParameter(),
                rc.getRedirectBackUrlCookieName());
                extCallbacks.add(copy[i]);
            } else {
                debug.error("unknown callback " + original[i]);
            }
            // more callbacks need to be handled here if ...
        }
        
        // construct external Callback[]
        Callback[] ext = new Callback[extCallbacks.size()];
        if (!extCallbacks.isEmpty()) {
            Iterator it = extCallbacks.iterator();
            int i = 0;
            while (it.hasNext()) {
                ext[i++] = (Callback) it.next();
            }
        }
        
        // set external/internal callbacks
        internal.set(index, copy);
        external.set(index, ext);
        
        return ext;
    }
    
    /**
     * Returns an administration SSOToken for use the OpenAM APIs.
     *
     * <I>NB:</I>This is not the SSOToken that represents the user, if you wish
     * to set/get user session properties use the <code>setUserSessionProperty</code>
     * and <code>getUserSessionProperty</code> method respectively.
     *
     * @return An administrative <code>SSOToken</code>.
     * @exception AuthLoginException if the authentication SSO session
     *         is null.
     * @supported.api
     */
    public SSOToken getSSOSession() throws AuthLoginException {
        SSOToken sess = AuthD.getAuth().getSSOAuthSession();
        if (sess == null) {
            throw new AuthLoginException(bundleName, "nullSess", null);
        }
        return sess;
    }
    
    /**
     * Returns a Callback array for a specific state.
     * <p>
     * This method can be used to retrieve Callback[] for any state. All
     * previous submitted Callback[] information are kept until the login
     * process is completed.
     * @param index  order of state
     * @return Callback array for this state, return 0-length Callback array
     *     if there is no Callback defined for this state
     * @throws AuthLoginException if unable to read the callbacks
     * @supported.api
     */
    public Callback[] getCallback(int index) throws AuthLoginException {
        return getCallback(index, false);
    }

    /**
     * Return a Callback array for a specific state.
     * <p>
     * This method can be used to retrieve Callback[] for any state. All
     * previous submitted Callback[] information are kept until the login
     * process is completed.
     * @param index	order of state
     * @param fetchOrig	boolean indicating even if the callbacks for this
     *        state have been previously retrieved, get the original callbacks
     *        from AMModuleProperties, if set to "true".
     * @return Callback array for this state, return 0-length Callback array
     *     if there is no Callback defined for this state
     * @throws AuthLoginException if unable to read the callbacks
     * @supported.api
     */
    public Callback[] getCallback(int index, boolean fetchOrig) 
        throws AuthLoginException 
    {
        // This method will be called by customer module, so it will
        // return Callback[] from external callback List
        // check if there is no callbacks defined for this module
        if (noCallbacks || ( (isSharedState) && (!forceCallbacksRead) )) {
            return EMPTY_CALLBACK;
        }
        
        if ((internal == null) || ( fetchOrig )) {
            forceCallbacksInit();
            if (origList == null || origList.isEmpty()) {
                return EMPTY_CALLBACK;
            }

            if (debug.messageEnabled()) {
                debug.message("callback size for state " + index + "=" +
                stateLength);
            }
        }
        
        // get Callback[] for this page
        // use index-1 as order since page index starts with 1
        if (index > stateLength) {
            // invalid login state
            debug.error("getCallback, state " + index + " > " + stateLength);
            throw new AuthLoginException(bundleName, "invalidState",
            new Object[]{new Integer(index)});
        }
        Object temp = external.get(index-1);
        if (temp != null) {
            return (Callback[]) temp;
        }
        
        // callbacks has not been retrieved for this index yet
        // need to get it from AMModuleProperties
        // since the Callbacks could not be shared by different instances
        // we need to create clone copy here
        return cloneCallbacks(index-1, (Callback[]) origList.get(index-1));
    }

    protected void forceCallbacksInit () throws AuthLoginException {
        if (internal == null) {
            // get the callbacks for this class;
            origList = AMModuleProperties.getModuleProperties(fileName);
            if (origList == null || origList.isEmpty()) {
                // we got file whose size is zero, this is the case for
                // Cert/Anonymous based authentication
                noCallbacks = true;
                return;
            }
            // instantiate internal/external according to module callback size
            stateLength = origList.size();
            internal = new ArrayList();
            external = new ArrayList();
            if (debug.messageEnabled()) {
                debug.message("callback stateLength in file = " + stateLength);
            }
            for (int i = 0; i < stateLength; i++) {
                internal.add(null);
                external.add(null);
            }
        }
    }
    
    /**
     * Replace Callback object for a specific state.
     * @param state Order of login state
     * @param index Index of Callback in the Callback array to be replaced
     *      for the specified state. Here index starts with 0, i.e. 0 means the
     *      first Callback in the Callback[], 1 means the second callback.
     * @param callback Callback instance to be replaced
     * @exception AuthLoginException if state or index is out of 
     *         bound, or callback instance is null.
     * @supported.api
     */
    public void replaceCallback(int state, int index, Callback callback)
    throws AuthLoginException {
        if (debug.messageEnabled()) {
            debug.message("ReplaceCallback : state=" + state + ", index=" +
            index + ", callback=" + callback);
        }
        // check state length
        if (state > stateLength) {
            throw new AuthLoginException(bundleName, "invalidState",
            new Object[]{new Integer(state)});
        }
        // check callback length for the state
        Callback[] ext = getCallback(state);
        if (index < 0 || index >= ext.length) {
            throw new AuthLoginException(bundleName, "invalidCallbackIndex",
            new Object[]{new Integer(index)});
        }
        // check callback instance
        if (callback == null) {
            throw new AuthLoginException(bundleName, "nullCallback", null);
        }
        
        // replace callback in external & internal Callback array
        ext[index] = callback;
        // in internal, first Callback is always PagePropertiesCallback
        // so add one here for the index
        ((Callback[]) internal.get(state-1))[index + 1] = callback;
    }
    
    /**
     * Replace page header for a specific state.
     * @param state  Order of login state
     * @param header header messages to be replaced
     * @throws AuthLoginException if state is out of bound.
     */
    public void replaceHeader(int state, String header)
    throws AuthLoginException {
        if (debug.messageEnabled()) {
            debug.message("ReplaceHeader : state=" + state + ", header=" +
            header);
        }

        if (lastState != state) {
            alreadyReplaced = false;
        }
        lastState = state;

        // check state length
        if (state > stateLength) {
            throw new AuthLoginException(bundleName, "invalidState",
            new Object[]{new Integer(state)});
        }
        // check callback length for the state
        Callback[] ext = getCallback(state, true);
        if (ext.length<=0) {
            throw new AuthLoginException(bundleName, "invalidCallbackIndex",
            null);
        }
        
        // in internal, first Callback is always PagePropertiesCallback
        if ((header!=null)&&(header.length() != 0)) {
            PagePropertiesCallback pc =
            (PagePropertiesCallback)((Callback[]) internal.get(state-1))[0];
            // retrieve header with REPLACE tag
            if ( !(alreadyReplaced) ) {
                headerWithReplaceTag = pc.getHeader();
            }
            // replace string
            int idx = headerWithReplaceTag.indexOf("#REPLACE#");
            if (idx != -1) {
                String newHeader = headerWithReplaceTag.substring(0, idx) + header;
                pc.setHeader(newHeader);
                alreadyReplaced = true;
            }else{
                String newHeader = headerWithReplaceTag.substring(0, 
                    headerWithReplaceTag.indexOf("<BR></BR>")) + "<BR></BR>" + header;
                pc.setHeader(newHeader);            	
            }
        }
    }
    
    /**
     * Allows you to set the info text for a specific callback. Info Text is shown
     * under the element in the Login page. It is used in the membership module to
     * implement in-line feedback.
     * 
     * @param state state in which the Callback[] to be reset
     * @param callback the callback to associate the info text
     * @param infoText the infotext for the callback
     * @throws AuthLoginException if state/callback is out of bounds
     * @supported.api
     */
    public void substituteInfoText(int state, int callback, String infoText) 
    throws AuthLoginException {
        if (debug.messageEnabled()) {
            debug.message("setInfoText : state=" + state + ", infoText=" + infoText);
        }
        
        // check state length
        if (state > stateLength) {
            throw new AuthLoginException(bundleName, "invalidState",
                new Object[]{new Integer(state)});
        }
        
        // check callback length for the state
        Callback[] ext = getCallback(state);
        if (ext.length<=0) {
            throw new AuthLoginException(bundleName, "invalidCallbackIndex", null);
        }

        // in internal, first Callback is always PagePropertiesCallback
        if ((infoText != null) && (infoText.length() != 0)) {
            PagePropertiesCallback pc =
            (PagePropertiesCallback)((Callback[]) internal.get(state - 1))[0];

            // substitute string
            List<String> infoTexts = pc.getInfoText();
            infoTexts.set(callback, infoText);
            pc.setInfoText(infoTexts);
        }     
    }
    
    /**
     * Clears the info text for a given callback state
     * 
     * @param state The state to clear all infotexts
     * @throws AuthLoginException Invalid state
     * @supported.api
     */
    public void clearInfoText(int state)
    throws AuthLoginException {
        if (debug.messageEnabled()) {
            debug.message("clearInfoText : state=" + state);
        }        
        
        // check state length
        if (state > stateLength) {
            throw new AuthLoginException(bundleName, "invalidState",
                new Object[]{new Integer(state)});
        }
        
        // check callback length for the state
        Callback[] ext = getCallback(state);
        if (ext.length<=0) {
            throw new AuthLoginException(bundleName, "invalidCallbackIndex", null);
        }
        
        // in internal, first Callback is always PagePropertiesCallback
        PagePropertiesCallback pc =
            (PagePropertiesCallback)((Callback[]) internal.get(state - 1))[0];

        // clear info text
        List<String> infoTexts = pc.getInfoText();
        
        for (int i = 0; i < infoTexts.size(); i++) {
            infoTexts.set(i, EMPTY_STRING);
        }
        
        pc.setInfoText(infoTexts);
    }

    /**
     * Use this method to replace the header text from the XML file with new
     * text. This method can be used multiple times on the same state replacing
     * text with new text each time. Useful for modules that control their own
     * error handling.
     *
     * @param state state state in which the Callback[] to be reset
     * @param header The text of the header to be replaced
     * @throws AuthLoginException if state is out of bounds
     * @supported.api
     */
    public void substituteHeader(int state, String header)
    throws AuthLoginException {
        if (debug.messageEnabled()) {
            debug.message("substituteHeader : state=" + state + ", header=" +
            header);
        }
        // check state length
        if (state > stateLength) {
            throw new AuthLoginException(bundleName, "invalidState",
            new Object[]{new Integer(state)});
        }
        // check callback length for the state
        Callback[] ext = getCallback(state);
        if (ext.length<=0) {
            throw new AuthLoginException(bundleName, "invalidCallbackIndex",
            null);
        }

        // in internal, first Callback is always PagePropertiesCallback
        if ((header!=null)&&(header.length() != 0)) {
            PagePropertiesCallback pc =
            (PagePropertiesCallback)((Callback[]) internal.get(state-1))[0];

            // substitute string
            pc.setHeader(header);
        }
    }
    
    /**
     * Reset a Callback instance to the original Callback for the specified
     * state and the specified index. This will override change to the Callback
     * instance by the <code>replaceCallback()</code> method.
     * @param state state in which the Callback[] to be reset
     * @param index index order of the Callback in the Callback[], index starts
     *        with 0, i.e. 0 means first callback instance, 1 means
     *        the second callback instance.
     * @throws AuthLoginException if state or index is out of bound.
     * @supported.api
     */
    public void resetCallback(int state, int index)
    throws AuthLoginException {
        if (debug.messageEnabled()) {
            debug.message("resetCallback: state=" + state + ",index=" + index);
        }
        // check state length
        if (state > stateLength) {
            throw new AuthLoginException(bundleName, "invalidState",
            new Object[]{new Integer(state)});
        }
        // check callback length for the state
        Callback[] ext = getCallback(state);
        if (index < 0 || index >= ext.length) {
            throw new AuthLoginException(bundleName, "invalidCallbackIndex",
            new Object[]{new Integer(index)});
        }
        
        // get the Callback from AMModuleProperties
        // add one to index here since first one is the PagePropertiesCallback
        Callback callback = ((Callback[]) origList.get(state-1))[index+1];
        Callback newCallback = null;
        if (callback instanceof NameCallback) {
            newCallback = new NameCallback(
            ((NameCallback) callback).getPrompt());
        } else if (callback instanceof PasswordCallback) {
            newCallback = new PasswordCallback(
            ((PasswordCallback) callback).getPrompt(),
            ((PasswordCallback) callback).isEchoOn());
        } else if (callback instanceof ChoiceCallback) {
            int selection = ((ChoiceCallback) callback).getDefaultChoice();
            newCallback = new ChoiceCallback(
            ((ChoiceCallback) callback).getPrompt(),
            ((ChoiceCallback) callback).getChoices(),
            selection,
            ((ChoiceCallback) callback).allowMultipleSelections());
            ((ChoiceCallback) newCallback).setSelectedIndex(selection);
        } else {
            // should never come here since only above three will be supported
            debug.error("Unsupported call back instance " + callback);
            throw new AuthLoginException(bundleName, "unknownCallback", null);
        }
        
        if (debug.messageEnabled()) {
            debug.message("original=" + callback + ",new=" + newCallback);
        }
        
        // set external & internal callback instance
        ((Callback[]) internal.get(state-1))[index+1] = newCallback;
        ((Callback[]) external.get(state-1))[index] = newCallback;
    }
    
    /**
     * Implements initialize() method in JAAS LoginModule class.
     * <p>
     * The purpose of this method is to initialize Login Module,
     * it will call the init() method implemented by user's Login
     * Module to do initialization.
     * <p>
     * This is a final method.
     * @param subject - the Subject to be authenticated.
     * @param callbackHandler - a CallbackHandler for communicating with the
     *     end user (prompting for usernames and passwords, for example).
     * @param sharedState - state shared with other configured LoginModules.
     * @param options - options specified in the login Configuration for this
     *     particular LoginModule.
     */
    public final void initialize(Subject subject,
    CallbackHandler callbackHandler,
    java.util.Map sharedState,
    java.util.Map options) {
        this.subject = subject;
        this.handler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;
        // get class name
        String className = this.getClass().getName();
        int index = className.lastIndexOf(".");
        moduleClass = className.substring(index + 1);
        moduleName = (String) options.get(ISAuthConstants.
        MODULE_INSTANCE_NAME);
        
        // get module properties file path
        
        loginState = getLoginState();
        
        fileName = loginState.getFileName(moduleClass+ ".xml");
        
        // get resource bundle
        
        bundle = amCache.getResBundle(bundleName, getLoginLocale());
        
        if (debug.messageEnabled()) {
            debug.message("AMLoginModule resbundle locale="+getLoginLocale());
            debug.message("Login, class = " + className +
            ", module=" + moduleName + ", file=" + fileName);
        }
        isSharedState = Boolean.valueOf(CollectionHelper.getMapAttr(
            options, ISAuthConstants.SHARED_STATE_ENABLED, "false")
            ).booleanValue();
        
        isStore = Boolean.valueOf(CollectionHelper.getMapAttr(
            options, ISAuthConstants.STORE_SHARED_STATE_ENABLED, "true")
            ).booleanValue();

        sharedStateBehaviorPattern = Misc.getMapAttr(options,
            ISAuthConstants.SHARED_STATE_BEHAVIOR_PATTERN,
            "tryFirstPass");
        
        if (debug.messageEnabled()) {
            debug.message("AMLoginModule" +
            ISAuthConstants.SHARED_STATE_BEHAVIOR_PATTERN +
            " is set to " + sharedStateBehaviorPattern);
        }        
       
        // Check for composite Advice
        String compositeAdvice = loginState.getCompositeAdvice();
        if (compositeAdvice != null) {
            if (debug.messageEnabled()) {
                debug.message("AMLoginModule.initialize: "
                    + "Adding Composite Advice " + compositeAdvice);
            }
            sharedState.put(ISAuthConstants.COMPOSITE_ADVICE_XML,
                compositeAdvice);
        } 
        // call customer init method
        init(subject, sharedState, options);
    }
    
    /**
     * Initialize this LoginModule.
     * <p>
     * This is an abstract method, must be implemented by user's Login Module
     * to initialize this LoginModule with the relevant information. If this
     * LoginModule does not understand any of the data stored in sharedState
     * or options parameters, they can be ignored.
     * @param subject - the Subject to be authenticated.
     * @param sharedState - state shared with other configured LoginModules.
     * @param options - options specified in the login Configuration for this
     *     particular LoginModule. It contains all the global and organization
     *     attribute configuration for this module. The key of the map is the
     *     attribute name (e.g. <code>iplanet-am-auth-ldap-server</code>) as
     *     String, the value is the value of the corresponding attribute as Set.
     * @supported.api
     */
    abstract public void init(Subject subject,
    java.util.Map sharedState,
    java.util.Map options);
    
    /**
     * Abstract method must be implemented by each login module to
     * control the flow of the login process.
     * <p>
     * This method takes an array of sbumitted
     * Callback, process them and decide the order of next state to go.
     * Return -1 if the login is successful, return 0 if the
     * LoginModule should be ignored.
     * @param callbacks Callback[] for this Login state
     * @param state  Order of state. State order starts with 1.
     * @return order of next state.  return -1 if authentication
     *     is successful, return 0 if the LoginModule should be ignored.
     * @exception LoginException if login fails.
     * @supported.api
     */
    abstract public int process(Callback[] callbacks, int state)
    throws LoginException;
    
    /**
     * Abstract method must be implemeted by each login module to
     * get the user Principal
     * @return Principal
     * @supported.api
     */
    abstract public java.security.Principal getPrincipal();
    
    /**
     * This method should be overridden by each login module
     * to destroy dispensable state fields.
     *
     * @supported.api
     */
    public void  destroyModuleState(){};
    
    /**
     * This method should be overridden by each login module
     * to do some garbage collection work after the module
     * process is done. Typically those class wide global variables
     * that will not be used again until a logout call should be nullified.
     */
    public void  nullifyUsedVars() {};
    
    /**
     * Wrapper for process() to utilize AuthLoginException.
     * @param callbacks associated with authentication
     * @param state of callbacks
     * @return state of auth login
     * @exception AuthLoginException if login fails.
     */
    private int wrapProcess(Callback[] callbacks, int state)
    throws AuthLoginException {
        try {
            if (callbacks != null) {
                for (int i = 0; i < callbacks.length; i++) {
                    if (callbacks[i] instanceof NameCallback) {
                        String newUser = null;
                        try {
                            newUser = IdUtils.getIdentityName(
                                ((NameCallback) callbacks[i]).getName(), 
                                getRequestOrg());
                        } catch (IdRepoException idRepoExp) {
                            //Print message and let Auth proceed.
                            debug.message(
                                "AMLoginModule.wrapProcess: Cannot get "+
                                "username from idrepo. ", idRepoExp);
                        } 
                        if (newUser != null) {
                            ((NameCallback) callbacks[i]).setName(newUser);
                        }
                    }
                }
            }
            return process(callbacks, state);
        } catch (LoginException e) {
            currentState = ISAuthConstants.LOGIN_IGNORE;
            setFailureModuleName(moduleName);
            if (e instanceof InvalidPasswordException){
                setFailureID(((InvalidPasswordException)e).getTokenId());
                throw new InvalidPasswordException(e);
            } else {
                throw new AuthLoginException(e);
            }
        } catch (RuntimeException re) {
            currentState = ISAuthConstants.LOGIN_IGNORE;
            setFailureModuleName(moduleName);
            //rethrow the exception
            throw re;
        }
    }
    
    /**
     * Returns true if a module in authentication chain has already done, either
     * succeeded or failed.
     *
     * @return true if a module in authentication chain has already done, either
     * succeeded or failed.
     */
    private boolean moduleHasDone() {
        return (currentState == ISAuthConstants.LOGIN_SUCCEED) ||
        (currentState == ISAuthConstants.LOGIN_IGNORE);
    }
    
    /**
     * Implements login() method in JAAS LoginModule class.
     * <p>
     * This method is responsible for retrieving corresponding Callback[] for
     * current state, send as requirement to user, get the submitted Callback[],
     * call the process() method. The process() method will decide the next
     * action based on those submitted Callback[].
     * <p>
     * This method is final.
     * @return <code>true</code> if the authentication succeeded, or 
         *         <code>false</code> if this LoginModule should be ignored.
     * @throws AuthLoginException - if the authentication fails
     */
    public final boolean login() throws AuthLoginException {
        if (moduleHasDone()) {
            debug.message("This module has already done.");
            if (currentState == ISAuthConstants.LOGIN_SUCCEED) {
                return true;
            } else {
                return false;
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("This module is not done yet. CurrentState: "
                + currentState);
            }
        }
        
        // make one getCallback call to populate first state
        // this will set the noCallbacks variable
        if (internal == null) {
            getCallback(1);
        }
        // if this module does not define any Callbacks (such as Cert),
        // pass control right to module, then check return code from module
        if (noCallbacks) {
            currentState =  wrapProcess(EMPTY_CALLBACK, 1);
            // check login status
            if (currentState == ISAuthConstants.LOGIN_SUCCEED) {
                setSuccessModuleName(moduleName);
                succeeded = true;
                nullifyUsedVars();
                return true;
            } else if (currentState == ISAuthConstants.LOGIN_IGNORE) {
                // index = 0;
                setFailureModuleName(moduleName);
                succeeded = false;
                destroyModuleState();
                principal = null;
                return false;
            } else {
                setFailureModuleName(moduleName);
                succeeded = false;
                cleanup();
                throw new AuthLoginException(bundleName, "invalidCode",
                new Object[]{new Integer(currentState)});
            }
        }
        
        if (handler == null) {
            debug.error("Handler is null");
            throw new AuthLoginException(bundleName, "nullHandler", null);
        }
        try {
            Callback[] lastCallbacks = null;
            boolean needToExit = false;
            
            // starting from first page
            //currentState = 1;
            while (currentState != ISAuthConstants.LOGIN_SUCCEED &&
            currentState != ISAuthConstants.LOGIN_IGNORE) {
                if (debug.messageEnabled()) {
                    debug.message("Login, state = " + currentState);
                }
                if (isSharedState) {
                    currentState =  wrapProcess(EMPTY_CALLBACK, 1);
                    isSharedState = false;
                    continue;
                }
                // get current set of callbacks
                getCallback(currentState);
                // check if this is an error state, if so, throw exception
                // to terminate login process
                Callback[] cbks = ((Callback[]) internal.get(currentState-1));
                PagePropertiesCallback callback =
                (PagePropertiesCallback) cbks[0];
                
                if (callback.getErrorState()) {
                    // this is an error state
                    setFailureModuleName(moduleName);
                    String template = callback.getTemplateName();
                    String errorMessage = callback.getHeader();
                    if (template == null || template.length() == 0) {
                        // this is the case which no error template is
                        // defined, only exception message in header
                        throw new MessageLoginException(errorMessage);
                    } else {
                        // send error template
                        //setLoginFailureURL(template);
                        setModuleErrorTemplate(template);
                        throw new AuthLoginException(errorMessage);
                    }
                }
                // call handler to handle the internal callbacks
                handler.handle(cbks);
                
                // Get the page state from the PagePropertiesCallback
                Callback[] cbksPrev= ((Callback[])internal.get(currentState-1));
                
                PagePropertiesCallback callbackPrev =
                (PagePropertiesCallback) cbksPrev[0];
                String pageState = callbackPrev.getPageState();
                if ((pageState != null) &&
                (pageState.length() != 0) &&
                (!pageState.equals(Integer.toString(currentState)))) {
                    int loginPage = Integer.parseInt(pageState);
                    
                    //Set the current page state in PagePropertiesCallback
                    callbackPrev.setPageState(Integer.toString(currentState));
                    
                    currentState = loginPage;
                    if (debug.messageEnabled()) {
                        debug.message("currentState from UI " + currentState);
                    }
                }
                
                // Get the last submitted callbacks to auth module and submit
                // those callbacks to do DataStore authentication if the incoming
                // user is special / internal user and auth module is other than
                // "DataStore" and "Application" auth modules.
                lastCallbacks = (Callback[])external.get(currentState-1);
                if ((!moduleName.equalsIgnoreCase("DataStore")) && 
                        (!moduleName.equalsIgnoreCase("Application"))) {
                    if (!authenticateToDatastore(lastCallbacks)) {
                        needToExit = true;
                        break;
                    }
                }
                
                // send external callback and send to module for processing
                currentState = wrapProcess((Callback[])
                external.get(currentState-1), currentState);
                
                if (debug.messageEnabled()) {
                    debug.message("Login NEXT State : " + currentState);
                }
            }  
                        
            if (needToExit) {
                nullifyUsedVars();
                throw new AuthLoginException(AMAuthErrorCode.AUTH_MODULE_DENIED);
            }
            
            // check login status
            if (currentState == ISAuthConstants.LOGIN_SUCCEED) {       
                setSuccessModuleName(moduleName);
                succeeded = true;
                nullifyUsedVars();
                return true;
            } else {
                // currentState = 0;
                setFailureModuleName(moduleName);
                succeeded = false;
                destroyModuleState();
                principal = null;
                return false;
            }
        } catch (IOException e) {
            setFailureModuleName(moduleName);
            if (e.getMessage().equals(AMAuthErrorCode.AUTH_TIMEOUT)) {
                debug.message("login timed out ", e);
            } else {
                debug.message("login ", e);
            } throw new AuthLoginException(e);
        } catch (UnsupportedCallbackException e) {
            setFailureModuleName(moduleName);
            debug.message("Login", e);
            throw new AuthLoginException(e);
        }
    }
    
    /**
     * Returns authentication level that has been set for the module
     *
     * @return authentication level of this authentication session
     * @supported.api
     */
    public int getAuthLevel() {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                return 0;
            }
        }
        return loginState.getAuthLevel();
    }
    
    /**
     * Sets the <code>AuthLevel</code> for this session.
     * The authentication level being set cannot be downgraded
     * below that set by the module configuration.
     *
     * @param auth_level authentication level string to be set
     * @return <code>true</code> if setting is successful,<code>false</code> 
     *         otherwise
     * @supported.api
     */
    public boolean setAuthLevel(int auth_level) {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                // may be should throw AuthLoginException here
                debug.error("Unable to set auth level : " + auth_level);
                return false;
            }
        }
        loginState.setModuleAuthLevel(auth_level);
        return true;
    }
    
    /**
     * Returns the current state in the authentication process.
     *
     * @return the current state in the authentication process.
     * @supported.api
     */
    public int getCurrentState() {
        return currentState;
    }
    
    /**
     * Returns the <code>HttpServletRequest</code> object that
     * initiated the call to this module.
     *
     * @return <code>HttpServletRequest</code> for this request, returns null
     *         if the <code>HttpServletRequest</code> object could not be
     *         obtained.
     * @supported.api
     */
    public HttpServletRequest getHttpServletRequest() {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                return null;
            }
        }
        return loginState.getHttpServletRequest();
    }
    
    /**
     * Returns the authentication <code>LoginState</code>
     * @param methodName Name of the required methd in 
     *        <code>LoginState</code> object
     * @return <code>com.sun.identity.authentication.service.LoginState</code>
     *         for this authentication method.
     * @throws AuthLoginException if fails to get the Login state
     */
    protected com.sun.identity.authentication.service.LoginState getLoginState(
    String methodName) throws AuthLoginException {
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                throw new AuthLoginException(bundleName, "wrongCall",
                new Object[]{methodName});
            }
        }
        return loginState;
    }
    
    /**
     * Returns the Login <code>Locale</code> for this session
     * @return <code>Locale</code> used for localizing text
     */
    protected java.util.Locale getLoginLocale()  {
        try {
            String loc = getLocale();
            return com.sun.identity.shared.locale.Locale.getLocale(loc);
        } catch (AuthLoginException ex) {
            debug.message("unable to determine loginlocale ", ex);
            return java.util.Locale.ENGLISH;
        }
    }
    
    /*
     * Returns the Login State object
     * @return com.sun.identity.authentication.service.LoginState
     */
    private com.sun.identity.authentication.service.LoginState getLoginState() {
        Callback[] callbacks = new Callback[1];
        try {
            callbacks[0] = new LoginStateCallback();
            if (handler == null) {
                return null;
            }
            handler.handle(callbacks);
            return ((LoginStateCallback) callbacks[0]).getLoginState();
        } catch (Exception e) {
            debug.message("Error.." ,e );
            return null;
        }
    }
    
    /**
     * Returns the <code>HttpServletResponse</code> object for the servlet
     * request that initiated the call to this module. The servlet response
     * object will be the response to the <code>HttpServletRequest</code>
     * received by the authentication module.
     *
     * @return <code>HttpServletResponse</code> for this request, returns null
     * if the <code>HttpServletResponse</code> object could not be obtained.
     * @supported.api
     */
    public HttpServletResponse getHttpServletResponse() {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                return null;
            }
        }
        return loginState.getHttpServletResponse();
    }
    
    /**
     * Returns the CallbackHandler object for the module. This method
     * will be used internally.
     *
     * @return CallbackHandler for this request, returns null if the
     *         CallbackHandler object could not be obtained.
     */
    public CallbackHandler getCallbackHandler() {
        return handler;
    }
    
    /**
     * Returns the locale for this authentication session.
     *
     * @return <code>java.util.Locale</code> locale for this authentication
     *         session.
     * @throws AuthLoginException if problem in accessing the 
               locale.
     * @supported.api
     */
    public String getLocale() throws AuthLoginException {
        // get login state for this authentication session
        return  getLoginState("getLocale()").getLocale();
    }
    
    /**
     * Returns the number of authentication states for this
     * login module.
     *
     * @return the number of authentication states for this login module.
     * @supported.api
     */
    public int getNumberOfStates() {
        return stateLength;
    }
    
    /**
     * Returns the organization DN for this authentication session.
     *
     * @return organization DN.
     * @supported.api
     */
    public String getRequestOrg() {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                return null;
            }
        }
        return loginState.getOrgDN();
    }
    
    /**
     * Returns a unique key for this authentication session.
     * This key will be unique throughout an entire Web browser session.
     *
     * @return null is unable to get the key,
     * @supported.api
     */
    public String getSessionId() {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                return null;
            }
        }
       return loginState.getSid().toString();
/*
        InternalSession sess = loginState.getSession();
        if (sess != null) {
            return sess.getID().toString();
        } else {
            return null;
        }
*/
    }
    
    /**
     * Returns the organization attributes for specified organization.
     *
     * @param orgDN Requested organization DN.
     * @return Map that contains all attribute key/value pairs defined
     *         in the organization.
     * @throws AuthLoginException if cannot get organization profile.
     * @supported.api
     */
    public Map getOrgProfile(String orgDN) throws AuthLoginException {
        Map orgMap = null;
        if (orgDN == null || orgDN.length() == 0) {
            // get login state for this authentication session
            orgDN = getLoginState("getOrgProfile(String)").getOrgDN();
        }
        
        try {
           OrganizationConfigManager orgConfigMgr =
                AuthD.getAuth().getOrgConfigManager(orgDN);
            orgMap = orgConfigMgr.getAttributes(
                ISAuthConstants.IDREPO_SVC_NAME);
           if (debug.messageEnabled()) {
              debug.message("orgMap is : " + orgMap);
           }
        } catch (Exception ex) {
            debug.message("getOrgProfile", ex);
            throw new AuthLoginException(ex);
        }
        return orgMap;
    }
    
    /**
     * Returns service template attributes defined for the specified
     * organization.
     *
     * @param orgDN Organization DN.
     * @param serviceName Requested service name.
     * @return Map that contains all attribute key/value pairs defined in the
     *         organization service template.
     * @throws AuthLoginException if cannot get organization service
     *         template.
     * @supported.api
     */
    public Map getOrgServiceTemplate(String orgDN, String serviceName)
            throws AuthLoginException {
        Map orgMap = null;
        if (orgDN == null || orgDN.length() == 0) {
            // get login state for this authentication session
            orgDN = getLoginState(
            "getOrgServiceTemplate(String, String)").getOrgDN();
        }
        try {
           OrganizationConfigManager orgConfigMgr = 
                            AuthD.getAuth().getOrgConfigManager(orgDN);
           orgMap = orgConfigMgr.getServiceConfig(serviceName).getAttributes();
        }
        catch (Exception ex) {
            debug.message("getOrgServiceTemplate", ex);
            throw new AuthLoginException(ex);
        }
        return orgMap;
    }
    
    /**
     * Checks if persistent cookie is on.
     *
     * @return <code>true</code> if persistent cookie is set.
     * @supported.api
     */
    public boolean getPersistentCookieOn() {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                return false;
            }
        }
        return loginState.getPersistentCookieMode();
    }

    /**
     * Checks if dynamic profile creation is enabled.
     *
     * @return <code>true</code> if dynamic profile creation is enabled.
     */
    public boolean isDynamicProfileCreationEnabled() {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                return false;
            }
        }
        return loginState.isDynamicProfileCreationEnabled();
    }
    
    /**
     * Attempts to set the Persistent Cookie for this session.  Can be called
     * from any state in the authentication module.  It will return whether
     * "Core Authentication" will add the persistent cookie (name is specified
     * in the <code>/etc/opt/SUNWam/config/AMConfig.properties</code>:
     * <code>com.iplanet.am.pcookie.name</code> property)
     *
     * @return <code>true</code> when setting is successful, <code>false</code>
     *         if the persistent cookie mode attribute is not set for the
     *         organization.
     * @supported.api
     */
    public boolean setPersistentCookieOn() {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                return false;
            }
        }
        
        if (!loginState.getPersistentCookieMode()) {
            return false;
        }
        
        loginState.setPersistentCookieOn();
        return true;
    }
    
    /**
     * Returns service configuration attributes.
     * @param name Requested service name.
     * @return Map that contains all attribute key/value pairs defined in
     *         the service configuration.
     * @throws AuthLoginException if error in accessing the service schema.
     *
     * @supported.api
     */
    public Map getServiceConfig(String name) throws AuthLoginException {
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(name,
            AuthD.getAuth().getSSOAuthSession());
            ServiceSchema sc = scm.getGlobalSchema();
            HashMap retMap = new HashMap();
            if (sc != null) {
                retMap.putAll(sc.getAttributeDefaults());
            }
            
            sc = scm.getOrganizationSchema();
            if (sc != null) {
                retMap.putAll(sc.getAttributeDefaults());
            }
            
            sc = scm.getUserSchema();
            if (sc != null) {
                retMap.putAll(sc.getAttributeDefaults());
            }
            
            sc = scm.getPolicySchema();
            if (sc != null) {
                retMap.putAll(sc.getAttributeDefaults());
            }
            
            return retMap;
        } catch (Exception ex) {
            debug.message("getServiceConfig", ex);
            throw new AuthLoginException(ex);
        }
    }
    
    /**
     * Returns the user profile for the user specified. This
     * method may only be called in the validate() method.
     *
     * @param userDN distinguished name os user.
     * @return <code>AMUser</code> object for the user's distinguished name.
     * @throws AuthLoginException if it fails to get the user profile for
     *         <code>userDN</code>.
     * @deprecated This method has been deprecated. Please use the
     *             IdRepo API's to get the AMIdentity object for the user. More
     *             information on how to use the Identity Repository APIs is
     *             available in the OpenSSO Developer's Guide.
     *
     * @supported.api
     */
     public AMUser getUserProfile(String userDN) throws AuthLoginException{
        AMUser user = null;
        try {
            user = AuthD.getAuth().getSDK().getUser(userDN);
        } catch (Exception ex) {
            debug.message("getUserProfile", ex);
            throw new AuthLoginException(ex);
        }
        return user;
    }
    
    /**
     * Returns the property from the user session. If the session is being force
     * upgraded then set on the old session otherwise set on the current session.
     *
     * @param name The property name.
     * @return The property value.
     * @throws AuthLoginException if the user session is invalid.
     *
     * @supported.api
     */
    public String getUserSessionProperty(String name)
            throws AuthLoginException {
        InternalSession sess = null;

        if (getLoginState(null).isSessionUpgrade() &&
                getLoginState(null).getForceFlag()) {
            sess = getLoginState(null).getOldSession();
        } else {
            sess = getLoginState("getUserSessionProperty()").getSession();
        }

        if (sess != null) {
            return sess.getProperty(name);
        } else {
            return null;
        }
    }
    
    /**
     * Sets a property in the user session. If the session is being force
     * upgraded then set on the old session otherwise set on the current session.
     *
     * @param name The property name.
     * @param value The property value.
     * @throws AuthLoginException if the user session is invalid.
     *
     * @supported.api
     */
    public void setUserSessionProperty(String name, String value)
            throws AuthLoginException {
        InternalSession sess = null;

        if (getLoginState(null).isSessionUpgrade() &&
                getLoginState(null).getForceFlag()) {
            sess = getLoginState(null).getOldSession();
        } else {
            sess = getLoginState("setUserSessionProperty()").getSession();
        }

        if (sess != null) {
            sess.putProperty(name, value);
        } else {
            throw new AuthLoginException(bundleName, "wrongCall",
            new Object[]{" setUserSessionProperty()"});
        }
    }
    
    /**
     * Returns a set of user IDs generated from the class defined
     * in the Core Authentication Service. Returns null if the
     * attribute <code>iplanet-am-auth-username-generator-enabled</code> is
     * set to false.
     *
     * @param attributes the keys in the <code>Map</code> contains the
     *        attribute names and their corresponding values in
     *        the <code>Map</code> is a <code>Set</code> that
     *        contains the values for the attribute
     * @param num the maximum number of returned user IDs; 0 means there
     *        is no limit
     * @return a set of auto-generated user IDs
     * @throws AuthLoginException if the class instantiation failed
     *
     * @supported.api
     */
    public Set getNewUserIDs(Map attributes, int num)
            throws AuthLoginException {
        boolean enabled = getLoginState(
        "getNewUserIDs(Map, int)").userIDGeneratorEnabled;
        
        if (!enabled) {
            return null;
        }
        
        String className = getLoginState(
        "getNewUserIDs(Map, int)").userIDGeneratorClassName;
        String orgDN = getLoginState("getNewUserIDs(Map, int)").getOrgDN();
        
        // if className is null or empty, use the default user ID
        // generator class name
        if (className == null || className.length() == 0) {
            className = ISAuthConstants.DEFAULT_USERID_GENERATOR_CLASS;
        }
        
        UserIDGenerator idGenerator = null;
        try {
            // instantiate the Java class
            Class theClass = Class.forName(className);
            idGenerator = (UserIDGenerator)theClass.newInstance();
            
        } catch (Exception e) {
            debug.message("getNewUserIDs(): unable to instantiate " +
            className, e);
            return null;
        }
        
        return (idGenerator.generateUserIDs(orgDN, attributes, num));
    }
    
    /**
     * Sets the the login failure URL for the user. This method does not
     * change the URL in the user's profile. When the user authenticates
     * failed, this URL will be used by the authentication for the
     * redirect.
     *
     * @param url URL to go when authentication failed.
     * @throws AuthLoginException if unable to set the URL.
     *
     * @supported.api
     */
    public void setLoginFailureURL(String url) throws AuthLoginException {
        getLoginState("setLoginFailureURL()").setFailureLoginURL(url);
    }
    
    /**
     * Sets the error template for the module
     * @param templateName the error template for the module
     * @throws AuthLoginException when unable to set the template 
     */
    public void setModuleErrorTemplate(String templateName)
        throws AuthLoginException {
        getLoginState(
            "setModuleTemplate()").setModuleErrorTemplate(templateName);
    }
    
    /**
     * Sets the the login successful URL for the user. This method does not
     * change the URL in the user's profile. When the user authenticates
     * successfully, this URL will be used by the authentication for the
     * redirect.
     *
     * @param url <code>URL</code> to go when authentication is successful.
     * @throws AuthLoginException if unable to set the URL.
     * @supported.api
     */
    public void setLoginSuccessURL(String url) throws AuthLoginException {
        getLoginState("setLoginSuccessURL()").setSuccessLoginURL(url);
    }
    
    /**
     * Sets the user organization. This method should only be called when the
     * user authenticates successfully. It allows the user authentication
     * module to decide in which domain the user profile should be created.
     *
     * @param orgDN The organization DN.
     * @throws AuthLoginException
     */
    public void setOrg(String orgDN) throws AuthLoginException {
/* TODO
        if (orgDN.indexOf("=") == -1) {
            throw new AuthLoginException(bundleName, "invalidDN",
                new Object[]{orgDN});
        }
        getLoginState("setOrg()").setOrg(orgDN);
 */
        return;
    }
    
    /**
     * Checks if a Callback is required to have input.
     * @param state Order of state.
     * @param index Order of the Callback in the Callback[], the index.
     *        starts with 0.
     * @return <code>true</code> if the callback corresponding to the number
     *         in the specified state is required to have value,
     *         <code>false</code> otherwise
     * @supported.api
     */
    public boolean isRequired(int state, int index) {
        // check state
        if (state > stateLength) {
            // invalid state, return false now
            return false;
        }
        // get internal callbacks for the state
        Callback[] callbacks = (Callback[]) internal.get(state - 1);
        if (callbacks == null || callbacks.length == 0) {
            // no callbacks defined for this state, return false
            return false;
        }
        // check first Callback
        Callback callback = callbacks[0];
        if (callback instanceof PagePropertiesCallback) {
            List req = ((PagePropertiesCallback) callback).getRequire();
            if (req == null || req.isEmpty() || index >= req.size()) {
                return false;
            } else {
                String tmp = (String) req.get(index);
                if (tmp.equalsIgnoreCase("true")) {
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }
    
    /**
     * Returns the info text associated with a specific callback
     * 
     * @param state The state to fetch the info text
     * @param index The callback to fetch the info text
     * @return The info text
     * @supported.api
     */
    public String getInfoText(int state, int index) {
        // check state
        if (state > stateLength) {
            // invalid state, return empty string now
            return EMPTY_STRING;
        }
        // get internal callbacks for the state
        Callback[] callbacks = (Callback[]) internal.get(state - 1);
        if (callbacks == null || callbacks.length == 0) {
            // no callbacks defined for this state, return empty string
            return EMPTY_STRING;
        }
        // check first Callback
        Callback callback = callbacks[0];
        if (callback instanceof PagePropertiesCallback) {
            List<String> infoText = ((PagePropertiesCallback) callback).getAttribute();
            if (infoText == null || infoText.isEmpty() || index >= infoText.size()) {
                return EMPTY_STRING;
            } else {
                return infoText.get(index);
            }
        } else {
            return EMPTY_STRING;
        }
    }
    
    /**
     * Returns the attribute name for the specified callback in the
     * specified login state.
     *
     * @param state Order of state
     * @param index Order of the Callback in the Callback[], the index
     *        starts with 0.
     * @return Name of the attribute, empty string will be returned
     *         if the attribute is not defined.
     * @supported.api
     */
    public String getAttribute(int state, int index) {
        // check state
        if (state > stateLength) {
            // invalid state, return empty string now
            return EMPTY_STRING;
        }
        // get internal callbacks for the state
        Callback[] callbacks = (Callback[]) internal.get(state - 1);
        if (callbacks == null || callbacks.length == 0) {
            // no callbacks defined for this state, return empty string
            return EMPTY_STRING;
        }
        // check first Callback
        Callback callback = callbacks[0];
        if (callback instanceof PagePropertiesCallback) {
            List req = ((PagePropertiesCallback) callback).getAttribute();
            if (req == null || req.isEmpty() || index >= req.size()) {
                return EMPTY_STRING;
            } else {
                return (String) req.get(index);
            }
        } else {
            return EMPTY_STRING;
        }
    }
    
    /**
     * Aborts the authentication process.
     * <p>
     * This JAAS LoginModule method must be implemented by user's module.
     * <p>
     * This method is called if the overall authentication
     * failed. (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules did not succeed).
     * If this LoginModule's own authentication attempt succeeded (checked by
     * retrieving the private state saved by the login method), then this
     * method cleans up any state that was originally saved.
     *
     * @return <code>true</code> if this method succeeded,<code>false</code>
     *         if this LoginModule should be ignored.
     * @throws AuthLoginException if the abort fails
     * @see javax.security.auth.spi.LoginModule#abort
     */
    public final boolean abort() throws AuthLoginException {
        debug.message("ABORT return.... false");
        if (succeeded == false) {
            return false;
        } else {
            logout();
        }
        return true;
    }
    
    /**
     * Commit the authentication process (phase 2).
     * <p>
     * This JAAS LoginModule method must be implemented by user's module.
     * <p>
     * This method is called if the overall authentication
     * succeeded (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules succeeded).
     * <p>
     * If this LoginModule's own authentication attempt succeeded (checked by
     * retrieving the private state saved by the login method), then this
     * method associates relevant Principals and Credentials with the Subject
     * located in the LoginModule. If this LoginModule's own authentication
     * attempted failed, then this method removes/destroys any state that was
     * originally saved.
     *
     * @return <code>true</code> if this method succeeded, or <code>false</code>
     *         if this <code>LoginModule</code> should be ignored.
     * @throws AuthLoginException if the commit fails
     * @see javax.security.auth.spi.LoginModule#commit
     */
    public final boolean commit() throws AuthLoginException {
        principal = getPrincipal();
        if (debug.messageEnabled()) {
            debug.message(
                "AMLoginModule.commit():Succeed,principal=" + principal);
        }
        if (succeeded == false || principal == null) {
            return false;
        } else if (!subject.getPrincipals().contains(principal)) {
            subject.getPrincipals().add(principal);
            debug.message("Done added user to principal");
        }
        cleanup();
        return true;
    }
    
    /**
     * Logs out a Subject.
     * <p>
     * This JAAS LoginModule method must be implemented by user's module.
     * <p>
     * An implementation of this method might remove/destroy a Subject's
     * Principals and Credentials.
     *
     * @return <code>true</code> if this method succeeded, or <code>false</code>
     *         if this LoginModule should be ignored.
     * @throws AuthLoginException if the logout fails
     * @see javax.security.auth.spi.LoginModule#logout
     */
    public final boolean logout() throws AuthLoginException {
        // logging out
        if (subject.getPrincipals().contains(principal)) {
            subject.getPrincipals().remove(principal);
        }
        succeeded = false;
        cleanup();
        return true;
    }
    
    /**
     * Sets the <code>userID</code> of user who failed authentication.
     * This <code>userID</code> will be used to log failed authentication in
     * the OpenSSO error logs.
     *
     * @param userID user name of user who failed authentication.
     * @supported.api
     */
    public void setFailureID(String userID) {
        // get login state for this authentication session
        if (userID == null) {
            return;
        }
        debug.message("setFailureID : " + userID);
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                // may be should throw AuthLoginException here
                debug.error("Unable to set set userId : " + userID);
                return;
            }
        }
        loginState.setFailedUserId(userID);
        return ;
    }
    
    /**
     * Sets a Map of attribute value pairs to be used when the authentication
     * service is configured to dynamically create a user.
     *
     * @param attributeValuePairs A map containing the attributes
     * and its values. The key is the attribute name and the value
     * is a Set of values.
     *
     * @supported.api
     */
    public void setUserAttributes(Map attributeValuePairs) {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                debug.error("Unable to set user attributes");
                return;
            }
        }
        loginState.setUserCreationAttributes(attributeValuePairs);
        return;
    }
    
    /**
     * Validates the given user name by using validation plugin if exists
     * else it checks invalid characters in the source string.
     *
     * @param userName source string which should be validated.
     * @param regEx the pattern for which to search.
     * @throws UserNamePasswordValidationException if user name is invalid.
     * @supported.api
     */
    public void validateUserName(String userName, String regEx)
            throws UserNamePasswordValidationException {
        try {
            AMUserPasswordValidation plugin = getUPValidationInstance();
            if (plugin != null) {
                debug.message("Validating username...");
                Map envMap = new HashMap(2);
                envMap.put(
                 com.sun.identity.shared.Constants.ORGANIZATION_NAME,
                 getRequestOrg());
                plugin.validateUserID(userName, envMap);
            } else if (regEx != null && (regEx.length() != 0)) {
                if (! (ISValidation.validate(userName, regEx, debug))) {
                    throw new UserNamePasswordValidationException(bundleName,
                    "invalidChars", null);
                }
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("User Name validation Failed" + ame.getMessage());
            }
            throw new UserNamePasswordValidationException(ame);
        } catch (Exception ex) {
            debug.message(
            "unKnown Exception occured during username validation");
            throw new UserNamePasswordValidationException(ex);
        }
    }
    
    
    /**
     * Sets the moduleName of successful LoginModule.
     * This moduleName will be populated in the session
     * property "AuthType"
     * @param moduleName name of module
     */
    private void setSuccessModuleName(String moduleName) {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                debug.error("Unable to set moduleName : " + moduleName);
                return;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("SETTING Module name.... :" + moduleName);
        }
        loginState.setSuccessModuleName(moduleName);
    }
    
    /**
     * Checks if valid user exists.
     *
     * @param userDN the distinguished name of the user.
     * @return <code>true</code> if user exists,<code>false</code>otherwise
     */
    public boolean isValidUserEntry(String userDN)  {
        // TODO - IdRepo does not have an equivalent of this
        // this method is mainly called to validate DSAME Users
        // which are going to be processed differently.

        boolean isValidUser = false;
        try {
            isValidUser = 
                (AuthD.getAuth().getIdentity(IdType.USER, userDN, "/") != null);
        } catch (AuthException e) {
            debug.message("User Valid :" + isValidUser);
        }
        return isValidUser;
    }
    
    /**
     * Checks if distinguished user name is a super admin.
     *
     * @param userDN the distinguished name of the user.
     * @return <code>true</code> if distinguished user name is a super admin.
     */
    public boolean isSuperAdmin(String userDN)  {
        boolean isSuperAdmin = AuthD.getAuth().isSuperAdmin(userDN);
        if (debug.messageEnabled()) {
            debug.message("is SuperAdmin : " + isSuperAdmin);
        }
        return isSuperAdmin;
    }
    
    /**
     * Validate password for the distinguished user, this will use validation 
     * plugin if exists to validate password
     *
     * @param userPassword source string which should be validated.
     * @throws UserNamePasswordValidationException if user password is invalid.
     *
     * @supported.api
     */
    public void validatePassword(String userPassword)
    throws UserNamePasswordValidationException {
        AMUserPasswordValidation plugin = getUPValidationInstance();
        try {
            if (plugin != null) {
                if (debug.messageEnabled()) {
                    debug.message("Validating password...");
                }
                
                plugin.validatePassword(userPassword);
            } else {
                if (debug.messageEnabled()) {
                    debug.message("No plugin found");
                }
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("Password validation Failed " + ame.getMessage());
            }
            
            throw new UserNamePasswordValidationException(ame);
        } catch (Exception ex) {
            if (debug.messageEnabled()) {
                debug.message("Unknown Exception occured during password validation");
            }
            
            throw new UserNamePasswordValidationException(ex);
        }
    }
    
    /*
     * this method instantiates and returns plugin object
     */
    private AMUserPasswordValidation getUPValidationInstance() {
        
        try {
            String className ;
            String orgDN = getRequestOrg();
            if (orgDN != null){
            	className = getOrgPluginClassName(orgDN);
            }
            else {
                className = getPluginClassName();
            }

            if (debug.messageEnabled()) {
                debug.message("UserPasswordValidation Class Name is : " +
                className);
            }
            if ( (className == null) || (className.length() == 0)) {
                return null;
            }
            
            AMUserPasswordValidation userPasswordInstance =
            (AMUserPasswordValidation)
            (Class.forName(className).newInstance());
            return userPasswordInstance;
        } catch (ClassNotFoundException ce) {
            if (debug.messageEnabled()) {
                debug.message("Class not Found :", ce);
            }
            return null;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error: ", e);
            }
            return null;
        }
    }

    /*
     * this method gets plugin classname from adminstration service for the org
     */
    private String getOrgPluginClassName(String orgDN) {
        try {
            String cachedValue = AdministrationServiceListener.
                getOrgPluginNameFromCache(orgDN);
            if (cachedValue != null) {
                return cachedValue;
            }
            Map config =
            getOrgServiceTemplate(orgDN,ISAuthConstants.ADMINISTRATION_SERVICE);
            String className =
            Misc.getServerMapAttr(config,
            ISAuthConstants.USERID_PASSWORD_VALIDATION_CLASS);
            if (debug.messageEnabled()) {
                debug.message("Org Plugin Class:  " + className);
            }
            AdministrationServiceListener.setOrgPluginNameInCache(
                orgDN, className);
            return className;
        } catch (Exception ee) {
            debug.message("Error while getting UserPasswordValidationClass " ,ee );
            return null;
        }
    }
    
    /*
     * this method gets plugin classname from adminstration service
     */
    private String getPluginClassName() throws AuthLoginException {
        String cachedValue = AdministrationServiceListener.
           getGlobalPluginNameFromCache();
        if (cachedValue != null) {
               return cachedValue;
        }
        Map config = getServiceConfig(ISAuthConstants.ADMINISTRATION_SERVICE);
        String className =
        CollectionHelper.getServerMapAttr(
            config, ISAuthConstants.USERID_PASSWORD_VALIDATION_CLASS);
        if (debug.messageEnabled()) {
            debug.message("Plugin Class:  " + className);
        }
        AdministrationServiceListener.setGlobalPluginNameInCache(
            className);
        return className;
    }
    
    /**
     * Sets the moduleName of failed login module
     * @param moduleName - module name of the failed module
     */
    
    private void setFailureModuleName(String moduleName) {
        // get login state for this authentication session
        if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                debug.error("Unable to set moduleName : " + moduleName);
                return;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("SETTING Failure Module name.... :" + moduleName);
        }
        loginState.setFailureModuleName(moduleName);
        return ;
    }
    
    /**
     * Returns JAAS shared state user key.
     *
     * @return user key.
     */
    public String getUserKey() {
        return ISAuthConstants.SHARED_STATE_USERNAME;
    }
    
    /**
     * Returns JAAS shared state password key.
     *
     * @return password key
     */
    public String getPwdKey() {
        return ISAuthConstants.SHARED_STATE_PASSWORD;
    }
    
    // cleanup method for Auth constants
    private void cleanup() {
        principal = null;
        if (sharedState !=null) {
            sharedState.remove(ISAuthConstants.SHARED_STATE_USERNAME);
            sharedState.remove(ISAuthConstants.SHARED_STATE_PASSWORD);
        }
        sharedState = null;
        destroyModuleState();
    }
    
    /**
     * Stores user name password into shared state map
     * this method should be called after successfull
     * authentication by each individual modules.
     *
     * @param user user name
     * @param passwd user password
     */
    public void storeUsernamePasswd(String user, String passwd) {
        // store only if store shared state is enabled
        if (isStore && sharedState !=null) {
            sharedState.put(ISAuthConstants.SHARED_STATE_USERNAME, user);
            sharedState.put(ISAuthConstants.SHARED_STATE_PASSWORD, passwd);
        }
    }
    
    /**
     * Checks if shared state enabled for the module.
     *
     * @return <code>true</code> if shared state enabled for the module.
     */
    public boolean isSharedStateEnabled() {
        return isSharedState;
    }

    /**
     * Sets flag to force read call backs in auth chain process.
     * @param val - value to force reading call backs
     */
    public void setForceCallbacksRead(boolean val) {
        forceCallbacksRead = val;
    }

    /**
     * This method returns use first pass enabled or not
     * @return return true if use first pass is enabled for the module
     */
    public boolean isUseFirstPassEnabled() {
        return (sharedStateBehaviorPattern != null) && 
                sharedStateBehaviorPattern.equals("useFirstPass");
    }
    
    /**
     * Returns <code>AMIdentityRepostiory</code> handle for an organization.
     *
     * @param orgDN the organization name.
     * @return <code>AMIdentityRepostiory</code> object
     */
    public AMIdentityRepository getAMIdentityRepository(String orgDN) {
       return AuthD.getAuth().getAMIdentityRepository(orgDN);
    }

    /**
     * Creates <code>AMIdentity</code> in the repository.
     *
     * @param userName name of user to be created.
     * @param userAttributes Map of default attributes.
     * @param userRoles Set of default roles.
     * @throws IdRepoException
     * @throws SSOException
     */
    public void createIdentity(
        String userName,
        Map userAttributes,
        Set userRoles
    ) throws IdRepoException, SSOException {
       if (loginState == null) {
           loginState = getLoginState();
            if (loginState == null) {
                debug.error("Unable to create Identity: " + userName); 
               return ;
            }
        }
       loginState.createUserIdentity(userName,userAttributes,userRoles);
       return;
    }
   
    /**
     * Get the number of failed login attempts for a user when account locking
     * is enabled.
     * @return number of failed attempts, -1 id account locking is not enabled. 
     * @throws AuthenticationException if the user name passed in is not valid 
     * or  null, or for any other error condition.
     * @supported.api
     */
     public int getFailCount(AMIdentity amIdUser) throws AuthenticationException {
         AccountLockoutInfo acInfo = null;
         if (loginState == null) {
            loginState = getLoginState();
            if (loginState == null) {
                throw new AuthenticationException(bundleName, "nullLoginState", 
                    null);
            }
         }
         ISAccountLockout isAccountLockout = new ISAccountLockout(
            loginState.getLoginFailureLockoutMode(),
            loginState.getLoginFailureLockoutTime(),
            loginState.getLoginFailureLockoutCount(),
            loginState.getLoginLockoutNotification(),
            loginState.getLoginLockoutUserWarning(),
            loginState.getLoginLockoutAttrName(),
            loginState.getLoginLockoutAttrValue(),
            loginState.getLoginFailureLockoutDuration(),
            loginState.getLoginFailureLockoutMultiplier(),
            loginState.getInvalidAttemptsDataAttrName(),
            bundleName);
         isAccountLockout.setStoreInvalidAttemptsInDS(
         loginState.getLoginFailureLockoutStoreInDS());

         try {
             if (!isAccountLockout.isLockoutEnabled()) {
                  debug.message("Failure lockout mode disabled");
                    return -1;
             } else {
                 if (debug.messageEnabled()) {
                     debug.message("AMLogiModule.getFailCount()::"
                         +"lockout is enabled");
                 }

                 String userDN = null;
                 userDN = normalizeDN(IdUtils.getDN(amIdUser));

                 if (acInfo == null) {
                     acInfo = isAccountLockout.getAcInfo(userDN,amIdUser);
                 }
                 int failCount = acInfo.getFailCount();
                 if (debug.messageEnabled()) {
                     debug.message("AMLoginModule.getFailCount:failCount "
                          +"returned:" +failCount);
                 }
                 return failCount;
             }
         } catch (Exception ex) {
             debug.error("AMLoginModule.getFailCount:Error", ex);
             throw new AuthenticationException(ex.getMessage());
         }
    }

    /**
     * Get the maximum number failed login attempts permitted for a user
     * before when their account is locked out.
     *
     * @return the maximum number of failed attempts
     * @supported.api
     */
    public int getMaximumFailCount()
    throws AuthenticationException {
        if (loginState == null) {
            loginState = getLoginState();

            if (loginState == null) {
                throw new AuthenticationException(bundleName, "nullLoginState",
                    null);
            }
        }

        return loginState.getLoginFailureLockoutCount();
    }

    /**
     * Increments the fail count for the given user.
     *
     * @throws AuthenticationException if the user name passed in is not valid
     * or null, or for any other error condition.
     * @supported.api
     */
    public void incrementFailCount(String userName)
    throws AuthenticationException {
        if (loginState == null) {
            loginState = getLoginState();

            if (loginState == null) {
                throw new AuthenticationException(bundleName, "nullLoginState",
                    null);
            }
        }

        loginState.incrementFailCount(userName);
    }

    /**
     * Returns true if the named account is locked out, false otherwise.
     *
     * @throws AuthenticationException if the user name passed in is not valid
     * or null, or for any other error condition.
     * @supported.api
     */
    public boolean isAccountLocked(String userName)
    throws AuthenticationException {
        if (loginState == null) {
            loginState = getLoginState();

            if (loginState == null) {
                throw new AuthenticationException(bundleName, "nullLoginState",
                    null);
            }
        }

        boolean accountLocked = loginState.isAccountLocked(userName);

        if (ad.debug.messageEnabled()) {
            ad.debug.message("isAccountLocked for user=" + userName + " :" + accountLocked);
        }

        return accountLocked;
    }

    /* returns the normalized DN  */
    private String normalizeDN(String userDN) {
        String normalizedDN = userDN;
        if ((userDN != null) && DN.isDN(userDN)) {
            normalizedDN = DNUtils.normalizeDN(userDN);
        }
        if (ad.debug.messageEnabled()) {
            ad.debug.message("Original DN is:" + userDN);
            ad.debug.message("Normalized DN is:" + normalizedDN);
        }
        return normalizedDN;
    }    
    
    /**
     * Authenticates to the datastore using idRepo API
     *
     * @param callbacks Array of last submitted callbacks to the 
     * authentication module
     * @return <code>true</code> if success. <code>false</code> if failure
     * @throws <code> AuthLoginException </code> 
     */
    private boolean authenticateToDatastore(Callback[] callbacks) 
            throws AuthLoginException {
        boolean retval = false;
        boolean needToCheck = false;
        Callback[] idrepoCallbacks = new Callback[2];
        String userName = null;
        char[] password = null;
        
        for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback nc = (NameCallback) callbacks[i];
                    userName = nc.getName();
                    if (debug.messageEnabled()){
                        debug.message("AMLoginModule.authenticateToDatastore:: "
                        + " user is : " + userName);
                        debug.message("AMLoginModule.authenticateToDatastore:: "
                        + " Internal users : " + LoginState.internalUsers);
                    }
                    
                    if (LoginState.internalUsers.contains(
                            userName.toLowerCase())) {
                        needToCheck = true;
                    } else {
                        break;
                    }
                    
                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callbacks[i];
                    password = pc.getPassword();
                }
        }
        if (needToCheck == false) {
            return true;
        }
        
        if (debug.messageEnabled()){
            debug.message("AMLoginModule.authenticateToDatastore:: "
                + "Authenticating Internal user to configuration store");
        }
        NameCallback nameCallback = new NameCallback("NamePrompt");
        nameCallback.setName(userName);
        idrepoCallbacks[0] = nameCallback;
        PasswordCallback passwordCallback = new PasswordCallback(
            "PasswordPrompt",false);
        passwordCallback.setPassword(password);
        idrepoCallbacks[1] = passwordCallback;
        try {
            AMIdentityRepository idrepo = getAMIdentityRepository(
                getRequestOrg());
            retval = idrepo.authenticate(idrepoCallbacks);
            if (debug.messageEnabled()){
                debug.message("AMLoginModule.authenticateToDatastore:: " + 
                    " IDRepo authentication successful");
            }
        } catch (IdRepoException idrepoExp) {
            if (debug.messageEnabled()){
                debug.message("AMLoginModule.authenticateToDatastore::  "
                    + "IdRepo Exception : ", idrepoExp);
            }
        } catch (InvalidPasswordException ipe) {
            throw new AuthLoginException(AMAuthErrorCode.AUTH_MODULE_DENIED);
        }
        return retval;

    }

    public boolean isSessionQuotaReached(String userName) {
        int sessionCount = -1;
        int sessionQuota = -1;

        if (userName == null || userName.equals(Constants.EMPTY)) {
            debug.error("AMLoginModule.isSessionQuotaReached :: called with null username");
            return false;
        }

        try {
            // Get the universal ID
            AMIdentity amIdUser = ad.getIdentity(IdType.USER, userName,
                    loginState.getOrgDN());

            String univId = IdUtils.getUniversalId(amIdUser);

            if (univId != null) {
                sessionQuota = getSessionQuota(amIdUser);
                sessionCount = SessionCount.getAllSessionsByUUID(univId).size();

                if (debug.messageEnabled()) {
                    debug.message("AMLoginModule.isSessionQuotaReached :: univId= "
                            + univId + " - Session Quota Reached =  " + (sessionCount >= sessionQuota));
                }
            } else {
                debug.error("AMLoginModule.isSessionQuotaReached :: "
                        + "univId is null , amIdUser is " + amIdUser);
                return false;
            }
        } catch (Exception ex) {
            debug.error("AMLoginModule.getSessionQuotaLevel::  "
                    + "Exception : ", ex);
        }

        return (sessionCount >= sessionQuota);
    }

    private int getSessionQuota(AMIdentity iden) {
        int quota = SessionConstraint.getDefaultSessionQuota();
        
        if (iden == null) {
            debug.error("AMLoginModule.getSessionQuota :: AMIdentity is null, returning default quota");
            return quota;
        }

        try {
             Map serviceAttrs =
                iden.getServiceAttributesAscending("iPlanetAMSessionService");

             Set s = (Set)serviceAttrs.get("iplanet-am-session-quota-limit");
             Iterator attrs = s.iterator();
             if (attrs.hasNext()) {
                String attr = (String) attrs.next();
                quota = (Integer.valueOf(attr)).intValue();
             }
        } catch (Exception ex) {
            debug.error("Failed to get the session quota via the "+
                        "IDRepo interfaces, => Use the default " +
                        "value from the dynamic schema instead.", ex);
        }

        return quota;
   }
}
