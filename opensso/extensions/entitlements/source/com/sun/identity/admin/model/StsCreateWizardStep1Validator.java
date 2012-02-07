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
 * $Id: StsCreateWizardStep1Validator.java,v 1.2 2009/08/03 22:25:31 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;
import java.net.MalformedURLException;
import java.net.URL;
import javax.faces.application.FacesMessage;

public class StsCreateWizardStep1Validator extends WizardStepValidator {

    private static final int MAX_SERVICE_NAME_LENGTH = 255;
    private static final int MAX_URL_LENGTH = 1024;

    public StsCreateWizardStep1Validator(WizardBean wizardBean) {
        super(wizardBean);
    }

    public boolean validate() {

        if( !validServiceName() || !validServiceEndPoint() || !validMexEndPoint() ) {
            return false;
        }

        return true;
    }

    private boolean validServiceName() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();
        String serviceName = wizardBean.getServiceName();

        // what is the maximum length for a service name?
        if( serviceName != null
            && serviceName.matches("[\\w ]{0," + MAX_SERVICE_NAME_LENGTH + "}?") ) {
            return true;
        }

        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidServiceNameSummary"));
        mb.setDetail(r.getString(this, "invalidServiceNameDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        Effect e;
        e = new InputFieldErrorEffect();
        wizardBean.setServiceNameInputEffect(e);

        e = new MessageErrorEffect();
        wizardBean.setServiceNameMessageEffect(e);

        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(StsCreateWizardStep.SERVICENAME_ENDPOINT.toInt());

        return false;
    }


    private boolean validServiceEndPoint() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();

        try {
            String serviceEndPoint = wizardBean.getServiceEndPoint();
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
        wizardBean.setServiceEndPointInputEffect(e);

        e = new MessageErrorEffect();
        wizardBean.setServiceEndPointMessageEffect(e);

        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(StsCreateWizardStep.SERVICENAME_ENDPOINT.toInt());

        return false;
    }

    private boolean validMexEndPoint() {
        StsCreateWizardBean wizardBean = (StsCreateWizardBean) getWizardBean();

        try {
            String mexEndPoint = wizardBean.getMexEndPoint();
            URL url = new URL(mexEndPoint);

            // what is the maximum length of a service end point?
            if( mexEndPoint != null && mexEndPoint.length() <= MAX_URL_LENGTH ) {
                return true;
            }

        } catch (MalformedURLException e) {

            // Do nothing but continue with the flow below for error messaging
        }

        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, "invalidMexEndPointSummary"));
        mb.setDetail(r.getString(this, "invalidMexEndPointDetail"));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        Effect e;
        e = new InputFieldErrorEffect();
        wizardBean.setMexEndPointInputEffect(e);

        e = new MessageErrorEffect();
        wizardBean.setMexEndPointMessageEffect(e);

        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(StsCreateWizardStep.SERVICENAME_ENDPOINT.toInt());

        return false;
    }
}