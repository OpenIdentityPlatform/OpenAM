/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.identity.servicetag.registration;

import java.util.Map;
import java.util.HashMap;
import com.sun.scn.servicetags.SunOnlineAccount;

public class SOAccount implements RegistrationAccount {
    
    /** Creates a new instance of SunOnlineAccount */
    public SOAccount(Map map) {        
        account = new SunOnlineAccount();
        setCity(getValue(map, CITY));
        setCompany(getValue(map, COMPANY));
        setCountry(getValue(map, COUNTRY));
        setPassword(getValue(map, PASSWORD));
        setConfirmPassword(getValue(map, CONFIRMPASSWORD));
        setEmail(getValue(map, EMAIL));
        setFirstName(getValue(map, FIRSTNAME));
        setLastName(getValue(map, LASTNAME));
        setSecurityAnswer(getValue(map, SECURITYANSWER));
        setState(getValue(map, STATE));
        setAddress(getValue(map, ADDRESS));
        setZip(getValue(map, ZIP));
        setUserID(getValue(map, USERID));
        account.setTouVersion("SMI_TOU_1.2"); //whats this??
        account.setTouResponse("Accepted");       
    }

    public SOAccount(RegistrationAccountConfig rc) {
        this((Map)(rc.getParams()[0]));
    }
    
    public void setCity(String str) {        
        account.setCity(str);
    }
    
    public void setCompany(String str) {        
        account.setCity(str);        
    }
    
    public void setCountry(String str) {        
        account.setCountry(str);
    }
    
    public void setPassword(String str) {
        account.setPassword(str);        
    }
    
    public void setConfirmPassword(String str) {        
        account.setConfirmPassword(str);
    }
    
    public void setEmail(String str) {  
        account.setEmail(str);
    }
    
    public void setFirstName(String str) {        
        account.setFirstname(str);
    }
    
    public void setLastName(String str) {        
        account.setLastname(str);       
    }
    
    public void setSecurityAnswer(String str) {        
        account.setSecurityAnswer(str);        
    }
    
    public void setState(String str) {        
        account.setState(str);        
    }
    
    public void setAddress(String str) {        
        account.setStreetAddress(str);               
    }
    
    public void setZip(String str) {        
        account.setZip(str);        
    }
    
    public void setUserID(String str) {        
        account.setUserid(str);        
    }
   
    public String getCity() {        
        return account.getCity();
    }
    
    public String getCompany() {        
        return account.getCity();        
    }
    
    public String getPassword() {
        return account.getPassword();        
    }
    
    public String getConfirmPassword() {        
        return account.getConfirmPassword();
    }
    
    
    public String getEmail() {  
        return account.getEmail();
    }
    
    public String getFirstName() {        
       return account.getFirstname();
    }
    
    public String getLastName() {        
        return account.getLastname();       
    }
    
    public String getSecurityAnswer() {        
        return account.getSecurityAnswer();        
    }
    
    public String getState() {        
        return account.getState();        
    }
    
    public String getAddress() {        
        return account.getStreetAddress();               
    }
    
    public String getZip() {        
        return account.getZip();        
    }
    
    public String getUserID() {        
        return account.getUserid();        
    }

    public SunOnlineAccount getSunOnlineAccount() {
        return account;
    }
    
    private String getValue(Map map, String key) {
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        return null;        
    }

    public static final String CITY = "city";
    public static final String COMPANY = "company";
    public static final String CONFIRMPASSWORD = "confirmPassword";
    public static final String COUNTRY = "country";
    public static final String EMAIL = "email";
    public static final String FIRSTNAME = "firstName";
    public static final String LASTNAME = "lastName";
    public static final String PASSWORD = "password";
    public static final String SECURITYANSWER = "securityAnswer";
    public static final String STATE = "state";
    public static final String ADDRESS = "address";
    public static final String USERID = "userID";
    public static final String ZIP = "zip";
    
    private final SunOnlineAccount account;
}
