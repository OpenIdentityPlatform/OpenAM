/* The contents of this file are subject to the terms
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
 * $Id: VALid.java,v 1.1 2009/04/21 10:23:28 ja17658 Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.authentication.modules.valid;

// import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.Debug;
// import com.sun.identity.shared.datastruct.CollectionHelper;
import com.iplanet.am.util.Misc;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.spi.AMAuthCallBackException;
// import com.sun.identity.shared.Constants;
// import com.sun.identity.idm.AMIdentityRepository;
// import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.ServiceConfig;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.io.IOException;
import java.security.Principal;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.MalformedURLException;

import com.validsoft.products.valid.validsupport.*;
import com.validsoft.utils.sockets.exceptions.*;

public class VALid extends AMLoginModule {
    // local variables
    ResourceBundle bundle = null;
    protected String validatedUserID;
    private String userName;
    private String userPassword;
    private ServiceConfig sc;
    private int currentState;
    private String currentConfigName;

    private static String AUTHLEVEL = "iplanet-am-auth-valid-auth-level";
    private static String VALID_HOSTS = "iplanet-am-auth-valid-auth-hosts";
    private static String VALID_USESSL = "iplanet-am-auth-valid-auth-usessl";
    private static String VALID_APPNAME = "iplanet-am-auth-valid-auth-appname";
    private static String VALID_METHOD = "iplanet-am-auth-valid-auth-method";
    
    private static String validHosts;
    private boolean validUseSSL;
    private static String validApp;
    private static String validMethod;

    private ValidClient validclient;
    private final int ENTER_PIN = 2;

    private Map sharedState;
    public Map currentConfig;
    
    protected Debug debug = null;
    protected String amAuthVALid;
    protected Principal userPrincipal;

    public VALid() {
        amAuthVALid = "VALid";
        debug = Debug.getInstance(amAuthVALid);
    }
    
    public void init(Subject subject, Map sharedState, Map options) {
        sc = (ServiceConfig) options.get("ServiceConfig");
        currentConfig = options;
        currentConfigName = 
            (String)options.get(ISAuthConstants.MODULE_INSTANCE_NAME);
        String authLevel = CollectionHelper.getServerMapAttr(currentConfig, AUTHLEVEL);
	debug.message("VALid: Getting auth level ...");
        // String authLevel = Misc.getServerMapAttr(options, AUTHLEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                debug.error("Unable to set auth level " + authLevel,e);
            }
        }
/** BEGIN: VALid Specific Code **/
	debug.message("VALid: auth level="+authLevel);
	debug.message("VALid: Initializing ...");
	validHosts = CollectionHelper.getServerMapAttr(currentConfig, VALID_HOSTS);
	debug.message("VALid: Service Hosts="+validHosts);
	// validUseSSL = Boolean.valueOf(CollectionHelper.getServerMapAttr(currentConfig, VALID_USESSL)).booleanValue();
	validUseSSL = false;
	debug.message("VALid: Use SSL="+validUseSSL);
	validApp = CollectionHelper.getServerMapAttr(currentConfig, VALID_APPNAME);
	debug.message("VALid: ApplicationName="+validApp);
	validMethod = CollectionHelper.getServerMapAttr(currentConfig, VALID_METHOD);
	debug.message("VALid: Method="+validMethod);

	// (1) Create the ValidClient Object
	debug.message("VALid: Getting ValidClient ...");
        validclient = ValidClientFactory.getInstance().createValidClient();
        if (validclient != null) {
            try {
                // (2) Connect to the VALid server
		debug.message("VALid: Connecting to application "+validApp+"...");
                validclient.connectToServer(validApp);

                // ValidationResponse validateResponse = client.validateUser(username);
                // ValidationResponse validateResponse = client.validateUser("joachim");

                // 3.) If user validated, get methods
                // List methods = client.getMethods();
            }
            catch (NoAvailableServersException ex) {
                debug.error("VALid: VALid server not available.");
                // ex.printStackTrace();
            }
            catch (IOException ex) {
                debug.error("VALid: Exception when connecting to VALid server");
              	//  ex.printStackTrace();
            }
	}
	debug.message("VALid: Initializing done.");
	
/** END: VALid Specific Code **/
 
        java.util.Locale locale = getLoginLocale();
        bundle = amCache.getResBundle(amAuthVALid, locale);
        if (debug.messageEnabled()) {
            debug.message("VALid resbundle locale=" + locale);
        }
        this.sharedState = sharedState;
    }
 
    public int process(Callback[] callbacks, int state)
            throws AuthLoginException {
        currentState = state;
        int retVal = 0;
        Callback[] idCallbacks = new Callback[2];
	
	debug.message("VALid: process called with state = "+currentState);
        if (currentState == ISAuthConstants.LOGIN_START) {
                if (callbacks !=null && callbacks.length == 0) {
                    userName = (String) sharedState.get(getUserKey());
                    userPassword = (String) sharedState.get(getPwdKey());
                    if (userName == null || userPassword == null) {
                        return ISAuthConstants.LOGIN_START;
                    }
                    NameCallback nameCallback = new NameCallback("dummy");
                    nameCallback.setName(userName);
                    idCallbacks[0] = nameCallback;
                    PasswordCallback passwordCallback = new PasswordCallback
                        ("dummy",false);
                    passwordCallback.setPassword(userPassword.toCharArray());
                    idCallbacks[1] = passwordCallback;
                } else {
                    idCallbacks = callbacks;
                    //callbacks is not null
                    userName = ( (NameCallback) callbacks[0]).getName();
                    // userPassword = String.valueOf(((PasswordCallback)
                    //    callbacks[1]).getPassword());
                }
                //store username password both in success and failure case
                storeUsernamePasswd(userName, userPassword);
                
		debug.message("VALid: Login Attempt User = "+userName);

/** BEGIN: VALid Specific Code **/

		// int authResult = authenticate(authURLUser);
		int validateResult = -1;
		// (3) Validate User
                try {
		    debug.message("VALid: Validating user "+userName);
                    ValidationResponse vr = validclient.validateUser(userName);

                    switch (vr.getType()) {
                	case USER_OK: // all good
		            validateResult = 0;
                            break;
                        // There's some specific action on specific
                        // errors that should be followed here...
                        default:
			    validateResult = -1;
                        }

                } catch (IOException ioe) {
                        debug.error("VALid: Problem with validateUser with the VALid Auth Server");
                } catch (Exception ioe) {
                        debug.error("VALid: Exception thrown from the VALid client on validateUser");
                }
		
		if (validateResult == 0) {
			// retVal=ISAuthConstants.LOGIN_SUCCEED;
			currentState = ENTER_PIN;
			retVal = currentState;
			validatedUserID = userName;
			int selectedMethodId = -1;
			debug.message("VALid: User validation successful for user "+userName);

			// (4) Identify the method Id for the authentication request
			//     Loop through the methods and find the the one from the config
                        try {
                            List methods = validclient.getMethods();

                            if (methods.size() > 0) {
                                Iterator iter = methods.iterator();
                                while (iter.hasNext()) {
                                    ContactMethod cm = (ContactMethod) iter.next();
				    if (cm.getName().equalsIgnoreCase(validMethod)) {
					selectedMethodId = Integer.parseInt(cm.getId());
				    }
                                }
                            } else {
                                debug.error("VALid: The are no enabled contact methods for this user");
			    }
                        } catch (IOException ioe) {
                            debug.error("VALid: Problem with getMethods with the VALid Auth Server");
                	} catch (Exception ioe) {
                            debug.error("VALid: Exception thrown from the VALid client on getMethods");
                	}

			if (selectedMethodId < 0) {
                            debug.error("VALid: Method "+validMethod+" not available for user "+userName);
			} else {
                            debug.message("VALid: Method "+validMethod+" for user "+userName+" has Id="+selectedMethodId);
			}

			// (5) Issue the authentication request
			if (selectedMethodId > 0) {
                            try {
                                debug.message("VALid: Requesting authentication with method "+validMethod+" for user "+userName);
			    	validclient.requestAuthorisation(selectedMethodId);
                            } catch (IOException ioe) {
                            	debug.error("VALid: Problem with requestAuthorisation with the VALid Auth Server");
                	    } catch (Exception ioe) {
                            	debug.error("VALid: Exception thrown from the VALid client on requestAuthorisation");
                	    }
			} else {
			    setFailureID(userName);
			    debug.error("VALid: Could not find suitabel method for user "+userName);
			    throw new AuthLoginException("VALid Authentication failed: Invalid Method");
			}
		} else {
			setFailureID(userName);
			debug.error("VALid: User validation failed for user "+userName);
			// throw new AuthLoginException(amAuthVALid, "FAuth", null);
			// debug.message("VALid: User Authentication URL: "+authURLUser.toString());
			throw new AuthLoginException("VALid Authentication failed: Invalid user");
		}
            }  else if (currentState == ENTER_PIN){
                if (callbacks != null && callbacks.length > 0) {
                    idCallbacks = callbacks;
                    //callbacks is not null
                    userPassword = String.valueOf(((PasswordCallback) callbacks[0]).getPassword());
		    int authResult = -1;
		    boolean repeat = false;
		    try {
		        AuthorisationResponse vr = validclient.validateResponseCode(userPassword);
			switch (vr.getResult()) {
                            case OK:
				//all good
				authResult = 0;
                                break;
                            case INCORRECT: //incorrect OTP
				authResult = -1;
                                repeat = true;
                                break;

                            // There's some specific action on specific errors that should be followed here...
                            default:
                               debug.error("VALid: validateResponseCode FAILED -- "+ vr.getResult());
                        }
                    } catch (IOException ioe) {
                        debug.error("VALid: Problem with validateResponseCode with the VALid Auth Server");
                    } catch (Exception ioe) {
                        debug.error("VALid: Exception thrown from the VALid client on validateResponseCode");
               	    }	
		    if (authResult == 0) {
			retVal=ISAuthConstants.LOGIN_SUCCEED;
			currentState = retVal;
			debug.message("VALid: PIN validation successful for user "+userName);
		    } else if (repeat) {
			retVal=ENTER_PIN;
			currentState = retVal;
			debug.message("VALid: PIN validation failed. Please re-enter PIN");
		    }

		} 

/** END: VALid Specific Code **/

            }  else {
                setFailureID(userName);
                // throw new AuthLoginException(amAuthVALid, "FAuth",
		throw new AuthLoginException("Authentication failed: Internal Error");
            }

        return retVal;
        
    }

    private URL constructAuthURL(String strUserName, String strUserPassword) {
	URL url = null;
	//try {
	 // url = new URL(authURL+"?"+authURLParameterUser+"="+strUserName+"&"+authURLParameterPassword+"="+strUserPassword);
	//}
	//catch (MalformedURLException e) {
	//  debug.error("VALid: MalformedURLException\n"+e.getMessage());		}
	return url;
    }

    private int authenticate(URL url) {
	String line = null;
	String strAuthResult = null;
	Boolean authResultFound = false;
	int authResult = -9999;

	if (url != null) {
	   try {
		InputStream inputstream = (InputStream)  url.getContent();
		InputStreamReader inputstreamreader = new InputStreamReader(inputstream);

		BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
		line = bufferedreader.readLine();
		while ((line != null) && (!authResultFound)) {
			debug.message("VALid: Line = "+line);
			if ((line != null) && (line.contains("<identification>")))
			{
				// This line contains the result
				int posCloseBracket = line.indexOf('>');
				int posOpenBracket = line.indexOf('<', posCloseBracket+1);
			debug.message("VALid: posCloseBracket = "+posCloseBracket+", posOpenBracket = "+posOpenBracket);
				strAuthResult = line.substring(posCloseBracket+1, posOpenBracket);
			debug.message("VALid: strAuthResult = "+strAuthResult);
				authResult = Integer.parseInt(strAuthResult);
			debug.message("VALid: intAuthResult = "+authResult);
				authResultFound = true;
			}
			line = bufferedreader.readLine();
		}
	    } // try
	    catch (Exception e) {
		debug.error("VALid: Exception when trying to authenticate: "+e.getMessage());
	    }
	}
	debug.error("VALid: Authentication result = "+authResult);
	return (authResult);
    }
    
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        }
        else if (validatedUserID != null) {
            userPrincipal = new VALidPrincipal(validatedUserID);
            return userPrincipal;
        } else {
            return null;
        }
    }
    
    // cleanup state fields
    public void destroyModuleState() {
        validatedUserID = null;
        userPrincipal = null;
    }
    
    public void nullifyUsedVars() {
        bundle = null;
        userName = null ;
        userPassword = null;
        sc = null;
        
        sharedState = null;
        currentConfig = null;
        
        amAuthVALid = null;
    }

    public static void main(String args[]) {
        ValidClient client = ValidClientFactory.getInstance().createValidClient();
        if (client != null) {
            try {
		// (1) Connect to the VALid server
                // client.connectToServer(appName);
                client.connectToServer("OWA");

                // ValidationResponse validateResponse = client.validateUser(username);
                // ValidationResponse validateResponse = client.validateUser("joachim");

                // 3.) If user validated, get methods
                // List methods = client.getMethods();
            }
            catch (NoAvailableServersException ex) {
                // debug.error("VALid: VALid server not available.");
                ex.printStackTrace();
            }
            catch (IOException ex) {
                // debug.error("VALid: Exception when authenticating user again VALid");
                ex.printStackTrace();
            }
        } else {
            // debug.error("VALid: ValidClient object is null. Cannt authenticate");
        }
	
    }
    
}
