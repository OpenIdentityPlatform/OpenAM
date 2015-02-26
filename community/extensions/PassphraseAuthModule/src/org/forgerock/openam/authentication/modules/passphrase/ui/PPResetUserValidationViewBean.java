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
 * $Id: PPResetUserValidationViewBean.java,v 1.2 2008/06/25 05:43:42 qcheng Exp $
 *
 *    "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.ui;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.Button;
import com.iplanet.jato.view.html.HiddenField;
import com.iplanet.jato.view.html.StaticTextField;
import com.iplanet.jato.view.html.TextField;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetException;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetModel;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetUserValidationModel;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetUserValidationModelImpl;
import javax.servlet.http.HttpServletRequest;

/**
 * <code>PPResetUserValidationViewBean</code> validates user's identity for the
 * passphrase reset service.
 */
public class PPResetUserValidationViewBean extends PPResetViewBeanBase {

	/**
	 * Name of title peer component
	 */
	public static final String USER_VALIDATION_TITLE = "userValidationTitle";

	/**
	 * Name of user validation peer component
	 */
	public static final String LBL_USER_ATTR = "lblUserAttr";

	/**
	 * Name of component for user validation
	 */
	public static final String TF_USER_ATTR = "tfUserAttr";

	/**
	 * Name of OK button peer component
	 */
	public static final String NEXT_BUTTON = "btnNext";

	/**
	 * Page Name
	 */
	public static final String PAGE_NAME = "PPResetUserValidation";

	/**
	 * Name of user attribute
	 */
	public static final String FLD_USER_ATTR = "fldUserAttr";

	private static final String ORG = "org";

	/**
	 * Default display URL
	 */
	public static final String DEFAULT_DISPLAY_URL = "/passphrase/ppui/PPResetUserValidation.jsp";

	/**
	 * Constructs a user validation view bean
	 */
	public PPResetUserValidationViewBean() {
		super(PAGE_NAME);
		setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
		registerChildren();
	}

	/**
	 * Registers child components/views
	 */
	protected void registerChildren() {
		super.registerChildren();
		registerChild(USER_VALIDATION_TITLE, StaticTextField.class);
		registerChild(LBL_USER_ATTR, StaticTextField.class);
		registerChild(TF_USER_ATTR, TextField.class);
		registerChild(NEXT_BUTTON, Button.class);
		registerChild(FLD_USER_ATTR, HiddenField.class);
	}

	/**
	 * Creates child component
	 * 
	 * @param name of child component
	 * @return child component
	 */
	protected View createChild(String name) {
		View child = null;
		if (name.equals(LBL_USER_ATTR)) {
			child = new StaticTextField(this, LBL_USER_ATTR, "");
		} else if (name.equals(TF_USER_ATTR)) {
			child = new TextField(this, TF_USER_ATTR, "");
		} else if (name.equals(NEXT_BUTTON)) {
			child = new Button(this, NEXT_BUTTON, "");
		} else if (name.equals(USER_VALIDATION_TITLE)) {
			child = new StaticTextField(this, USER_VALIDATION_TITLE, "");
		} else if (name.equals(FLD_USER_ATTR)) {
			return new HiddenField(this, FLD_USER_ATTR, "");
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
		PPResetUserValidationModel model = (PPResetUserValidationModel) getModel();
		if (model.isValidRealm()) {
			String orgDN = (String) getPageSessionAttribute(ORG_DN);
			String userAttr = model.getUserAttr(orgDN);
			HiddenField hf = (HiddenField) getChild(FLD_USER_ATTR);
			hf.setValue(userAttr);

			setDisplayFieldValue(USER_VALIDATION_TITLE, model.getUserValidateTitleString());
			setDisplayFieldValue(LBL_USER_ATTR, model.getLocalizedStrForAttr(userAttr));
			setDisplayFieldValue(NEXT_BUTTON, model.getNextBtnLabel());

			String userAttrValue = (String) getPageSessionAttribute(USER_ATTR_VALUE);
			if (userAttrValue != null && userAttrValue.length() > 0) {
				setDisplayFieldValue(TF_USER_ATTR, userAttrValue);
				removePageSessionAttribute(USER_ATTR_VALUE);
			}
		}
	}

	/**
	 * Forwards to current view bean after validating organization
	 * 
	 * @param context request context
	 */
	public void forwardTo(RequestContext context) {
		HttpServletRequest req = context.getRequest();
		PPResetUserValidationModel model = (PPResetUserValidationModel) getModel();
		String orgDN = (String) getPageSessionAttribute(ORG_DN);
		if (orgDN == null) {
			String orgName = req.getParameter(ORG);
			try {
				orgDN = model.getRealm(orgName);
				setPageSessionAttribute(ORG_DN, orgDN);
				 // Set the flag to indicate that user enter the orgname in the url.
				if (orgName != null && orgName.length() > 0) {
					setPageSessionAttribute(ORG_DN_FLAG, STRING_TRUE);
					model.setRealmFlag(true);
				}
			} catch (PPResetException pwe) {
				setErrorMessage(model.getErrorTitle(), pwe.getMessage());
			}
		} else {
			model.setValidRealm(orgDN);
		}
		super.forwardTo(context);
	}

	/**
	 * Handles form submission request
	 * 
	 * @param event request invocation event
	 */
	public void handleBtnNextRequest(RequestInvocationEvent event) {
		PPResetUserValidationModel model = (PPResetUserValidationModel) getModel();

		String userAttrValue = (String) getDisplayFieldValue(TF_USER_ATTR);
		if (userAttrValue != null) {
			userAttrValue = userAttrValue.trim();
		}
		HiddenField hf = (HiddenField) getChild(FLD_USER_ATTR);
		String userAttrName = (String) hf.getValue();
		String orgDN = (String) getPageSessionAttribute(ORG_DN);
		String orgDNFlag = (String) getPageSessionAttribute(ORG_DN_FLAG);
		String locale = (String) getPageSessionAttribute(URL_LOCALE);

		if (orgDNFlag != null && orgDNFlag.equals(STRING_TRUE)) {
			model.setRealmFlag(true);
		}

		if (userAttrValue == null || userAttrValue.length() == 0) {
			setErrorMessage(model.getErrorTitle(), model.getMissingUserAttrMessage(userAttrName));
			forwardTo();
		} else {
			if (model.isUserExists(userAttrValue, userAttrName, orgDN) && model.isUserActive(model.getUserRealm())) {
				forwardToPPResetQuestionVB(orgDN, userAttrValue, orgDNFlag, locale);
			} else {
				setErrorMessage(model.getErrorTitle(), model.getErrorMessage());
				forwardTo();
			}
		}
	}

	/**
	 * Forwards to <code>PWResetQuestionViewBean</code>
	 * 
	 * @param orgDN organization DN
	 * @param value user attribute value
	 * @param orgDNFlag true if organization came from URL, false otherwise
	 */
	private void forwardToPPResetQuestionVB(String orgDN, String value, String orgDNFlag, String locale) {
		RequestContext rc = getRequestContext();
		PPResetUserValidationModel model = (PPResetUserValidationModel) getModel();

		PPResetQuestionViewBean vb = (PPResetQuestionViewBean) getViewBean(PPResetQuestionViewBean.class);
		vb.setPageSessionAttribute(USER_DN, model.getUserId());
		vb.setPageSessionAttribute(USER_ATTR_VALUE, value);
		vb.setPageSessionAttribute(URL_LOCALE, locale);
		vb.setPageSessionAttribute(INITIAL_ORG_DN, orgDN);
		vb.setPageSessionAttribute(ORG_DN, model.getUserRealm());

		if (orgDNFlag != null && orgDNFlag.equals(STRING_TRUE)) {
			vb.setPageSessionAttribute(ORG_DN_FLAG, STRING_TRUE);
		}
		vb.forwardTo(rc);
	}

	/**
	 * Gets model for this view bean
	 * 
	 * @return model for this view bean
	 */
	protected PPResetModel getModel() {
		if (model == null) {
			model = new PPResetUserValidationModelImpl();
		}
		return model;
	}

	/**
	 * Begins password reset user validation content
	 * 
	 * @param event child display event
	 * @return true if password reset user validation content is to displayed, false otherwise
	 */
	public boolean beginResetPageDisplay(ChildDisplayEvent event) {
		PPResetUserValidationModel model = (PPResetUserValidationModel) getModel();
		return isPPResetEnabled() && model.isValidRealm();
	}
}