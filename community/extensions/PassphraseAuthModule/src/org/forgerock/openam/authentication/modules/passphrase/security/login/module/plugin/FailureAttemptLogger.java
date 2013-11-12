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

package org.forgerock.openam.authentication.modules.passphrase.security.login.module.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import com.iplanet.sso.SSOToken;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * This authentication post processing plug-in is used to log all invalid
 * login atempts into an oracle db.
 * 
 * @author Satheesh M
 */
@SuppressWarnings("unchecked")
public class FailureAttemptLogger implements AMPostAuthProcessInterface {

	private static Debug debug = Debug.getInstance("FailureAttemptLogger");

	/**
	 * This method creates a record in the oracle db for every failure login
	 * (both invalid password & passphrase authentication).
	 * 
	 * @param paramsMap contains HttpServletRequest parameters
	 * @param req HttpServlet request
	 * @param res HttpServlet response
	 * @throws AuthenticationException if there is an error
	 */
	public void onLoginFailure(Map paramsMap, HttpServletRequest req, HttpServletResponse res) throws AuthenticationException {
		Connection conn = null;
		PreparedStatement pst = null;
		int failureType = PassphraseConstants.TYPE_INVALID_PASSPHRASE;
		
		try {
			String userName = (String) req.getAttribute("userName");
			if (userName == null) {
				userName = (String) paramsMap.get("IDToken1");
				failureType = PassphraseConstants.TYPE_INVALID_PASSWORD;
			}
			
			AMIdentity user = CommonUtilities.getUser(userName, req);
			
			if (user != null && user.isActive()) {
				debug.message("Authentication failed for user: " + user.getName());
				// get the opensso atribute which is mapped as the screen name atribute in Liferay-OpenSSO setting.
				String screenName = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.SCREEN_NAME_ATTRIBUTE));
				
				InitialContext ctx = new InitialContext();
				DataSource ds = (DataSource) ctx.lookup(CommonUtilities.getProperty(PassphraseConstants.JNDI_NAME));
				
				conn = ds.getConnection();
				pst = conn.prepareStatement("INSERT INTO \"failure_login_attempts\" (\"user_id\", \"failure_type\", " +
						"\"ip_address\", \"failure_time\", \"is_read\") VALUES (?, ?, ?, ?, 0)");
				pst.setString(1, getNormalizedName(screenName));
				pst.setInt(2, failureType);
				pst.setString(3, req.getRemoteAddr());
				pst.setTimestamp(4, new Timestamp(new Date().getTime()));
				
				pst.executeUpdate();
			}
		} catch (Exception e) {
			debug.error("Error occured while logging failure attempts: ", e);
		} finally {
			if (pst != null) {
				try {
					pst.close();
				} catch (SQLException e) {}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {}
			}
		}
	}

	/**
	 * This method is used to replace all the special characters with a '-' symbol as
	 * few older version of Liferay automatically converts the special characters to '-'.
	 * 
	 * @param userName
	 * @return
	 */
	private String getNormalizedName(String userName) {
		return userName.trim().replaceAll("\\W+", "-");
	}

	/**
	 * This method is used to set the final redirection page to the success url set to the user profile,
	 * which takes the highest precedence over the default success url.
	 */
	public void onLoginSuccess(Map paramsMap, HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
		try {
			AMIdentity user = IdUtils.getIdentity(ssoToken);
    		String successURL = CollectionHelper.getMapAttr(user.getAttributes(), "iplanet-am-user-success-url");
    		if (StringUtils.isNotBlank(successURL))
    			request.setAttribute(AMPostAuthProcessInterface.POST_PROCESS_LOGIN_SUCCESS_URL, successURL);
		} catch (Exception e) {
			debug.error("Error occured while seting the success url: ", e);
		}
	}

	public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
		// do nothing
	}
}