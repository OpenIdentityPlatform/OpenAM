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
 * $Id: IdentityMembershipHelper.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.view.html.Option;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.plugins.AMIdentityMembershipCondition;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.view.addremove.CCAddRemove;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This is a helper class to Identity Membership Condition View Beans.
 */
public class IdentityMembershipHelper {
    private static IdentityMembershipHelper instance =
        new IdentityMembershipHelper();

    private IdentityMembershipHelper() {
    }

    /**
     * Returns an instance of <code>IdentityMembershipHelper</code>.
     *
     * @return an instance of <code>IdentityMembershipHelper</code>.
     */
    public static IdentityMembershipHelper getInstance() {
        return instance;
    }

    /**
     * Populates the add remove child component with identity IDs.
     *
     * @param child The add remove child component.
     * @param token Administrator Single Sign Token which is used to create
     *        <code>AMIdentity</code> object.
     * @param userLocale User Locale.
     * @param viewBean Instance of the view bean object that is used to create
     *        option list.
     * @param  values Map of
     *         <code>AMIdentityMembershipCondition.AM_IDENTITY_NAME</code>
     *         to a set of identity IDs.
     */
    public static void setSelectedIdentities(
        CCAddRemove child,
        SSOToken token,
        Locale userLocale,
        AMViewBeanBase viewBean,
        Map values
    ) {
        if ((values != null) && !values.isEmpty()) {
            Set setValues = (Set)values.get(
                AMIdentityMembershipCondition.AM_IDENTITY_NAME);
            if ((setValues != null) && !setValues.isEmpty()) {
                CCAddRemoveModel addRemoveModel =
                    (CCAddRemoveModel)child.getModel();
                OptionList optList = createOptionList(token, userLocale,
                    viewBean, setValues);
                addRemoveModel.setSelectedOptionList(optList);
            }
        }
    }

    /**
     * Returns a map of
     * <code>AMIdentityMembershipCondition.AM_IDENTITY_NAME</code> to a set
     * of identity IDs.
     *
     * @param child Add Remove Child Component which contains the selected
     *        identity IDs.
     * @return a map of
     *         <code>AMIdentityMembershipCondition.AM_IDENTITY_NAME</code>
     *         to a set of identity IDs.
     */
    public static Map getSelectedIdentities(CCAddRemove child) {
        Map map = new HashMap(2);
        child.restoreStateData();
        CCAddRemoveModel addRemoveModel = (CCAddRemoveModel)child.getModel();
        OptionList selected = addRemoveModel.getSelectedOptionList();
        Set setValues = new HashSet();
        for (int i = 0; i < selected.size(); i++) {
            Option opt = selected.get(i);
            setValues.add(opt.getValue());
        }
        map.put(AMIdentityMembershipCondition.AM_IDENTITY_NAME, setValues);
        return map;
    }
   
    /**
     * Returns an option list that contains <code>AMIdentity</code> object
     * options.
     *
     * @param token Administrator Single Sign On token that is used to create
     *        the <code>AMIdentity</code> object.
     * @param userLocale User Locale.
     * @param viewBean Instance of the view bean object that is used to create
     *        option list.
     * @param values Either a collection of identity IDs or a collection of
     *        <code>AMIdentity</code> objects.
     * @return an option list that contains <code>AMIdentity</code> object
     *         options.
     */
     public static OptionList createOptionList(
        SSOToken token,
        Locale userLocale,
        AMViewBeanBase viewBean,
        Collection values
    ) {
        OptionList optList = new OptionList();

        if ((values != null) && !values.isEmpty()) {
            /*
             * Need to convert to AMIdentity object if the set contains
             * universal Ids
             */
            Collection amIdentity = (values.iterator().next() instanceof String)
                ? getAMIdentity(token, values) : values;
            Map entries = new HashMap(values.size()*2);

            for (Iterator iter = amIdentity.iterator(); iter.hasNext(); ) {
                AMIdentity identity = (AMIdentity)iter.next();
                entries.put(IdUtils.getUniversalId(identity),
                    PolicyUtils.getDNDisplayString(identity.getName()));
            }
            optList = viewBean.createOptionList(entries, userLocale);
        }

        return optList;
    }

    /**
     * Returns a set of <code>AMIdentity</code> object names.
     *
     * @param token Administrator Single Sign On token to be used for 
     *        creating the <code>AMIdentity</code> object.
     * @param ids Collection of identity IDs.
     * @return a set of <code>AMIdentity</code> object names.
     */
    public static Set getAMIdentityNames(SSOToken token, Collection ids) {
        Set identities = getAMIdentity(token, ids);
        Set names = new HashSet(identities.size() *2);
        for (Iterator i = identities.iterator(); i.hasNext(); ) {
            AMIdentity amid = (AMIdentity)i.next();
            names.add(PolicyUtils.getDNDisplayString(amid.getName()));
        }
        return names;
    }

    /**
     * Returns a set of <code>AMIdentity</code> objects.
     *
     * @param token Administrator Single Sign On token to be used for 
     *        creating the <code>AMIdentity</code> object.
     * @param ids Collection of identity IDs.
     * @return a set of <code>AMIdentity</code> objects.
     */
    public static Set getAMIdentity(SSOToken token, Collection ids) {
        Set values = new HashSet(ids.size()*2);
        for (Iterator i = ids.iterator(); i.hasNext(); ) {
            String id = (String)i.next();
            try {
                AMIdentity amid = IdUtils.getIdentity(token, id);
                values.add(amid);
            } catch(IdRepoException e) {
                // ignored if cannot get identity.
            }
        }
        return values;
    }
}
