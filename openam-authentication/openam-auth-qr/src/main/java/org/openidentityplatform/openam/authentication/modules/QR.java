package org.openidentityplatform.openam.authentication.modules;

import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.service.access.SessionQueryManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.AuthenticationSessionStore;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionType;
import com.iplanet.services.util.Crypt;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.spi.UserNamePasswordValidationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

public class QR extends AMLoginModule {
	
    private static Debug debug = Debug.getInstance("amAuthQR");
	
	public QR() {
		super();
	}

	public @SuppressWarnings("rawtypes") Map options;
	public @SuppressWarnings("rawtypes") Map sharedState;
	
	@Override
	@SuppressWarnings("rawtypes") 
	public void init(Subject subject, Map sharedState, Map options) {
    	this.options=options;
	    this.sharedState=sharedState;
	}
	
	Principal principal=null;
	@Override
	public int process(Callback[] in_callbacks, int state) throws LoginException {
		final LoginState ls=getLoginState(QR.class.getName());
		try {
			final Callback [] cb=ls.getSubmittedInfo();
			ls.setReceivedCallback_NoThread(null); //reset last received callback
			if (ls.getOldSession()==null) { //get or check QR
				final Map<String, Long> sessions = InjectorHolder.getInstance(SessionQueryManager.class).getAllSessionsByUUID("qr-"+getSessionId());
				if (sessions.size()==1) {
					final Session session=Session.getSession(new SessionID(sessions.keySet().iterator().next()));
					final String uid=session.getProperty("am.protected.qr.uid");
					if (uid!=null) {
						principal=new QRPrincipal(uid);
						return ISAuthConstants.LOGIN_SUCCEED; 
					}
				}
				getCallbackHandler().handle(getQR());
			}else { //submit QR
				if (cb==null || cb.length==0) {
					getCallbackHandler().handle(
						requestQR()
					);
				}else  {
					final String secret=new String(((PasswordCallback)cb[0]).getPassword());
					try {
						Session pre_session=Session.getSession(new SessionID(Crypt.decode(secret)));
						if (pre_session!=null) {
							pre_session.setProperty("am.protected.qr.uid", ls.getOldSession().getProperty("sun.am.UniversalIdentifier"));
							ls.setReceivedCallback_NoThread(null); //reset last received callback
							getCallbackHandler().handle(
									new Callback[]{
											new PagePropertiesCallback("QR", "QR code correct", null, 1*60, "Login.jsp", false, null)
											,new TextOutputCallback(TextOutputCallback.INFORMATION,"OK")
										}
							);
						}
					}catch (SessionException e) {}
					throw new UserNamePasswordValidationException("Invalid token");
				}
			}
        }catch (Exception e) {
	        debug.warning("{}: {}",(e instanceof AuthLoginException)?((AuthLoginException)e).getMessage():"error",(e instanceof AuthLoginException) ? e.toString():e);
	        throw (e instanceof AuthLoginException)?(AuthLoginException)e:new UserNamePasswordValidationException(e);
       	}
		return ISAuthConstants.LOGIN_IGNORE; 
	}

	protected Callback[] requestQR() {
		return new Callback[]{
				new PagePropertiesCallback("QR", "Please enter secret from QR code", null, 1*60, "Login.jsp", false, null)
				,new PasswordCallback("Secret from QR", false)
			};
	}


	@Override
	public Principal getPrincipal() {
		return principal;
	}
	
	
	Callback[] qr=null;
	protected Callback[] getQR() {
		if (qr!=null)
			return qr;
		try {
			final String secret=Crypt.encode(makeSecret());
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(new MultiFormatWriter().encode(secret, BarcodeFormat.QR_CODE, 300, 300), "PNG", out);
			qr=new Callback[]{
					new PagePropertiesCallback("QR", "Please scan QR code", null, 1*60, "Login.jsp", false, null)
					,new TextOutputCallback(TextOutputCallback.INFORMATION, Base64.getEncoder().encodeToString(out.toByteArray()))
					,new TextOutputCallback(TextOutputCallback.INFORMATION,secret)
				};
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
		return qr;
	}
	
	String makeSecret() throws AuthLoginException {
		//make session
		final LoginState ls=getLoginState(QR.class.getName());
		final String uid="qr-"+getSessionId();
		final InternalSession is=InjectorHolder.getInstance(SessionService.class).newInternalSession(ls.getOrgDN(), false);
		is.setClientID(uid);
		is.setClientDomain(ls.getOrgDN());
		is.putProperty("sun.am.UniversalIdentifier",uid);
		is.setMaxCachingTime(1);
		is.setMaxIdleTime(2);
		is.setMaxSessionTime(20);  
		is.setType(SessionType.USER);
		is.activate(uid);
		InjectorHolder.getInstance(AuthenticationSessionStore.class).promoteSession(is.getID());
		return is.getSessionID().toString();
	}
}
