/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identityprovider.springsecurity.example;

import javax.annotation.security.RolesAllowed;

/**
 *
 * @author warrenstrange
 */
public class HelloBean {

    @RolesAllowed("ROLE_ADMIN")
    public  String  sayHello() {
        return "Hello";
    }
}
