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
 * $Id: NewPassword.java,v 1.4 2008/06/25 05:42:32 qcheng Exp $
 *
 */
package com.sun.identity.config;

import com.sun.identity.config.util.AjaxPage;
import net.sf.click.control.Form;
import net.sf.click.control.HiddenField;
import net.sf.click.control.PasswordField;
import net.sf.click.control.Submit;

/**
 * @author Les Hazlewood
 */
public class NewPassword extends AjaxPage {

    public Form newPasswordForm = new Form("newPasswordForm");

    public void onInit() {
        super.onInit();
        newPasswordForm.add( new HiddenField("username", "amAdmin" ) );
        newPasswordForm.add(new PasswordField("password", getLocalizedString("configuration.new_password.password"), true));
        newPasswordForm.add(new PasswordField("passwordConfirm", getLocalizedString("configuration.new_password.confirm"), true));

        Submit submit = new Submit("save", getLocalizedString("configuration.new_password.save"), this, "onSubmit");
        submit.setAttribute( "onclick", "submitNewPasswordForm(); return false;");

        newPasswordForm.add( submit );
    }

    public boolean onSubmit() {

        if ( newPasswordForm.isValid() ) {
            String password = newPasswordForm.getField( "password" ).getValue();
            String passwordConfirm = newPasswordForm.getField( "passwordConfirm" ).getValue();

            if ( !password.equals(passwordConfirm) ) {
                newPasswordForm.setError(getLocalizedString("configuration.new_password.error"));
                getContext().getResponse().setHeader( "formError", "true" );
            } else {
                String username = newPasswordForm.getField("username").getValue();
                getConfigurator().setPassword( username, password );
            }
        } else {
            getContext().getResponse().setHeader("formError", "true" );
        }
        return true;
    }
}
