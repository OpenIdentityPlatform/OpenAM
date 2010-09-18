/**
 * StringManager.java
 * 
 * Created on Oct 29, 2007, 2:05:52 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.servicetag.registration;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 * @author tjquinn
 */
public class StringManager {
    private static final ResourceBundle resourceBundle = 
            ResourceBundle.getBundle(StringManager.class.getPackage().getName() + ".LocalStrings");
    
    public static String getString(String messageKey, Object... args) {
        String message = resourceBundle.getString(messageKey);
        return MessageFormat.format(message, args);
    }
}
