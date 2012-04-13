/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FeedBean.java,v 1.3 2009/06/04 11:49:15 veiming Exp $
 */

package com.sun.identity.admin.model;

import com.sun.identity.admin.Resources;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class FeedBean implements Serializable {
    private SyndFeed feed;
    private String urlKey;
    private Exception exception;

    public void setUrlKey(String urlKey) {
        this.urlKey = urlKey;
    }

    public SyndFeed getFeed() {
        if (feed == null) {
            loadFeed();
        }
        return feed;
    }

    private void loadFeed() {
        Resources r = new Resources();
        String u = r.getString(urlKey);

        try {
            URL url = new URL(u);

            SyndFeedInput input = new SyndFeedInput();
            feed = input.build(new XmlReader(url));
            exception = null;
        } catch (MalformedURLException mfue) {
            this.exception = mfue;
        } catch (IOException ioe) {
            this.exception = ioe;
        } catch (FeedException fe) {
            this.exception = fe;
        }
    }

    public Exception getException() {
        return exception;
    }
}
