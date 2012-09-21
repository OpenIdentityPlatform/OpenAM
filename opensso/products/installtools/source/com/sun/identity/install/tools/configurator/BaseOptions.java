/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: BaseOptions.java,v 1.2 2008/06/25 05:51:17 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.List;

abstract public class BaseOptions {

    public BaseOptions() {
        setResponseOptions(new ArrayList());
        setIgnoreCaseFlag(false);
        setDefaultOption(null);
    }

    abstract public void display();

    public void add(UserOptionItem option) {
        getResponseOptions().add(option);
    }

    public void remove(UserOptionItem option) {
        getResponseOptions().remove(option);
    }

    public boolean contains(UserOptionItem option) {
        return getResponseOptions().contains(option);
    }

    public void setDefaultOption(UserOptionItem option) {
        defaultOption = option;
    }

    public boolean getIgnoreCaseFlag() {
        return ignoreCaseFlag;
    }

    public UserOptionItem getDefaultOption() {
        return defaultOption;
    }

    public void setIgnoreCaseFlag(boolean flag) {
        ignoreCaseFlag = flag;
    }

    public UserOptionItem getSelectedOption(String item) {

        UserOptionItem selectedOption = null;
        int count = getResponseOptions().size();
        for (int i = 0; i < count; i++) {
            UserOptionItem option = (UserOptionItem) 
                getResponseOptions().get(i);
            String displayItem = option.getDisplayItem();

            boolean isSame = (getIgnoreCaseFlag()) ? displayItem
                    .equalsIgnoreCase(item) : displayItem.equals(item);
            if (isSame) {
                selectedOption = option;
                break;
            }
        }

        return selectedOption;
    }

    public List getResponseOptions() {
        return responseOptions;
    }

    protected void setResponseOptions(ArrayList list) {
        responseOptions = list;
    }

    private boolean ignoreCaseFlag;

    private UserOptionItem defaultOption;

    private List responseOptions;
}
