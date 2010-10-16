/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * $Id: WspCreateWizardStep1Validator.java,v 1.3 2009/12/09 23:27:07 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.net.MalformedURLException;
import java.net.URL;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;

public class WspCreateWizardStep1Validator 
        extends WspCreateWizardStepValidator
{
    public WspCreateWizardStep1Validator(WizardBean wb) {
        super(wb);
    }

    @Override
    public boolean validate() {
        WspCreateWizardBean wb = getWspCreateWizardBean();
        WspProfileBean wsp = wb.getWspProfileBean();

        if( !validProfileName() ) {
            return false;
        }
        
        if( !validEndPoint() ) {
            return false;
        }
        
        if( wsp.isUsingMexEndPoint() && !validMexEndPoint() ) {
            return false;
        }
        
        return true;
    }

    private boolean validProfileName() {
        WspCreateWizardBean wb = getWspCreateWizardBean();
        String profileName = wb.getWspProfileBean().getProfileName();
        String pattern = "[\\w \\-\\?\\$\\@\\:\\&\\.\\/]{1,1024}?";
        
        if( profileName != null && profileName.matches(pattern) ) {
            return true;
        }

        Effect e;
        e = new InputFieldErrorEffect();
        wb.getWspProfileBean().setProfileNameInputEffect(e);

        e = new MessageErrorEffect();
        wb.getWspProfileBean().setProfileNameMessageEffect(e);

        showErrorMessage("invalidProfileNameSummary", 
                         "invalidProfileNameDetail");
        return false;
    }

    private boolean validEndPoint() {
        WspCreateWizardBean wb = getWspCreateWizardBean();
        String endPoint = wb.getWspProfileBean().getEndPoint();
        String pattern = ".{1,1024}?";

        try {
            new URL(endPoint);
            if( endPoint != null && endPoint.matches(pattern) ) {
                return true;
            }
        } catch (MalformedURLException ex) {
            // do nothing but flow through below for error message
        }

        Effect e;
        e = new InputFieldErrorEffect();
        wb.getWspProfileBean().setEndPointInputEffect(e);

        e = new MessageErrorEffect();
        wb.getWspProfileBean().setEndPointMessageEffect(e);

        showErrorMessage("invalidEndPointSummary", 
                         "invalidEndPointDetail");
        return false;
    }

    private boolean validMexEndPoint() {
        WspCreateWizardBean wb = getWspCreateWizardBean();
        String mexEndPoint = wb.getWspProfileBean().getMexEndPoint();
        String pattern = ".{1,1024}?";

        try {
            new URL(mexEndPoint);
            if( mexEndPoint != null && mexEndPoint.matches(pattern) ) {
                return true;
            }
        } catch (MalformedURLException ex) {
            // do nothing but flow through below for error message
        }

        Effect e;
        e = new InputFieldErrorEffect();
        wb.getWspProfileBean().setMexEndPointInputEffect(e);

        e = new MessageErrorEffect();
        wb.getWspProfileBean().setMexEndPointMessageEffect(e);

        showErrorMessage("invalidMexEndPointSummary", 
                         "invalidMexEndPointDetail");
        return false;
    }
}
