/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.upgrade;

import com.sun.identity.config.SetupWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author steve
 */
public class SetUpgradeProgress extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse response) 
        throws ServletException, IOException
    { 
        req.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        String mode = req.getParameter("mode");
        boolean isTextMode = ((mode != null) && (mode.equals("text")));
        
        PrintWriter out = response.getWriter();
        if (!isTextMode) {
            out.println(
                "<html>\n"+
                  "<head>\n"+
                  "<link rel=\"stylesheet\" type=\"text/css\" href=\"../com_sun_web_ui/css/css_ns6up.css\">\n" +
              "    <script language=\"Javascript\">\n"+
                    "function addProgressText(str)\n"+ 
                    "{\n"+ 
                       "var obj = document.getElementById(\"progressText\");\n"+ 
                       "obj.innerHTML += str;\n"+
                       "var obj = document.getElementById(\"progressP\");\n"+
                       "obj.scrollTop = obj.scrollHeight;\n"+ 
                     "}\n"+
                   "</script>\n"+
                   "</head>\n"+
                   "<body>\n"+
                     "<p id=\"progressP\" style=\"height:200px; overflow:auto; border:1px solid grey;\">\n"+
                       "<span id=\"progressText\"></span>\n"+
                     "</p>\n");
            out.flush();
        }
        SetupWriter writer = new SetupWriter(out);
        UpgradeProgress.setTextMode(isTextMode);
        UpgradeProgress.setWriter(writer);
        writer.realFlush();
        
        if (!isTextMode) {
            out.println("</body>\n");
            out.println("</html>\n");
        }
        
        out.flush();
    }    
}
