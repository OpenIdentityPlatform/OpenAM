/* The contents of this file are subject to the terms
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
 * $Id: SetupBean.java,v 1.2 2009/08/24 11:48:24 hubertlvg Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan and Robert Nguyen
 */
package com.sun.identity.openid.provider;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.event.PhaseEvent;
import javax.servlet.http.HttpSession;

import org.openid4java.association.AssociationException;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.DirectError;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.message.sreg.SRegResponse;
import org.openid4java.server.ServerException;
import org.openid4java.server.ServerManager;

import com.sun.identity.openid.persistence.AttributePersistenceFactory;
import com.sun.identity.openid.persistence.BackendException;
import java.net.URL;
import java.security.Principal;

/**
 * TODO: Description.
 * 
 * @author pbryan, robert nguyen, hubert A. le van gong
 * 
 */
public class SetupBean extends CheckidBean {

	/**
	 * persistent instance, used to retrieve and save attibutes. This is Ldap
	 * implementation, a database can be used instead
	 */
	private AttributePersistenceFactory persistor = AttributePersistenceFactory
			.getInstance();

	/** TODO: Description. */
	private static final String QUERY = ParameterList.class.getName();
	/** Attribute name to store types map in view. */
	private static final String TYPES = SetupBean.class.getName() + ".types";
	/** Indicates that a field contains a date value. */
	private static final String TYPE_DATE = "date";
	/** Indicates that a field contains a select value. */
	private static final String TYPE_SELECT = "select";
	/** Indicates that a field contains a text input value. */
	private static final String TYPE_TEXT = "text";
	/** Map of required field names for ax. Lazily initialized. */
	private Map<String, Boolean> required = null;
	/** Map of optional field names for ax. Lazily initialized. */
	private Map<String, Boolean> optional = null;
	/** Map of selected fields for ax. Lazily initialized. */
	private Map<String, Boolean> selected = null;
	/** Map of required field names for sr. Lazily initialized. */
	private HashMap<String, Boolean> requiredSR;
	/** Map of optional field names for sr. Lazily initialized. */
	private HashMap<String, Boolean> optionalSR;
	/** Map of selected fields for sr. Lazily initialized. */
	private Map<String, Boolean> selectedSR;
	/** Map of requested fields (required + optional). Lazily initialized. */
	private Map<String, Boolean> requested = null;
	/** Map of field names to data types. */
	private static Map<String, String> types = null;
	/** Map of field names to field values. */
	private Map<String, Object> values = new HashMap<String, Object>();
	/** auth response */
	private Message responsem;
	/** auth response message */
	String responseText;
	/** request parameter from auth request */
	private ParameterList requestp;
	/** Error to display if query population failed. */
	private String error = null;
	/** openid.claimed_id */
	private String userSelectedId = null;
	/** openid.identity */
	private String userSelectedClaimedId = null;
	/** provider manager */
	private ServerManager manager = null;
	/** check if auth request has attributes exchange */
	private Boolean attrX = false;
	/** check if auth request has simple registration */
	private Boolean sr = false;
	/** simple registration request */
	private SRegRequest sregReq = null;
	/** attribute exchange request */
	private FetchRequest fetchReq = null;
	/** user id */
	private String uid;
	/** if user want to save attibutes map for thereply party */
	private Boolean saved = Boolean.FALSE;

	/** The list of AM Attributes and types from Provider.properties */
	private static String[] amAttrNames = null;

	/* check the configuration file if persistent enable */
	private Boolean persistEnabled = Config.getBoolean(Config.PERSIST);

	public Boolean getPersistEnabled() {
		return persistEnabled;
	}

	public Boolean getSaved() {
		return saved;
	}

	public void setSaved(Boolean saved) {
		this.saved = saved;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}

	public void setSelected(Map<String, Boolean> selected) {
		this.selected = selected;
	}

	public Map<String, Boolean> getRequired() {
		return required;
	}

	public Boolean getAttrX() {
		return attrX;
	}

	public HashMap<String, Boolean> getRequiredSR() {
		return requiredSR;
	}

	public Boolean getSr() {
		return sr;
	}

	/**
	 * TODO: Description.
	 */
	@SuppressWarnings("unchecked")
	public SetupBean() {
		super();
		init();
		// setup parameter

		// valid query not in request; try to find one in view
		if (requestp == null) {
			requestp = (ParameterList) attributes.get(QUERY);
		}

		// store query in view so it will persist between requests
		attributes.put(QUERY, requestp);

		// rebuild types map (if any) to initialize values map with empty values
		define((HashMap<String, String>) attributes.get(TYPES));

		// types goes into view to have it available at next view creation
		attributes.put(TYPES, types);
	}

	static {
		String fetchingAttrs = Config.getString(Config.AM_PROFILE_ATTRIBUTES);
		amAttrNames = fetchingAttrs.split(",");
		
		// set types for ax and sr from Provider.properties
		types = Config.getTypes();
	}

	public Map<String, String> getTypes() {
		return types;
	}

	/**
	 * TODO: Description.
	 * 
	 * @param list
	 *            TODO.
	 * @return TODO.
	 */
	private static HashMap<String, Boolean> listMap(List<String> list) {
		HashMap<String, Boolean> map = new HashMap<String, Boolean>();

		// null list yields empty (non-null) map
		if (list == null) {
			return map;
		}

		for (String item : list) {
			map.put(item, true);
		}

		return map;
	}

	/**
	 * Returns a value, padded with leading zeroes to satisfy the requested
	 * length.
	 * 
	 * @param value
	 *            the value to pad with leading zeroes.
	 * @param length
	 *            length value should be with leading zeroes.
	 * @return the value, padded with leading zeroes, to satisfy length.
	 */
	private static String leadingZeroes(String value, int length) {
		// absence of a value indicates not selected (all zeroes)
		if (value == null) {
			value = "";
		}

		StringBuffer buf = new StringBuffer(length);

		for (int n = value.length(); n < length; n++) {
			buf.append('0');
		}

		buf.append(value);

		return buf.toString();
	}

	/**
	 * Returns a value, where the leading zeroes have been removed
	 * 
	 * @param value
	 *            the value to remove the leading zeroes.
	 * 
	 * @return the value,with leading zeroes removed.
	 */
	private static String removeLeadingZeroes(String value) {
		// absence of a value indicates not selected (all zeroes)
		if (value == null) {
			return null;
		}

		char[] chars = value.toCharArray();
		int index = 0;
		for (; index < value.length(); index++) {
			if (chars[index] != '0') {
				break;
			}
		}
		return (index == 0) ? value : value.substring(index);
	}

	/**
	 * Returns a date field in the format required by the OpenID Simple
	 * Registration Extension specification (YYYY-MM-DD), with zeroes
	 * representing any component of the date the user has not specified.
	 * 
	 * @return the date field in the Simple Registration Extension format.
	 */
	private static String srxDate(HashMap<String, String> field) {
		if (field == null) {
			return null;
		}

		return leadingZeroes(field.get("year"), 4) + '-'
				+ leadingZeroes(field.get("month"), 2) + '-'
				+ leadingZeroes(field.get("day"), 2);
	}

	/**
	 * Sets the field type definitions from an another field types map.
	 * 
	 * This method is used to initialize the types map from one found in the
	 * view, in order to initialize associated values in the values map.
	 * 
	 * @param map
	 *            a types map found in the view to use as a source.
	 */
	private void define(HashMap<String, String> map) {
		if (map == null || map.size() == 0) {
			return;
		}

		for (String name : map.keySet()) {
			define(name, map.get(name));
		}
	}

	/** get Atrribute exhange and Simple Registration info from request */
	@SuppressWarnings("unchecked")
	private void getAXnSR() {
		try {
			AuthRequest authReq = null;
			authReq = AuthRequest.createAuthRequest(requestp, manager
					.getRealmVerifier());
			MessageExtension ext = null;
			if ((Config.getBoolean(Config.ATTRIBUTE_EXCHANGE))
					&& authReq.hasExtension(AxMessage.OPENID_NS_AX)) {
				ext = authReq.getExtension(AxMessage.OPENID_NS_AX);
				attrX = true;
				if (ext instanceof FetchRequest) {
					fetchReq = (FetchRequest) ext;
					required = fetchReq.getAttributes(true);
					optional = fetchReq.getAttributes(false);

				}
			}
			if (Config.getBoolean(Config.SIMPLE_REGISTRATION)
					&& authReq.hasExtension(SRegMessage.OPENID_NS_SREG)) {
				ext = authReq.getExtension(SRegMessage.OPENID_NS_SREG);
				sr = true;
				sregReq = (SRegRequest) ext;
				requiredSR = listMap(sregReq.getAttributes(true));
				optionalSR = listMap(sregReq.getAttributes(false));
			}

			requested = getRequested();
			selected = getSelected();
			selectedSR = getSelectedSR();

		} catch (MessageException ex) {
			Logger.getLogger(SetupBean.class.getName()).log(Level.SEVERE, null,
					ex);
		} catch (Exception e) {
			Logger.getLogger(SetupBean.class.getName()).log(Level.SEVERE, null,
					e);
		}
	}

	/** populate and set attributes from request */
	private void init() {		
		// get session
		HttpSession session = request.getSession();

		/* get provider manager and query attributes for RP */
		manager = ProviderServlet.getManager();
		requestp = (ParameterList) session.getAttribute("parameterlist");
		userSelectedId = requestp.hasParameter("openid.claimed_id") ? requestp
				.getParameterValue("openid.claimed_id") : null;
		userSelectedClaimedId = requestp.hasParameter("openid.identity") ? requestp
				.getParameterValue("openid.identity")
				: null;

		/* populate user attributes */

		Map principalAttrs = (Map) session.getAttribute("principalAttrs");
		if (null != principalAttrs) {
			Set val = (Set) principalAttrs.get(Config
					.getString(Config.AM_SEARCH_ATTRIBUTE));
			if (val != null) {
				uid = (String) val.iterator().next();
				Map<String, String> retrievedMap = null;

				try {
					retrievedMap = persistor.getAttributes(uid, getTrustRoot());
				} catch (BackendException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (null != retrievedMap) {
					setAttributes2Values(retrievedMap);
				} else {
					setAMAttributes2Values(principalAttrs);
				}

			} else {
				Logger
						.getLogger(SetupBean.class.getName())
						.log(
								Level.WARNING,
								Config.AM_PROFILE_ATTRIBUTES
										+ "does not contain "
										+ Config
												.getString(Config.AM_SEARCH_ATTRIBUTE)
										+ "\n the current value is: "
										+ Config
												.getString(Config.AM_PROFILE_ATTRIBUTES)
										+ "\nThis will prevent getting attributes from session or backend");

			}
		}

		/* get the AX and SR attributes from RP */
		getAXnSR();
	}

	/**
	 * Reponds with a redirect to the relying party with a positive assertion
	 * that the user authenticated successfully.
	 */
	private void grant() {
		try {
			manager.sign((AuthSuccess) responsem);
			request.getSession().removeAttribute("servermanager");
			request.getSession().removeAttribute("parameterlist");
			request.getSession().removeAttribute("principalAttrs");
			request.getSession().removeAttribute("login");

		} catch (ServerException ex) {
			Logger.getLogger(SetupBean.class.getName()).log(Level.SEVERE, null,
					ex);
		} catch (AssociationException ex) {
			Logger.getLogger(SetupBean.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		sendRedirect(((AuthSuccess) responsem).getDestinationUrl(true));
	}


    private void cancel() {
		Boolean authenticatedAndApproved = Boolean.FALSE;

		responsem = manager.authResponse(requestp, userSelectedId,
				userSelectedClaimedId, authenticatedAndApproved.booleanValue());
		request.getSession().removeAttribute("parameterlist");
		request.getSession().removeAttribute("servermanager");

		sendRedirect((responsem).getDestinationUrl(true));
    }


	/**
	 * Redirects to the OpenSSO login page.
	 */
	private void redirectToLogin() {

		String loginURL = Config.getString(Config.LOGIN_URL);
		String gotoURL = Config.getString(Config.LOCAL_AUTH_URL);

		StringBuffer buf = new StringBuffer(loginURL);

		if (loginURL.indexOf("?") == -1) {
			buf.append("?goto=");
		} else {
			buf.append("&goto=");
		}

		try {
			buf.append(URLEncoder.encode(gotoURL, "UTF-8"));
			// buf.append(gotoURL);
		} catch (UnsupportedEncodingException uee) {
			throw new IllegalStateException(uee);
		}

		Logger.getLogger(SetupBean.class.getName()).log(Level.INFO,
				"redirectring for OpenSSO auth\n" + buf.toString());

		sendRedirect(buf.toString());
	}

	/**
	 * Exposes the read-only bean property "requested", which is an intersection
	 * of the entries of the "required" and "optional" properties.
	 * 
	 * @return TODO.
	 */
	public Map<String, Boolean> getRequested() {
		// lazy initialization (synchronization not required)
		if (requested == null) {
			HashMap<String, Boolean> map = new HashMap<String, Boolean>();
			if (null != required) {
				map.putAll(required);
			}
			if (null != optional) {
				map.putAll(optional);
			}
			if (null != requiredSR) {
				map.putAll(requiredSR);
			}
			if (null != optionalSR) {
				map.putAll(optionalSR);
			}
			requested = map;
		}

		return requested;
	}

	/**
	 * Exposes the bean property "selected", which is a map containing the
	 * selected checkboxes in the AX form.
	 * 
	 * @return TODO.
	 */
	public Map<String, Boolean> getSelected() {
		// lazy initialization (synchronization not required)
		if (selected == null) {
			HashMap<String, Boolean> map = new HashMap<String, Boolean>();
			if (null != required) {
				map.putAll(required);
			}
			if (null != optional) {
				map.putAll(optional);
			}
			selected = map;

		}

		return selected;
	}

	/**
	 * Exposes the bean property "selected", which is a map containing the
	 * selected checkboxes in the SR form.
	 * 
	 * @return TODO.
	 */
	public Map<String, Boolean> getSelectedSR() {
		if (selectedSR == null) {
			HashMap<String, Boolean> map = new HashMap<String, Boolean>();
			if (null != requiredSR) {
				map.putAll(requiredSR);
			}
			if (null != optionalSR) {
				map.putAll(optionalSR);
			}
			selectedSR = map;

		}

		return selectedSR;

	}

	public void setSelectedSR(Map<String, Boolean> selectedSR) {
		this.selectedSR = selectedSR;
	}

	/**
	 * Returns the page field values, each having a name and value.
	 * 
	 * @return a map containing the field values.
	 */
	public Map<String, Object> getValues() {
		return values;
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 */
	@Override
	public void beforeRenderResponse(PhaseEvent event)
			throws BadRequestException {
		// if error encountered in constructor, display it now in error page
		String mode = requestp.hasParameter("openid.mode") ? requestp
				.getParameterValue("openid.mode") : null;
		if (mode == null) {
			throw new BadRequestException(error);
		}

		// ensure that user is logged in
		HttpSession session = request.getSession();

		if (session.getAttribute("login") == null) {
			redirectToLogin();
			return;
		}

        Principal principal = (Principal) session.getAttribute("principal");


        URL userSelectedIdUrl = null;
        try {
            userSelectedIdUrl = new URL(userSelectedId);
        } catch (MalformedURLException ex) {
            Logger.getLogger(SetupBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!this.identityMatches(principal, userSelectedIdUrl)) {
            cancel();
            return;
        }


		// TODO: persistent trust: call grant/deny method here accordingly

		/*
		 * If the user is required to interact with the user interface, this
		 * method completes normally (here), causing the trust management form
		 * to be rendered.
		 */
	}

	/**
	 * Defines a form field and its associated data type.
	 * 
	 * This is called by the page in the form of my:define tags, which are used
	 * during view creation to establish what fields are being handled by this
	 * backing bean, and what data types each field has.
	 * 
	 * This method allows the backing bean to handle forms abstractly without
	 * needing to know the actual fields or data types being presented in the
	 * page.
	 * 
	 * @param name
	 *            name of field in form.
	 * @param type
	 *            data type of field (either: text, select or date).
	 */
	public void define(String name, String type) {
		// store the type in the type map for future reference
		types.put(name, type);

		// don't create placeholder in values if value already present
		if (values.get(name) != null) {
			return;
		}

		// add string type to value map for text or select type fields
		if (type.equals(TYPE_TEXT) || type.equals(TYPE_SELECT)) {
			values.put(name, "");
		} // add map type to value map for date type fields
		else if (type.equals(TYPE_DATE)) {
			values.put(name, new HashMap<String, String>());
		}
	}

	/**
	 * TODO: Description.
	 * 
	 * @return the string constant "grant".
	 */
	@SuppressWarnings("unchecked")
	public String grantOnce() throws BadRequestException {

		Boolean authenticatedAndApproved = Boolean.TRUE;

		// --- process an authentication request ---

		responsem = manager.authResponse(requestp, userSelectedId,
				userSelectedClaimedId, authenticatedAndApproved.booleanValue(),
				false); // Sign after we added extensions.

		if (response instanceof DirectError) {
			responseText = responsem.keyValueFormEncoding();
			return responseText;
		} else {
			Map saveMap = new HashMap<String, String>();
			if (attrX) {
				try {
					Map<String, String> userDataExt;
					userDataExt = setUserDataExtensions(selected);
					FetchResponse fetchResp = FetchResponse
							.createFetchResponse(fetchReq, userDataExt);
					responsem.addExtension(fetchResp);
					saveMap.putAll(userDataExt);
				} catch (MessageException ex) {

					Logger.getLogger(SetupBean.class.getName()).log(
							Level.SEVERE, null, ex);
				}
			}
			if (sr) {
				try {
					Map<String, String> userDataSReg = setUserDataExtensions(selectedSR);
					SRegResponse sregResp = SRegResponse.createSRegResponse(
							sregReq, userDataSReg);
					responsem.addExtension(sregResp);
					saveMap.putAll(userDataSReg);
				} catch (MessageException ex) {
					Logger.getLogger(SetupBean.class.getName()).log(
							Level.SEVERE, null, ex);
				}
			}
			if (saved) {
				try {
					persistor.setAttributes(uid, getTrustRoot(), saveMap);
				} catch (BackendException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// grant authentication
			grant();

			// navigation here should be irrelevant given redirect to provider
			return "grantOnce";
		}
	}

	/**
	 * TODO: Description.
	 * 
	 * @return the string constant "denyOnce".
	 */
	public String denyOnce() {

		Boolean authenticatedAndApproved = Boolean.FALSE;// cancel
		// authentication

		responsem = manager.authResponse(requestp, userSelectedId,
				userSelectedClaimedId, authenticatedAndApproved.booleanValue());
		request.getSession().removeAttribute("parameterlist");
		request.getSession().removeAttribute("servermanager");

		sendRedirect((responsem).getDestinationUrl(true));
		// navigation here is irrelevant; redirects to relying party
		return "denyOnce";
	}

	/** populate am attribute into form */
	@SuppressWarnings("unchecked")
	private void setAMAttributes2Values(Map principalAttrs) {
		if(amAttrNames==null){
			return;
		}
		for (Iterator<String> i = principalAttrs.keySet().iterator(); i
				.hasNext();) {
			String principalAttrName = (String) i.next();
			for (int j = 0; j < amAttrNames.length; j++) {
				Set val = (Set) principalAttrs.get(principalAttrName);
				if (null != val && (!val.isEmpty())) {
					int index = amAttrNames[j].indexOf("|");
					if (amAttrNames[j].startsWith(principalAttrName)) {

						String v = (String) val.iterator().next();
						if (v != null) {
							values.put(amAttrNames[j].substring(index + 1), v);
						}
					}
				}
			}
		}

	}

	/** set saved attributes to values map */
	private void setAttributes2Values(Map<String, String> retrievedMap) {
		for (Iterator<String> i = retrievedMap.keySet().iterator(); i.hasNext();) {
			String attrName = (String) i.next();
			String aVal = (String) retrievedMap.get(attrName);
			String type = types.get(attrName);
			if (aVal != null) {
				if (!type.equals(TYPE_DATE)) {
					values.put(attrName, aVal);
				} else {
					HashMap<String, String> dateVal = new HashMap<String, String>();
					String[] splitVal = aVal.split("-");
					if (splitVal.length == 3) {
						dateVal.put("year", splitVal[0]);
						dateVal.put("month", splitVal[1]);
						dateVal.put("day", removeLeadingZeroes(splitVal[2]));
						// removeLeadingZeroes(splitVal[2]));
						values.put(attrName, dateVal);
					} else {
						Logger
								.getLogger(SetupBean.class.getName())
								.log(
										Level.WARNING,
										"The following attribute was specified as type date: "
												+ attrName
												+ "\nThe current value is is not in the correct format "
												+ aVal
												+ "\nIts value should be of type yy-mm-dd");
					}
				}
			} else {
				Logger.getLogger(SetupBean.class.getName()).log(Level.WARNING,
						"The following attribute was not found: " + attrName);
			}
		}
	}

	/** set user attributes values for ax and sr */
	@SuppressWarnings("unchecked")
	private Map<String, String> setUserDataExtensions(Map<String, Boolean> sel) {
		Map<String, String> userDataExt = new HashMap<String, String>();

		for (String name : sel.keySet()) {
			String type = types.get(name);

			// ignore fields that have not been defined by page
			if (type == null) {
				continue;
			}

			// ignore fields that have not been selected by user
			if (!sel.get(name)) {
				continue;
			}
			String value = null;

			if (type.equals(TYPE_TEXT) || type.equals(TYPE_SELECT)) {
				value = (String) values.get(name);

			} else if (type.equals(TYPE_DATE)) {
				value = srxDate((HashMap<String, String>) values.get(name));
			}

			userDataExt.put(name, value);
		}

		return userDataExt;
	}

	// openid identity
	public String getIdentity() {
		String identity = requestp.hasParameter("openid.identity") ? requestp
				.getParameterValue("openid.identity") : null;
		return identity;
	}

	public String getTrustRoot() {
		String openidrealm = requestp.hasParameter("openid.realm") ? requestp
				.getParameterValue("openid.realm") : null;
		String openidreturnto = requestp.hasParameter("openid.return_to") ? requestp
				.getParameterValue("openid.return_to")
				: null;
		String trustRoot = (openidrealm == null ? openidreturnto : openidrealm);
		int index = trustRoot.indexOf("?");
		if (index > 0) {
			return trustRoot.substring(0, index);
		} else {
			return trustRoot;
		}
	}
}
