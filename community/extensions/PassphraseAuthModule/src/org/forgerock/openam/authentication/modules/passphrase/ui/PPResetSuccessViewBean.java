/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PPResetSuccessViewBean.java,v 1.2 2008/06/25 05:43:42 qcheng Exp $
 *
 *    "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.ui;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.StaticTextField;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetModel;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetSuccessModel;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetSuccessModelImpl;

/**
 * <code>PPResetSuccessViewBean</code> displays reset password success message.
 */
public class PPResetSuccessViewBean extends PPResetViewBeanBase {

	/**
	 * Name of title peer component
	 */
	public static final String CC_TITLE = "ccTitle";

	/**
	 * Page name
	 */
	public static final String PAGE_NAME = "PPResetSuccess";

	/**
	 * Name of reset message stored in this view bean
	 */
	public static final String RESET_MESSAGE = "resetMsg";

	/**
	 * Default display URL
	 */
	public static final String DEFAULT_DISPLAY_URL = "/passphrase/ppui/PPResetSuccess.jsp";

	private String resetMsg = null;

	/**
	 * Constructs a reset password success view bean
	 */
	public PPResetSuccessViewBean() {
		super(PAGE_NAME);
		setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
		registerChildren();
	}

	/**
	 * Registers child components/views
	 */
	protected void registerChildren() {
		super.registerChildren();
		registerChild(CC_TITLE, StaticTextField.class);
		registerChild(RESET_MESSAGE, StaticTextField.class);
	}

	/**
	 * Creates child component
	 * 
	 * @param name of child component
	 * @return child component
	 */
	protected View createChild(String name) {
		View child = null;
		if (name.equals(CC_TITLE)) {
			child = new StaticTextField(this, CC_TITLE, "");
		} else if (name.equals(RESET_MESSAGE)) {
			child = new StaticTextField(this, RESET_MESSAGE, "");
		} else {
			child = super.createChild(name);
		}
		return child;
	}

	/**
	 * Set the required information to display the page.
	 * 
	 * @param event display event.
	 * @throws ModelControlException if problem access value of component.
	 */
	public void beginDisplay(DisplayEvent event) throws ModelControlException {
		super.beginDisplay(event);
		PPResetSuccessModel model = (PPResetSuccessModel) getModel();
		setDisplayFieldValue(CC_TITLE, model.getTitleString());
		setDisplayFieldValue(RESET_MESSAGE, resetMsg);
	}

	/**
	 * Forwards to current view bean after after verifying that reset reset
	 * message exists. It will forward to
	 * <code>PWResetUserValidationViewBean</code> if the user DN or organization
	 * DN does not exists.
	 * 
	 * @param context request context
	 */
	public void forwardTo(RequestContext context) {
		if (resetMsg == null || resetMsg.length() == 0) {
			PPResetUserValidationViewBean vb = (PPResetUserValidationViewBean) getViewBean(PPResetUserValidationViewBean.class);
			vb.forwardTo(context);
		} else {
			super.forwardTo(context);
		}
	}

	/**
	 * Gets model for this view bean
	 * 
	 * @return model for this view bean
	 */
	protected PPResetModel getModel() {
		if (model == null) {
			model = new PPResetSuccessModelImpl();
		}
		return model;
	}

	/**
	 * Sets password reset success message
	 * 
	 * @param msg password reset message
	 */
	public void setResetMessage(String msg) {
		resetMsg = msg;
	}
}