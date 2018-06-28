package org.forgerock.openam.authentication.modules.oauth2.service;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.BUNDLE_NAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_CLIENT_ID;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_CLIENT_SECRET;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_CODE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_GRANT_TYPE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_REDIRECT_URI;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_SCOPE;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.openam.authentication.modules.oauth2.OAuthConf;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.oauth2.OAuth2Constants;

import com.sun.identity.authentication.spi.AuthLoginException;

public class DefaultServiceUrlProvider implements ServiceUrlProvider {

	@Override
	public String getServiceUri(OAuthConf config, String originalUrl, String state) throws AuthLoginException {
		try {
            StringBuilder sb = new StringBuilder(config.getAuthServiceUrl());
            addParam(sb, PARAM_CLIENT_ID, config.getClientId());
            addParam(sb, PARAM_SCOPE, OAuthUtil.oAuthEncode(config.getScope()));
            addParam(sb, PARAM_REDIRECT_URI, OAuthUtil.oAuthEncode(originalUrl));
            addParam(sb, "response_type", "code");
            addParam(sb, "state", state);
            return sb.toString();
        } catch (UnsupportedEncodingException ex) {
            OAuthUtil.debugError("OAuthConf.getAuthServiceUrl: problems while encoding "
                    + "the scope", ex);
            throw new AuthLoginException("Problem to build the Auth Service URL", ex);
        }
	}
	
    public Map<String, String> getTokenServicePOSTparameters(OAuthConf config, String code, String authServiceURL)
            throws AuthLoginException {

        Map<String, String> postParameters = new HashMap<String, String>();
        if (code == null) {
            OAuthUtil.debugError("process: code == null");
            throw new AuthLoginException("amAuthOAuth", "authCode == null", null);
        }
        OAuthUtil.debugMessage("authentication code: " + code);

        try {
            postParameters.put(PARAM_CLIENT_ID, config.getClientId());
            postParameters.put(PARAM_REDIRECT_URI, OAuthUtil.oAuthEncode(authServiceURL));
            postParameters.put(PARAM_CLIENT_SECRET, config.getClientSecret());
            postParameters.put(PARAM_CODE, OAuthUtil.oAuthEncode(code));
            postParameters.put(PARAM_GRANT_TYPE, OAuth2Constants.TokenEndpoint.AUTHORIZATION_CODE);

        } catch (UnsupportedEncodingException ex) {
            OAuthUtil.debugError("OAuthConf.getTokenServiceUrl: problems while encoding "
                    + "and building the Token Service URL", ex);
            throw new AuthLoginException("Problem to build the Token Service URL", ex);
        }
        return postParameters;
    }
    
	@Override
	public Map<String, String> getTokenServiceGETparameters(OAuthConf oAuthConf, String code, String authServiceURL)
			throws AuthLoginException {
		return null;
	}
	
    private void addParam(StringBuilder url, String key, String value) {
        url.append(url.toString().contains("?") ? "&" : "?")
                .append(key).append("=").append(value);
    }



}
