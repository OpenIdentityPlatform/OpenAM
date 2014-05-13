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
 * $Id: FAMSTSImpl.java,v 1.6 2010/01/15 18:54:35 mrudul_uchil Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.wss.sts;

import com.sun.identity.shared.xml.XMLUtils;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;

import javax.annotation.Resource;

import com.sun.xml.ws.api.security.trust.WSTrustException;
import com.sun.xml.ws.api.security.trust.config.STSConfiguration;
import com.sun.xml.ws.policy.impl.bindings.AppliesTo;
import com.sun.xml.ws.security.trust.GenericToken;
import com.sun.xml.ws.security.trust.WSTrustVersion;
import com.sun.xml.ws.security.trust.WSTrustConstants;
import com.sun.xml.ws.security.trust.WSTrustElementFactory;
import com.sun.xml.ws.security.trust.WSTrustFactory;
import com.sun.xml.ws.security.trust.sts.BaseSTSImpl;

import com.sun.xml.ws.api.security.trust.WSTrustContract;
import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.security.IssuedTokenContext;
import com.sun.xml.ws.security.impl.IssuedTokenContextImpl;
import com.sun.xml.ws.security.impl.policy.Constants;
import com.sun.xml.ws.security.trust.elements.BaseSTSRequest;
import com.sun.xml.ws.security.trust.elements.BaseSTSResponse;
import com.sun.xml.ws.security.trust.elements.RequestSecurityToken;
import com.sun.xml.ws.security.trust.impl.DefaultSTSConfiguration;
import com.sun.xml.ws.security.trust.impl.DefaultTrustSPMetadata;
import com.sun.xml.ws.security.trust.util.WSTrustUtil;
import com.sun.xml.wss.SecurityEnvironment;
import com.sun.xml.wss.SubjectAccessor;
import com.sun.xml.wss.XWSSecurityException;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Iterator;

@ServiceMode(value=Service.Mode.PAYLOAD)
@WebServiceProvider(wsdlLocation="WEB-INF/wsdl/famsts.wsdl")
public class FAMSTSImpl extends BaseSTSImpl implements Provider<Source>{
    @Resource
    protected WebServiceContext context;

   public Source invoke(final Source rstElement){
       final STSConfiguration config = getConfiguration();
       WSTrustElementFactory eleFac = WSTrustElementFactory.newInstance(wstVer);
       Source rstrEle = null;
       try{
           final RequestSecurityToken rst = parseRST(rstElement);

           String appliesTo = null;
           final AppliesTo applTo = rst.getAppliesTo();
           if(applTo != null){
               appliesTo = WSTrustUtil.getAppliesToURI(applTo);
           }

           if (appliesTo == null){
               appliesTo = DEFAULT_APPLIESTO;
           }
           if(rst.getRequestType().toString().equals(
               wstVer.getIssueRequestTypeURI())){
               rstrEle = issue(config, appliesTo, eleFac, rst);
           } else if(rst.getRequestType().toString().equals(
               wstVer.getCancelRequestTypeURI())){
               rstrEle = cancel(config, appliesTo, eleFac, rst);
           } else if(rst.getRequestType().toString().equals(
               wstVer.getRenewRequestTypeURI())){
               rstrEle = renew(config, appliesTo, eleFac, rst);
           } else if(rst.getRequestType().toString().equals(
               wstVer.getValidateRequestTypeURI())){
               rstrEle = validate(config, appliesTo, eleFac, rst);
           }
       } catch (Exception ex){
           //ex.printStackTrace();
           throw new WebServiceException(ex);
       }

       return rstrEle;
   }

   protected MessageContext getMessageContext() {
       MessageContext msgCtx = context.getMessageContext();
       return msgCtx;
   }

   private RequestSecurityToken parseRST(Source source) throws WSTrustException{
       Element ele = null;
       try{
           DOMResult result = new DOMResult();
           Transformer tf = XMLUtils.getTransformerFactory().newTransformer();
           tf.transform(source, result);

           Node node = result.getNode();
           if (node instanceof Document){
               ele = ((Document)node).getDocumentElement();
           } else if (node instanceof Element){
               ele = (Element)node;
           }
       } catch(Exception xe){
           throw new WSTrustException("Error occurred while trying to parse " +
               "RST stream", xe);
       }
       WSTrustElementFactory fact = WSTrustElementFactory.newInstance(wstVer);
       RequestSecurityToken rst = fact.createRSTFrom(ele);

       // handling OnBehalfOf
       NodeList list =
           ele.getElementsByTagNameNS(ele.getNamespaceURI(), "OnBehalfOf");
       if (list.getLength() > 0){
           Element oboToken = (Element)list.item(0).getChildNodes().item(0);
           rst.setOnBehalfOf(fact.createOnBehalfOf(new GenericToken(oboToken)));
       }

       return rst;
   }

   STSConfiguration getConfiguration() {
       final MessageContext msgCtx = getMessageContext();
       //final CallbackHandler handler =
           //(CallbackHandler)msgCtx.get(WSTrustConstants.STS_CALL_BACK_HANDLER);
       final SecurityEnvironment secEnv =
           (SecurityEnvironment)msgCtx.get(WSTrustConstants.SECURITY_ENVIRONMENT);
       WSTrustVersion wstVersion =
           (WSTrustVersion)msgCtx.get(WSTrustConstants.WST_VERSION);
       String authnCtxClass =
           (String)msgCtx.get(WSTrustConstants.AUTHN_CONTEXT_CLASS);
       if (wstVersion != null){
           wstVer = wstVersion;
       }
       //Get Runtime STSConfiguration
       STSConfiguration rtConfig = WSTrustFactory.getRuntimeSTSConfiguration();
       if (rtConfig != null){
           if (rtConfig.getCallbackHandler() == null){
               rtConfig.getOtherOptions().put(
                   WSTrustConstants.SECURITY_ENVIRONMENT, secEnv);
           }
           if (wstVersion == null){
               wstVersion = (WSTrustVersion)rtConfig.getOtherOptions().get(
                   WSTrustConstants.WST_VERSION);
               if (wstVersion != null){
                   wstVer = wstVersion;
               }
           }

           rtConfig.getOtherOptions().put(WSTrustConstants.WST_VERSION, wstVer);

           return rtConfig;
       }

       // Get default STSConfiguration
       DefaultSTSConfiguration config = new DefaultSTSConfiguration();
       config.getOtherOptions().put(
           WSTrustConstants.SECURITY_ENVIRONMENT, secEnv);
       //config.setCallbackHandler(handler);
       final Iterator iterator = (Iterator)msgCtx.get(
               Constants.SUN_TRUST_SERVER_SECURITY_POLICY_NS);
       if (iterator == null){
           throw new WebServiceException("STS configuration is not available");
       }

       while(iterator.hasNext()) {
           final PolicyAssertion assertion = (PolicyAssertion)iterator.next();
           if (!STS_CONFIGURATION.equals(assertion.getName().getLocalPart())) {
               continue;
           }
           config.setEncryptIssuedToken(Boolean.parseBoolean(
               assertion.getAttributeValue(new QName("",ENCRYPT_TOKEN))));
           config.setEncryptIssuedKey(Boolean.parseBoolean(
               assertion.getAttributeValue(new QName("",ENCRYPT_KEY))));
           final Iterator<PolicyAssertion> stsConfig =
               assertion.getNestedAssertionsIterator();
           while(stsConfig.hasNext()){
               final PolicyAssertion serviceSTSPolicy = stsConfig.next();
               if(LIFETIME.equals(serviceSTSPolicy.getName().getLocalPart())){
                   config.setIssuedTokenTimeout(
                       Integer.parseInt(serviceSTSPolicy.getValue()));
                   continue;
               }
               if(CONTRACT.equals(serviceSTSPolicy.getName().getLocalPart())){
                   config.setType(serviceSTSPolicy.getValue());
                   continue;
               }
               if(ISSUER.equals(serviceSTSPolicy.getName().getLocalPart())){
                   config.setIssuer(serviceSTSPolicy.getValue());
                   continue;
               }

               if(SERVICE_PROVIDERS.equals(
                   serviceSTSPolicy.getName().getLocalPart())){
                   final Iterator<PolicyAssertion> serviceProviders =
                   serviceSTSPolicy.getNestedAssertionsIterator();
                   String endpointUri = null;
                   while(serviceProviders.hasNext()){
                       final PolicyAssertion serviceProvider =
                           serviceProviders.next();
                       endpointUri = serviceProvider.getAttributeValue(
                           new QName("",END_POINT));
                       if (endpointUri == null){
                            endpointUri = serviceProvider.getAttributeValue(
                                new QName("", END_POINT.toLowerCase()));
                       }
                       final DefaultTrustSPMetadata data =
                           new DefaultTrustSPMetadata(endpointUri);
                       final Iterator<PolicyAssertion> spConfig =
                           serviceProvider.getNestedAssertionsIterator();
                       while(spConfig.hasNext()){
                           final PolicyAssertion policy = spConfig.next();
                           if(ALIAS.equals(policy.getName().getLocalPart())){
                               data.setCertAlias(policy.getValue());
                           }else if (TOKEN_TYPE.equals(
                               policy.getName().getLocalPart())){
                               data.setTokenType(policy.getValue());
                           }else if (KEY_TYPE.equals(
                               policy.getName().getLocalPart())){
                               data.setKeyType(policy.getValue());
                           }
                       }

                       config.addTrustSPMetadata(data, endpointUri);
                   }
               }
           }
       }
       config.getOtherOptions().put(WSTrustConstants.WST_VERSION, wstVer);

       if(authnCtxClass != null){
           config.getOtherOptions().put(
               WSTrustConstants.AUTHN_CONTEXT_CLASS, authnCtxClass);
       }
       config.getOtherOptions().putAll(msgCtx);

       return config;
   }

   private Source issue(final STSConfiguration config, final String appliesTo,
       final WSTrustElementFactory eleFac, final BaseSTSRequest rst)
       throws WSTrustException, TransformerException {

       // Create the RequestSecurityTokenResponse message
       final WSTrustContract<BaseSTSRequest, BaseSTSResponse> contract = 
           WSTrustFactory.newWSTrustContract(config, appliesTo);
       final IssuedTokenContext context = new IssuedTokenContextImpl();
       try {
           context.setRequestorSubject(
               SubjectAccessor.getRequesterSubject(getMessageContext()));
       } catch (XWSSecurityException ex) {
           throw new WSTrustException("error getting subject",ex);
       }

       final BaseSTSResponse response = contract.issue(rst, context);

       return eleFac.toSource(response);
   }

   private Source cancel(final STSConfiguration config,
       final String appliesTo, final WSTrustElementFactory eleFac,
       final BaseSTSRequest rst) {
       return null;
   }

   private Source renew(final STSConfiguration config,final String appliesTo,
       final WSTrustElementFactory eleFac, final RequestSecurityToken rst)
       throws WSTrustException {
       Source rstrEle;

       // Create the RequestSecurityTokenResponse message
       final WSTrustContract<BaseSTSRequest, BaseSTSResponse> contract =
           WSTrustFactory.newWSTrustContract(config, appliesTo);
       final IssuedTokenContext context = new IssuedTokenContextImpl();

       final BaseSTSResponse rstr = contract.renew(rst, context);

       rstrEle = eleFac.toSource(rstr);
       return rstrEle;
   }

   private Source validate(final STSConfiguration config,final String appliesTo,
       final WSTrustElementFactory eleFac, final BaseSTSRequest rst)
       throws WSTrustException {
       Source rstrEle;

       // Create the RequestSecurityTokenResponse message
       final WSTrustContract<BaseSTSRequest, BaseSTSResponse> contract = 
           WSTrustFactory.newWSTrustContract(config, appliesTo);
       final IssuedTokenContext lcontext = new IssuedTokenContextImpl();

       final BaseSTSResponse rstr = contract.validate(rst, lcontext);

       rstrEle = eleFac.toSource(rstr);
       return rstrEle;
   }
}
