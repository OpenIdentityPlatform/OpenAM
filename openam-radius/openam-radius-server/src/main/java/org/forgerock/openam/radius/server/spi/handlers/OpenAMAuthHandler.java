/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions Copyrighted 2016 ForgeRock AS.
 */
package org.forgerock.openam.radius.server.spi.handlers;

import static org.forgerock.openam.utils.Time.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.inject.Named;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.guava.common.base.Strings;
import org.forgerock.guava.common.eventbus.EventBus;
import org.forgerock.openam.radius.common.AccessAccept;
import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AccessReject;
import org.forgerock.openam.radius.common.ReplyMessageAttribute;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.forgerock.openam.radius.common.UserPasswordAttribute;
import org.forgerock.openam.radius.server.RadiusProcessingException;
import org.forgerock.openam.radius.server.RadiusRequest;
import org.forgerock.openam.radius.server.RadiusRequestContext;
import org.forgerock.openam.radius.server.RadiusResponse;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.forgerock.openam.radius.server.events.AuthRequestReceivedEvent;
import org.forgerock.openam.radius.server.spi.AccessRequestHandler;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.OpenAMAuthFactory;

import com.google.inject.Inject;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.shared.debug.Debug;

/**
 * This RADIUS handler authenticates against an authentication realm and chain specified via configuration. It uses
 * OpenAM's {@link com.sun.identity.authentication.AuthContext} object. This flow proceeds as follows. It is also
 * important to note that challenge answers are passed in the RADIUS packet via the password field as per the RFC 2865
 * specification. In the diagram below are shown the RADIUS CLIENT and the OPENAM instance acting as a radius server.
 * Time generally increases as we move from the top of the diagram to the bottom. However, the while and for loops
 * violate that aspect. Vertical bars imply where processing or user input is currently proceeding. Periods imply an
 * idle or waiting system.
 * <p/>
 *
 * <pre>
 *
 * RADIUS CLIENT                            OPENAM
 *      |                                      .
 *      | AccessRequest                        .
 *      | [username + password]                .
 *      + -----------------------------------> +
 *      .                                      | ac = new AuthContext(realm)
 *      .                                      | ac.login(VIA_CHAIN, chain)
 *      .                                      |
 *      .   at a minimum the auth chain used   | ac.hasMoreRequirements()
 *      .   must have a first module that      | callback[] cbs = ac.getRequirements(true)
 *      .   accepts username and password -->  | find nameCallback and inject username
 *      .                                      | find passwordCallback and inject password
 *      . AccessReject                         |
 *      + <----------------------------------- + if unable to find name/password callbacks or inject values
 *      .                                      |
 *      .                                      +-- while ac.hasMoreRequirements()
 *      .                                      .    | callback[] cbs = ac.getRequirements(true)
 *      .                                      .    |
 *      .                                      .    +-- for n=0 to cbs.length-1 for each cbs that accepts user input
 *      .                                      .    .   | issue challenge, gather response, and inject into the
 * callback
 *      . AccessChallenge                      .    .   |
 *      . [message + state(n)]                 .    .   |
 *      + <---------------------------------------------+
 *      |                                      .    .   .
 *      | AccessRequest                        .    .   .
 *      | [username + answer + state(n)]       .    .   .
 *      + --------------------------------------------->+
 *      .                                      .    .   | inject value into cbs(n)
 *      .                                      .    +---+
 *      .                                      .    |
 *      .                                      .    | ac.submit(cbs)
 *      .                                      +----+
 *      .                                      |
 *      . AccessAccept                         | s = ac.getStatus()
 *      + <----------------------------------- + if s == SUCCESS
 *      .                                      |
 *      . AccessReject                         |
 *      + <----------------------------------- + all else
 *      |                                      .
 *      |                                      .
 * </pre>
 * <p/>
 * Of special note to authentication module implementors is what modules are allowed in the chain used by a radius
 * client. If an authentication module uses {@link javax.servlet.http.HttpServletRequest} or
 * {@link javax.servlet.http.HttpServletResponse} they generally won't work for radius clients without modification. For
 * non-http clients the {@link javax.servlet.http.HttpServletRequest} and {@link javax.servlet.http.HttpServletResponse}
 * objects will be null typically leading to a {@link java.lang.NullPointerException}. Looking for a value of null is
 * how such modules can tell if they are dealing with a non-http client and adjust their behavior accordingly.
 * <p/>
 * This may include having different sets of callbacks for http clients than for radius clients. For example, a module
 * may support a checkbox causing a cookie to be set in the user's browser to remember that module's use for a period of
 * time and not require that the user leverage its unique authentication mechanism until that cookie has expired. Such a
 * feature won't work for radius clients since they have no such persistent client-side mechanism. Hence for non-http
 * clients a different callback set would most likely be needed that didn't include that checkbox and its label.
 * <p/>
 */
public class OpenAMAuthHandler implements AccessRequestHandler {

    private static final Debug LOG = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * Holds the ContextHolder instances between calls from clients. ContextHolder includes the OpenAM AuthContext
     * object that keeps track of where the user is in the process of authenticating.
     */
    private final ContextHolderCache contextCache;

    /**
     * The key in the config map whose value holds the name of the realm to which we should authenticate users.
     */
    private static final String REALM_KEY = "realm";

    /**
     * The key in the config map whose value holds the name of the authentication chain in the specified realm that
     * should be used for authenticating users.
     */
    private static final String AUTH_CHAIN_KEY = "chain";

    /**
     * The realm containing the authentication chain through which we will be authenticating.
     */
    private String realm = null;

    /**
     * The authentication chain through which we will be authenticating.
     */
    private String authChain = null;

    /**
     * A factory from which a context holder can be obtained.
     */
    private final OpenAMAuthFactory amAuthFactory;

    /**
     * The radius event bus that will be used to notify interested parties of events as they occur.
     */
    private EventBus eventBus;

    /**
     * Constructor.
     *
     * @param amAuthFactory - a factory that provides OpenAM auth entities such as AuthContexts.
     * @param contextHolderCache - the cache to be used to hold ContextHolder objects that persist state in the server
     *            (here) when control is with the client, e.g. when the client is providing user input to meet a
     *            challenge request.
     * @param eventBus - the Radius event bus that will be used to notify interested parties of
     *            <code>org.forgerock.openam.radius.server.events</code> as they occur.
     */
    @Inject
    public OpenAMAuthHandler(OpenAMAuthFactory amAuthFactory, ContextHolderCache contextHolderCache,
            @Named("RadiusEventBus") EventBus eventBus) {
        this.amAuthFactory = amAuthFactory;
        this.contextCache = contextHolderCache;
        this.eventBus = eventBus;
    }

    @Override
    public void init(Properties config) {
        realm = getConfigProperty(REALM_KEY, config, true);
        authChain = getConfigProperty(AUTH_CHAIN_KEY, config, true);

    }

    /**
     * Handles the request in potentially two distinct ways depending on whether a state attribute is found in the
     * request or not. When no state field is found this is an initial request starting the authentication process and
     * the request will have username and password embedded and ready for consumption by the first module in the chain.
     * Any request with a state attribute is a user response to a previous challenge response that we sent back to them
     * in a previously started authentication process. The number of challenge responses that are sent and their
     * corresponding replies is dependent upon the number of modules in the chain and the number of callback fields in
     * each set of callbacks. A set of callbacks represents one grouping of data needed by a module to complete its next
     * step in the authentication process that it implements. This grouping in a web environment constitutes a single
     * page into which a number of fields can receive data. However, to gather additional feedback from a user the
     * radius protocol only supports a challenge response with a text message and state and radius clients typically
     * present that message and a single text input field with a label like, "Answer", and submit and cancel buttons.
     * This means that we only get a single answer per radius challenge response. Therefore, for some callback groupings
     * we will need to return multiple challenge responses before we can submit the callback set's user response values
     * back to the module to take the next step in authentication.
     *
     * @param request
     *            the access request
     * @param response
     *            - the response to be sent to the client.
     * @param context
     *            - provides methods that the handler can use to obtain information about the context in which the
     *            request was made, for example the name and IP address of the client from which the request was
     *            received.
     * @return
     * @throws RadiusProcessingException
     *             - when the response can not be sent.
     */
    @Override
    public void handle(RadiusRequest request, RadiusResponse response, RadiusRequestContext context)
            throws RadiusProcessingException {
        LOG.message("Entering OpenAMAuthHandler.handle");

        response.setRealm(realm);

        final StateAttribute state = (StateAttribute) request.getAttribute(StateAttribute.class);
        ContextHolder holder = null;

        if (state != null) {
            final String cacheKey = state.getState();
            holder = contextCache.get(cacheKey);
        }

        // always get password attribute regardless of whether starting or returning more input since user input is
        // always sent via the password field.
        final UserPasswordAttribute credAtt = (UserPasswordAttribute) request.getAttribute(UserPasswordAttribute.class);
        String credential = null;

        try {
            credential = credAtt.extractPassword(context.getRequestAuthenticator(), context.getClientSecret());
        } catch (final IOException e) {
            LOG.error("Unable to extract credential field from RADIUS request. Denying Access.", e);
            rejectAccessAndTerminateProcess(response, holder);
            LOG.message("Leaving OpenAMAuthHandler.handle();");
            return;
        }

        if (holder == null) {
            holder = this.contextCache.createCachedContextHolder();
            request.setContextHolderKey(holder.getCacheKey());
            eventBus.post(new AuthRequestReceivedEvent(request, response, context));

            final UserNameAttribute usrAtt = (UserNameAttribute) request.getAttribute(UserNameAttribute.class);

            holder = startAuthProcess(holder, response, usrAtt, credential);
            if (holder == null || holder.getAuthPhase() == ContextHolder.AuthPhase.TERMINATED) {
                // oops. something happened and reject message was already sent. so drop out here.
                LOG.message("Leaving OpenAMAuthHandler.handle(); Auth phase is TERMINATED.");
                return;
            }
        } else {
            request.setContextHolderKey(holder.getCacheKey());
            eventBus.post(new AuthRequestReceivedEvent(request, response, context));
        }

        gatherUserInput(response, holder, credential, state);

        if (holder.getAuthPhase() == ContextHolder.AuthPhase.FINALIZING) {
            finalizeAuthProcess(response, holder);
        }

        LOG.message("Leaving OpenAMAuthHandler.handle();");
        return;
    }

    /**
     * Gets the specified property or throws an IllegalStateException if the property is not found or is empty.
     *
     * @return
     */
    private static String getConfigProperty(String propName, Properties config, boolean required) {
        final String value = config.getProperty(propName);

        if (required && Strings.isNullOrEmpty(value)) {
            throw new IllegalStateException("Configuration property '" + propName
                    + "' not found in handler configuration. "
                    + "It must be added to the Configuration Properties for this class in the Radius Client's "
                    + "configuration.");
        }
        return value;
    }

    /**
     * Evaluates if they successfully authenticated or failed and sends an AccessAllow or AccessReject accordingly.
     *
     * @param response
     *            the response that will be sent to the client.
     * @param holder
     *            holds the context for this request.
     * @throws RadiusProcessingException
     */
    private void finalizeAuthProcess(RadiusResponse response, ContextHolder holder)
            throws RadiusProcessingException {
        LOG.message("Entering OpenAMAuthHandler.finalizeAuthProcess()");
        final AuthContext.Status status = holder.getAuthContext().getStatus();

        if (status.equals(AuthContext.Status.SUCCESS)) {
            // they made it. Let them in.
            allowAccessAndTerminateProcess(response, holder);
            return;
        }

        rejectAccessAndTerminateProcess(response, holder);
    }

    private void gatherUserInput(RadiusResponse response, ContextHolder holder,
            String answer,
            StateAttribute state) {
        LOG.message("Entering gatherUserInput();");

        // we have a while loop here because there are callback sets that are empty of input callbacks and contain
        // only a properties callback. Those callback sets must simply be submitted without any input being injected
        // allowing the auth process to move to the next set. The while loop allows us to flow through without issuing
        // a challenge response, get the next set loaded, and then start sending a challenges for that set.
        while (holder.getAuthPhase() == ContextHolder.AuthPhase.GATHERING_INPUT) {
            if (holder.getCallbacks() == null) {
                LOG.message("--- callbacks == null in gatherUserInput");
                // either just starting process or just finished submitting a set of callback input values
                if (!isNextCallbackSetAvailable(response, holder)) {
                    // no further input from user needed or error occurred
                    if (holder.getAuthPhase() == ContextHolder.AuthPhase.TERMINATED) {
                        return;
                    }

                    LOG.message("--- NextCallbackSet not-available in gatherUserInput - move to finalization");
                    holder.setAuthPhase(ContextHolder.AuthPhase.FINALIZING);
                    return;
                }
            } else {
                LOG.warning("--- callbacks[" + holder.getCallbacks().length + "] in gatherUserInput - ");
                // we are gathering for current set.
                final boolean injected = injectAnswerForCallback(response, holder, answer); // answers
                                                                                                         // always come
                                                                                                         // through
                // the request's password field

                if (!injected) {
                    return; // couldn't inject and already sent reject response so exit out
                }
            }

            // new callbacks available or still gathering input for the current set. if all callbacks have values
            // then submit and loop around again to get next set else send challenge response to gather input for the
            // next callback
            final Callback[] callbacks = holder.getCallbacks();
            if (holder.getIdxOfCurrentCallback() > callbacks.length - 1) {
                LOG.warning("--- holder.idxOfCurrentCallback " + holder.getIdxOfCurrentCallback()
                        + " > holder.callbacks.length-1 " + (holder.getCallbacks().length - 1)
                        + " in gatherUserInput - submitting/set callbacks=null");
                try {
                    holder.getAuthContext().submitRequirements(callbacks);
                } catch (final Throwable t) {
                    LOG.error("Exception thrown while submitting callbacks. Rejecting access.", t);
                    rejectAccessAndTerminateProcess(response, holder);
                    return;
                }
                holder.setCallbacks(null);
            } else {
                final ReplyMessageAttribute msg = getNextCallbackReplyMsg(response, holder);

                if (msg == null) {
                    return; // failed to inject and already sent a reject msg so stop processing at this point.
                }
                // if we get here then we have a challenge response message ready to send
                final AccessChallenge challenge = new AccessChallenge();

                if (state == null) { // as when starting authentication
                    state = new StateAttribute(holder.getCacheKey());
                }
                challenge.addAttribute(state);
                challenge.addAttribute(msg);

                response.setResponsePacket(challenge);
                return; // exit out and await response to challenge response
            }
        }
    }

    /**
     * Obtains the next set of OpenAM authorization callbacks, updating our info set or sets the callbacks to null if
     * unable to acquire and update the info set and sends an accessReject response in that case. Returns true if
     * callback set was loaded into holder. Returns false if they couldn't be loaded or were empty which may be a valid
     * state depending on the caller. Sets holder.authPhase = TERMINATED if something happened causing the
     * authentication process to fail.
     *
     * @param context
     * @param holder
     * @return
     */
    private boolean isNextCallbackSetAvailable(RadiusResponse response,
            ContextHolder holder) {
        final boolean moreCallbacksAvailable = holder.getAuthContext().hasMoreRequirements();

        if (!moreCallbacksAvailable) {
            // cLog.warning("--- no callbacks available, set callbacks=null in isNextCallbackSetAvailable");
            holder.setCallbacks(null);
            return false;
        }

        // true means do NOT filter PagePropertiesCallbacks
        final Callback[] callbacks = holder.getAuthContext().getRequirements(true);
        holder.setCallbacks(callbacks);

        if (holder.getCallbacks() == null) { // should never happen but example online included check
            // cLog.warning("--- callbacks == null after ac.getReqs() called in isNextCallbackSetAvailable");
            return false;
        }

        // process page properties piece
        if (callbacks[0] instanceof PagePropertiesCallback) { // not a formal callback, openam specific
            final PagePropertiesCallback pp = (PagePropertiesCallback) callbacks[0];
            holder.setCallbackSetProps(pp);
            holder.setIdxOfCurrentCallback(1); // since page properties cb is at zero index
            final String moduleName = pp.getModuleName();

            if (!moduleName.equals(holder.getModuleName())) {
                // entering new module
                holder.setModuleName(moduleName);
                holder.incrementChainModuleIndex();
                holder.setIdxOfCallbackSetInModule(0);
                // cLog.warning("New Module Incurred: " + holder.moduleName + " with callbacks["
                // + holder.callbacks.length + "]");
            } else {
                holder.incrementIdxOfCallbackSetInModule();
                // cLog.warning("New Callback Set[" + holder.callbacks.length + "] Incurred in Module: "
                // + holder.moduleName);
            }
            // update the
            holder.setMillisExpiryForCurrentCallbacks(1000L * pp.getTimeOutValue());
            holder.setMillisExpiryPoint(currentTimeMillis() + holder.getMillisExpiryForCurrentCallbacks());
        } else {
            LOG.error("Callback at index 0 is not of type PagePropertiesCallback!!!");
            rejectAccessAndTerminateProcess(response, holder);
            return false;
        }

        // now fail fast if we find unsupportable callback types
        boolean httpCbIncurred = false;
        boolean redirectCbIncurred = false;

        for (int i = 1; i < callbacks.length; i++) {
            final Callback cb = callbacks[i];
            if (cb instanceof HttpCallback) {
                httpCbIncurred = true;
                break;
            } else if (cb instanceof RedirectCallback) {
                redirectCbIncurred = true;
                break;
            }
        }
        if (httpCbIncurred || redirectCbIncurred) {
            LOG.error("Radius can not support "
                    + (httpCbIncurred ? HttpCallback.class.getSimpleName() : RedirectCallback.class.getSimpleName())
                    + " used by module " + holder.getChainModuleIndex() + " with name " + holder.getModuleName()
                    + " in chain '" + this.authChain + "'. Denying Access.");
            rejectAccessAndTerminateProcess(response, holder);
            return false;
        }
        return true;
    }

    /**
     * Sends a RADIUS AccessReject response and cleans up the cache and authentication context if it not null by calling
     * its logout method.
     *
     * @param respHandler
     *            the response handler for the request
     * @param holder
     *            - the context holder for this radius server
     */
    private void rejectAccessAndTerminateProcess(RadiusResponse response, ContextHolder holder) {
        response.setResponsePacket(new AccessReject());
        response.setUniversalId(holder.getUniversalId());
        terminateAuthnProcess(holder);
    }

    /**
     * Sends RADIUS AccessAccept response and cleans up the cache and authentication context.
     *
     * @param respHandler
     * @param holder
     * @throws RadiusProcessingException
     */
    private void allowAccessAndTerminateProcess(RadiusResponse response, ContextHolder holder)
            throws RadiusProcessingException {
        response.setResponsePacket(new AccessAccept());
        response.setUniversalId(holder.getUniversalId());
        terminateAuthnProcess(holder);
    }

    /**
     * Removes the holder from cache, sets the state to terminated, and calls logout() on OpenAM's AuthContextLocal to
     * terminate open am's session since RADIUS only uses open am for authenticating and won't send any further requests
     * related to this access grant.
     *
     * @param holder
     */
    private void terminateAuthnProcess(ContextHolder holder) {
        contextCache.remove(holder.getCacheKey());
        holder.setAuthPhase(ContextHolder.AuthPhase.TERMINATED);

        if (holder.getAuthContext() != null && holder.getAuthContext().getStatus() == AuthContext.Status.SUCCESS) {
            try {
                holder.getAuthContext().logout();
            } catch (final AuthLoginException e) {
                LOG.error("Unable to logout of AuthContext while terminating RADIUS auth sequence.", e);
            }
        }
    }

    /**
     * Injects the user's answer into the callback currently waiting for one with proper handling for the type of
     * callback. Increments the index of the current callback and returns true if the value was successly injected or
     * false if it failed and terminated authentication.
     *
     * @param respHandler
     * @param holder
     * @param answer
     */
    private boolean injectAnswerForCallback(RadiusResponse response, ContextHolder holder, String answer) {
        final Callback[] callbacks = holder.getCallbacks();
        if (callbacks == null) {
            return false;
        }
        final Callback cb = callbacks[holder.getIdxOfCurrentCallback()];
        holder.incrementIdxOfCurrentCallback(); // so that we are sitting on that callback in the next call

        if (cb instanceof NameCallback) {
            final NameCallback nc = (NameCallback) cb;
            ((NameCallback) cb).setName(answer);
            // cLog.warning("--- set NameCallback=" + answer);
        } else if (cb instanceof PasswordCallback) {
            final PasswordCallback pc = (PasswordCallback) cb;
            pc.setPassword(answer.toCharArray());
            // cLog.warning("--- set PasswordCallback=" + answer);
        } else if (cb instanceof ChoiceCallback) {
            final ChoiceCallback cc = (ChoiceCallback) cb;
            final int maxIdx = cc.getChoices().length - 1;

            if ("".equals(answer)) {
                // user didn't provide an answer so accept default
                cc.setSelectedIndex(cc.getDefaultChoice());
                // cLog.warning("--- set ChoiceCallback=default(" + cc.getDefaultChoice() + ")");
                return true;
            }
            final boolean answerContainsSeparator = answer.indexOf(' ') != -1;
            if (cc.allowMultipleSelections() && answerContainsSeparator) {
                // may need to parse answer
                if (answerContainsSeparator) {
                    final String[] answers = answer.split(" ");
                    final List<Integer> idxs = new ArrayList<Integer>();

                    for (final String ans : answers) {
                        if (!"".equals(ans)) {
                            final int idx = parseInt(response, ans, answer, maxIdx, holder, cb);
                            if (idx == -1) {
                                // failed parsing and sent reject message so return.
                                // cLog.warning("--- ChoiceCallback failed parsing mult");
                                return false;
                            }
                            idxs.add(idx);
                        }
                    }
                    final int[] selected = new int[idxs.size()];
                    for (int i = 0; i < selected.length; i++) {
                        selected[i] = idxs.get(i);
                    }
                    cc.setSelectedIndexes(selected);
                    // cLog.warning("--- set ChoiceCallback=" + Arrays.asList(selected));

                }
            } else {
                final int idx = parseInt(response, answer, answer, maxIdx, holder, cb);
                if (idx == -1) {
                    // failed parsing and send reject message so return.
                    // cLog.warning("--- ChoiceCallback failed parsing");
                    return false;
                }
                cc.setSelectedIndex(idx);
                // cLog.warning("--- set ChoiceCallback=" + idx);
            }
        } else if (cb instanceof ConfirmationCallback) {
            final ConfirmationCallback cc = (ConfirmationCallback) cb;
            final int maxIdx = cc.getOptions().length - 1;

            if ("".equals(answer)) {
                // user didn't provide an answer so accept default
                cc.setSelectedIndex(cc.getDefaultOption());
                // cLog.warning("--- set ConfirmationCallback=default(" + cc.getDefaultOption() + ")");
                return true;
            }
            final int idx = parseInt(response, answer, answer, maxIdx, holder, cb);
            if (idx == -1) {
                // failed parsing and send reject message so return.
                // cLog.warning("--- ConfirmationCallback failed parsing");
                return false;
            }
            cc.setSelectedIndex(idx);
            // cLog.warning("--- set ConfirmationCallback=" + idx);
        } else {
            LOG.error("Unrecognized callback type '" + cb.getClass().getSimpleName()
                    + "' while processing challenge response. Unable to submit answer. Denying Access.");
            rejectAccessAndTerminateProcess(response, holder);
            return false;
        }
        // reset the timeout since we just received confirmation that the user is still there.
        holder.setMillisExpiryPoint(currentTimeMillis() + holder.getMillisExpiryForCurrentCallbacks());
        return true;
    }

    /**
     * Parses the String intVal as an integer returning that value or returning a -1 indicating that parsing failed
     * terminating authentication, logging a suitable message, and sending the access reject response if the string is
     * not a valid number or is out of range.
     *
     * @param response
     *            the response to the radius request.
     * @param intVal
     * @param answer
     * @param maxIdx
     * @param holder
     * @param cb
     * @param respHandler
     * @return
     */
    private int parseInt(RadiusResponse response, String intVal, String answer, int maxIdx, ContextHolder holder,
            Callback cb) {
        int idx = -1;
        try {
            idx = Integer.parseInt(intVal);
        } catch (final NumberFormatException e) {
            LOG.error("Invalid number '" + intVal + "' specified in answer '" + answer + "' for callback "
                    + holder.getIdxOfCurrentCallback() + " of type " + cb.getClass().getSimpleName()
                    + " for callback set " + holder.getIdxOfCallbackSetInModule() + " in module "
                    + holder.getChainModuleIndex()
                    + (holder.getModuleName() != null ? " with name " + holder.getModuleName() : "")
                    + " of authentication chain " + authChain + " in realm " + realm + ". Denying Access.");
            rejectAccessAndTerminateProcess(response, holder);
            return idx;
        }
        if (idx < 0 || idx > maxIdx) {
            LOG.error("Out of range index specified in answer '" + answer + "' for callback "
                    + holder.getIdxOfCurrentCallback() + " of type " + cb.getClass().getSimpleName()
                    + " for callback set " + holder.getIdxOfCallbackSetInModule() + " in module "
                    + holder.getChainModuleIndex()
                    + (holder.getModuleName() != null ? " with name " + holder.getModuleName() : "")
                    + " of authentication chain " + authChain + " in realm " + realm + ". Must be from 0 to " + maxIdx
                    + ". Denying Access.");
            rejectAccessAndTerminateProcess(response, holder);
            return -1;
        }
        return idx;
    }

    /**
     * Starts the authentication process by creating a new AuthContextLocale and the submitted username and password and
     * passing those to the first module in the authentication chain and completing authentication if that is the only
     * module in the chain or crafting a suitable challenge response to start gathering values for the next module's
     * callbacks. Returns true if authentication was started and user requirements beyond username and password can now
     * be solicited or false if starting failed and a reject message has already been generated.
     *
     * @param response
     *            the response object for the radius request
     * @param reqAttsMap
     * @param credential
     */
    private ContextHolder startAuthProcess(ContextHolder holder, RadiusResponse response, UserNameAttribute usrAtt,
            String credential) {
        LOG.message("Entering OpenAMAuthHandler.startAuthProcess");

        // now create an authContext and trigger loading of whatever authN modules will be used
        try {
            holder.setAuthContext(amAuthFactory.getAuthContext(realm));
        } catch (final AuthLoginException e) {
            LOG.error("Unable to start create " + AuthContext.class.getName() + ". Denying Access.", e);
            rejectAccessAndTerminateProcess(response, holder);
            LOG.message("Leaving OpenAMAuthHandler.startAuthProcess");
            return holder;
        }

        try {
            holder.getAuthContext().login(AuthContext.IndexType.SERVICE, authChain);
        } catch (final AuthLoginException e) {
            LOG.error("Unable to start login process. Denying Access.", e);
            rejectAccessAndTerminateProcess(response, holder);
            LOG.message("Leaving OpenAMAuthHandler.startAuthProcess");
            return holder;
        }

        if (!isNextCallbackSetAvailable(response, holder)) {
            // couldn't get the callbacks or failure occurred. If failure didn't occur then we need to fail out here
            // since we must have callbacks when starting up the authn process to handle username and password.
            if (holder.getAuthPhase() != ContextHolder.AuthPhase.TERMINATED) {
                LOG.error("Unable to start login process. No callbacks available. Denying Access.");
                rejectAccessAndTerminateProcess(response, holder);
            }
            LOG.message("Leaving OpenAMAuthHandler.startAuthProcess");
            return holder;
        }

        // for RADIUS we have username and password within the initial request. Therefore, the first module in the
        // chain must support a name and password callback. so walk the set of callbacks representing the first
        // module and inject and then test for further module requirements. if any exist then we must craft a
        // suitable challenge response and await the next request that gets submitted after the radius client has
        // gathered those values.
        boolean injectedUsr = false;
        boolean injectedPwd = false;

        final Callback[] callbacks = holder.getCallbacks();
        for (int i = holder.getIdxOfCurrentCallback(); i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                holder.incrementIdxOfCurrentCallback();
                final NameCallback nm = (NameCallback) callbacks[i];
                nm.setName(usrAtt.getName());
                injectedUsr = true;
            } else if (callbacks[i] instanceof PasswordCallback) {
                holder.incrementIdxOfCurrentCallback();
                final PasswordCallback pc = (PasswordCallback) callbacks[i];
                pc.setPassword(credential.toCharArray());
                injectedPwd = true;
            } else {
                holder.incrementIdxOfCurrentCallback();
            }
        }
        // did we have NameCallback and PasswordCallback to inject the username and password?
        if (injectedUsr && injectedPwd) {
            holder.getAuthContext().submitRequirements(callbacks);
            // cLog.warning("--- submitting usr/pwd in startAuthProcess, set callbacks=null");
            holder.setCallbacks(null); // triggers loading of next set, conveys to gatherer that we have just started
        } else {
            // if we get here and didn't submit, then the callbacks array representing the requirements of the first
            // module in the chain didn't support username and password. So log the error and reject access.
            final String msg = "First callback set for first module"
                    + (holder.getModuleName() != null ? " '" + holder.getModuleName() + "'" : "")
                    + " in authentication chain '" + this.authChain
                    + "' does not support Username and Password callbacks. Denying Access.";
            LOG.error(msg);
            rejectAccessAndTerminateProcess(response, holder);
        }
        // if we get here then we successfully started the authN process
        holder.setAuthPhase(ContextHolder.AuthPhase.GATHERING_INPUT);
        LOG.message("Leaving OpenAMAuthHandler.startAuthProcess");
        return holder;
    }

    /**
     * Generates reply message for the current callback to be embedded in a challenge response to gather an answer for
     * that callback. If an unknown/unexpected callback type is incurred the process is terminated with a reject
     * response.
     *
     * @param respHandler
     * @param holder
     * @return
     */
    private ReplyMessageAttribute getNextCallbackReplyMsg(RadiusResponse response,
            ContextHolder holder) {
        LOG.message("Entering getNextCallbackReplyMsg()");
        ReplyMessageAttribute msg = null;
        final Callback[] callbacks = holder.getCallbacks();
        if (callbacks == null) {
            return null;
        }
        final Callback cb = callbacks[holder.getIdxOfCurrentCallback()];

        String header = "";
        final PagePropertiesCallback pagePropCallback = holder.getCallbackSetProps();
        if (pagePropCallback != null && !"".equals(pagePropCallback.getHeader())) {
            header = pagePropCallback.getHeader() + " ";
        }

        if (cb instanceof NameCallback) {
            LOG.message("getNextCallbackReplyMsg(); - processing NameCallback.");
            msg = new ReplyMessageAttribute(header + ((NameCallback) cb).getPrompt());
        } else if (cb instanceof PasswordCallback) {
            LOG.message("getNextCallbackReplyMsg(); - processing PasswordCallback.");
            msg = new ReplyMessageAttribute(header + ((PasswordCallback) cb).getPrompt());
        } else if (cb instanceof ChoiceCallback) {
            LOG.message("getNextCallbackReplyMsg(); - processing ChoiceCallback.");
            final ChoiceCallback cc = (ChoiceCallback) cb;
            final StringBuilder sb = new StringBuilder();
            sb.append(header);
            sb.append(cc.getPrompt());
            if (cc.allowMultipleSelections()) {
                // ugh. we'll have to figure out how to translate this suitably in view of sentence structure for
                // a given locale.
                sb.append(" (Separate Selected Numbers by Spaces"); // TODO: LOCALIZE
                if (cc.getDefaultChoice() >= 0) {
                    sb.append(". Default is " + cc.getDefaultChoice());
                }
                sb.append(".)");
            }
            sb.append('\n');
            final String[] choices = cc.getChoices();

            for (int j = 0; j < choices.length; j++) {
                final String choice = choices[j];
                if (j != 0) {
                    sb.append(",\n");
                }
                sb.append(j);
                sb.append(" = ");
                sb.append(choice);
            }
            msg = new ReplyMessageAttribute(sb.toString());
        } else if (cb instanceof ConfirmationCallback) {
            LOG.message("getNextCallbackReplyMsg(); - processing ConformationCallback.");
            final ConfirmationCallback cc = (ConfirmationCallback) cb;
            final StringBuilder sb = new StringBuilder();
            sb.append(header);
            String prompt = cc.getPrompt();
            if (prompt != null) {
               sb.append(prompt);
            }
            if (cc.getDefaultOption() >= 0) {
                // ugh. ditto on above translation concern
                sb.append(" (Default is ");
                sb.append(cc.getDefaultOption());
                sb.append(".)");
            }
            sb.append('\n');
            final String[] options = cc.getOptions();

            for (int j = 0; j < options.length; j++) {
                final String option = options[j];
                if (j != 0) {
                    sb.append(",\n");
                }
                sb.append(j);
                sb.append(" = ");
                sb.append(option);
            }
            msg = new ReplyMessageAttribute(sb.toString());
        } else { // unknown and unexpected type
            LOG.error("Radius can not support " + cb.getClass().getSimpleName() + " used by module "
                    + holder.getChainModuleIndex() + " with name " + holder.getModuleName() + " in chain '"
                    + this.authChain + "'. Denying Access.");
            rejectAccessAndTerminateProcess(response, holder);
        }
        LOG.message("Entering getNextCallbackReplyMsg() returning '" + msg + "'");
        return msg;
    }

}
