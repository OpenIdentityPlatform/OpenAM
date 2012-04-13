/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PolicyListener.java,v 1.2 2008/06/25 05:43:47 qcheng Exp $
 *
 */



package com.sun.identity.policy.interfaces;
import com.sun.identity.policy.PolicyEvent;

/**
 * The interface <code>PolicyListener</code> defines an interface 
 * for listeners that would register with policy framework 
 * to receive notifications whenever a <code>Policy</code> is added, removed 
 * or changed
 *
 * @see 
 * com.sun.identity.policy.PolicyEvaluator#addPolicyListener(PolicyListener)
 * @supported.all.api
 */
public interface PolicyListener {

    /**
     * Returns the service type name for which this listener wants to get
     * notifications
     *
     * @return String representing the service type name.
     */
     public String getServiceTypeName(); 

    /** This method is called by the policy framework whenever 
     *  a policy is added, removed or changed. The notification
     *  is sent only if the policy has any rule that has the 
     *  <code>serviceTypeName</code> of this listener
     *
     *  @param policyEvent event object sent by the policy framework
     *  @see com.sun.identity.policy.PolicyEvent
     */
     public void policyChanged(PolicyEvent policyEvent);

}
