/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.provider.springsecurity;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.springframework.security.AuthorizationServiceException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;

/**
 *
 * @author warrenstrange
 */
public class OpenSSOUserDetails implements UserDetails {

    private SSOToken ssoToken;

    public OpenSSOUserDetails(SSOToken ssoToken) {
        this.ssoToken = ssoToken;
    }
    
    public GrantedAuthority[] getAuthorities() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getPassword() {
        return "**secret**";
    }

    public String getUsername() {
        try {
            return ssoToken.getPrincipal().getName();
        } catch (SSOException ex) {
           throw new AuthorizationServiceException("Cant access SSOToken",ex);
        }
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
       return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }

}
