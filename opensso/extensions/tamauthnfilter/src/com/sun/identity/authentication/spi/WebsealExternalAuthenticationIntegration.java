package com.sun.identity.authentication.spi;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;

public class WebsealExternalAuthenticationIntegration implements
		AMPostAuthProcessInterface {

    protected static Debug debug =
            Debug.getInstance(
                WebsealExternalAuthenticationIntegration.class.
                    getCanonicalName()
            );
    private static final String eaiMapping = "be.is4u.eai.mapping";

	public void onLoginFailure(Map requestParamsMap,
			HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		// TODO Auto-generated method stub

	}

	public void onLoginSuccess(Map requestParamsMap,
			HttpServletRequest request, HttpServletResponse response,
			SSOToken ssoToken) throws AuthenticationException
	{
        AMIdentity amIdentityUser;
        String headerkey = null;
        String headervalue = null;
        String name = null;
        debug.message("Entering method: onLoginSuccess");
        /*
         * mapdefinition should have a value like
         *          am-eai-user-id;cn=%name%,ou=users,o=is4u,c=be
         */
        String mapdefinition =
                SystemProperties.get(
                    WebsealExternalAuthenticationIntegration.eaiMapping
                );
        debug.message("Got system property " +
                    WebsealExternalAuthenticationIntegration.eaiMapping +
                    " value: " + mapdefinition
                );
        if( mapdefinition != null && mapdefinition.split(";").length == 2 )
        {
            headerkey = mapdefinition.split(";")[0];
            debug.message("HTTP Header name to be set: " + headerkey);
            headervalue = mapdefinition.split(";")[1];
            debug.message("HTTP Header value definition: " + headervalue);
            if( headerkey != null && headervalue != null )
            {
                try {
                    amIdentityUser = IdUtils.getIdentity(ssoToken);
                    name = amIdentityUser.getName();
                    debug.message("HTTP header value: " + name);
                    response.setHeader(headerkey,
                                headervalue.replaceFirst("%name%", name));
                    debug.message("HTTP header value set");

                } catch (IdRepoException ex) {
                    debug.message("Caught exception", ex);
                } catch (SSOException ex) {
                    debug.message("Caught exception", ex);
                }
            }
        }
        debug.message("Exiting method: onLoginSuccess");
	}

	public void onLogout(HttpServletRequest request,
			HttpServletResponse response, SSOToken ssoToken)
			throws AuthenticationException {
		// TODO Auto-generated method stub

	}

}
