/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * 
 *
 */

package com.sun.identity.fedlet.ag;



public class UserData {

    
    String relaystate;
    String[] selos;
    String[] selos2;
    
    public void setrelaystate( String value )
    {
        relaystate = value;
    }
    
    public void setselos( String[] value )
    {
        selos = value;
    }
    
    public void setselos2( String[] value )
    {
        selos2 = value;
    }
   

    public String getrelaystate() { return relaystate; }
    
    public String[] getselos() { return selos; }
    
    public String[] getselos2() { return selos2; }

}
