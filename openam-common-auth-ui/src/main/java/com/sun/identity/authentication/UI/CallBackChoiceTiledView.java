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
 * $Id: CallBackChoiceTiledView.java,v 1.2 2008/06/25 05:41:49 qcheng Exp $
 *
 */



package com.sun.identity.authentication.UI;

import com.iplanet.jato.RequestHandler;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.model.DatasetModel;
import com.iplanet.jato.view.RequestHandlingTiledViewBase;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.TiledView;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.html.StaticTextField;

/**
 * This class contains a set of choices for choice typed callback
 */
public class CallBackChoiceTiledView
    extends RequestHandlingTiledViewBase
    implements TiledView, RequestHandler
{
    private String[] choices = null;
    private int defaultValue = 0;
    private int curTile = 0;
    private int parentIdx = 9;

    /** index of parent tile */
    public static final String TXT_PARENT_INDEX = "txtParentIndex";

    /** index of current tile */
    public static final String TXT_INDEX = "txtIndex";

    /** choice label */
    public static final String TXT_CHOICE = "txtChoice";

    /**
     * constructs a tiled view of choices
     *
     * @param parent The reference of the parent container
     * @param name The name of this view.
     */
    public CallBackChoiceTiledView(View parent, String name) {
        super(parent, name);
        setPrimaryModel((DatasetModel) getDefaultModel());
        registerChildren();
    }

    /**
     * registers child components/views
     */
    protected void registerChildren() {
        registerChild(TXT_PARENT_INDEX, StaticTextField.class);
        registerChild(TXT_INDEX, StaticTextField.class);
        registerChild(TXT_CHOICE, StaticTextField.class);
    }


    /**
     * creates child component
     *
     * @param name of child component
     * @return child component
     */
    protected View createChild(String name) {
        if (name.equals(TXT_PARENT_INDEX)) {
            return new StaticTextField(this, TXT_PARENT_INDEX, "");
        }
        if (name.equals(TXT_INDEX)) {
            return new StaticTextField(this, TXT_INDEX, "");
        }
        if (name.equals(TXT_CHOICE)) {
            return new StaticTextField(this, TXT_CHOICE, "");
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

        if (choices != null) {
            getPrimaryModel().setSize(choices.length);
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
            setDisplayFieldValue(TXT_PARENT_INDEX, parentIdx);
            setDisplayFieldValue(TXT_CHOICE, choices[curTile]);
            setDisplayFieldValue(TXT_INDEX, Integer.toString(curTile));
        }

        return movedToRow;
    }

    /**
     * set choices array
     *
     * @param parentIdx - current parent tiled index
     * @param choices array
     * @param defVal - default value
     */
    public void setChoices(int parentIdx, String[] choices, int defVal) {
        this.parentIdx = parentIdx;
        this.choices = choices;
        defaultValue = defVal;
    }

    /**
     * begins display of selected choice
     *
     * @param event - child display event
     * @return true if current choice is default
     */
    public boolean beginSelectedChoiceDisplay(ChildDisplayEvent event) {
        return (curTile == defaultValue);
    }

    /**
     * begins display of unselected choice
     *
     * @param event - child display event
     * @return true if current choice is not default
     */
    public boolean beginUnselectedChoiceDisplay(ChildDisplayEvent event) {
        return (curTile != defaultValue);
    }
}
