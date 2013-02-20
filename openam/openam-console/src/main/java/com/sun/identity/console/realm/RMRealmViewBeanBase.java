/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RMRealmViewBeanBase.java,v 1.2 2008/06/25 05:43:11 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.realm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HREF;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.sm.SMSSchema;
import com.sun.web.ui.model.CCBreadCrumbsModel;
import com.sun.web.ui.view.breadcrumb.CCBreadCrumbs;
import com.sun.web.ui.view.html.CCButton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public abstract class RMRealmViewBeanBase
    extends AMPrimaryMastHeadViewBean
{
    private static final String PARENTAGE_PATH = "parentagepath";
    private static final String TXT_ROOT = "txtRoot";
    private static final String PARENTAGE_PATH_HREF = "parentagepathHref";

    protected static final String BTN_SHOW_MENU = "btnShowMenu";

    public RMRealmViewBeanBase(String name) {
        super(name);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(BTN_SHOW_MENU, CCButton.class);
        registerChild(PARENTAGE_PATH, CCBreadCrumbs.class);
        registerChild(PARENTAGE_PATH_HREF, HREF.class);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PARENTAGE_PATH)) {
            view = createParentagePath(name);
        } else if (name.equals(PARENTAGE_PATH_HREF)) {
            view = new HREF(this, name, null);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event, false);
        setSelectedTabNode(getCurrentRealm());
    }

    private CCBreadCrumbs createParentagePath(String name) {
        CCBreadCrumbsModel model = null;
        AMModel ammodel = (AMModel)getModel();
        String curRealm = getCurrentRealm();
        if (curRealm.charAt(0) != '/') {
            curRealm = "/" + curRealm;
        }

        String startDN = ammodel.getStartDN();
        if (startDN.charAt(0) != '/') {
            startDN = "/" + startDN;
        }

        String startDNString = 
            AMFormatUtils.DNToName(ammodel, ammodel.getStartDSDN());

        if (curRealm.equals(startDN)) {
            model = new CCBreadCrumbsModel();
            setDisplayFieldValue(TXT_ROOT, startDNString);
        } else {
            int idx = curRealm.indexOf(startDN);
            String subRealm = (idx == 0) ?
                curRealm.substring(startDN.length()) : curRealm;
            List list = reverseParentagePath(subRealm);
            if (!list.isEmpty()) {
                list.remove(list.size() -1);
            }

            /*
             * The model is initialized with the name of the current realm.
             * This entry is not selectable, just displayed as a label.
             */
            idx = subRealm.lastIndexOf("/");
            if (idx != -1) {
              subRealm = subRealm.substring(idx+1);
            }
            model = new CCBreadCrumbsModel(SMSSchema.unescapeName(subRealm));

            StringBuilder baseDN = new StringBuilder(200);
            baseDN.append(startDN);
            /*
             * each row added to the model is a selectable entry in the
             * parentage path
             */
            model.appendRow();
            model.setValue(CCBreadCrumbsModel.LABEL,
                SMSSchema.unescapeName(startDNString));
            model.setValue(CCBreadCrumbsModel.COMMANDFIELD,
                PARENTAGE_PATH_HREF);
            model.setValue(CCBreadCrumbsModel.HREF_VALUE, baseDN.toString());

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                String tok = (String)iter.next();
                if (!baseDN.toString().equals("/")) {
                    baseDN.append("/").append(tok);
                } else {
                    baseDN.append(tok);
                }
                model.appendRow();
                model.setValue(CCBreadCrumbsModel.LABEL,
                    SMSSchema.unescapeName(tok));
                model.setValue(CCBreadCrumbsModel.COMMANDFIELD,
                    PARENTAGE_PATH_HREF);
                model.setValue(CCBreadCrumbsModel.HREF_VALUE,
                    baseDN.toString());
            }
        }

        return new CCBreadCrumbs(this, model, name);
    }
                                                                                
    private List reverseParentagePath(String path) {
        List list = new ArrayList();
        StringTokenizer st = new StringTokenizer(path, "/");
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return list;
    }

    /**
     * Handles parentage path request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleParentagepathHrefRequest(RequestInvocationEvent event) {
        String path = (String)getDisplayFieldValue(PARENTAGE_PATH_HREF);
        setPageSessionAttribute(AMAdminConstants.CURRENT_REALM, path);
        setCurrentLocation(path);
        unlockPageTrailForSwapping();
        forwardTo();
    }
}
