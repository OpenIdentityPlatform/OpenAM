/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2022-2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.docs.authmodules;

import org.apache.commons.text.TextStringBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.List;

public class HtmlConverter {
    public void convertToAsciidoc(String html, TextStringBuilder builder) throws Exception {
        html = html.replaceAll("(?i)(<br\\s*/?>\\s*){2,}", "<br/>");
        Document doc = Jsoup.parse(html);

        convertNodesToAsciidoc(doc.body().childNodes(), builder);

    }

    private void convertNodesToAsciidoc(List<Node> nodes, TextStringBuilder builder) {
        for(Node node : nodes) {
            if (node instanceof TextNode) {
                builder.append(((TextNode) node).text());
            } else if (node instanceof Element) {
                Element element = (Element)node;
                switch (element.nodeName().toLowerCase()) {
                    case "br":
                        builder.appendNewLine().appendNewLine();
                        break;
                    case "code":
                    case "pre":
                        builder.append("`");
                        convertNodesToAsciidoc(node.childNodes(), builder);
                        builder.append("`");
                        break;
                    case "i":
                        builder.append("__");
                        convertNodesToAsciidoc(node.childNodes(), builder);
                        builder.append("__");
                        break;
                    case "a":
                        String text = element.text();

                        String href = element.attr("href");
                        String window = "";
                        String targetAttr = element.attr("target");
                        if (targetAttr.trim().isEmpty()) {
                            if(targetAttr.startsWith("_")) {
                                targetAttr = "\\" + targetAttr;
                            }
                            window = ", window=" + targetAttr;
                        }
                        builder.append(href).append("[").append(text).append(window).append("]");
                        break;
                    case "ul":
                        builder.appendNewLine();
                        for (Element ul: element.children()) {
                            builder.append("* ");
                            convertNodesToAsciidoc(ul.childNodes(), builder);
                            builder.appendNewLine();
                        }
                        break;
                    case "ol":
                        builder.appendNewLine();
                        for (Element ul: element.children()) {
                            builder.append(". ");
                            convertNodesToAsciidoc(ul.childNodes(), builder);
                            builder.appendNewLine();
                        }
                        break;
                    case "b":
                        builder.append("*");
                        convertNodesToAsciidoc(node.childNodes(), builder);
                        builder.append("*");
                        break;
                    default:
                        System.err.println("Unhandled tag: " + node.nodeName());
                        throw new RuntimeException(node.nodeName());
                }
            }
        }
    }
}
