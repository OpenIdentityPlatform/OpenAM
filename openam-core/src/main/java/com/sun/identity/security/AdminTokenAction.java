package com.sun.identity.security;

import com.iplanet.am.util.AdminUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.debug.Debug;
import java.security.PrivilegedAction;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.util.thread.listener.ShutdownListener;

public class AdminTokenAction
  implements PrivilegedAction<SSOToken>
{
  public static final String AMADMIN_MODE = "com.sun.identity.security.amadmin";
  public static final String VALIDATE_SESSION = "openam.identity.security.validateSession";
  static final Debug debug = Debug.getInstance("amSecurity");
  private static final String ADMIN_TOKEN_PROVIDER = "com.sun.identity.security.AdminToken";
  private static final String APP_USERNAME = "com.sun.identity.agents.app.username";
  private static final String APP_SECRET = "com.iplanet.am.service.secret";
  private static final String APP_PASSWORD = "com.iplanet.am.service.password";
  private static volatile AdminTokenAction instance;
  private SSOTokenManager tokenManager;
  private SSOToken appSSOToken;
  private SSOToken internalAppSSOToken;
  private boolean authInitialized;
  private boolean validateSession;
  
  public static AdminTokenAction getInstance()
  {
    if (instance == null) {
      synchronized (AdminTokenAction.class)
      {
        if (instance == null) {
          try
          {
            try
            {
              Class.forName("org.forgerock.guice.core.InjectorHolder");
              instance = (AdminTokenAction)InjectorHolder.getInstance(AdminTokenAction.class);
            }
            catch (ClassNotFoundException e)
            {
              instance = new AdminTokenAction();
            }
          }
          catch (Exception e)
          {
            debug.error("AdminTokenAction::init Unable to get SSOTokenManager", e);
          }
        }
      }
    }
    return instance;
  }
  
  protected AdminTokenAction()
    throws SSOException
  {
    init();
  }
  
  protected void init()
    throws SSOException
  {
    this.tokenManager = SSOTokenManager.getInstance();
    ShutdownManager.getInstance().addApplicationSSOTokenDestroyer(new ShutdownListener()
    {
      public void shutdown()
      {
        AdminTokenAction.this.resetInstance();
      }
    });
    this.validateSession = SystemProperties.getAsBoolean("openam.identity.security.validateSession");
  }
  
  public void authenticationInitialized()
  {
    this.authInitialized = true;
    
    this.appSSOToken = getSSOToken();
    if (debug.messageEnabled()) {
      debug.message("AdminTokenAction:authenticationInit called. AppSSOToken className=" + (this.appSSOToken == null ? "null" : this.appSSOToken.getClass().getName()));
    }
    this.internalAppSSOToken = null;
  }
  
  public static void invalid()
  {
    getInstance().invalidate();
    if (debug.messageEnabled()) {
      debug.message("AdminTokenAction:invalid called");
    }
  }
  
  private void invalidate()
  {
    this.appSSOToken = null;
  }
  
  private void resetInstance()
  {
    if (this.appSSOToken != null) {
      this.appSSOToken = null;
    }
    this.internalAppSSOToken = null;
  }
  
  public SSOToken run()
  {
    if ((this.appSSOToken != null) && (this.tokenManager.isValidToken(this.appSSOToken))) {
      try
      {
        if (this.validateSession) {
          this.tokenManager.refreshSession(this.appSSOToken);
        }
        if (this.tokenManager.isValidToken(this.appSSOToken)) {
          return this.appSSOToken;
        }
      }
      catch (SSOException ssoe)
      {
        debug.error("AdminTokenAction.reset: couldn't retrieve valid token.", ssoe);
      }
    }
    if ((this.internalAppSSOToken != null) && (this.tokenManager.isValidToken(this.internalAppSSOToken))) {
      return this.internalAppSSOToken;
    }
    SSOToken answer = getSSOToken();
    if (answer != null)
    {
      if ((!SystemProperties.isServerMode()) || (this.authInitialized)) {
        this.appSSOToken = answer;
      }
      return answer;
    }
    if (debug.messageEnabled()) {
      debug.message("AdminTokenAction::run Unable to get SSOToken from serverconfig.xml");
    }
    String appTokenProviderName = SystemProperties.get("com.sun.identity.security.AdminToken");
    if (appTokenProviderName != null)
    {
      try
      {
        AppSSOTokenProvider appSSOTokenProvider = (AppSSOTokenProvider)Class.forName(appTokenProviderName).asSubclass(AppSSOTokenProvider.class).newInstance();
        
        answer = appSSOTokenProvider.getAppSSOToken();
      }
      catch (Throwable ce)
      {
        debug.error("AdminTokenAction: Exception while calling appSSOToken provider plugin.", ce);
      }
    }
    else
    {
      String appUserName = SystemProperties.get("com.sun.identity.agents.app.username");
      String encryptedPassword = SystemProperties.get("com.iplanet.am.service.secret");
      String password = SystemProperties.get("com.iplanet.am.service.password");
      String appPassword = null;
      if ((password != null) && (!password.isEmpty())) {
        appPassword = password;
      } else if ((encryptedPassword != null) && (!encryptedPassword.isEmpty())) {
        try
        {
          appPassword = Crypt.decode(encryptedPassword);
        }
        catch (Throwable t)
        {
          debug.error("AdminTokenAction::run Unable to decrypt secret password", t);
        }
      }
      if ((appUserName == null) || (appUserName.isEmpty()) || (appPassword == null) || (appPassword.isEmpty()))
      {
        debug.error("AdminTokenAction: App user name or password is empty");
      }
      else
      {
        if (debug.messageEnabled()) {
          debug.message("App user name: " + appUserName);
        }
        SystemAppTokenProvider tokenProd = new SystemAppTokenProvider(appUserName, appPassword);
        
        answer = tokenProd.getAppSSOToken();
      }
    }
    if (answer == null)
    {
      String errorMessage = "AdminTokenAction: FATAL ERROR: Cannot obtain Application SSO token.";
      debug.error("AdminTokenAction: FATAL ERROR: Cannot obtain Application SSO token.");
      throw new AMSecurityPropertiesException("AdminTokenAction: FATAL ERROR: Cannot obtain Application SSO token.");
    }
    if ((!SystemProperties.isServerMode()) || (this.authInitialized)) {
      this.appSSOToken = answer;
    }
    return answer;
  }
  
  private SSOToken getSSOToken()
  {
    SSOToken ssoAuthToken = null;
    try
    {
      if (AdminUtils.getAdminPassword() != null)
      {
        String adminDN = AdminUtils.getAdminDN();
        String adminPassword = new String(AdminUtils.getAdminPassword());
        if ((!this.authInitialized) && ((SystemProperties.isServerMode()) || (SystemProperties.get("com.sun.identity.security.amadmin") != null)))
        {
          AuthContext ac = new AuthContext(new AuthPrincipal(adminDN), adminPassword.toCharArray());
          
          this.internalAppSSOToken = (ssoAuthToken = ac.getSSOToken());
        }
        else
        {
          boolean authInit = this.authInitialized;
          if (authInit) {
            this.authInitialized = false;
          }
          ssoAuthToken = new SystemAppTokenProvider(adminDN, adminPassword).getAppSSOToken();
          if ((authInit) && (ssoAuthToken != null)) {
            this.authInitialized = true;
          }
        }
      }
    }
    catch (NoClassDefFoundError ne)
    {
      debug.error("AdminTokenAction::getSSOToken Not found AdminDN and AdminPassword.", ne);
    }
    catch (Throwable t)
    {
      debug.error("AdminTokenAction::getSSOToken Exception reading from serverconfig.xml", t);
    }
    return ssoAuthToken;
  }
}
