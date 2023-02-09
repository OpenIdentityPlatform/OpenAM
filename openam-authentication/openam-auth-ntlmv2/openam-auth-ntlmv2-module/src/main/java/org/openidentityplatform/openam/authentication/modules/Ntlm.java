/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2023 Open Identity Platform Community.
 */

package org.openidentityplatform.openam.authentication.modules;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.Principal;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jcifs.ntlmssp.Type1Message;
import jcifs.ntlmssp.Type2Message;
import jcifs.ntlmssp.Type3Message;
import jcifs.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.ntlmv2.liferay.NtlmManager;
import org.ntlmv2.liferay.NtlmUserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Ntlm extends AMLoginModule {
	public static Logger logger=LoggerFactory.getLogger(Ntlm.class);
	
	static Field AMIdentity_isSharedStateField;
	static{
		try{
			AMIdentity_isSharedStateField=AMLoginModule.class.getDeclaredField("isSharedState");
			AMIdentity_isSharedStateField.setAccessible(true);
		}catch(Exception e){
			logger.error("AMIdentity_isSharedStateField",e);
		}
	}
	
	public Ntlm() {
    }
	
	public static NtlmManager ntlmManager = null;
	
    @SuppressWarnings("rawtypes")
	public void init(Subject subject, Map sharedState, Map options) {
    	setForceCallbacksRead(true);
    	setSharedStateEnabled(true);
        setAuthLevel(Integer.parseInt(CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.authlevel","0")));
        if (logger.isDebugEnabled())
        	System.setProperty("jcifs.util.loglevel", "4");
        System.setProperty("jcifs.smb.client.connTimeout", "3000");
        System.setProperty("jcifs.smb.client.soTimeout", "1800000" );
        System.setProperty("jcifs.netbios.cachePolicy", "1200" );
//        System.setProperty("jcifs.smb.lmCompatibility", "3" );
//        System.setProperty("jcifs.smb.client.useExtendedSecurity", "true" );
        if (ntlmManager==null
        		||!StringUtils.equals(ntlmManager.getDomain(), CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.domain"))
        		||!StringUtils.equals(ntlmManager.getDomainController(), CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.domainController"))
        		||!StringUtils.equals(ntlmManager.getDomainControllerName(), CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.domainControllerHostName"))
        		||!StringUtils.equals(ntlmManager.getServiceAccount(), CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.serviceAccount"))
        		||!StringUtils.equals(ntlmManager.getServicePassword(), CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.servicePassword"))
        	)
        	synchronized (Ntlm.class) {
				ntlmManager = new NtlmManager(
						CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.domain"), 
						CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.domainController"),
						CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.domainControllerHostName"),
						CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.serviceAccount"),
						CollectionHelper.getMapAttr(options, "org.openidentityplatform.openam.authentication.modules.ntlm.servicePassword")
						);
			}
    }

    Principal userPrincipal=null;
    public Principal getPrincipal() {
        return userPrincipal;
    }
    public String schema = "NTLM"; 
    
    public int returnCallback(String message) {
    	try{
    		replaceCallback(1, 0, new HttpCallback("X-Authorization", "WWW-Authenticate", schema.concat((message==null)?"":" ".concat(message)), 401));
    	}catch(Exception e){
    		throw new RuntimeException(e);
    	}
    	return ISAuthConstants.LOGIN_START;
    }

    public static SecureRandom rnd=new SecureRandom();
    
	public int process(Callback[] callbacks, int state) throws AuthLoginException {
		HttpServletRequest request = getHttpServletRequest();
		
		
		if(callbacks.length == 1 
				&& callbacks[0] instanceof HttpCallback
				&& ((HttpCallback)callbacks[0]).getNegotiationCode() > 0) { //ignore auth when got default callback
			return ISAuthConstants.LOGIN_IGNORE;
		}
		
		if(request == null || 
				("true".equals(request.getParameter("skipKerberos")) //if not fallback from kerberos to NTLM, ignore
						&& request.getHeader("Authorization") == null)) {
			return ISAuthConstants.LOGIN_IGNORE;
		}
		

		final String msg = request.getHeader("Authorization");
		try {
			if (!StringUtils.isBlank(msg)){
				final HttpSession session=request.getSession(false);
				if (StringUtils.startsWith(msg, "NTLM ") 
						|| StringUtils.startsWith(msg,"Negotiate ")) { 
					final byte[] src = StringUtils.startsWith(msg, "NTLM ") ? Base64.decode(msg.substring(5)) : Base64.decode(msg.substring(10));
					schema = StringUtils.startsWith(msg, "NTLM ") ? "NTLM" : "Negotiate";
					if (src[8] == 1) {
						final Type1Message type1=new Type1Message(src);
						byte[] serverChallenge = new byte[8];
						rnd.nextBytes(serverChallenge);
						final Type2Message type2=ntlmManager.negotiateType2Message(src, serverChallenge); 
		                logger.info("type1=[{}] -> type2=[{}]",type1,type2);
		                session.setAttribute("challenge",serverChallenge);
		                return returnCallback(Base64.encode(type2.toByteArray()));
		            } else if (src[8] == 3 && session.getAttribute("challenge")!=null) {
		            	final Type3Message type3 = new Type3Message(src);
		    			try {
		    				NtlmUserAccount ntlmUserAccount = ntlmManager.authenticate(src, (byte[])session.getAttribute("challenge"));
		    				userPrincipal=new NtlmPrincipal(ntlmUserAccount.getUserName());
		    				setUserSessionProperty("UserTokenDomain", (StringUtils.isBlank(type3.getDomain())?ntlmManager.getDomain():type3.getDomain()).toUpperCase());
		    	            return ISAuthConstants.LOGIN_SUCCEED;
		    			} catch (Exception e) {
		    				logger.warn("type3={}: {}",type3,e.getMessage());
		    				return returnCallback(null);
		    				//throw new UserNamePasswordValidationException(MessageFormat.format("{0}: {1}", type3,e.getMessage()));
		    			} finally {
		    				session.removeAttribute("challenge");
		    			}
		            } else if (src[8] == 3 && session.getAttribute("challenge")==null){ 
		            	final Type3Message type3 = new Type3Message(src);
		            	logger.warn("type3={}: empty secret",type3);
		            	return returnCallback(null);
		            }
		            else
						throw new AuthLoginException(MessageFormat.format("Unsupported: type={0}", src[8]));
				}else
					throw new AuthLoginException(MessageFormat.format("Unsupported: schema={0}", msg));
			} 
			return returnCallback(null);
		} catch (Exception e) {
			if (logger.isDebugEnabled())
				logger.debug("error", e);
			throw (e instanceof AuthLoginException)? (AuthLoginException)e: new AuthLoginException("Ntlm", e);
		}

    }
}