package org.forgerock.openam.authentication.modules.oauth2;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.BUNDLE_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.IOUtils;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.shared.debug.Debug;

public class HttpRequestContent {
	
	private static Debug DEBUG = Debug.getInstance("amAuthOAuth2");

	private static final HttpRequestContent INSTANCE = new HttpRequestContent();
	
	public static HttpRequestContent getInstance() {
		return INSTANCE;
	}
	
	public String getContentUsingPOST(String serviceUrl, String authorizationHeader, Map<String, String> getParameters,
            Map<String, String> postParameters) throws LoginException {
        return getContent(serviceUrl, authorizationHeader, getParameters, postParameters, "POST");
    }

	public String getContentUsingGET(String serviceUrl, String authorizationHeader, Map<String, String> getParameters)
            throws LoginException {
        return getContent(serviceUrl, authorizationHeader, getParameters, null, "GET");

    }
	
	private String getContent(String serviceUrl, String authorizationHeader, Map<String, String> getParameters,
            Map<String, String> postParameters, String httpMethod) throws LoginException {

        InputStream inputStream;
        if ("GET".equals(httpMethod)) {
            inputStream = getContentStreamByGET(serviceUrl, authorizationHeader, getParameters);
        } else if ("POST".equals(httpMethod)) {
            inputStream = getContentStreamByPOST(serviceUrl, authorizationHeader, getParameters, postParameters);
        } else {
            throw new IllegalArgumentException("httpMethod='" + httpMethod + "' is not valid. Expecting POST or GET");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder buf = new StringBuilder();
        try {
            String str;
            while ((str = in.readLine()) != null) {
                buf.append(str);
            }
        } catch (IOException ioe) {
            OAuthUtil.debugError("OAuth.getContent: IOException: " + ioe.getMessage());
            throw new AuthLoginException(BUNDLE_NAME, "ioe", null, ioe);
        } finally {
            IOUtils.closeIfNotNull(in);
        }
        return buf.toString();
    }
	
	public InputStream getContentStreamByGET(String serviceUrl, String authorizationHeader,
            Map<String, String> getParameters) throws LoginException {

        OAuthUtil.debugMessage("service url: " + serviceUrl);
        OAuthUtil.debugMessage("GET parameters: " + getParameters);
        try {
            InputStream is;
            if (!CollectionUtils.isEmpty(getParameters)) {
                if (!serviceUrl.contains("?")) {
                    serviceUrl += "?";
                } else {
                    serviceUrl += "&";
                }
                serviceUrl += getDataString(getParameters);
            }
            URL urlC = new URL(serviceUrl);

            HttpURLConnection connection = HttpURLConnectionManager.getConnection(urlC);
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            if (authorizationHeader != null) {
                connection.setRequestProperty("Authorization", authorizationHeader);
            }
            connection.setRequestProperty("Accept", "application/json"); //deal with MS https://graph.microsoft.com/v1.0/me issue, this header required
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                OAuthUtil.debugMessage("OAuth.getContentStreamByGET: HTTP Conn OK");
                is = connection.getInputStream();
            } else {
                // Server returned HTTP error code.
                String errorStream = getErrorStream(connection);
                if (OAuthUtil.debugMessageEnabled()) {
                  OAuthUtil.debugMessage("OAuth.getContentStreamByGET: HTTP Conn Error:\n" +
                        " Response code: " + connection.getResponseCode() + "\n " +
                        " Response message: " + connection.getResponseMessage() + "\n" +
                        " Error stream: " + errorStream + "\n");
                }
                is = getContentStreamByPOST(serviceUrl, authorizationHeader, getParameters, Collections
                .<String, String>emptyMap());
            }

            return is;

        } catch (MalformedURLException mfe) {
            throw new AuthLoginException(BUNDLE_NAME,"malformedURL", null, mfe);
        } catch (IOException ioe) {
            DEBUG.warning("OAuth.getContentStreamByGET URL={} caught IOException", serviceUrl, ioe);
            throw new AuthLoginException(BUNDLE_NAME,"ioe", null, ioe);
        }
    }
	
	 public InputStream getContentStreamByPOST(String serviceUrl, String authorizationHeader,
	            Map<String, String> getParameters, Map<String, String> postParameters) throws LoginException {

	        InputStream is = null;

	        try {
	            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: URL = " + serviceUrl);
	            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: GET parameters = " + getParameters);
	            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: POST parameters = " + postParameters);

	            if (!CollectionUtils.isEmpty(getParameters)) {
	                if (!serviceUrl.contains("?")) {
	                    serviceUrl += "?";
	                } else {
	                    serviceUrl += "&";
	                }
	                serviceUrl += getDataString(getParameters);
	            }
	            URL url = new URL(serviceUrl);
	            String query = url.getQuery();
	            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: Query: " + query);

	            HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
	            connection.setDoOutput(true);
	            connection.setRequestMethod("POST");
	            if (authorizationHeader != null) {
	                connection.setRequestProperty("Authorization", authorizationHeader);
	            }
	            if (postParameters != null && !postParameters.isEmpty()) {
	                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
	                writer.write(getDataString(postParameters));
	                writer.close();
	            }
	            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
	                OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: HTTP Conn OK");
	                is = connection.getInputStream();
	            } else { // Error Code
	                String data2[] = {String.valueOf(connection.getResponseCode())};
	                OAuthUtil.debugError("OAuth.getContentStreamByPOST: HTTP Conn Error:\n" +
	                        " Response code: " + connection.getResponseCode() + "\n" +
	                        " Response message: " + connection.getResponseMessage() + "\n" +
	                        " Error stream: " + getErrorStream(connection) + "\n");
	                throw new AuthLoginException(BUNDLE_NAME, "httpErrorCode", data2);
	            }
	        } catch (MalformedURLException e) {
	            throw new AuthLoginException(BUNDLE_NAME,"malformedURL", null, e);
	        } catch (IOException e) {
	            DEBUG.warning("OAuth.getContentStreamByPOST URL={} caught IOException", serviceUrl, e);
	            throw new AuthLoginException(BUNDLE_NAME,"ioe", null, e);
	        }

	        return is;
	    }

	  
	
    private String getErrorStream(HttpURLConnection connection) {
        InputStream errStream = connection.getErrorStream();
        if (errStream == null) {
            return "Empty error stream";
        } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(errStream));
            StringBuilder buf = new StringBuilder();
            try {
                String str;
                while ((str = in.readLine()) != null) {
                    buf.append(str);
                }
            }
            catch (IOException ioe) {
                OAuthUtil.debugError("OAuth.getErrorStream: IOException: " + ioe.getMessage());
            } finally {
                IOUtils.closeIfNotNull(in);
            }
            return buf.toString();
        }
    }
    
    public Map<String, List<String>> getHeadersUsingHEAD(String serviceUrl) {
    	Map<String, List<String>> headers = new HashMap<>();
    	try {
	    	URL url = new URL(serviceUrl);
	    	final HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
	        connection.setDoOutput(true);
	        connection.setRequestMethod("HEAD");
	        connection.getResponseCode();
	        headers = connection.getHeaderFields();
    	} catch (Exception e) {
    		DEBUG.warning("OAuth.getHeadersUsingHEAD URL={} caught IOException", serviceUrl, e);
    	}
		return headers;
    }
    
    private String getDataString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {

            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            // We don't need to encode the key/value as they are already encoded
            result.append(entry.getKey());
            result.append("=");
            result.append(entry.getValue());
        }

        return result.toString();
    }
		
	
}
