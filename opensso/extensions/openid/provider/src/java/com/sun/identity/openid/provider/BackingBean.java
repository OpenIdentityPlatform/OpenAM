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
 * $Id: BackingBean.java,v 1.1 2009/04/24 21:01:58 rparekh Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 * Portions Copyrighted 2007 Paul C. Bryan
 */

package com.sun.identity.openid.provider;

import java.io.IOException;
import java.util.Map;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base class for managed beans in the Facelets installation. Provides common
 * methods to subclasses and overridable event methods for various phases in the
 * request lifecycle.
 * 
 * @author pbryan
 */
public class BackingBean {
	/** Describes if event occurred before or after a processing phase. */
	private enum Point {
		BEFORE, AFTER
	};

	/** State information related to the processing of Faces request. */
	protected final FacesContext facesContext;

	/** State information related to application environment. */
	protected final ExternalContext externalContext;

	/** Client request information. */
	protected final HttpServletResponse response;

	/** Response to send back to client. */
	protected final HttpServletRequest request;

	/** The root of the component tree. */
	protected final UIViewRoot view;

	/** Attributes associated with the view root. */
	protected final Map<String, Object> attributes;

	/**
	 * Constructs a new backing bean instance.
	 */
	protected BackingBean() {
		// fetch commonly used context variables
		facesContext = FacesContext.getCurrentInstance();
		externalContext = facesContext.getExternalContext();
		request = (HttpServletRequest) externalContext.getRequest();
		response = (HttpServletResponse) externalContext.getResponse();
		view = facesContext.getViewRoot();
		attributes = view.getAttributes();
	}

	/**
	 * Dispatches to the method associated with the current phase of the request
	 * lifecycle. Catches bad message exceptions and internal server errors, and
	 * renders associated error pages.
	 * 
	 * @param event
	 *            the event that contains the current phase in the lifecycle.
	 * @param point
	 *            specifies if before or after phase in lifecycle.
	 */
	private void dispatch(PhaseEvent event, Point point) {
		PhaseId id = event.getPhaseId();

		try {
			if (id.equals(PhaseId.APPLY_REQUEST_VALUES)) {
				switch (point) {
				case BEFORE:
					beforeApplyRequestValues(event);
					break;
				case AFTER:
					afterApplyRequestValues(event);
					break;
				}
			}

			else if (id.equals(PhaseId.PROCESS_VALIDATIONS)) {
				switch (point) {
				case BEFORE:
					beforeProcessValidations(event);
					break;
				case AFTER:
					afterProcessValidations(event);
					break;
				}
			}

			else if (id.equals(PhaseId.UPDATE_MODEL_VALUES)) {
				switch (point) {
				case BEFORE:
					beforeUpdateModelValues(event);
					break;
				case AFTER:
					afterUpdateModelValues(event);
					break;
				}
			}

			else if (id.equals(PhaseId.INVOKE_APPLICATION)) {
				switch (point) {
				case BEFORE:
					beforeInvokeApplication(event);
					break;
				case AFTER:
					afterInvokeApplication(event);
					break;
				}
			}

			else if (id.equals(PhaseId.RENDER_RESPONSE)) {
				switch (point) {
				case BEFORE:
					beforeRenderResponse(event);
					break;
				case AFTER:
					afterRenderResponse(event);
					break;
				}
			}
		}

		catch (BadRequestException bre) {
			sendError(response.SC_BAD_REQUEST, bre.getMessage());
		}

		catch (InternalServerException ise) {
			sendError(response.SC_INTERNAL_SERVER_ERROR, ise.getMessage());
		}
	}

	/**
	 * Returns the externally-facing URL to access the OpenID provider service.
	 * If the service URL is defined in the configuration properties, it is
	 * returned; otherwise the URL is constructed from the incomingrequest.
	 * 
	 * @return the externally-facing URL of the OpenID provider service.
	 */
	protected String getServiceURL() {
		// try to get explicit URL from properties file
		String url = Config.getString(Config.SERVICE_URL);

		// found one in the configuration, so use it
		if (url != null) {
			return url;
		}

		// begin constructing service URL from incoming request
		StringBuffer buf = new StringBuffer();

		// get all of the requisite information from the request
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String contextPath = request.getContextPath();

		// suppress explicit port number if it's the default for the scheme
		if ((scheme.equalsIgnoreCase("http") && serverPort == 80)
				|| (scheme.equalsIgnoreCase("https") && serverPort == 443)
				|| serverPort < 0) {
			serverPort = 0;
		}

		// http://host
		buf.append(scheme).append("://").append(serverName);

		// :port (if required)
		if (serverPort != 0) {
			buf.append(':').append(serverPort);
		}

		// /provider/service
		buf.append(contextPath).append("/service");

		return buf.toString();
	}

	/**
	 * Sends and error response to the client using the specified status.
	 * 
	 * @param sc
	 *            the error status code.
	 * @param msg
	 *            the descriptive message.
	 */
	protected void sendError(int sc, String msg) {
		// TODO: provide redirect error response
		// (openid.mode=error,openid.error=msg)
		try {
			response.sendError(sc, msg);
		}

		// exception would be thrown if user agent disconnects; ignore it
		catch (IOException ioe) {
		}

		facesContext.responseComplete();
	}

	/**
	 * TODO: Description
	 * 
	 * @param url
	 *            TODO.
	 */
	protected void sendRedirect(String url) {
		try {
			externalContext.redirect(url);
		}

		// exception would be thrown if user agent disconnects; ignore it
		catch (IOException ioe) {
		}

		facesContext.responseComplete();
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 */
	protected void beforeApplyRequestValues(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void afterApplyRequestValues(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void beforeProcessValidations(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void afterProcessValidations(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void beforeUpdateModelValues(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void afterUpdateModelValues(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void beforeInvokeApplication(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void afterInvokeApplication(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void beforeRenderResponse(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws BadRequestException
	 *             TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	protected void afterRenderResponse(PhaseEvent event)
			throws BadRequestException, InternalServerException {
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 */
	public void beforePhase(PhaseEvent event) {
		dispatch(event, Point.BEFORE);
	}

	/**
	 * TODO: Description.
	 * 
	 * @param event
	 *            TODO.
	 * @throws InternalServerException
	 *             TODO.
	 */
	public void afterPhase(PhaseEvent event) {
		dispatch(event, Point.AFTER);
	}
}
