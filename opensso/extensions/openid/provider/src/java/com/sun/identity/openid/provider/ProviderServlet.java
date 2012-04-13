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
 * $Id: ProviderServlet.java,v 1.2 2009/09/07 15:03:48 hubertlvg Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan and Robert Nguyen
 */

package com.sun.identity.openid.provider;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openid4java.message.Message;
import org.openid4java.message.ParameterList;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.openid4java.server.ServerManager;

/**
 * TODO: Description.
 * 
 * @author pbryan, Robert Nguyen, Hubert A. Le Van Gong
 */
public class ProviderServlet extends HttpServlet {
	
	private static ServerManager serverManager = null;
	/**
	 * TODO: Description.
	 * 
	 * @param request
	 *            TODO.
	 * @param response
	 *            TODO.
	 * @throws IOException
	 *             TODO.
	 * @throws ServletException
	 *             TODO.
	 */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		doPost(request, response);
	}

	/**
	 * TODO: Description.
	 * 
	 * @param request
	 *            TODO.
	 * @param response
	 *            TODO.
	 * @throws IOException
	 *             TODO.
	 * @throws ServletException
	 *             TODO.
	 */
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession(true);
		ServerManager manager = getManager();

		ParameterList requestp = getRequestP(request, session);

		String mode = requestp.hasParameter("openid.mode") ? requestp
				.getParameterValue("openid.mode") : null;

		Message responsem = null;
		String responseText = null;

		if (mode == null) {
			dispatchFacelet(request, response, "unknown.jsf");
		}

		else if (mode.equals("associate")) {
			responsem = manager.associationResponse(requestp);
			responseText = responsem.keyValueFormEncoding();

		} else if (mode.equals("checkid_setup")
				|| mode.equals("checkid_immediate")) {

			dispatchFacelet(request, response, "setup.jsf");
		}

		else if (mode.equals("check_authentication")) {
			responsem = manager.verify(requestp);
			responseText = responsem.keyValueFormEncoding();
		}

		else {
			dispatchFacelet(request, response, "unknown.jsf");
		}

		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(
				responseText != null ? responseText : "error");

	}


	
	public static  ServerManager getManager() {

		if (serverManager == null) {
			String serviceURL=Config.getString(Config.SERVICE_URL);
			if(serviceURL==null){
				Logger.getLogger(ProviderServlet.class.getName()).log(Level.SEVERE,
						Config.SERVICE_URL + " not defined in Provider.properties");
						
				throw new RuntimeException(Config.SERVICE_URL + " not defined in Provider.properties");
			}
			serverManager = new ServerManager();
			serverManager.setSharedAssociations(new InMemoryServerAssociationStore());
			serverManager.setPrivateAssociations(new InMemoryServerAssociationStore());
			serverManager.setOPEndpointUrl(serviceURL);
			
			serverManager.setEnforceRpId(Config.getBoolean(Config.ENFORCERPID));
		}
		return serverManager;

	}
	


	private ParameterList getRequestP(HttpServletRequest request,
			HttpSession session) {
		ParameterList requestp = null;

		requestp = (ParameterList) session.getAttribute("parameterlist");
		if (requestp == null) {
			requestp = new ParameterList(request.getParameterMap());
			session.setAttribute("parameterlist", requestp);
		}

		return requestp;

	}

	/**
	 * TODO: Description.
	 * 
	 * @param request
	 *            object representing request made from client.
	 * @param response
	 *            object representing response to be sent to client.
	 * @param path
	 *            pathname to facelet to dispatch to.
	 * @throws IOException
	 *             if the target facelet throws this exception.
	 * @throws ServletException
	 *             if the facelet throws exception, or could not dispatch.
	 */
	private static void dispatchFacelet(HttpServletRequest request,
			HttpServletResponse response, String path) throws IOException,
			ServletException {
		RequestDispatcher dispatcher = request.getRequestDispatcher(path);

		if (dispatcher == null) {
			throw new ServletException("no dispatcher found for facelet");
		}

		// forward request to facelet to allow it to manage multiple user
		// interactions
		dispatcher.forward(request, response);
	}
}
