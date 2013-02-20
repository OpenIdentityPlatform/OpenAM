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
 * $Id: SessionProperty.java,v 1.3 2009/01/27 00:16:37 nithyas Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.session;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.qatest.common.TestCommon;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.Test;

/**
 * This class tests setting the following properties in an ssotoken:
 * (a) Fixed property
 * (b) Protected property
 * (c) Custom property
 */
public class SessionProperty extends TestCommon {

    private SSOToken ssotoken;
    private boolean bVal = false;
    private String strRB = "SessionProperty";
    private ResourceBundle rb;

   /**
    * Empty constructor
    */
    public SessionProperty() {
        super("SessionProtectedProperty");
        rb = ResourceBundle.getBundle("session" + fileseparator + strRB);
    }

    /**
     * Test setting fixed property in an sso token.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testFixedPropertyChange()
    throws Exception {
        entering("testFixedPropertyChange", null);
        try {
            bVal = false;
            try {
                ssotoken = getToken(adminUser, adminPassword, basedn);
                ssotoken.setProperty(rb.getString(strRB +
                        ".fixedPropertyName"),
                        rb.getString(strRB + ".fixedPropertyValue"));
            } catch(SSOException e) {
               log(Level.FINEST, "testFixedPropertyChange",
                       rb.getString(strRB + ".fixedPropertyName") +
                       " property change failed. This is the expected result.");
               bVal = true;
            }
            assert (bVal);
        } catch(Exception e) {
            log(Level.SEVERE, "testFixedPropertyChange",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(ssotoken);
        }
        exiting("testFixedPropertyChange");
    }

    /**
     * Test setting protected property in an sso token.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testProtectedPropertyChange()
    throws Exception {
        entering("testProtectedPropertyChange", null);
        try {
            bVal = false;
            try {
                ssotoken = getToken(adminUser, adminPassword, basedn);
                ssotoken.setProperty(rb.getString(strRB +
                        ".protectedPropertyName"),
                        rb.getString(strRB + ".protectedPropertyValue"));
            } catch(SSOException e) {
               log(Level.FINEST, "testProtectedPropertyChange",
                       rb.getString(strRB + ".protectedPropertyName") +
                       "property change failed." +
                       " This is the expected result.");
               bVal = true;
            }
            assert (bVal);
        } catch(Exception e) {
            log(Level.SEVERE, "testProtectedPropertyChange",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(ssotoken);
        }
        exiting("testProtectedPropertyChange");
    }

    /**
     * Test setting and updating a custom property in an sso token.
     */
    @Test(groups={"ldapv3", "ldapv3_sec", "s1ds", "s1ds_sec", "ad", "ad_sec", 
      "amsdk", "amsdk_sec", "jdbc", "jdbc_sec"})
    public void testCustomProperty()
    throws Exception {
        entering("testCustomProperty", null);
        try {
            ssotoken = getToken(adminUser, adminPassword, basedn);
            ssotoken.setProperty(rb.getString(strRB + ".customPropertyName"),
                    rb.getString(strRB + ".customPropertyValue"));

            if (!(ssotoken.getProperty(rb.getString(strRB +
                    ".customPropertyName"))).
                    equals(rb.getString(strRB + ".customPropertyValue")))
                assert false;

            ssotoken.setProperty(rb.getString(strRB + ".customPropertyName"),
                    rb.getString(strRB + ".customPropertyValueUpdated"));
            if (!(ssotoken.getProperty(rb.getString(strRB +
                    ".customPropertyName"))).
                    equals(rb.getString(strRB + ".customPropertyValueUpdated")))
                assert false;
        } catch(Exception e) {
            log(Level.SEVERE, "testCustomProperty",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(ssotoken);
        }
        exiting("testCustomProperty");
    }
}
