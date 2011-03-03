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
 * $Id: SamlV2RemoteIdpCreateWizardHandler.java,v 1.11 2009/07/13 19:42:42 farble1670 Exp $
 */
package com.sun.identity.admin.handler;

import com.icesoft.faces.component.inputfile.FileInfo;
import com.icesoft.faces.component.inputfile.InputFile;
import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.dao.SamlV2RemoteIdpCreateDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.SamlV2RemoteIdpCreateWizardBean;
import com.sun.identity.admin.model.SamlV2RemoteIdpCreateWizardStep;
import com.sun.identity.admin.model.WizardBean;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.CotSamlV2RemoteIdpCreateWizardStepValidator;
import com.sun.identity.admin.model.MetadataSamlV2RemoteIdpCreateWizardStepValidator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.EventObject;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

public class SamlV2RemoteIdpCreateWizardHandler
        extends SamlV2RemoteCreateWizardHandler {

    private SamlV2RemoteIdpCreateDao samlV2RemoteIdpCreateDao;

    @Override
    public void initWizardStepValidators() {
        getWizardStepValidators()[SamlV2RemoteIdpCreateWizardStep.COT.toInt()] = new CotSamlV2RemoteIdpCreateWizardStepValidator(getWizardBean());
        getWizardStepValidators()[SamlV2RemoteIdpCreateWizardStep.METADATA.toInt()] = new MetadataSamlV2RemoteIdpCreateWizardStepValidator(getWizardBean());
    }

    public void setSamlV2RemoteIdpCreateDao(
            SamlV2RemoteIdpCreateDao samlV2RemoteIdpCreateDao) {
        this.samlV2RemoteIdpCreateDao = samlV2RemoteIdpCreateDao;
    }

    private void popUpErrorMessage(String summaryMsg, String detailMsg, int step) {
        getSamlV2RemoteIdpCreateWizardBean().setStdMetaFileProgress(0);
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, summaryMsg));
        mb.setDetail(r.getString(this, detailMsg));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);
        Effect e = new InputFieldErrorEffect();
        getSamlV2RemoteIdpCreateWizardBean().setSamlV2RemoteCreateEntityInputEffect(e);
        getMessagesBean().addMessageBean(mb);
        getSamlV2RemoteIdpCreateWizardBean().gotoStep(step);
    }

    public void stdMetaUploadFile(ActionEvent event) throws IOException {
        InputFile inputFile = (InputFile) event.getSource();
        FileInfo fileInfo = inputFile.getFileInfo();
        if (fileInfo.getStatus() == FileInfo.SAVED) {
            // read the file into a string
            // reference our newly updated file for display purposes and
            // added it to filename string object in our bean
            File file = new File(fileInfo.getFile().getAbsolutePath());

            StringBuffer contents = new StringBuffer();
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
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            getSamlV2RemoteIdpCreateWizardBean().setStdMetaFilename(
                    fileInfo.getFileName());
            getSamlV2RemoteIdpCreateWizardBean().setStdMetaFile(
                    contents.toString());
            file.delete();
        }
    }

    public void stdMetaFileUploadProgress(EventObject event) {
        InputFile ifile = (InputFile) event.getSource();
        getSamlV2RemoteIdpCreateWizardBean().setStdMetaFileProgress(
                ifile.getFileInfo().getPercent());
    }

    private SamlV2RemoteIdpCreateWizardBean getSamlV2RemoteIdpCreateWizardBean() {
        return (SamlV2RemoteIdpCreateWizardBean) getWizardBean();
    }

    @Override
    public void setWizardBean(WizardBean wizardBean) {
        super.setWizardBean(wizardBean);
    }

    @Override
    public void cancelListener(ActionEvent event) {
        getSamlV2RemoteIdpCreateWizardBean().reset();
        doCancelNext();
    }

    @Override
    public void finishListener(ActionEvent event) {
        if (!validateFinish(event)) {
            return;
        }

        String cot;
        boolean choseFromExisintCot =
                getSamlV2RemoteIdpCreateWizardBean().isCot();
        if (choseFromExisintCot) {
            cot = getSamlV2RemoteIdpCreateWizardBean().getSelectedCot();
        } else {
            cot = getSamlV2RemoteIdpCreateWizardBean().getNewCotName();
        }
        String selectedRealmValue =
                getSamlV2RemoteIdpCreateWizardBean().getSelectedRealm();
        int idx = selectedRealmValue.indexOf("(");
        int end = selectedRealmValue.indexOf(")");
        String realm = selectedRealmValue.substring(idx + 1, end).trim();
        boolean result = false;
        if (getSamlV2RemoteIdpCreateWizardBean().isMeta()) {
            String stdMeta =
                    getSamlV2RemoteIdpCreateWizardBean().getStdMetaFile();
            result = samlV2RemoteIdpCreateDao.importSamlv2RemoteIdp(
                    realm, cot, stdMeta);
        } else {
            String metaUrl =
                    getSamlV2RemoteIdpCreateWizardBean().getMetaUrl();
            result = samlV2RemoteIdpCreateDao.importSamlv2RemoteIdpFromURL(
                    realm, cot, metaUrl);

        }

        if (!result) {
            popUpErrorMessage(
                    "creationFailedSummary",
                    "creationFailedDetail",
                    SamlV2RemoteIdpCreateWizardStep.SUMMARY.toInt());
        } else {
            getSamlV2RemoteIdpCreateWizardBean().reset();
            doFinishNext();
        }
    }
}
