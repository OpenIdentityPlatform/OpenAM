/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthenticationServlet.java,v 1.2 2009/08/24 11:55:07 hubertlvg Exp $
 *
 */ 

package com.sun.identity.openid.provider;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * 
 * @author robert, hubert A. le van gong
 */

public class AuthenticationServlet extends HttpServlet {
	/**
	 * A set of attribute names that need to be retrieved from the user's
	 * profile. This is set during the init()
	 */
	private Set<String> profileAttributesToFetch = null;
	
	
	
	

	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 */
	protected void processRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {

			SSOTokenManager manager;

			// get the singleton instance of the SSO token manager
			manager = SSOTokenManager.getInstance();

			SSOToken token;
			Map attrs = null;

			// create single sign on token from the request
			token = manager.createSSOToken(request);

			if (manager.isValidToken(token)) {
//				Set<String> needAttrs = new HashSet<String>();
//				String fetchingAttrs = Config
//						.getString(Config.AM_PROFILE_ATTRIBUTES);
//				String[] fetchAttrNames = fetchingAttrs.split(",");
//				for (int i = 0; i < fetchAttrNames.length; i++) {
//					int index = fetchAttrNames[i].indexOf("|");
//					needAttrs.add(fetchAttrNames[i].substring(0, index));
//				}

				AMIdentity amid;
				try {
					amid = IdUtils.getIdentity(token);
					attrs = amid.getAttributes(profileAttributesToFetch);
				} catch (IdRepoException ex) {
					Logger.getLogger(AuthenticationServlet.class.getName()).log(
							Level.SEVERE, null, ex);
				}
				HttpSession session = request.getSession();
				session.setAttribute("login", "true");
				session.setAttribute("principalAttrs", attrs);
                session.setAttribute("principal", token.getPrincipal());

			}
		} catch (SSOException ex) {
			Logger.getLogger(AuthenticationServlet.class.getName()).log(Level.SEVERE,
					null, ex);
		}

		response.sendRedirect(Config.getString(Config.SETUP_URL));

	}
	
	
	public void init(ServletConfig config) throws ServletException{
		profileAttributesToFetch = new HashSet<String>();
		String fetchingAttrs = Config
				.getString(Config.AM_PROFILE_ATTRIBUTES);
		String[] fetchAttrNames = fetchingAttrs.split(",");
		for (int i = 0; i < fetchAttrNames.length; i++) {
			int index = fetchAttrNames[i].indexOf("|");
			if(index>0){
				profileAttributesToFetch.add(fetchAttrNames[i].substring(0, index));
			}
		}
	}

	// <editor-fold defaultstate="collapsed" desc=
	// "HttpServlet methods. Click on the + sign on the left to edit the code.">
	/**
	 * Handles the HTTP <code>GET</code> method.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 * 
	 * @param request
	 *            servlet request
	 * @param response
	 *            servlet response
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 */
	public String getServletInfo() {
		return "Short description";
	}
	// </editor-fold>
}
