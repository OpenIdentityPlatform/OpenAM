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
 * $Id: LibertyAuthnResponseHelper.java,v 1.5 2008/08/04 20:03:33 huacui Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.agents.common;

import java.net.URLDecoder;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.AudienceRestrictionCondition;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.protocol.Response;

/**
 * This is a helper class which provides utility methods to handle Liberty
 * AuthnResponse messages. 
 * 
 * The key goal behind having this class in place is to isolate the IS SDK 
 * related Liberty code to one place for better manageability.  
 */
public class LibertyAuthnResponseHelper extends SurrogateBase 
    implements ILibertyAuthnResponseHelper {
    
    public LibertyAuthnResponseHelper(Module module) {
        super(module);
    }
    
    public void initialize(int skewFactor) {
        setSkewFactor(skewFactor*1000L);
    }
        
    private Response getDecodedAuthnResponse(String encodedAuthnResponse) 
        throws AgentException {
        // The response is Base64 encoded. Decode it
        String authnResponseXML = new String(Base64.decode(
            encodedAuthnResponse));
        
        try {                
            // Parse and initialize the AuthnResponse
            return FSAuthnResponse.parseAuthnResponseXML(authnResponseXML);   
        } catch (Exception e) {
            logError("LibertyAuthnResponseHandler: initialization failed: "
                     + e.getMessage());

            if(isLogWarningEnabled()) {
                logWarning("LibertyAuthnResponseHandler: ", e);
            }

            throw new AgentException("LibertyAuthnResponseHandler " 
                                   + "initialization failed:", e);           
        }           
    }
    
    /**
     * Validates the AuthnResponse to verify the authenticity. The response is 
     * examined to determine if it corresponds to the specified requestID
     *
     * @param authnResponse Authentication Response.
     * @param requestID the Liberty AuthnRequest's requestID
     * @param trustedIDProvider the identity provider URL (Identity Server Login
     * URL which when configured for CDSSO will be the CDC Servlet URL)
     * @param serviceProvider the service provider URL which is the URL of the
     * agent protected application.
     * @throws AgentException if the validation fails.   
     */
    private void validate(
        Response authnResponse, 
        String requestID, 
        List trustedIDProviders, 
        String serviceProvider
    ) throws AgentException {
        // Check if the reponse corresponds to the request made
        String responseID = authnResponse.getInResponseTo();
        if (!requestID.equals(responseID)) {
            logError("LibertyAuthnResponseHandler : ResponseID -" + responseID 
                   + " does not match with RequestID - " + requestID);
            throw new AgentException("LibertyAuthnResponseHandler : validation" 
                                   + " failed - responseID does not match" 
                                   + " requestID");        
        }
        
        // Verify the status code of the response
        String status = authnResponse.getStatus().getStatusCode().getValue();
        if (!status.equals(IFSConstants.STATUS_CODE_SUCCESS)) {
            logError("LibertyAuthnResponseHandler : Unsuccessful in getting" +
                "AuthnResponse. Status is :" + status);
            throw new AgentException("LibertyAuthnResponseHandler : validation" 
                                   + " failed - unsuccessful status response");        
            
        }
        
        List assertions = authnResponse.getAssertion();
        int numAssertions = assertions.size();
        if (numAssertions != 1) {
            logError("LibertyAuthnResponseHandler : Unable to retrive the " 
                   + "right Assertion. Invalid number of assertions: " + 
                    numAssertions); 
            throw new AgentException("LibertyAuthnResponseHandler invalid " +
                "number of assertions");
        } 
        
        // Verify if the response was from a trusted Identity Provider (IDP)
        Assertion assertion = (Assertion) assertions.get(0);
        String issuer = assertion.getIssuer();     
        if (!trustedIDProviders.contains(issuer)) {
            logError("LibertyAuthnResponseHandler : Response received from " +
                "an untrusted provider - " + issuer);
            throw new AgentException("LibertyAuthnResponseHandler invalid " +
                "number of assertions");            
        }
        
        // Verify if the assertion conditions are satisfied.
        Conditions conditions = assertion.getConditions();
        Set audience = conditions.getAudienceRestrictionCondition();
        
        boolean present = isPresent(audience, serviceProvider);                         
        boolean dateValid = checkDateValidity(conditions);
        if (!present || !dateValid) {   
            // NOTE: Checking for date validity means time synchronization 
            // with identity server host dependency exists!
            logError("LibertyAuthnResponseHandler : One or more Assertion " 
                   + "conditions have not been met. Present in audience - " 
                   + present + " Date valid - " + dateValid);   
            
            throw new AgentException("LibertyAuthnResponseHandler assertion " 
                                   + "conditions not satisfied");                        
        }
    }
    
    private boolean checkDateValidity(Conditions conditions) {
        // Obtain the skew factor from the configuration
        long skew = getSkewFactor();
        if(skew == 0L) {
            if (isLogWarningEnabled()) {
                logWarning("LibertyAuthnResponseHandler : "
                        + "cdsso.clock.skew value  is 0.");
            }
        }
        
        Date notBefore = conditions.getNotBefore();
        Date notOnOrAfter = conditions.getNotOnorAfter();
        Date systemDate = new Date();
        
        if (isLogMessageEnabled()) {
            logMessage("LibertyAuthnResponseHandler : Checking assertion " 
                     + "validity. notBefore: " + notBefore + " notOnOrAfter: " 
                     + notOnOrAfter + " SystemDate: " + systemDate + " skew: "
                     + skew);
        }
        
        // Adjust the notBefore and notOnOrAfter with skew factor
        long adjustedNotBefore = notBefore.getTime() - skew;
        long adjustedNotOnOrAfter = notOnOrAfter.getTime() + skew;
        long systemTime = systemDate.getTime();
        
        return (((systemTime >= adjustedNotBefore) &&
            (systemTime < adjustedNotOnOrAfter)));
    }
    
    private boolean isPresent(Set audience, String serviceProvider) {
        boolean present = false;
        if (audience == null || audience.isEmpty()) {
            present = true; // As there is no restriction                
        }
        Iterator itr = audience.iterator();        
        while (itr.hasNext() && !present) {
            AudienceRestrictionCondition arc = (AudienceRestrictionCondition) 
               itr.next();
            if (arc.containsAudience(serviceProvider)) {
                present = true;
            }
        }
        return present;
    }

    /**
     * Gets the encrypted SSOToken string.
     * 
     * <p> This method first validates the AuthnResponse to verify its 
     * authenticity. The verification process includes validating the requestID,
     * the response status, issuer's authenticity and the assertion conditions.
     *
     * @param encodedAuthnResponse Encoded Authentication Response.
     * @param requestID the Liberty AuthnRequest's requestID
     * @param trustedIDProvider the identity provider URL (OpenSSO server) 
     * Server Login URL which when configured for CDSSO will be the CDC Servlet 
     * URL)
     * @param serviceProvider the service provider URL which is the URL of the
     * agent protected application.
     * @throws AgentException if the validation fails.   
     */    
    public String getSSOTokenString(
        String encodedAuthnResponse,
        String requestID, 
        List trustedIDProviders, 
        String serviceProvider
    ) throws AgentException {
        Response authnResponse = getDecodedAuthnResponse(encodedAuthnResponse);
        
        validate(authnResponse, requestID, trustedIDProviders, serviceProvider);
        List assertions = authnResponse.getAssertion();
        Assertion assertion = (Assertion) assertions.get(0);
        
        Set statements = assertion.getStatement();
        int numStatements = statements.size();
        if (numStatements != 1) {                                     
            logError("LibertyAuthnResponseHandler : Unable to retrive the " 
                   + "correct Statement. Invalid number of statements: " + 
                    numStatements); 
            throw new AgentException("LibertyAuthnResponseHandler invalid " +
                "number of Statements found");                  
        }
        
        AuthenticationStatement authStatement = (AuthenticationStatement) 
            statements.iterator().next();
        String tokenStr = 
            authStatement.getSubject().getNameIdentifier().getName();                
        return URLDecoder.decode(tokenStr);   
    }
    
    private long getSkewFactor() {
        return _skewFactor;
    }
    
    private void setSkewFactor(long skewFactor) {
        _skewFactor = skewFactor;
    }
    
    private long _skewFactor;
}
