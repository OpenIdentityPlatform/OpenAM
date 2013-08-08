/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.upgrade.apt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.utils.IOUtils;

/**
 * This compilation-time annotation processor tries to find all classes marked
 * with {@link UpgradeStepInfo}, then it will look through the dependencies and
 * try to detect an upgrade path based on that.
 *
 * @author Peter Major
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("org.forgerock.openam.upgrade.UpgradeStepInfo")
public class UpgradeStepProcessor extends AbstractProcessor {

    private final Map<String, DependencyInfo> stepMappings = new HashMap<String, DependencyInfo>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(UpgradeStepInfo.class);
            for (Element element : elements) {
                Set<DependencyInfo> depends = new LinkedHashSet<DependencyInfo>(2);
                String annotatedType = element.asType().toString();
                UpgradeStepInfo annotation = element.getAnnotation(UpgradeStepInfo.class);
                for (String name : annotation.dependsOn()) {
                    DependencyInfo depInfo = stepMappings.get(name);
                    if (depInfo == null) {
                        depInfo = new DependencyInfo(name);
                        stepMappings.put(name, depInfo);
                    }
                    depends.add(depInfo);
                }

                DependencyInfo depInfo = stepMappings.get(annotatedType);
                if (depInfo == null) {
                    depInfo = new DependencyInfo(annotatedType);
                    stepMappings.put(annotatedType, depInfo);
                }
                for (DependencyInfo dep : depends) {
                    depInfo.dependencies.add(dep);
                }
            }
            createDAG();
        }

        return true;
    }

    /**
     * Finds a directed acyclic graph between the upgradestep dependencies, so we can have an order where it's
     * guaranteed that the upgradestep dependencies are always executed before the step in question.
     */
    private void createDAG() {
        List<DependencyInfo> sorted = new ArrayList<DependencyInfo>(stepMappings.size());
        boolean updated = true;
        while (!stepMappings.isEmpty() && updated) {
            updated = false;
            Iterator<Map.Entry<String, DependencyInfo>> it = stepMappings.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, DependencyInfo> entry = it.next();
                DependencyInfo depInfo = entry.getValue();
                depInfo.dependencies.removeAll(sorted);
                if ((depInfo.dependencies.isEmpty() && !depInfo.className.equals("*"))
                        || (stepMappings.size() == 2) && depInfo.className.equals("*")) {
                    sorted.add(depInfo);
                    it.remove();
                    updated = true;
                }
            }
        }
        if (!stepMappings.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Unable to detect DAG, there seems to be a directed cycle across the following dependencies: "
                    + stepMappings.values().toString());
        } else {
            String outputFile = processingEnv.getOptions().get("outputFile");
            StringBuilder sb = new StringBuilder(200);
            sb.append("upgrade.step.order=");
            for (DependencyInfo dependencyInfo : sorted) {
                if (!dependencyInfo.className.equals("*")) {
                    sb.append(dependencyInfo.className).append(' ');
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            writeFile(outputFile, sb.toString());
        }
    }

    private void writeFile(String path, String content) {
        File file = new File(path);
        file.getParentFile().mkdirs();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(path);
            pw.println(content);
        } catch (IOException ioe) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "An IO error occured, while writing file: "
                    + ioe.getMessage());
        } finally {
            IOUtils.closeIfNotNull(pw);
        }
    }

    private static final class DependencyInfo {

        private String className;
        private Set<DependencyInfo> dependencies = new HashSet<DependencyInfo>();

        public DependencyInfo(String className) {
            this.className = className;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + (this.className != null ? this.className.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DependencyInfo other = (DependencyInfo) obj;
            if ((this.className == null) ? (other.className != null) : !this.className.equals(other.className)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "DependencyInfo{" + "className=" + className + "}";
        }
    }
}
