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
 * Portions copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.scripting.factories;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.PackageNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

/**
 * A Groovy {@link CompilationCustomizer} that rejects compile-time AST transformation annotations such as
 * {@code @groovy.transform.ASTTest} and the {@code @Grab} family.
 * <p>
 * The {@code org.kohsuke.groovy.sandbox} sandbox used elsewhere in this module is a purely <em>runtime</em> control: it
 * intercepts method and property access while a compiled script executes. Local AST transformations, by contrast, run
 * <em>inside the compiler</em> &mdash; {@code @ASTTest} executes an arbitrary closure during the
 * {@code SEMANTIC_ANALYSIS} phase and {@code @Grab} resolves dependencies during {@code CONVERSION} &mdash; long before
 * the runtime sandbox is ever applied. Simply submitting a script for validation triggers compilation, so an attacker
 * with rights to validate or create scripts can run arbitrary code on the host without the script ever being executed.
 * <p>
 * This customizer runs at the {@link CompilePhase#CONVERSION} phase, which is strictly earlier than the phase in which
 * {@code @ASTTest} fires, and aborts compilation with a normal compilation error before any transform code executes. It
 * is registered on the shared {@code CompilerConfiguration} in {@link GroovyEngineFactory} so that it covers both the
 * validate/create paths and script execution.
 */
public final class RejectASTTransformsCustomizer extends CompilationCustomizer {

    /**
     * Fully-qualified names of the annotations that execute code, fetch artefacts, or hide such annotations at compile
     * time. {@code @AnnotationCollector} is included because it lets a meta-annotation bundle (and thereby conceal) a
     * transform such as {@code @ASTTest}: at the {@code CONVERSION} phase only the collector and its alias are visible,
     * and the bundled transform materialises later when the collector expands, bypassing a name check on the alias.
     */
    private static final Set<String> BLOCKED_TRANSFORMS = new HashSet<>(Arrays.asList(
            "groovy.transform.ASTTest",
            "groovy.transform.AnnotationCollector",
            "groovy.lang.Grab",
            "groovy.lang.Grapes",
            "groovy.lang.GrabConfig",
            "groovy.lang.GrabResolver",
            "groovy.lang.GrabExclude"));

    public RejectASTTransformsCustomizer() {
        super(CompilePhase.CONVERSION);
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode)
            throws CompilationFailedException {
        final ModuleNode module = source.getAST();
        final Map<String, String> importAliases = importAliases(module);

        // @Grab and friends are typically attached to import statements or the package declaration rather than a class.
        if (module != null) {
            for (ImportNode importNode : module.getImports()) {
                checkAnnotations(source, importNode, importAliases);
            }
            for (ImportNode importNode : module.getStarImports()) {
                checkAnnotations(source, importNode, importAliases);
            }
            for (ImportNode importNode : module.getStaticImports().values()) {
                checkAnnotations(source, importNode, importAliases);
            }
            for (ImportNode importNode : module.getStaticStarImports().values()) {
                checkAnnotations(source, importNode, importAliases);
            }
            final PackageNode packageNode = module.getPackage();
            if (packageNode != null) {
                checkAnnotations(source, packageNode, importAliases);
            }
        }

        // Walk the class (its annotations, methods, fields, properties, ...) rejecting any blocked annotation.
        new RejectASTTransformsVisitor(source, importAliases).visitClass(classNode);
    }

    /**
     * Builds a map of imported simple/alias name to fully-qualified class name, so that blocked annotations referenced
     * through a plain or aliased import (e.g. {@code import groovy.transform.ASTTest as Foo}) can still be resolved and
     * rejected.
     */
    private static Map<String, String> importAliases(ModuleNode module) {
        final Map<String, String> aliases = new HashMap<>();
        if (module != null) {
            for (ImportNode importNode : module.getImports()) {
                final ClassNode type = importNode.getType();
                final String alias = importNode.getAlias();
                if (type != null && alias != null) {
                    aliases.put(alias, type.getName());
                }
            }
        }
        return aliases;
    }

    private static void checkAnnotations(SourceUnit source, AnnotatedNode node, Map<String, String> importAliases) {
        if (node == null) {
            return;
        }
        for (AnnotationNode annotation : node.getAnnotations()) {
            final String written = annotation.getClassNode().getName();
            if (isBlocked(written) || isBlocked(importAliases.get(written))
                    || isBlocked("groovy.transform." + written) || isBlocked("groovy.lang." + written)) {
                reject(source, annotation, written);
            }
        }
    }

    private static boolean isBlocked(String name) {
        return name != null && BLOCKED_TRANSFORMS.contains(name);
    }

    /**
     * Aborts compilation with a normal syntax error so that callers such as {@code StandardScriptValidator} surface it
     * as an ordinary {@code ScriptError} rather than an unchecked exception.
     */
    private static void reject(SourceUnit source, AnnotationNode annotation, String name) {
        final SyntaxException error = new SyntaxException(
                "Annotation @" + name + " is not allowed in server-side scripts.",
                annotation.getLineNumber(), annotation.getColumnNumber());
        source.getErrorCollector().addErrorAndContinue(new SyntaxErrorMessage(error, source));
        throw new MultipleCompilationErrorsException(source.getErrorCollector());
    }

    /**
     * Visits every annotated element of a class and rejects blocked AST-transform annotations.
     */
    private static final class RejectASTTransformsVisitor extends ClassCodeVisitorSupport {

        private final SourceUnit sourceUnit;
        private final Map<String, String> importAliases;

        RejectASTTransformsVisitor(SourceUnit sourceUnit, Map<String, String> importAliases) {
            this.sourceUnit = sourceUnit;
            this.importAliases = importAliases;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return sourceUnit;
        }

        @Override
        public void visitAnnotations(AnnotatedNode node) {
            super.visitAnnotations(node);
            checkAnnotations(sourceUnit, node, importAliases);
        }
    }
}
