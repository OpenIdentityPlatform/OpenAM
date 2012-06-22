package com.sun.security.sam.openid2;

import java.util.HashMap;

/**
 * Opaque session token added to HttpSession to preserve result of openID
 * Params. Used by Association and OpenIDServerAuthModule to exchange OpenID params.
 * 
 * @author rsoika
 */
public class OpenIDToken extends HashMap {

	public static final String OPENID_SESSION_TOKEN = "openid_session_token";
	
	public static final String MODE = "openid.mode";
	public static final String IDP = "openid.idp";
	public static final String USER_SETUP_URL = "openid.user_setup_url";
	public static final String IDENTITY = "openid.identity";
	public static final String ASSOC_HANDLE = "openid.assoc_handle";
	public static final String RETURN_TO = "openid.return_to";
	public static final String SIGNED = "openid.signed";
	public static final String SIG = "openid.sig";
	
	public static final String TRUST_ROOT = "openid.trust_root";
	public static final String SESSION_TYPE = "openid.session.type";

	public static final String IS_VERIFIED = "openid_is_verified";

	public static final String VERSION = "openid.version";
	
	
	// names of openid parameters in response from provider
	public final static String[] openIDParams = new String[] { "openid.mode",
			"openid.user_setup_url", "openid.identity", "openid.assoc_handle",
			"openid.return_to", "openid.signed", "openid.sig",
			"openid.invalidate_handle","openid.claimed_id" };
	


}