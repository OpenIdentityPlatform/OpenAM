/*
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
 * $Id: IDPPAuthorizer.java,v 1.6 2008/08/19 19:12:22 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.liberty.ws.idpp.plugin;

import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.*;
import com.sun.identity.policy.interfaces.Condition;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import com.sun.identity.liberty.ws.interfaces.Authorizer;
import com.sun.identity.liberty.ws.idpp.common.*;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;


/**
 * The class <code>IDPPAuthorizer</code> is the default implementation of
 * IDPP Authorization and implements <code>Authorizer</code>. It 
 * provides an allow ,deny, interact for consent, interact for value action 
 * based on the resource that a WSC is requesting for.
 * This makes use of the existing OpenAM policy framework by defining
 * policy as action values in the personal profile service.
 */
public class IDPPAuthorizer implements Authorizer {

    private static ResourceBundle bundle = 
        Locale.getInstallResourceBundle("fmPersonalProfile");
    private static Debug debug = Debug.getInstance("libIDWSF");
    private static PolicyEvaluator evaluator = null;
    static {
      try {
          evaluator = new PolicyEvaluator(IDPPConstants.IDPP_SERVICE);
      } catch (Exception ex) {
          debug.error("IDPPAuthorizer:Static Init failed", ex);
      }
    }

    
    /**
     *Default constructor
     */
    public IDPPAuthorizer() {}

    /**
     * Checks whether this is authorized 
     * IDPPAuthorizer implements this class.
     * @param credential credential 
     * @param action action 
     * @param data object 
     * @param env env map 
     * @return true if authorized, otherwise false. 
     */
    public boolean isAuthorized(Object credential, String action, 
    Object data, Map env) {
       return false;
    }

    /**
     * Returns authorization decision to query or modify the select data
     * @param credential SSOToken of a WSC.
     * @param action request action.
     * @param data Object who is being accessed.
     * @param env A Map contains information useful for policy evaluation.
     *          The following key is defined and its value should be passed in:
     *          Key: <code>USER_ID</code>
     *          Value: id of the user whose resource is being accessed.
     *          Key: <code>AUTH_TYPE</code>
     *          Value: The authentication mechanism WSC used.
     *          Key: <code>MESSAGE</code>
     *          Value:
     *          <code>com.sun.identity.liberty.ws.soapbinding.Message</code>.
     * @return Object AuthorizationDecision object contains authorization 
     *             decision information for the given data.
     *             For Personal Profile service, this object would be the 
     *             String authZ decision value.
     * @exception Exception
     */

    public Object getAuthorizationDecision(
                  Object credential,
                  String action,
                  Object data,
                  java.util.Map env)
    throws Exception {
        debug.message("IDPPAuthorizer.getAuthorizationDecision:Init");
        if(credential == null || action == null || data == null) {
          debug.error("IDPPAuthorizer.isAuthorized:null input");
          throw new Exception(
          bundle.getString("nullInputParams"));
       }
       try {
           SSOToken token = (SSOToken)credential;
           String resource = (String)data;
           Set actions = new HashSet(1);
           actions.add(action);

           Map map = null; 
           String userid = (String) env.get(USER_ID); 
           if (debug.messageEnabled()) {
               debug.message("IDPPAuthorizer.getAuthorizationDecision: uid="
                   + userid);
           }
           if ((userid != null) && (userid.length() != 0)) {
               HashSet set = new HashSet();
               set.add(userid);
               map = new HashMap();
               map.put(Condition.INVOCATOR_PRINCIPAL_UUID, set);
           } 
 
           PolicyDecision policyDecision =  evaluator.getPolicyDecision(
                     token, resource, actions, map);

           if(policyDecision == null) {
              if(debug.messageEnabled()) {
                 debug.message("IDPPAuthorizer.getAuthorization" +
                     "Decision:PolicyDecision is null");
              }
              return IDPPConstants.AUTHZ_DENY;
           }

           Map actionDecisions = policyDecision.getActionDecisions();
           ActionDecision actionDecision = (ActionDecision)
                          actionDecisions.get(action);

           if(actionDecision == null) {
              if(debug.messageEnabled()) {
                 debug.message("IDPPAuthorizer.getAuthorization" +
                     "Decision:ActionDecision is null");
              }
              return IDPPConstants.AUTHZ_DENY;
           }

           Set values = (Set)actionDecision.getValues();
           if(values == null || values.isEmpty()) {
              if(debug.messageEnabled()) {
                 debug.message("IDPPAuthorizer.getAuthorization" +
                     "Decision:values are null");
              }
              return IDPPConstants.AUTHZ_DENY;
           }
          
           if(debug.messageEnabled()) {
              debug.message("IDPPAuthorizer.getAuthorization" +
                  "Decision: action values:" + values);
           } 
           
           if(values.contains(IDPPConstants.AUTHZ_DENY)) {
              return IDPPConstants.AUTHZ_DENY;
           }

           if(values.contains(IDPPConstants.INTERACT_FOR_VALUE)) {
              return IDPPConstants.INTERACT_FOR_VALUE;
           }

           if(values.contains(IDPPConstants.INTERACT_FOR_CONSENT)) {
              return IDPPConstants.INTERACT_FOR_CONSENT;
           }

           Iterator iter = values.iterator();
           return (String)iter.next();

       } catch (Exception ex) {
           debug.error("IDPPAuthorizer.getAuthorizationDecision:"+
               "Exception during authorization.", ex); 
           throw ex;
       }

    }
   
}
