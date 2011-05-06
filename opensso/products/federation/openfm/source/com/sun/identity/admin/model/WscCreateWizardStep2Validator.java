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
 * $Id: WscCreateWizardStep2Validator.java,v 1.3 2009/10/16 19:39:22 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.net.MalformedURLException;
import java.net.URL;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;

public class WscCreateWizardStep2Validator 
        extends WscCreateWizardStepValidator
{
    public WscCreateWizardStep2Validator(WizardBean wb) {
        super(wb);
    }

    @Override
    public boolean validate() {
        WscCreateWizardBean wb = getWscCreateWizardBean();
        SecurityTokenServiceType stst 
                = SecurityTokenServiceType.valueOf(wb.getStsType());

        switch(stst) {
            case OTHER:
                if( !validEndPoint() ) {
                    return false;
                } else if( !validMexEndPoint() ) {
                    return false;
                }
                break;
            case OPENSSO:
            case NONE:
                break;
        }
        
        return true;
    }

    private boolean validEndPoint() {
        WscCreateWizardBean wb = getWscCreateWizardBean();
        String endPoint = wb.getStsClientProfileBean().getEndPoint();
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
        wb.getStsClientProfileBean().setEndPointInputEffect(e);

        e = new MessageErrorEffect();
        wb.getStsClientProfileBean().setEndPointMessageEffect(e);

        showErrorMessage("invalidEndPointSummary", 
                         "invalidEndPointDetail");
        return false;
    }

    private boolean validMexEndPoint() {
        WscCreateWizardBean wb = getWscCreateWizardBean();
        String mexEndPoint = wb.getStsClientProfileBean().getMexEndPoint();
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
        wb.getStsClientProfileBean().setMexEndPointInputEffect(e);

        e = new MessageErrorEffect();
        wb.getStsClientProfileBean().setMexEndPointMessageEffect(e);

        showErrorMessage("invalidMexEndPointSummary", 
                         "invalidMexEndPointDetail");
        return false;
    }
}
