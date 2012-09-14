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
 
 /**
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package org.forgerock.openam.console.ui.taglib.propertysheet;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.jato.util.NonSyncStringBuffer;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.jato.view.html.SelectableGroup;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.web.ui.common.CCBodyContentImpl;
import com.sun.web.ui.common.CCDebug;
import com.sun.web.ui.common.CCImage;
import com.sun.web.ui.common.CCJspWriterImpl;
import com.sun.web.ui.model.CCPropertySheetModelInterface;
import com.sun.web.ui.taglib.common.CCTagBase;
import com.sun.web.ui.taglib.html.CCCheckBoxTag;
import com.sun.web.ui.taglib.html.CCRadioButtonTag;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Peter Major
 * @author Steve Ferris
 */
public class CCPropertySheetTag extends com.sun.web.ui.taglib.propertysheet.CCPropertySheetTag {
    private static final String TXT = ".txt";
    private static final String URI = ".uri";
    private static final String CONTEXT_ROOT =
            SystemProperties.get(Constants.AM_CONSOLE_DEPLOYMENT_DESCRIPTOR);
    private static final String HELP_TEMPLATE =
            "<div id=\"help{0}\" class=\"helpPanel\">"
            + "<div class=\"closeBtn\"><a href=\"#\" id=\"close{0}\" onclick=\"hideHelp({0}); event.cancelBubble = true;return false;\">"
            + "<img alt=\"Close help\" src=\"" + CONTEXT_ROOT + "/console/images/tasks/close.gif\" border=\"0\" /></a></div>"
            + "<div class=\"helpHeader\">{2}</div>{3}</div>"
            + "<a href=\"#\" onclick=\"showHelp({0}); event.cancelBubble = true; return false;\" "
            + "onmouseover=\"hoverHelp({0}); event.cancelBubble = true;\" "
            + "onmouseout=\"outHelp({0}); event.cancelBubble = true;\" "
            + "onfocus=\"hoverHelp({0}); event.cancelBubble = true;\" "
            + "onblur=\"outHelp({0}); event.cancelBubble = true;\" id=\"i{0}\">"
            + "<img alt=\"{1}\" src=\"" + CONTEXT_ROOT + "/console/images/help/info.gif\" "
            + "id=\"helpImg{0}\" /></a><div class=\"helpText\">{1}</div>";
    private static final String URL_TEMPLATE =
            "<div class=\"helpFooter\"><img width=\"17px\" src=\""
            + CONTEXT_ROOT + "/com_sun_web_ui/images/favicon/favicon.ico\" alt=\"logo\" />"
            + "<a href=\"http://docs.forgerock.org/en/index.html?product=openam&version="
            + getDocVersion() + "&uri={0}\">Read more in the OpenAM online help</a></div>";
    //
    // Documentation is currently only available in English, hence the "en".
    //
    // URLs use a query string to specify documentation locations. This lets
    // docs.forgerock.org check the values and avoid some 404s. A populated
    // href value should therefore look something like this:
    //   http://docs.forgerock.org/en/index.html?product=openam&version=10.0.0&uri=/release-notes/index.html#fixes
    //
    // To guess uri arguments for not-yet-published documentation, have a look
    // at the documentation in progress at http://openam.forgerock.org/docs.html
    // For DocBook-based documentation sources, valid uris include
    // /<book-name>/index.html#<xml:id-value> where the xml:id attribute is
    // on some element in the source files, src/main/docbkx/<book-name>/*.xml.
    //
    private static volatile int uniqueID = 0;

    @Override
    protected String getMessage(String key) {
        String txtKey = key + TXT;
        String uriKey = key + URI;
        String helpTxt = super.getMessage(txtKey);
        if (helpTxt.equals(txtKey)) {
            //There is no such property, so let's just render the content as usual
            return super.getMessage(key);
        } else {
            String helpUri = super.getMessage(uriKey);
            if (helpUri.equals(uriKey)) {
                //There is no Help URL so only render the HelpText
                return MessageFormat.format(HELP_TEMPLATE, String.valueOf(uniqueID++), super.getMessage(key), helpTxt, "");
            } else {
                //Let's render everything
                return MessageFormat.format(HELP_TEMPLATE, String.valueOf(uniqueID++), super.getMessage(key), helpTxt, MessageFormat.format(URL_TEMPLATE, helpUri));
            }
        }
    }

    /**
     * Get version string used in the published documentation. This method makes
     * the assumption that the version string used in published documentation
     * corresponds to the first substring up to the initial space in
     * {@code SystemProperties.get(Constants.AM_VERSION)}, based on the
     * definition of {@code com.iplanet.am.version} in
     * {@code products/amserver/war/xml/template/sms/serverdefaults.properties}.
     *
     * @return Published documentation version string. Only released versions
     *         correspond to real, published documentation.
     */
    private static String getDocVersion() {
        String version = SystemProperties.get(Constants.AM_VERSION);

        final int indexFirstSpace = version.indexOf(' ');
        if (indexFirstSpace > 0) {
            version = version.substring(0, indexFirstSpace);
        }

        return version;
    }

    @Override
    protected String getValueHTML(Node valueNode, String labelId, boolean levelThree)
    throws JspException, IllegalArgumentException {
        if(valueNode == null) {
            CCDebug.trace1("Property node missing value element");
            return null;
        }

        String viewName = getAttributeValue(valueNode, "name", "");
        String tagclassName = getAttributeValue(valueNode, "tagclass", "com.sun.web.ui.taglib.html.CCStaticTextFieldTag");
        View child = null;

        if(!tagclassName.equals("com.sun.web.ui.taglib.spacer.CCSpacerTag") &&
                !tagclassName.equals("org.forgerock.openam.console.ui.taglib.spacer.CCSpacerTag")) {
            child = containerView.getChild(viewName);
        }

        CCTagBase tag = getCCTag(tagclassName);
        tag.setName(viewName);
        if (labelId != null) {
            tag.setElementId(labelId);
        }

        if (tagclassName.equals("com.sun.web.ui.taglib.html.CCCheckBoxTag")) {
            CCCheckBoxTag cb = (CCCheckBoxTag)tag;
            cb.setStyleLevel(levelThree ? "3" : "2");
            cb.setElementId(getNextLabelId());
        } else if(tagclassName.equals("com.sun.web.ui.taglib.html.CCRadioButtonTag")) {
            CCRadioButtonTag rb = (CCRadioButtonTag)tag;
            rb.setStyleLevel(levelThree ? "3" : "2");
            rb.setElementId(getNextLabelId());
        }

        if (valueNode.hasChildNodes()) {
            NodeList childNodeList = valueNode.getChildNodes();
            BodyContent bodyContent = null;
            if (tag instanceof BodyTag) {
                bodyContent = new CCBodyContentImpl(new CCJspWriterImpl(null, 100, false));
            }

            OptionList options = null;

            if (child != null && (child instanceof SelectableGroup)) {
                options = new OptionList();
            }

            for (int i = 0; i < childNodeList.getLength(); i++) {
                parseValueChildNode(childNodeList.item(i), tag, bodyContent, options);
            }

            if (bodyContent != null) {
                ((BodyTag)tag).setBodyContent(bodyContent);
            }

            if (options != null && options.size() > 0) {
                ((SelectableGroup)child).setOptions(options);
            }
        }

        if (tag.getBundleID() == null) {
            tag.setBundleID(getBundleID());
        }

        tag.setTabIndex(getTabIndex());
        String html = null;

        if (fireBeginDisplayEvent(containerView, tag)) {
            html = tag.getHTMLString(getParent(), pageContext, child);
        }

        return fireEndDisplayEvent(containerView, tag, html);
    }

    @Override
    protected void appendSubsection(NonSyncStringBuffer buffer,
                                    Node subsection,
                                    CCPropertySheetModelInterface model,
                                    int level,
                                    int labelWidth)
    throws JspException {
        super.appendSubsection(buffer, subsection, model, level, labelWidth);
        String spacer = XMLUtils.getNodeAttributeValue(subsection, "spacer");

        if (spacer != null && spacer.equalsIgnoreCase("true")) {
            buffer.append("\n<table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" title=\"\"><tr><td>\n")
                  .append(getImageHTMLString(CCImage.DOT, "1", "10"))
                  .append("</td><td class=\"ConLin\" width=\"100%\">")
                  .append(getImageHTMLString(CCImage.DOT, "1", "1"))
                  .append("</td></tr></table>\n\n");
        }
    }

    public static String getDynamicHelp(ResourceBundle bundle, String key) {
        String txtKey = key + TXT;
        String urlKey = key + URI;
        String helpTxt = getFromBundle(bundle, txtKey);
        if (helpTxt == null) {
            //There is no such property, so let's just render the content as usual
            return getFromBundle(bundle, key);
        } else {
            String helpUrl = getFromBundle(bundle, urlKey);
            if (helpUrl == null) {
                //There is no Help URL so only render the HelpText
                return MessageFormat.format(HELP_TEMPLATE, uniqueID++, getFromBundle(bundle, key), helpTxt, "");
            } else {
                //Let's render everything
                return MessageFormat.format(HELP_TEMPLATE, uniqueID++, getFromBundle(bundle, key), helpTxt, MessageFormat.format(URL_TEMPLATE, helpUrl));
            }
        }
    }

    private static String getFromBundle(ResourceBundle bundle, String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException mre) {
            return null;
        }
    }
}
