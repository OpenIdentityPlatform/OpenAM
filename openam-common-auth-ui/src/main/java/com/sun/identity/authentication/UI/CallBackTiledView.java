/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CallBackTiledView.java,v 1.4 2008/12/23 21:26:17 ericow Exp $
 *
 */

/*
 * Portions Copyrighted 2011-14 ForgeRock AS
 */

package com.sun.identity.authentication.UI;

import com.iplanet.jato.RequestHandler;
import com.iplanet.jato.model.DatasetModel;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.RequestHandlingTiledViewBase;
import com.iplanet.jato.view.TiledView;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.StaticTextField;
import com.iplanet.jato.view.html.HiddenField;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;
import java.util.List;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;

/**
 * This class contains a set of callbacks for login view bean
 */
public class CallBackTiledView
    extends RequestHandlingTiledViewBase
    implements TiledView, RequestHandler
{
    private Callback[] callbacks = null;
    private List requiredList = null;
    private List<String> infoText = null;
    private Callback curCallback = null;
    private int curTile = 0;

    /** location for possible JavaScript injections */
    public static final String SCRIPT_CONTENT = "scriptContent";

    /** location for possible text injections */
    public static final String TEXT_CONTENT = "textContent";

    /** location for messageType information */
    public static final String MESSAGE_TYPE = "messageType";

    /** index of current tile */
    public static final String TXT_INDEX = "txtIndex";

    /** id of current element */
    public static final String TXT_ID = "txtId";

    /** tiled view of choice */
    public static final String TILED_CHOICE = "tiledChoices";

    /** prompt/label for attribute */
    public static final String TXT_PROMPT = "txtPrompt";

    /** value for attribute */
    public static final String TXT_VALUE = "txtValue";
    
    /** info text value */
    public static String TXT_INFO = "txtInfo";

    /**
     * constructs a tiled view of callbacks
     *
     * @param parent The reference of the parent container
     * @param name The name of this view.
     */
    public CallBackTiledView(View parent, String name) {
        super(parent, name);
        setPrimaryModel((DatasetModel) getDefaultModel());
        registerChildren();
    }

    /**
     * registers child components/views
     */
    protected void registerChildren() {
        registerChild(TXT_INDEX, StaticTextField.class);
        registerChild(TXT_ID, StaticTextField.class);
        registerChild(TILED_CHOICE, CallBackChoiceTiledView.class);
        registerChild(TXT_PROMPT, StaticTextField.class);
        registerChild(TXT_VALUE, StaticTextField.class);
        registerChild(TXT_INFO, StaticTextField.class);
        registerChild(SCRIPT_CONTENT, StaticTextField.class);
        registerChild(TEXT_CONTENT, StaticTextField.class);
        registerChild(MESSAGE_TYPE, StaticTextField.class);
    }


    /**
     * creates child component
     *
     * @param name of child component
     * @return child component
     */
    protected View createChild(String name) {
        if (name.equals(TXT_INDEX)) {
            return new StaticTextField(this, TXT_INDEX, "");
        }
        if (name.equals(TXT_ID)) {
            return new StaticTextField(this, TXT_ID, "");
        }
        if (name.equals(TILED_CHOICE)) {
            return new CallBackChoiceTiledView(this, TILED_CHOICE);
        }
        if (name.equals(TXT_PROMPT)) {
            return new StaticTextField(this, TXT_PROMPT, "");
        }
        if (name.equals(TXT_VALUE)) {
            return new StaticTextField(this, TXT_VALUE, "");
        }
        if (name.equals(TXT_INFO)) {
            return new StaticTextField(this, TXT_INFO, "");
        }
        if (name.equals(SCRIPT_CONTENT)) {
            return new StaticTextField(this, SCRIPT_CONTENT, "");
        }
        if (name.equals(TEXT_CONTENT)) {
            return new StaticTextField(this, TEXT_CONTENT, "");
        }
        if (name.equals(MESSAGE_TYPE)) {
            return new StaticTextField(this, MESSAGE_TYPE, "");
        }

        throw new IllegalArgumentException("Invalid child name ["
            + name + "]");
    }

    /**
     * begins displaying page. we set the required information
     *
     * @param event   display event
     * @throws ModelControlException  if problem access value of component
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        if (getPrimaryModel() == null) {
            throw new ModelControlException("Primary model is null");
        }

        if (callbacks != null) {
            getPrimaryModel().setSize(callbacks.length);
        } else {
            getPrimaryModel().setSize(0);
        }

        super.beginDisplay(event);
        resetTileIndex();
    }

    /**
     * moves the current tile position to the next available tile.
     *
     * @return true if another tile was available, false if the position
     *         remained unchanged because no next tile was available.
     *
     * @throws ModelControlException if manipulation of model fails.
     */
    public boolean nextTile()
        throws ModelControlException {
        boolean movedToRow = super.nextTile();

        if (movedToRow) {
            curTile = getTileIndex();
            curCallback = callbacks[curTile];
            setDisplayFieldValue(TXT_INDEX, Integer.toString(curTile));

            if (curCallback instanceof HiddenValueCallback) {
                setHiddenValueCallbackInfo((HiddenValueCallback) curCallback);
            } else if (curCallback instanceof NameCallback) {
                setNameCallbackInfo((NameCallback) curCallback);
            } else if (curCallback instanceof PasswordCallback) {
                setPasswordCallbackInfo((PasswordCallback) curCallback);
            } else if (curCallback instanceof ChoiceCallback) {
                setChoiceCallbackInfo((ChoiceCallback) curCallback);
            } else if (curCallback instanceof TextOutputCallback) {
                setTextOutputCallbackInfo((TextOutputCallback) curCallback);
            } else {
                setDisplayFieldValue(TXT_PROMPT, "");
                setDisplayFieldValue(TXT_VALUE, "");
                setDisplayFieldValue(TXT_INFO, "");
                setDisplayFieldValue(SCRIPT_CONTENT, "");
                setDisplayFieldValue(TEXT_CONTENT, "");
                setDisplayFieldValue(MESSAGE_TYPE, "");

                CallBackChoiceTiledView tView =
                    (CallBackChoiceTiledView) getChild(TILED_CHOICE);
                tView.setChoices(curTile, null, 0);
            }
        }

        return movedToRow;
    }
    
    public void setCallBackArray(Callback[] callbacks, List requiredList) {
        this.callbacks = callbacks;
        this.requiredList = requiredList;
    }

    /**
     * set callback array
     *
     * @param callbacks array
     * @param requiredList - list of required attribute
     */
    public void setCallBackArray(Callback[] callbacks, List requiredList, List<String> infoText) {
        this.callbacks = callbacks;
        this.requiredList = requiredList;
        this.infoText = infoText;
    }

    private void setHiddenValueCallbackInfo(HiddenValueCallback hvc) {

        String value = hvc.getValue();

        if ((value == null) || (value.isEmpty())) {
            value = hvc.getDefaultValue();
        }

        String id = hvc.getId();
        setDisplayFieldValue(TXT_ID, id);

        setDisplayFieldValue(TXT_VALUE, value);

        CallBackChoiceTiledView tView =
                (CallBackChoiceTiledView) getChild(TILED_CHOICE);
        tView.setChoices(curTile, null, 0);
    }

    private void setNameCallbackInfo(NameCallback nc) {
        setDisplayFieldValue(TXT_PROMPT, nc.getPrompt());
        setDisplayFieldValue(SCRIPT_CONTENT, "");
        setDisplayFieldValue(TEXT_CONTENT, "");
        setDisplayFieldValue(MESSAGE_TYPE, "");

        String name = nc.getName();

        if ((name == null) || (name.equals(""))) {
            name = nc.getDefaultName();
        }

        setDisplayFieldValue(TXT_VALUE, name);
        setDisplayFieldValue(TXT_INFO, getInfoText());

        CallBackChoiceTiledView tView =
                (CallBackChoiceTiledView) getChild(TILED_CHOICE);
        tView.setChoices(curTile, null, 0);
    }

    private void setPasswordCallbackInfo(PasswordCallback pc) {
        setDisplayFieldValue(TXT_PROMPT, pc.getPrompt());
        setDisplayFieldValue(SCRIPT_CONTENT, "");
        setDisplayFieldValue(TEXT_CONTENT, "");
        setDisplayFieldValue(MESSAGE_TYPE, "");

        char[] tmp = pc.getPassword();

        if (tmp == null) {
            setDisplayFieldValue(TXT_VALUE, "");
        } else {
            setDisplayFieldValue(TXT_VALUE, new String(tmp));
        }

        setDisplayFieldValue(TXT_INFO, getInfoText());
        CallBackChoiceTiledView tView =
            (CallBackChoiceTiledView) getChild(TILED_CHOICE);
        tView.setChoices(curTile, null, 0);
    }

    private void setChoiceCallbackInfo(ChoiceCallback cc) {
        setDisplayFieldValue(TXT_PROMPT, cc.getPrompt());
        setDisplayFieldValue(TXT_VALUE, "");
        setDisplayFieldValue(TXT_INFO, getInfoText());
        setDisplayFieldValue(MESSAGE_TYPE, "");

        setDisplayFieldValue(SCRIPT_CONTENT, "");
        setDisplayFieldValue(TEXT_CONTENT, "");

        CallBackChoiceTiledView tView =
            (CallBackChoiceTiledView) getChild(TILED_CHOICE);
        tView.setChoices(curTile, cc.getChoices(), cc.getDefaultChoice());
    }

    private void setTextOutputCallbackInfo(TextOutputCallback textCallback) {
        if (textCallback.getMessageType() == ScriptTextOutputCallback.SCRIPT) {
            setDisplayFieldValue(TEXT_CONTENT, "");
            setDisplayFieldValue(SCRIPT_CONTENT, textCallback.getMessage());
            setDisplayFieldValue(MESSAGE_TYPE, String.valueOf(textCallback.getMessageType()));
        } else {
            setDisplayFieldValue(SCRIPT_CONTENT, "");
            setDisplayFieldValue(MESSAGE_TYPE, String.valueOf(textCallback.getMessageType()));
            setDisplayFieldValue(TEXT_CONTENT, textCallback.getMessage());
        }

        setDisplayFieldValue(TXT_PROMPT, "");
        setDisplayFieldValue(TXT_VALUE, "");
        setDisplayFieldValue(TXT_INFO, "");

        CallBackChoiceTiledView tView = (CallBackChoiceTiledView) getChild(TILED_CHOICE);
        tView.setChoices(curTile, null, 0);
    }

    /**
     * begins display of textbox field element
     *
     * @param event - child display event
     * @return true if current callback is for textbox
     */
    public boolean beginTextBoxDisplay(ChildDisplayEvent event) {
        return (curCallback != null) && (curCallback instanceof NameCallback);
    }

    /**
     * begins display of hiddenValueBox field element
     *
     * @param event - child display event
     * @return true if current callback is for hiddenValueBox
     */
    public boolean beginHiddenValueBoxDisplay(ChildDisplayEvent event) {
        return (curCallback instanceof HiddenValueCallback);
    }

    /**
     * begins display of password field element
     *
     * @param event - child display event
     * @return true if current callback is for password
     */
    public boolean beginPasswordDisplay(ChildDisplayEvent event) {
        return (curCallback != null) &&
            (curCallback instanceof PasswordCallback);
    }

    /**
     * begins display of choice field element
     *
     * @param event - child display event
     * @return true if current callback is for choices
     */
    public boolean beginChoiceDisplay(ChildDisplayEvent event) {
        return (curCallback != null) && (curCallback instanceof ChoiceCallback);
    }

    /**
     * begins display of text out field element
     *
     * @param event - child display event
     * @return true if current callback is for text out output
     */
    public boolean beginTextOutDisplay(ChildDisplayEvent event) {
        return (curCallback != null) && (curCallback instanceof TextOutputCallback);
    }

    /**
     * begins display of required marked element
     *
     * @param event - child display event
     * @return true if current callback is required
     */
    public boolean beginIsRequiredDisplay(ChildDisplayEvent event) {
        boolean required = false;

        if ((requiredList != null) && !requiredList.isEmpty()) {
            String s = (String) requiredList.get(curTile -1);

            if ((s != null) && (s.length() > 0)) {
                required = true;
            }
        }

        return required;
    }
    
    public boolean beginHasInfoTextDisplay(ChildDisplayEvent event) {
        boolean hasInfoText = false;

        if ((infoText != null) && !infoText.isEmpty()) {
            String s = infoText.get(curTile -1);

            if ((s != null) && (s.length() > 0)) {
                hasInfoText = true;
            }
        }

        return hasInfoText;
    }
    
    
    private String getInfoText() {
        String result = "";

        if ((infoText != null) && !infoText.isEmpty()) {
            String s = infoText.get(curTile -1);

            if ((s != null) && (s.length() > 0)) {
                result = s;
            }
        }

        return result;
    }
}
