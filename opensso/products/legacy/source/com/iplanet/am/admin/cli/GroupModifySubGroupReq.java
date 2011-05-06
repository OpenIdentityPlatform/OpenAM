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
 * $Id: GroupModifySubGroupReq.java,v 1.2 2008/06/25 05:52:28 qcheng Exp $
 *
 */

package com.iplanet.am.admin.cli;

import com.iplanet.am.sdk.AMAssignableDynamicGroup;
import com.iplanet.am.sdk.AMDynamicGroup;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStaticGroup;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.PrintUtils;
import com.iplanet.sso.SSOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class GroupModifySubGroupReq extends AdminReq {
    protected Map values;
    protected String subGroupDN;
    private Map subGroupReq = new HashMap();

    /**
     * Constructs a new GroupModifySubGroupReq.
     *
     * @param targetDN the Group DN. 
     */        
    GroupModifySubGroupReq(String targetDN) {
        super(targetDN);
    }

    /**
     * adds the Group DN and its avPair Map to the subGroupReq Map.
     *
     * @param modifyDN the SubGroup DN
     * @param avPair the Map which contains the attribute as key and value.
     */        
    void addSubGroupReq(String modifyDN, Map avPair) {
        subGroupDN = modifyDN;
        values = avPair;
        subGroupReq.put(modifyDN, avPair);
    }

    /**
     * converts this object into a string.
     *
     * @return String.
     */
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter prnWriter = new PrintWriter(stringWriter);
        PrintUtils prnUtl = new PrintUtils(prnWriter);
        prnWriter.println(bundle.getString("requestdescription72") +
            " " + targetDN);
        AdminUtils.printAttributeNameValuesMap(prnWriter, prnUtl, subGroupReq);
        prnWriter.flush();
        return stringWriter.toString();
    }
        
    void process(AMStoreConnection dpConnection)
        throws AdminException
    {
        if (AdminUtils.logEnabled()) {
            AdminUtils.log(bundle.getString("group") + " " + targetDN + "\n" +
                bundle.getString("modifygroup"));
        }
                                                                                
        AdminReq.writer.println(bundle.getString("group") + " " + targetDN +
            "\n" + bundle.getString("modifygroup"));

        int groupType = GroupUtils.getGroupType(subGroupDN, dpConnection,
            bundle);

        switch (groupType) {
        case AMObject.GROUP:
            modifyGroup(dpConnection);
            break;
        case AMObject.DYNAMIC_GROUP:
            modifyDynamicGroup(dpConnection);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            modifyAssignableDynamicGroup(dpConnection);
            break;
        }
    }

    private void modifyGroup(AMStoreConnection dpConnection)
        throws AdminException
    {
        AMStaticGroup grp = null;
        try {
            grp = dpConnection.getStaticGroup(subGroupDN);
            doLog(grp, AdminUtils.MODIFY_GROUP_ATTEMPT);
            grp.setAttributes(values);
            grp.store();
//            doLog(grp, "modify-group");
            doLog(grp, AdminUtils.MODIFY_GROUP);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }

    private void modifyDynamicGroup(AMStoreConnection dpConnection)
        throws AdminException
    {

        AMDynamicGroup grp = null;
        try {
            String strFilter = null;
                                                                                
            if ((values != null) && (values.get(GROUP_FILTER_INFO) != null)) {
                Set set = (Set)values.remove(GROUP_FILTER_INFO);
                                                                                
                if ((set != null) && !set.isEmpty()) {
                    strFilter = (String)set.iterator().next();
                }
            }

            grp = dpConnection.getDynamicGroup(subGroupDN);

            doLog(grp, AdminUtils.MODIFY_GROUP_ATTEMPT);
                                                                                
            if (strFilter != null) {
                grp.setFilter(strFilter);
            }
                                                                                
            if ((values != null) && !values.isEmpty()) {
                grp.setAttributes(values);
                grp.store();
            }
                                                                                
//          doLog(grp, "modify-group");
            doLog(grp, AdminUtils.MODIFY_GROUP);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }


    protected void modifyAssignableDynamicGroup(AMStoreConnection dpConnection)
        throws AdminException
    {
        AMAssignableDynamicGroup grp = null;
        try {
            grp = dpConnection.getAssignableDynamicGroup(subGroupDN);
            doLog(grp, AdminUtils.MODIFY_GROUP_ATTEMPT);
            grp.setAttributes(values);
            grp.store();
//          doLog(grp, "modify-group");
            doLog(grp, AdminUtils.MODIFY_GROUP);
        } catch (AMException ame) {
            throw new AdminException(ame);
        } catch (SSOException ssoe) {
            throw new AdminException(ssoe);
        }
    }
}
