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
 * $Id: PWResetQuestionTiledView.java,v 1.4 2009/12/08 10:32:23 bhavnab Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock Inc
 */
package com.sun.identity.password.ui;

import com.iplanet.jato.RequestHandler;
import com.iplanet.jato.model.DatasetModel;
import com.iplanet.jato.model.DefaultModel;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.RequestHandlingTiledViewBase;
import com.iplanet.jato.view.TiledView;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.HiddenField;
import com.iplanet.jato.view.html.StaticTextField;
import com.iplanet.jato.view.html.TextField;
import com.sun.identity.password.ui.model.PWResetQuestionModel;
import com.sun.identity.sm.DNMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.openam.utils.CollectionUtils;

/**
 * <code>PWResetQuestionTiledView</code> is a tiled view
 * for password reset question view bean.
 */
public class PWResetQuestionTiledView extends RequestHandlingTiledViewBase
    implements TiledView, RequestHandler {
    
    /**
     * Name of the question field 
     */
    public static final String LBL_QUESTION = "lblQuestion";

    /**
     * Name of the user answer to the secret question 
     */
    public static final String TF_ANSWER = "tfAnswer";

    /**
     * Name of the secret question attribute
     */
    public static final String FLD_ATTR_NAME = "fldAttrName";

    private List<String> questionKeys = null;
    private boolean missingData = false;

    
    /**
     * Constructs a password reset question tiled view
     *
     * @param parent of tiled view 
     * @param name  of view name
     */
    public PWResetQuestionTiledView (View parent, String name) {
	super(parent, name);
	setPrimaryModel((DatasetModel) getDefaultModel());
	registerChildren();
    }
    
    /**
     * Registers child components/views
     */
    protected void registerChildren() {
        registerChild(LBL_QUESTION, StaticTextField.class);
        registerChild(TF_ANSWER, TextField.class);
        registerChild(FLD_ATTR_NAME, HiddenField.class);
    }
    
    /**
     * Creates child component
     *
     * @param name of child component
     * @return child component
     */
    protected View createChild(String name) {
        if (name.equals(LBL_QUESTION)) {
            return new StaticTextField(this, LBL_QUESTION, "");
        } else if (name.equals(TF_ANSWER)) {
            return new TextField(this, TF_ANSWER, "");
        } else if (name.equals(FLD_ATTR_NAME)) {
            return new HiddenField(this, FLD_ATTR_NAME, "");
        }
	throw new IllegalArgumentException(
		"Invalid child name [" + name + "]");
    }
    
    /**
     * Set the required information to display a page.
     *
     * @param event display event.
     * @throws ModelControlException if problem access value of component.
     */
    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	if (getPrimaryModel() == null) {
	    throw new ModelControlException("Primary model is null");
	}
	
	if (questionKeys != null) {
	    getPrimaryModel().setSize(questionKeys.size());
	} else {
	    getPrimaryModel().setSize(0);
	}
	super.beginDisplay(event);
	resetTileIndex();
    }
    
    /**
     * Moves the current tile position to the next available tile.
     *
     * @return true if another tile was available, false if the position
     *         remained unchanged because no next tile was available.
     *
     * @throws ModelControlException if manipulation of model fails.
     */
    public boolean nextTile()
	throws ModelControlException {
	boolean movedToRow = super.nextTile();
	
        PWResetQuestionModel model = getModel();
	if (movedToRow) {
            String question = questionKeys.get(getTileIndex());
            String localizedStr = model.getLocalizedStrForQuestion(question);
            HiddenField hf = (HiddenField)getChild(FLD_ATTR_NAME);
            hf.setValue(question);
            setDisplayFieldValue(LBL_QUESTION, localizedStr);
	}	
	return movedToRow;
    }

    /**
     * Gets model from parent view bean
     *
     * @return model from parent view bean
     */
    private PWResetQuestionModel getModel() {
	    PWResetQuestionViewBean parentVB = (PWResetQuestionViewBean)getParentViewBean();
	    return (PWResetQuestionModel) parentVB.getModel();
    }

    /**
     * Populates secret questionKeys list
     *
     * @param userDN  user DN
     * @param orgDN  organization DN
     */
    public void populateQuestionsList(String userDN, String orgDN) {
        PWResetQuestionModel model = getModel();
        int maxQuestions = model.getMaxNumQuestions(DNMapper.orgNameToRealmName(orgDN));
        Map<String, String> secretMap = model.getSecretQuestions(userDN, orgDN);
        if (CollectionUtils.isNotEmpty(secretMap)) {
            if (maxQuestions >= 0 && maxQuestions < secretMap.size()) {
                // Shuffle the keys as a way to provide a random set of secret questions each time they are requested.
                List<String> secretKeys = new ArrayList<String>(secretMap.keySet());
                Collections.shuffle(secretKeys);
                questionKeys = new ArrayList<String>(maxQuestions);
                for (int i = 0; i < maxQuestions; i++) {
                    questionKeys.add(secretKeys.get(i));
                }
            } else {
                // Just return the entire set questions since they are equal to or less than the number requested.
                questionKeys = new ArrayList<String>(secretMap.keySet());
            }
        }
    }

    /**
     * Gets the users answers to the secret questions
     *
     * @return a map of the users answers to the secret questions
     */
    public Map<String, String> getAnswers() {

        Map<String, String> map = Collections.emptyMap();
        PWResetQuestionModel model = getModel();

        DatasetModel dataModel = getPrimaryModel();
        try {
            int size = dataModel.getSize();
            if (size > 0) {
                dataModel.first();
                map = new HashMap<String, String>(size);
            }
            for (int i = 0; i < size; i++) {
                HiddenField hf = (HiddenField)getChild(FLD_ATTR_NAME);
                String attrName = (String) hf.getValue();
                String answer = (String)getDisplayFieldValue(TF_ANSWER);
                if (answer != null) {
                    answer = answer.trim();
                }
                if (answer == null || answer.length() == 0) {
                    missingData = true;
                }
                map.put(attrName, answer);
                dataModel.next();
            }
        } catch (ModelControlException mce) {
            model.debugError("PWResetQuestionTiledView.getAnswers", mce);
        }

        // Attempt to clear out the answers after being collected to avoid them being pre-populated
        // in the case when the answers are wrong and the question screen is shown again.
        if (dataModel instanceof DefaultModel) {
            ((DefaultModel)dataModel).clear();
        }

        return map;
    }

    /**
     * Returns true if user does not answer all required questions
     *
     * @return true if user does not answer all required questions
     */
    public boolean isAnswerBlank() {
        return missingData;
    }
}
