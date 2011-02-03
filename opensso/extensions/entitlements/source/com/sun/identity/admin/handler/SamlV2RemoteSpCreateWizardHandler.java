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
 * $Id: SamlV2RemoteSpCreateWizardHandler.java,v 1.15 2009/07/13 23:22:02 asyhuang Exp $
 */
package com.sun.identity.admin.handler;

import com.icesoft.faces.component.dragdrop.DndEvent;
import com.icesoft.faces.component.dragdrop.DropEvent;
import com.icesoft.faces.component.inputfile.FileInfo;
import com.icesoft.faces.component.inputfile.InputFile;
import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.dao.SamlV2CreateSharedDao;
import com.sun.identity.admin.dao.SamlV2RemoteSpCreateDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.model.CotSamlV2RemoteSpCreateWizardStepValidator;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MetadataSamlV2RemoteSpCreateWizardStepValidator;
import com.sun.identity.admin.model.SamlV2RemoteSpCreateWizardBean;
import com.sun.identity.admin.model.SamlV2RemoteSpCreateWizardStep;
import com.sun.identity.admin.model.SamlV2ViewAttribute;
import com.sun.identity.admin.model.ViewAttribute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.ValueChangeEvent;

public class SamlV2RemoteSpCreateWizardHandler
        extends SamlV2RemoteCreateWizardHandler
        implements Serializable {

    private SamlV2RemoteSpCreateDao samlV2RemoteSpCreateDao;

    @Override
    public void initWizardStepValidators() {
        getWizardStepValidators()[SamlV2RemoteSpCreateWizardStep.COT.toInt()] = new CotSamlV2RemoteSpCreateWizardStepValidator(getWizardBean());
        getWizardStepValidators()[SamlV2RemoteSpCreateWizardStep.METADATA.toInt()] = new MetadataSamlV2RemoteSpCreateWizardStepValidator(getWizardBean());
    }

    public void setSamlV2RemoteSpCreateDao(SamlV2RemoteSpCreateDao samlV2RemoteSpCreateDao) {
        this.samlV2RemoteSpCreateDao = samlV2RemoteSpCreateDao;
    }

    @Override
    public void finishListener(ActionEvent event) {

        if (!validateFinish(event)) {
            return;
        }

        String cot;
        boolean choseFromExisintCot = getSamlV2RemoteSpCreateWizardBean().isCot();
        if (choseFromExisintCot) {
            cot = getSamlV2RemoteSpCreateWizardBean().getSelectedCot();
        } else {
            cot = getSamlV2RemoteSpCreateWizardBean().getNewCotName();
        }

        String selectedRealmValue =
                getSamlV2RemoteSpCreateWizardBean().getSelectedRealm();
        int idx = selectedRealmValue.indexOf("(");
        int end = selectedRealmValue.indexOf(")");
        String realm = selectedRealmValue.substring(idx + 1, end).trim();

        List attrMapping = new ArrayList();
        List viewAttributes =
                getSamlV2RemoteSpCreateWizardBean().getViewAttributes();
        attrMapping =
                getSamlV2RemoteSpCreateWizardBean().getToListOfStrings(viewAttributes);
        boolean result = false;
        if (getSamlV2RemoteSpCreateWizardBean().isMeta()) {
            String stdMetadataFile =
                    getSamlV2RemoteSpCreateWizardBean().getStdMetaFile();
            result = samlV2RemoteSpCreateDao.importSamlv2RemoteSpFromFile(
                    realm,
                    cot,
                    stdMetadataFile,
                    attrMapping);
        } else {
            String metaUrl =
                    getSamlV2RemoteSpCreateWizardBean().getMetaUrl();
            result = samlV2RemoteSpCreateDao.importSamlv2RemoteSpFromURL(
                    realm,
                    cot,
                    metaUrl,
                    attrMapping);
        }

        if (!result) {           
            popUpErrorMessage(
                    "creationFailedSummary",
                    "creationFailedDetail",
                    SamlV2RemoteSpCreateWizardStep.SUMMARY.toInt());
        } else {
            getSamlV2RemoteSpCreateWizardBean().reset();
            doFinishNext();
        }
    }

    @Override
    public void cancelListener(ActionEvent event) {
        getSamlV2RemoteSpCreateWizardBean().reset();
        doCancelNext();
    }

    private void popUpErrorMessage(String summary, String detail, int step) {
        getSamlV2RemoteSpCreateWizardBean().setStdMetaFileProgress(0);
        MessageBean mb = new MessageBean();
        Resources r = new Resources();
        mb.setSummary(r.getString(this, summary));
        mb.setDetail(r.getString(this, detail));
        mb.setSeverity(FacesMessage.SEVERITY_ERROR);
        Effect e = new InputFieldErrorEffect();
        getSamlV2RemoteSpCreateWizardBean().setSamlV2RemoteCreateEntityInputEffect(e);
        getMessagesBean().addMessageBean(mb);
        getSamlV2RemoteSpCreateWizardBean().gotoStep(step);
    }

    public boolean validateMetadata() {
        boolean usingMetaDataFile = getSamlV2RemoteSpCreateWizardBean().isMeta();

        if (!usingMetaDataFile) {

            String url = getSamlV2RemoteSpCreateWizardBean().getMetaUrl();
            if ((url == null) || (url.length() == 0)) {               
                popUpErrorMessage(
                        "invalidMetaUrlSummary",
                        "invalidMetaUrlDetail",
                        SamlV2RemoteSpCreateWizardStep.METADATA.toInt());
                return false;
            }
            if (!SamlV2CreateSharedDao.getInstance().validateUrl(url)) {
                popUpErrorMessage(
                        "urlErrorSummary",
                        "urlErrorDetail",
                        SamlV2RemoteSpCreateWizardStep.METADATA.toInt());
                return false;
            }

        } else {

            String meta = getSamlV2RemoteSpCreateWizardBean().getStdMetaFile();
            if ((meta == null) || meta.length() == 0) {              
                popUpErrorMessage(
                        "invalidMetafileSummary",
                        "invalidMetafileDetail",
                        SamlV2RemoteSpCreateWizardStep.METADATA.toInt());
                return false;
            }

            if (!SamlV2CreateSharedDao.getInstance().validateMetaFormat(meta)) {
                getSamlV2RemoteSpCreateWizardBean().setStdMetaFilename("");
                getSamlV2RemoteSpCreateWizardBean().setStdMetaFile("");
                popUpErrorMessage(
                        "invalidMetaFormatSummary",
                        "invalidMetaFormatDetail",
                        SamlV2RemoteSpCreateWizardStep.METADATA.toInt());
                return false;
            }
        }

        return true;
    }

    private SamlV2RemoteSpCreateWizardBean getSamlV2RemoteSpCreateWizardBean() {
        return (SamlV2RemoteSpCreateWizardBean) getWizardBean();
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
            getSamlV2RemoteSpCreateWizardBean().setStdMetaFilename(fileInfo.getFileName());
            getSamlV2RemoteSpCreateWizardBean().setStdMetaFile(contents.toString());
            file.delete();
        }
    }

    public void stdMetaFileUploadProgress(EventObject event) {
        InputFile ifile = (InputFile) event.getSource();
        getSamlV2RemoteSpCreateWizardBean().setStdMetaFileProgress(ifile.getFileInfo().getPercent());
    }

    // for attrmapping
    public void dropListener(DropEvent dropEvent) {
        int type = dropEvent.getEventType();
        if (type == DndEvent.DROPPED) {
            Object dragValue = dropEvent.getTargetDragValue();
            assert (dragValue != null);
            ViewAttribute va = (ViewAttribute) dragValue;

            getSamlV2RemoteSpCreateWizardBean().getViewAttributes().add(va);
        }
    }

    protected ViewAttribute getViewAttribute(FacesEvent event) {
        ViewAttribute va = (ViewAttribute) event.getComponent().
                getAttributes().get("viewAttribute");
        assert (va != null);

        return va;
    }

    public void removeListener(ActionEvent event) {
        ViewAttribute va = getViewAttribute(event);
        getSamlV2RemoteSpCreateWizardBean().getViewAttributes().remove(va);
    }

    public void addListener(ActionEvent event) {
        ViewAttribute va = newViewAttribute();
        va.setEditable(true);
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) va;
        sva.setValueEditable(true);
        sva.setAdded(true);

        getSamlV2RemoteSpCreateWizardBean().getViewAttributes().add(va);
    }

    public void editNameListener(ActionEvent event) {
        ViewAttribute va = (ViewAttribute) getViewAttribute(event);
        va.setNameEditable(true);
    }

    public void nameEditedListener(ValueChangeEvent event) {
        ViewAttribute va = (ViewAttribute) getViewAttribute(event);
        va.setNameEditable(false);
    }

    public void editValueListener(ActionEvent event) {
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) getViewAttribute(event);
        sva.setValueEditable(true);
    }

    public void valueEditedListener(ValueChangeEvent event) {
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) getViewAttribute(event);
        sva.setValueEditable(false);
    }

    public ViewAttribute newViewAttribute() {
        return new SamlV2ViewAttribute();
    }
}
