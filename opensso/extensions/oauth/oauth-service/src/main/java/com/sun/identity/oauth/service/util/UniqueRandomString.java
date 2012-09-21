/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.util;

import java.util.UUID;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */
public class UniqueRandomString {
    public UniqueRandomString(){}

    public String getString() {
        String tmp = UUID.randomUUID().toString();
        return tmp.replaceAll("-", "");
    }
}
