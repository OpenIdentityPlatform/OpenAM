/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.common.utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.iplanet.am.util.BrowserEncoding;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.cdm.G11NSettings;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.idm.*;
import com.sun.identity.password.ui.model.PWResetModel;
import com.sun.identity.password.ui.model.PWResetResBundleCacher;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * This utility class has utility methods which is required for all the
 * custom modules.
 */
public class CommonUtilities {
	private static Debug debug = Debug.getInstance("CustomModule");
	
	private static String mailServerHost = SystemProperties.get(Constants.AM_SMTP_HOST, "localhost");
    private static String mailServerPort = SystemProperties.get(Constants.SM_SMTP_PORT, "25");
    private static Properties properties = null;
    private static Properties props = new Properties();

    // Set the host smtp address
    static {
        props.put("mail.smtp.host", mailServerHost);
        props.put("mail.smtp.port", mailServerPort);
    }
	
	/**
	 * This method is used to return the value of the provided property key. 
	 * 
	 * @param property
	 * @return
	 */
	public static String getProperty(String key) {
		if (properties == null) {
			properties = new Properties();
			InputStream is = CommonUtilities.class.getResourceAsStream("/CustomModule.properties");
			try {
				properties.load(is);
			} catch (IOException e) {
				debug.error("Error occured while loading the CustomModule.properties file", e);
			}
		}
		String returnValue = properties.getProperty(key);
		return returnValue != null? returnValue.trim():null;
	}
	
	public static AMIdentity getUser(String userId, HttpServletRequest request) {
		return getUser(userId, request, getProperty(PassphraseConstants.USER_IDENTIFIER), null);
	}

	public static AMIdentity getUser(String userId, String realm) {
		return getUser(userId, null, getProperty(PassphraseConstants.USER_IDENTIFIER), realm);
	}
	
	/**
	 * This method is used to get the User object of the provided userID. 
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static AMIdentity getUser(String userId, HttpServletRequest request, String userIdentifier, String realm) {
		AMIdentity amid = null;
		try {
			SSOTokenManager mgr = SSOTokenManager.getInstance();
			SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
			AMIdentityRepository amir = null;
			
			try {
				if (realm == null)
					realm = request.getParameter("realm");
				realm = "null".equalsIgnoreCase(realm)? null:realm;
				if (mgr != null) {
					token = mgr.createSSOToken(request);
					AMIdentity user = new AMIdentity(token);
					String realmName = user.getRealm();
					if (realmName.contains("o=")) {
						realm = user.getRealm();
						int index = realm.indexOf("o=") + 2;
						realmName = realmName.substring(index, realmName.indexOf(",", index));
					} else {
						realmName = StringUtils.isNotBlank(realm)? realm:"/";
					}
					amir = new AMIdentityRepository(token, realmName);
				} else {
					amir = new AMIdentityRepository(token, StringUtils.isNotBlank(realm)? realm:"/");
				}
			} catch (Exception e) {
				realm = StringUtils.isNotBlank(realm)? realm:"/";
				amir = new AMIdentityRepository(token, realm);
			}

			Map<String, Set<String>> searchMap = new HashMap<String, Set<String>>(2);
			Set<String> searchSet = new HashSet<String>(2);
			searchSet.add(userId);
			searchMap.put(userIdentifier, searchSet);
			IdSearchControl isCtl = new IdSearchControl();
			isCtl.setSearchModifiers(IdSearchOpModifier.AND, searchMap);
			IdSearchResults isr = amir.searchIdentities(IdType.USER, "*", isCtl);
			Set results = isr.getSearchResults();
			if (results != null && !results.isEmpty()) {
				amid = (AMIdentity) results.iterator().next();
			}
		} catch (Exception e) {
			debug.error("Error retrieving the user object of userd: " + userId, e);
		}
		return amid;
	}

	/**
	 * This method is used to get the user name from the SSOToken of the request.
	 * 
	 * @param request
	 * @param realmName
	 * @return
	 */
	public static String getUserName(HttpServletRequest request) {
		String strUserName = null;
		try {
			SSOTokenManager mgr = SSOTokenManager.getInstance();
			SSOToken token = mgr.createSSOToken(request);
			AMIdentity user = new AMIdentity(token);
			String uid = user.getUniversalId();
			strUserName = uid.substring(uid.indexOf("=") + 1, uid.indexOf(","));
		} catch (Exception e) {
			debug.error("Error retrieving the user id from the request", e);
		}
		return strUserName;
	}
	
	/**
	 * This method is used to create a query string based on user realm and provided goto url.
	 * 
	 * @param request
	 * @param user 
	 * @return
	 */
	public static String getQueryString(HttpServletRequest request, AMIdentity user) {
		StringBuilder sb = new StringBuilder();
		if (user.getRealm().contains("o=")) {
			String realm = user.getRealm();
			int index = realm.indexOf("o=") + 2;
			realm = realm.substring(index, realm.indexOf(",", index));
			sb.append("&").append("realm=").append(realm);
		}
		if (StringUtils.isNotBlank(request.getParameter("goto")))
			sb.append("&").append("goto=").append(request.getParameter("goto"));
		return sb.toString();
	}

	/**
	 * Get the email address and prepare the mail.
	 * 
	 * @param emailAddress
	 * @param subject
	 * @param dataMap
	 * @param mailTemplatePath
	 * @param userLocale
	 * @throws MessagingException
	 */
	public static void sendEmailToUser(String emailAddress, String subject, Map<String, String> dataMap, String mailTemplatePath, Locale userLocale)
			throws MessagingException {
		String emailContent = null;
		ResourceBundle rb = PWResetResBundleCacher.getBundle(PWResetModel.DEFAULT_RB, userLocale);
		try {
			emailContent = getVelocityContent(dataMap, mailTemplatePath);
		} catch (Exception e) {
			debug.error("Error in getting message from VM template:", e);
		}
		String to[] = new String[1];
		to[0] = emailAddress;
		String from = rb.getString("fromAddress.label");
		G11NSettings g11nSettings = G11NSettings.getInstance();
		String charset = g11nSettings.getDefaultCharsetForLocale(userLocale);
		postMail(to, subject, emailContent, from, charset);
	}

	/**
	 * Send a HTML type mail.
	 * 
	 * @param recipients
	 * @param subject
	 * @param message
	 * @param from
	 * @param charset
	 * @throws MessagingException
	 */
	private static void postMail(String recipients[], String subject, String message,
			String from, String charset) throws MessagingException {
		// get the default mail Session
		Session session = Session.getDefaultInstance(props, null);
		session.setDebug(false);

		// create a message object
		MimeMessage msg = new MimeMessage(session);

		// set the from and to address
		InternetAddress addressFrom = new InternetAddress(from);
		msg.setFrom(addressFrom);

		InternetAddress[] addressTo = new InternetAddress[recipients.length];

		for (int i = 0; i < recipients.length; i++) {
			addressTo[i] = new InternetAddress(recipients[i]);
		}
		msg.setRecipients(Message.RecipientType.TO, addressTo);

		// Setting the Subject and Content Type
		if (charset == null) {
			msg.setSubject(subject);
			msg.setContent(message, "text/html");
		} else {
			charset = BrowserEncoding.mapHttp2JavaCharset(charset);
			msg.setSubject(subject, charset);
			msg.setContent(message, "text/html; charset=" + charset);
		}
		// Transport the message now
		Transport.send(msg);
	}
	 
	/**
	 * This method returns the content from the velocity template with the data merged into it.
	 * 
	 * @param dataMap
	 * @param mailTemplatePath
	 * @return
	 * @throws Exception
	 */
	public static String getVelocityContent(Map<String, String> dataMap, String mailTemplatePath) throws Exception {
		initVelocity();
		Template template = Velocity.getTemplate(mailTemplatePath);

		VelocityContext ctx = new VelocityContext();
		ctx.put("DATA", dataMap);

		Writer writer = new StringWriter();
		template.merge(ctx, writer);

		return writer.toString();
	}
	
	/**
	 * Initialise the velocity engine with classpath resource loading.
	 * 
	 * @throws Exception
	 */
	private static void initVelocity() throws Exception {
		Velocity.setProperty(Velocity.RESOURCE_LOADER, "class");
		Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init();
	}

	/**
	 * This method is used to get a direct LDAP Context.
	 * 
	 * @return
	 * @throws NamingException
	 */
	public static LdapContext getLdapContext(String realm) throws Exception {
		//String configName = AMAuthConfigUtils.getAuthConfigName(AuthContext.IndexType.MODULE_INSTANCE, instanceName, DNMapper.orgNameToDN(realm), "html");
		String configName = AMAuthConfigUtils.getAuthConfigName(DNMapper.orgNameToDN(realm), "html");
		AppConfigurationEntry[] entries = Configuration.getConfiguration().getAppConfigurationEntry(configName);
		Map<String, ?> contextConfig = entries[0].getOptions();
		
		return getLdapContext(contextConfig);
	}
	
	/**
	 * This method creates the LDAP context from the passed context config info of an authentication
	 * module instance.
	 * 
	 * @param contextConfig
	 * @return
	 * @throws Exception
	 */
	public static LdapContext getLdapContext(Map<String, ?> contextConfig ) throws Exception {
		String adminName = CollectionHelper.getMapAttr(contextConfig, "iplanet-am-auth-ldap-bind-dn", "");
		String adminPassword = CollectionHelper.getMapAttr(contextConfig, "iplanet-am-auth-ldap-bind-passwd", "");
		boolean sslEnabled = Boolean.valueOf(CollectionHelper.getMapAttr(contextConfig, "iplanet-am-auth-ldap-ssl-enabled", "false")).booleanValue();
		String ldapUrl = CollectionHelper.getServerMapAttr(contextConfig, "iplanet-am-auth-ldap-server");
		
		Hashtable<String, String> env = new Hashtable<String, String>();
		
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, adminName);
		env.put(Context.SECURITY_CREDENTIALS, adminPassword);
		env.put(Context.PROVIDER_URL, "ldap://" + ldapUrl);
		if (sslEnabled)
			env.put(Context.SECURITY_PROTOCOL, "ssl");

		return new InitialLdapContext(env, null);
	}
	
	/**
	 * This method is used to get the Context config based on the user mailId
	 * Apparently to get the AD (or) ED instance based on the user type. 
	 * 
	 * @param mailId
	 * @return
	 * @throws Exception 
	 */
	public static Map getContextConfig(AMIdentity user) throws Exception {
		String mailId = CollectionHelper.getMapAttr(user.getAttributes(), "mail", "").toLowerCase();		
		String authConfigName = null;
		if (!mailId.endsWith(PassphraseConstants.ID) && !mailId.endsWith(PassphraseConstants.CLEARNET_ID)) {
			authConfigName = CommonUtilities.getProperty(PassphraseConstants.ED_EXTERNAL_INSTANCE_NAME);
		} else {
			authConfigName = CommonUtilities.getProperty(PassphraseConstants.ED_INTERNAL_INSTANCE_NAME);
		}
		
		String configName = AMAuthConfigUtils.getAuthConfigName(AuthContext.IndexType.MODULE_INSTANCE, authConfigName, DNMapper.orgNameToDN("/"), PassphraseConstants.HTML);
		AppConfigurationEntry[] entries = Configuration.getConfiguration().getAppConfigurationEntry(configName);
		if (entries == null || entries.length == 0)
			throw new AMConfigurationException("No Authentication Instance exist with the name: " + authConfigName);		
		return entries[0].getOptions();		
	}

	/**
	 * This method is used to frame the fully qualified userDN from the Datastore configuration.
	 * 
	 * @param userName
	 * @param attrs
	 * @return
	 */
	public static String getMemberDn(String userName, Map attrs) {
		String memberDn = CollectionHelper.getMapAttr(attrs, "sun-idrepo-ldapv3-config-users-search-attribute") + "=" + userName + "," +
		CollectionHelper.getMapAttr(attrs, "sun-idrepo-ldapv3-config-people-container-name") + "=" + CollectionHelper.getMapAttr(attrs, "sun-idrepo-ldapv3-config-people-container-value")
		+ "," + CollectionHelper.getMapAttr(attrs, "sun-idrepo-ldapv3-config-organization_name");
		
		return memberDn;
	}

	/**
	 * This method is used to frame the fully qualified groupDN from the Datastore configuration.
	 * 
	 * @param groupName
	 * @param attrs
	 * @return
	 */
	public static String getGroupDn(String groupName, Map attrs) {
		String groupDn = CollectionHelper.getMapAttr(attrs, "sun-idrepo-ldapv3-config-groups-search-attribute") + "=" + groupName
		+ "," + CollectionHelper.getMapAttr(attrs, "sun-idrepo-ldapv3-config-group-container-name") + "=" + CollectionHelper.getMapAttr(attrs, "sun-idrepo-ldapv3-config-group-container-value")
		+ "," + CollectionHelper.getMapAttr(attrs, "sun-idrepo-ldapv3-config-organization_name");
		
		return groupDn;
	}

	/**
	 * This method returns the appropriate user DN based on whether the user is internal/external.
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static String getUserDN(AMIdentity user) throws Exception {
		String mailId = CollectionHelper.getMapAttr(user.getAttributes(), "mail", "").toLowerCase();		
		boolean isInternalUser = mailId.endsWith(PassphraseConstants.ID) || mailId.endsWith(PassphraseConstants.CLEARNET_ID);

		SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
		ServiceConfigManager svcCfgMgr = new ServiceConfigManager(IdConstants.REPO_SERVICE, token);
		ServiceConfig cfg = svcCfgMgr.getOrganizationConfig("/", null);
		
		ServiceConfig dsConfig = cfg.getSubConfig(CommonUtilities.getProperty(isInternalUser? PassphraseConstants.ED_INTERNAL_DATASTORE_NAME:PassphraseConstants.ED_EXTERNAL_DATASTORE_NAME));
		return getMemberDn(user.getName(), dsConfig.getAttributes());
	}
}