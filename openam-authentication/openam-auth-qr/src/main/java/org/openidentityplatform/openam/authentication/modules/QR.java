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

import org.apache.commons.lang.StringUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.iplanet.services.util.Crypt;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
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
	
	@Override
	public int process(Callback[] in_callbacks, int state) throws LoginException {
		LoginState ls=getLoginState(QR.class.getName());
		try {
			final Callback [] cb=ls.getSubmittedInfo();
			ls.setReceivedCallback_NoThread(null); //reset last received callback
			if (ls.getOldSession()==null) { //get or check QR
				getCallbackHandler().handle(getQR());
			}else { //accept QR
				if (cb==null) {
					getCallbackHandler().handle(
							new Callback[]{
									new PagePropertiesCallback("QR", "Please enter secret from QR code", null, 1*60, "Login.jsp", false, null)
									,new PasswordCallback("Secret from QR", false)
								}
					);
				}else {
					final String secret=new String(((PasswordCallback)cb[0]).getPassword());
					throw new InvalidPasswordException("Invalid token", secret);
				}
			}
        }catch (Exception e) {
	        debug.warning("{}: {}",(e instanceof AuthLoginException)?((AuthLoginException)e).getMessage():"error",(e instanceof AuthLoginException) ? e.toString():e);
	        throw (e instanceof AuthLoginException)?(AuthLoginException)e:new UserNamePasswordValidationException(e);
       	}
		return ISAuthConstants.LOGIN_IGNORE; 
	}


	@Override
	public Principal getPrincipal() {
		return null;
	}
	
	Callback[] qr=null;
	Callback[] getQR() {
		if (qr!=null)
			return qr;
		try {
			final String secret=Crypt.encode(getSessionId());
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
}
