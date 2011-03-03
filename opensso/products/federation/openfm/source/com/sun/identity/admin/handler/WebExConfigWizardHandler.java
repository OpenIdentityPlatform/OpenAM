/**
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
 * $Id: WebExConfigWizardHandler.java,v 1.2 2009/12/14 23:43:34 babysunil Exp $
 */
package com.sun.identity.admin.handler;

import com.icesoft.faces.component.inputfile.FileInfo;
import com.icesoft.faces.component.inputfile.InputFile;
import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.WebExDeleteConfigDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.WebExConfigWizardBean;
import com.sun.identity.admin.model.WebExConfigWizardStep;
import com.sun.identity.admin.model.WebExConfigWizardStep2Validator;
import com.sun.identity.admin.model.WebExConfigWizardStep3Validator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.application.FacesMessage;

public class WebExConfigWizardHandler
        extends WizardHandler
        implements Serializable {

    private MessagesBean messagesBean;

    public void doFinishNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "finishTitle"));
        npb.setMessage(r.getString(this, "finishMessage"));
        npb.setLinkBeans(getFinishLinkBeans());
    }

    public void doCancelNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "cancelTitle"));
        npb.setMessage(r.getString(this, "cancelMessage"));
        npb.setLinkBeans(getCancelLinkBeans());
    }

    @Override
    public void cancelListener(ActionEvent event) {
        WebExConfigWizardBean wizardBean = (WebExConfigWizardBean) getWizardBean();
        if (2 < getStep(event)) {
            deleteConfiguration(event);
        }
        wizardBean.reset();
        doCancelNext();
    }

    @Override
    public void initWizardStepValidators() {
        getWizardStepValidators()[WebExConfigWizardStep.SITEURL.toInt()] =
                new WebExConfigWizardStep2Validator(getWizardBean());
        getWizardStepValidators()[WebExConfigWizardStep.OPENSSOCONFIG.toInt()] =
                new WebExConfigWizardStep3Validator(getWizardBean());
    }

    private void deleteConfiguration(ActionEvent event) {
        boolean result = false;
        WebExConfigWizardBean wizardBean =
                (WebExConfigWizardBean) getWizardBean();
        WebExDeleteConfigDao webExDeleteConfigDao = new WebExDeleteConfigDao();
        String realmName = wizardBean.getRealmName();
        String spName = wizardBean.getWebexSiteUrl();

        //delete sp
        if (spName != null && spName.length() > 0) {
            result = webExDeleteConfigDao.deleteEntity(realmName, spName);
            if (!result) {
                showErrorMessage("deletionSpFailedSummary",
                        "deletionSpFailedDetail");
            }
        }
    }

    @Override
    public void finishListener(ActionEvent event) {
        doFinishNext();

    }

    private List<LinkBean> getFinishLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.COMMON_TASKS);
        return lbs;
    }

    private List<LinkBean> getCancelLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.COMMON_TASKS);
        return lbs;
    }

    public void addIdpListener(ActionEvent event) {
        WebExConfigWizardBean wizardBean =
                (WebExConfigWizardBean) getWizardBean();
        wizardBean.setChooseExistng(true);
        wizardBean.setCreateNew(false);
    }

    public void chooseIdpListener(ActionEvent event) {
        WebExConfigWizardBean wizardBean =
                (WebExConfigWizardBean) getWizardBean();
        wizardBean.setChooseExistng(false);
        wizardBean.setCreateNew(true);
    }

    public void addCotListener(ActionEvent event) {
        WebExConfigWizardBean wizardBean =
                (WebExConfigWizardBean) getWizardBean();
        wizardBean.setChooseCot(true);
        wizardBean.setCreateCot(false);
    }

    public void chooseCotListener(ActionEvent event) {
        WebExConfigWizardBean wizardBean =
                (WebExConfigWizardBean) getWizardBean();
        wizardBean.setChooseCot(false);
        wizardBean.setCreateCot(true);
    }

    public void selectedCotChangeListener(ValueChangeEvent event) {
        WebExConfigWizardBean wizardBean =
                (WebExConfigWizardBean) getWizardBean();
        String oldValue = (String) event.getOldValue();
        String newValue = (String) event.getNewValue();
        wizardBean.setSelectedCot(newValue);
        wizardBean.getAvailableIdpItems();
    }

    public void spcertFileUploadListener(ActionEvent event) throws IOException {
        WebExConfigWizardBean wizardBean =
                (WebExConfigWizardBean) getWizardBean();
        InputFile inputFile = (InputFile) event.getSource();
        FileInfo fileInfo = inputFile.getFileInfo();
        if (fileInfo.getStatus() == FileInfo.SAVED) {
            File file = new File(fileInfo.getFile().getAbsolutePath());
            StringBuilder contents = new StringBuilder();
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new FileReader(file));
                String text = null;

                // repeat until all lines is read
                while ((text = reader.readLine()) != null) {
                    contents.append(text).append(System.getProperty(
                            "line.separator"));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                file.delete();
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            wizardBean.setSpCertFilename(
                    fileInfo.getFileName());
            wizardBean.setSpCertFile(
                    contents.toString());
            if (!(wizardBean.getSpCertFile().startsWith("-"))) {
                showErrorMessage("corruptSPCertSummary", "corruptSPCertDetail");
            }
        }
    }

    private void popUpErrorMessage(String summaryMsg, String detailMsg, int step) {
        WebExConfigWizardBean wizardBean =
                (WebExConfigWizardBean) getWizardBean();
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, summaryMsg));
        mb.setDetail(r.getString(this, detailMsg));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);
        Effect e = new InputFieldErrorEffect();
        wizardBean.setWebExCreateEntityInputEffect(e);
        getMessagesBean().addMessageBean(mb);
        wizardBean.gotoStep(step);
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

    protected void showErrorMessage(String summaryKey, String detailKey) {
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, summaryKey));
        mb.setDetail(r.getString(this, detailKey));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);

        getMessagesBean().addMessageBean(mb);
    }
}
