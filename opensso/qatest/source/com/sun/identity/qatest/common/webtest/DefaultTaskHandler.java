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
 * $Id: DefaultTaskHandler.java,v 1.3 2008/06/26 20:05:14 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.webtest;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.webtest.DataEntry;
import com.sun.identity.qatest.common.webtest.DataSet;
import com.sun.identity.qatest.common.webtest.WebUtils;
import com.sun.identity.qatest.common.webtest.ITask;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

/**
 * This class is responsible for configuring the product.
 */
public class DefaultTaskHandler extends TestCommon implements ITask {
    private String base;
    
    public DefaultTaskHandler(String base) {
        super("DefaultTaskHandler");
        this.base = base;
    }
    
    /**
     * Executes the task i.e. visiting the URL and post information.
     *
     * @param webClient Web Client to hit the URL.
     * @return the resulting HTML page.
     * @throws Exception if task cannot be executed or assertion failed.
     */
    public HtmlPage execute(WebClient webClient)
        throws Exception {
        DataSet data = new DataSet(base);
        URL url = new URL(data.getKickOffURL());
        log(Level.FINEST, "execute", "Web client URL: " + url);
        HtmlPage page = (HtmlPage)webClient.getPage(url);
        List<DataEntry> entries = data.getEntries();
        for (DataEntry e : entries) {
            page = (HtmlPage)WebUtils.postForm(webClient, page, e);
        }
        return page;
    }
}
