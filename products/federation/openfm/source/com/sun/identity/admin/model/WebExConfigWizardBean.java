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
 * $Id: WebExConfigWizardBean.java,v 1.2 2009/12/14 23:42:15 babysunil Exp $
 */
package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.icesoft.faces.context.Resource;
import com.icesoft.faces.context.StringResource;
import com.sun.identity.admin.dao.IdpDao;
import com.sun.identity.admin.dao.CotDao;
import com.sun.identity.admin.dao.PubKeyDao;
import com.sun.identity.admin.dao.WebExUrlsDao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.faces.model.SelectItem;

public class WebExConfigWizardBean
        extends WizardBean
        implements Serializable {

    private boolean authnReq = false;
    private boolean spInit = true;
    private String idpInit;
    private boolean ssoonly = true;
    private boolean sloandsso = false;
    private boolean autocrup = false;
    private boolean createNew = true;
    private boolean chooseExistng = false;
    private boolean createCot = true;
    private boolean chooseCot = false;
    private String spCertFile;
    private String spCertFilename;
    private String webexSiteUrl;
    private String selectedSigningKey;
    private String selectedIdp;
    private String selectedCot;
    private String newCotName;
    private String newIdpName;
    private String pubKeyContent;
    private String singleLogoutUrl;
    private String singleSignOnUrl;
    private String authnContext;
    private String idpInitTestUrl;
    private Resource fileResource = new StringResource("text");
    private Effect webexSiteUrlNameInputEffect;
    private Effect webexSiteUrlMessageEffect;
    private Effect webExCreateEntityInputEffect;

    public WebExConfigWizardBean() {
        super();
    }

    @Override
    public void reset() {
        super.reset();
        spCertFile = null;
        spCertFilename = null;
        selectedIdp = null;
        selectedCot = null;
        pubKeyContent = null;
        singleLogoutUrl = null;
        webexSiteUrl = null;
        singleSignOnUrl = null;
        authnContext = null;
        idpInit = null;
        newIdpName = null;
        newCotName = null;
        idpInitTestUrl = null;
    }

    public boolean getAuthnReq() {
        return authnReq;
    }

    public void setAuthnReq(boolean authnReq) {
        this.authnReq = authnReq;
    }

    public boolean getSpInit() {
        return spInit;
    }

    public void setSpInit(boolean spInit) {
        this.spInit = spInit;
        if (spInit) {
            setIdpInit("SP Initiated");
        } else {
            setIdpInit("IDP Initiated");
        }
    }

    public String getIdpInit() {
        return idpInit;
    }

    public void setIdpInit(String idpInit) {
        this.idpInit = idpInit;
    }

    public boolean getSsoonly() {
        return ssoonly;
    }

    public void setSsoonly(boolean ssoonly) {
        this.ssoonly = ssoonly;
        if (ssoonly) {
            setSloandsso(false);
        } else {
            setSloandsso(true);
        }
    }

    public boolean getSloandsso() {
        return sloandsso;
    }

    public void setSloandsso(boolean sloandsso) {
        this.sloandsso = sloandsso;
    }

    public boolean getAutocrup() {
        return autocrup;
    }

    public void setAutocrup(boolean autocrup) {
        this.autocrup = autocrup;
    }

    public boolean getCreateNew() {
        return createNew;
    }

    public void setCreateNew(boolean createNew) {
        this.createNew = createNew;
    }

    public boolean getChooseExistng() {
        return chooseExistng;
    }

    public void setChooseExistng(boolean chooseExistng) {
        this.chooseExistng = chooseExistng;
    }

    public boolean getCreateCot() {
        return createCot;
    }

    public void setCreateCot(boolean createCot) {
        this.createCot = createCot;
    }

    public boolean getChooseCot() {
        return chooseCot;
    }

    public void setChooseCot(boolean chooseCot) {
        this.chooseCot = chooseCot;
    }

    public String getSpCertFile() {
        return spCertFile;
    }

    public void setSpCertFile(String spCertFile) {
        this.spCertFile = spCertFile;
    }

    public String getSpCertFilename() {
        return spCertFilename;
    }

    public void setSpCertFilename(String spCertFilename) {
        this.spCertFilename = spCertFilename;
    }

    public String getSelectedIdp() {
        return selectedIdp;
    }

    public void setSelectedIdp(String selectedIdp) {
        this.selectedIdp = selectedIdp;
    }

    public String getSelectedCot() {
        return selectedCot;
    }

    public void setSelectedCot(String selectedCot) {
        this.selectedCot = selectedCot;
    }

    public String getSelectedSigningKey() {
        return selectedSigningKey;
    }

    public void setSelectedSigningKey(String selectedSigningKey) {
        this.selectedSigningKey = selectedSigningKey;
    }

    public Resource getFileResource() {
        return fileResource;
    }

    public void setFileResource(Resource fileResource) {
        this.fileResource = fileResource;
    }

    public String getWebexSiteUrl() {
        return webexSiteUrl;
    }

    public void setWebexSiteUrl(String webexSiteUrl) {
        this.webexSiteUrl = webexSiteUrl;
    }

    public String getNewIdpName() {
        return newIdpName;
    }

    public void setNewIdpName(String newIdpName) {
        this.newIdpName = newIdpName;
    }

    public String getNewCotName() {
        return newCotName;
    }

    public void setNewCotName(String newCotName) {
        this.newCotName = newCotName;
    }

    public List<SelectItem> getAvailableCotItems() {
        List availableCotList = new ArrayList<SelectItem>();
        CotDao cotDao = new CotDao();
        availableCotList = cotDao.getCotNames(getRealmName());
        return availableCotList;
    }

    public List<SelectItem> getAvailableIdpItems() {
        List availableIdpList = new ArrayList<SelectItem>();
        IdpDao idpDao = new IdpDao();
        List<SelectItem> tmp = null;
        if (getSelectedCot() == null || (!(getSelectedCot().length() > 0))) {
            tmp = getAvailableCotItems();
            Iterator it = tmp.iterator();
            if (it.hasNext()) {
                SelectItem cotname = (SelectItem) it.next();
                setSelectedCot(cotname.getLabel());
            }
        }
        if (getSelectedCot() != null && getSelectedCot().length() > 0) {
            availableIdpList = idpDao.getAvailableIdpNames(getSelectedCot());
        }

        return availableIdpList;
    }

    public List<SelectItem> getAvailableSigningKeyItems() {
        SigningKeysBean skbean = SigningKeysBean.getInstance();
        List availableSigningKeyList = skbean.getSigningKeyBeanItems();
        SelectItem item = new SelectItem("", "-");
        availableSigningKeyList.add(0, item);
        return availableSigningKeyList;
    }

    public String getPubKeyContent() {
        PubKeyDao pubKeyDao = new PubKeyDao();
        String idpName = selectedIdp;
        if (getChooseExistng()) {
            idpName = getNewIdpName();
            setSelectedIdp(idpName);
        }
        pubKeyContent = pubKeyDao.getPublicKeyContent(getRealmName(), idpName);
        setFileResource(new StringResource(pubKeyContent));
        return pubKeyContent;
    }

    public String getSingleLogoutUrl() {
        WebExUrlsDao webExUrlsDao = new WebExUrlsDao();
        String idpName = selectedIdp;
        if (getChooseExistng()) {
            idpName = getNewIdpName();
            setSelectedIdp(idpName);
        }
        singleLogoutUrl =
                webExUrlsDao.getSingleLogoutUrl(getRealmName(), idpName);
        return singleLogoutUrl;
    }

    public String getSingleSignOnUrl() {
        WebExUrlsDao webExUrlsDao = new WebExUrlsDao();
        String idpName = selectedIdp;
        if (getChooseExistng()) {
            idpName = getNewIdpName();
            setSelectedIdp(idpName);
        }
        singleSignOnUrl =
                webExUrlsDao.getSingleSignOnUrl(getRealmName(), idpName);
        return singleSignOnUrl;
    }

    public String getAuthnContext() {
        WebExUrlsDao webExUrlsDao = new WebExUrlsDao();
        String idpName = selectedIdp;
        if (getChooseExistng()) {
            idpName = getNewIdpName();
            setSelectedIdp(idpName);
        }
        authnContext = webExUrlsDao.getAuthnContext(getRealmName(), idpName);
        return authnContext;
    }

    public String getIdpInitTestUrl() {
        WebExUrlsDao webExUrlsDao = new WebExUrlsDao();
        String idpName = selectedIdp;
        if (getChooseExistng()) {
            idpName = getNewIdpName();
            setSelectedIdp(idpName);
        }
        idpInitTestUrl = webExUrlsDao.getIdpinitTestUrl(
                getRealmName(), idpName, webexSiteUrl);
        return idpInitTestUrl;
    }

    public Effect getWebexSiteUrlNameInputEffect() {
        return webexSiteUrlNameInputEffect;
    }

    public void setWebexSiteUrlNameInputEffect(Effect webexSiteUrlNameInputEffect) {
        this.webexSiteUrlNameInputEffect = webexSiteUrlNameInputEffect;
    }

    public Effect getWebexSiteUrlMessageEffect() {
        return webexSiteUrlMessageEffect;
    }

    public void setWebexSiteUrlMessageEffect(Effect webexSiteUrlMessageEffect) {
        this.webexSiteUrlMessageEffect = webexSiteUrlMessageEffect;
    }

    public String getRealmName() {
        String realmName = RealmsBean.getInstance().getRealmBean().getName();
        return realmName;
    }

    public Effect getWebExCreateEntityInputEffect() {
        return webExCreateEntityInputEffect;
    }

    public void setWebExCreateEntityInputEffect(
            Effect webExCreateEntityInputEffect) {
        this.webExCreateEntityInputEffect = webExCreateEntityInputEffect;
    }
}

