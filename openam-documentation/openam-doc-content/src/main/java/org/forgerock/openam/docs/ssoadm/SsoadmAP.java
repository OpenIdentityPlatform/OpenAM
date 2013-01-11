/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock Inc. All Rights Reserved
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
package org.forgerock.openam.docs.ssoadm;

import com.sun.identity.cli.annotation.Macro;
import com.sun.identity.cli.annotation.SubCommandInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * Annotation processor to generate DocBook 5 XML-based reference content
 * for the ssoadm command.
 *
 * Annotations specifying ssoadm subcommand info are provided by two files:
 * openam-core/src/main/java/com/sun/identity/cli/definition/AccessManager.java
 * openam-federation/OpenFM/src/main/java/com/sun/identity/federation/cli/definition/FederationManager.java
 *
 * The ssoadm &lt;refentry&gt; incorporates a sorted list of subcommands and
 * their options.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("*")
public class SsoadmAP extends AbstractProcessor {
    // Macro content kept keyed by name to be mixed into SubCommandInfos

    Map<String, Macro> macros = new TreeMap<String, Macro>();
    // Formatted subcommand XML sorted as strings (alphanumeric)
    Set<String> subcommands = new TreeSet<String>();

    /**
     * After processing is complete, the set of subcommands contains the
     * documentation for the ssoadm &lt;referentry&gt;.
     */
    public Set<String> getSubcommands() {
        return subcommands;
    }

    /**
     * Two field annotations hold documentation content: @Macro and
     * @SubCommandInfo. @Macro holds field content to be merged with
     * @SubCommandInfo field content for @SubCommandInfo annotations that
     * reference @Macro.
     *
     * @SubCommandInfo field annotations hold descriptions,
     * manadatoryOptions, and optionalOptions used in reference
     * documentation.
     *
     * Names for subcommands are the names of the fields themselves,
     * with - substituted for _.
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Push Macro content into macros for use evaluating SubCommandInfos.
        for (Element e : roundEnv.getElementsAnnotatedWith(Macro.class)) {
            String name = e.getSimpleName().toString().replaceAll("_", "-");
            macros.put(name, e.getAnnotation(Macro.class));
        }

        // Push formatted SubCommandInfo content into the sorted set.
        for (Element e : roundEnv.getElementsAnnotatedWith(SubCommandInfo.class)) {
            String name = e.getSimpleName().toString().replaceAll("_", "-");
            SubCommandInfo info = e.getAnnotation(SubCommandInfo.class);

            String description = info.description();
            Collection<String> mandatory = new TreeSet<String>();
            mandatory.addAll(Arrays.asList(info.mandatoryOptions()));

            Collection<String> optional = new TreeSet<String>();
            optional.addAll(Arrays.asList(info.optionalOptions()));

            Macro macro = macros.get(info.macro()); // One @Macro name per
            if (macro != null) // @SubCommandInfo
            {
                mandatory.addAll(Arrays.asList(macro.mandatoryOptions()));
                optional.addAll(Arrays.asList(macro.optionalOptions()));
            }

            subcommands.add(SubCommandXML.parseSubCommandInfo(name, description, mandatory, optional));
        }

        StringBuilder content = readFileFromClassPath("/man-ssoadm-1.header");
        for (String subCommand : subcommands) {
            content.append(subCommand);
        }
        content.append(readFileFromClassPath("/man-ssoadm-1.footer"));
        writeFile(processingEnv.getOptions().get("outputFile"), content.toString());

        return true;
    }

    private static StringBuilder readFileFromClassPath(String path) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(SsoadmAP.class.getResourceAsStream(path)));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                }
            }
        }
        return sb;
    }

    private static void writeFile(String path, String content) {
        File file = new File(path);
        file.getParentFile().mkdirs();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(path);
            pw.println(content);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }
}
