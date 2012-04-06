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
 * $Id: WssCreateWizardStep1Validator.java,v 1.2 2009/07/23 20:46:54 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;
import java.net.MalformedURLException;
import java.net.URL;
import javax.faces.application.FacesMessage;

public class WssCreateWizardStep1Validator extends WizardStepValidator {

    private static final int MAX_URL_LENGTH = 1024;

    public WssCreateWizardStep1Validator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {

        if( !validServiceEndPoint() ) {
            return false;
        }

        return true;
    }

    private boolean validServiceEndPoint() {
        WssCreateWizardBean wizardBean = (WssCreateWizardBean) getWizardBean();

        try {
            String serviceEndPoint = wizardBean.getWspServiceEndPoint();
            URL url = new URL(serviceEndPoint);

            // what is the maximum length of a service end point?
            if( serviceEndPoint != null && serviceEndPoint.length() <= MAX_URL_LENGTH ) {
                return true;
            }

        } catch (MalformedURLException e) {

            // Do nothing but continue with the flow below for error messaging
        }

        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidServiceEndPointSummary"));
        mb.setDetail(r.getString(this, "invalidServiceEndPointDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        Effect e;
        e = new InputFieldErrorEffect();
        wizardBean.setWspServiceEndPointInputEffect(e);

        e = new MessageErrorEffect();
        wizardBean.setWspServiceEndPointMessageEffect(e);

        getMessagesBean().addMessageBean(mb);
        return false;
    }

}