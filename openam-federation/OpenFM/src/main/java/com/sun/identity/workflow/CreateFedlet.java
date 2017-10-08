/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CreateFedlet.java,v 1.20 2010/01/08 22:41:43 exu Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.workflow;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.cot.COTException;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.encode.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openam.utils.file.ZipUtils;

/**
 * Creates a Fedlet configuration that can be used in conjunction with the un-configured Fedlet WAR.
 */
public class CreateFedlet extends Task {

    private static final Map<String, String> FedConfigTagSwap = new LinkedHashMap<>();
    private static final String FEDLET_DEFAULT_SHARED_KEY = "JKEK7DosAgR3Aw3Ece20F8qZdXtiMYJ+";

    static {
        FedConfigTagSwap.put("@AM_ENC_PWD@", getRandomString());
        FedConfigTagSwap.put("%BASE_DIR%%SERVER_URI%", "@FEDLET_HOME@");
        FedConfigTagSwap.put("%BASE_DIR%", "@FEDLET_HOME@");
    }
    
    /**
     * Returns secure random string.
     *
     * @return secure random string.
     */
    private static String getRandomString() {
        String randomStr = null;
        try {
            byte [] bytes = new byte[24];
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.nextBytes(bytes);
            randomStr = Base64.encode(bytes).trim();
        } catch (NoSuchAlgorithmException e) {
            randomStr = null;
        }
        return (randomStr != null) ? randomStr : FEDLET_DEFAULT_SHARED_KEY;
    }

    public String execute(Locale locale, Map params) throws WorkflowException {
        validateParameters(params);
        String entityId = getString(params, ParameterKeys.P_ENTITY_ID);
        Pattern patern = Pattern.compile("[/:\\.?|*]");
        Matcher match = patern.matcher(entityId);
        String folderName = match.replaceAll("");
        String workDir = SystemProperties.get(SystemProperties.CONFIG_PATH) + "/myfedlets/" + folderName;
        File dir = new File(workDir);
        if (dir.exists()) {
            throw new WorkflowException("directory.already.exist", workDir);
        }
        
        if (!dir.getParentFile().exists())  {
            dir.getParentFile().mkdir();
        }
        dir.mkdir();
        String confDir = workDir + "/conf";
        dir = new File(confDir);
        dir.mkdir();
        
        loadMetaData(params, confDir);
        exportIDPMetaData(params, confDir);
        createCOTProperties(params, confDir);
        
        ServletContext servletCtx = (ServletContext) params.get(ParameterKeys.P_SERVLET_CONTEXT);
        copyBits(servletCtx, workDir);
        createFederationConfigProperties(servletCtx, confDir);

        String zipFileName = createZip(workDir);
        
        return MessageFormat.format(getMessage("Fedlet.created", locale), zipFileName);
    }
    
    private void copyBits(ServletContext servletCtx, String workDir) throws WorkflowException {

        copyFile(servletCtx, "/WEB-INF/fedlet/README", workDir + "/README");
    }
    
    private void copyFile(ServletContext servletCtx, String source, String dest)
        throws WorkflowException {
        File test = new File(dest);
        File parent = test.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        
        try (InputStream src = servletCtx.getResourceAsStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {

            if (src == null) {
                throw new WorkflowException("file-not-found", source);
            }

            IOUtils.copyStream(src, fos);
        } catch (IOException e) {
            throw new WorkflowException(e.getMessage());
        }
    }
    
    private void createCOTProperties(Map params, String workDir) throws WorkflowException {
        String sp = getString(params, ParameterKeys.P_ENTITY_ID);
        String idp = getString(params, ParameterKeys.P_IDP);
        String cot = getString(params, ParameterKeys.P_COT);

        Properties content = new Properties();
        content.setProperty("cot-name", cot);
        content.setProperty("sun-fm-cot-status", "Active");
        content.setProperty("sun-fm-trusted-providers", encodeVal(idp) + "," + encodeVal(sp));
        content.setProperty("sun-fm-saml2-readerservice-url", "");
        content.setProperty("sun-fm-saml2-writerservice-url", "");
        try (OutputStream out = new FileOutputStream(workDir + "/fedlet.cot")) {
            content.store(out, null);
        } catch (IOException e) {
            throw new WorkflowException(e.getMessage());
        }
    }
    
    private void exportIDPMetaData(Map params, String workDir) throws WorkflowException {
        String realm = getString(params, ParameterKeys.P_REALM);
        String idp = getString(params, ParameterKeys.P_IDP);
        String metadata = null;
        try {
            metadata = SAML2MetaUtils.exportStandardMeta(realm, idp, false);
        } catch (SAML2MetaException se) {
            throw new WorkflowException(se.getMessage());
        }
        String extended = ExportSAML2MetaData.exportExtendedMeta(realm, idp);
        
        String extendedModified = flipHostedParameter(extended, false);
        writeToFile(workDir + "/idp-extended.xml", extendedModified);
        writeToFile(workDir + "/idp.xml", metadata);
    }
    
    private void loadMetaData(Map params, String workDir) throws WorkflowException {
        String realm = getString(params, ParameterKeys.P_REALM);
        String entityId = getString(params, ParameterKeys.P_ENTITY_ID);
        String cot = getString(params, ParameterKeys.P_COT);
        String assertConsumer = getString(params, ParameterKeys.P_ASSERT_CONSUMER);
        List attrMapping = getAttributeMapping(params);

        String metadata = FedletMetaData.createStandardMetaData(entityId, assertConsumer);
        String extended = FedletMetaData.createExtendedMetaData(realm, entityId, attrMapping, assertConsumer);
        
        // Add the AttributeQueryConfig to SP extended meta data
        extended = addAttributeQueryTemplate(extended, cot);

        // Add the XACMLAuthzDecisionQueryConfig to SP extended meta data
        extended = addXACMLAuthzQueryTemplate(extended, cot);
          
        ImportSAML2MetaData.importData(realm, metadata, extended);
        if (!StringUtils.isBlank(cot)) {
            try {
                AddProviderToCOT.addToCOT(realm, cot, entityId);
            } catch (COTException e) {
                throw new WorkflowException(e.getMessage());
            }
            int idx = extended.indexOf("<Attribute name=\"cotlist\">");
            idx = extended.indexOf("</Attribute>", idx);
            extended = extended.substring(0, idx) + "<Value>" + cot + "</Value>" + extended.substring(idx);
        }
        
        String extendedModified = flipHostedParameter(extended, true);
        writeToFile(workDir + "/sp-extended.xml", extendedModified);
        writeToFile(workDir + "/sp.xml", metadata);
    }

    private void validateParameters(Map params) throws WorkflowException {
        String entityId = getString(params, ParameterKeys.P_ENTITY_ID);
        if (StringUtils.isBlank(entityId)) {
            throw new WorkflowException("entityId-required", null);
        }
        String assertConsumer = getString(params, ParameterKeys.P_ASSERT_CONSUMER);
        if (StringUtils.isBlank(assertConsumer)) {
            throw new WorkflowException("assertion.consumer-required", null);
        }
        
        try {
            new URL(assertConsumer);
        } catch (MalformedURLException e) {
            throw new WorkflowException("assertion.consumer-invalid", null);
        }

        String cot = getString(params, ParameterKeys.P_COT);
        if (StringUtils.isBlank(cot)) {
            throw new WorkflowException("missing-cot", null);
        }
        
        String realm = getString(params, ParameterKeys.P_REALM);
        if (StringUtils.isBlank(realm)) {
            throw new WorkflowException("missing-realm", null);
        }
    }
    
    private void createFederationConfigProperties(ServletContext servletCtx, String workDir) throws WorkflowException {
        
        String prop = getBitAsString(servletCtx, "/WEB-INF/fedlet/FederationConfig.properties");
        for (Map.Entry<String, String> replacement : FedConfigTagSwap.entrySet()) {
            prop = prop.replace(replacement.getKey(), replacement.getValue());
        }
        writeToFile(workDir + "/FederationConfig.properties", prop);
    }
    
    private String getBitAsString(ServletContext servletCtx, String bitName) throws WorkflowException {
        try {
            return IOUtils.readStream(servletCtx.getResourceAsStream(bitName));
        } catch (IOException ex) {
            throw new WorkflowException(ex.getMessage());
        }
    }
    
    private static void writeToFile(String fileName, String content) throws WorkflowException {
        try {
            IOUtils.writeToFile(fileName, content);
        } catch (IOException e) {
            throw new WorkflowException(e.getMessage());
        }
    }

    private static String flipHostedParameter(String xml, boolean bHosted) {
        int idx = xml.indexOf("<EntityConfig ");
        if (idx != -1) {
            idx = xml.indexOf("hosted=\"", idx);
         
            if (idx != -1) {
                int idx2 = xml.indexOf('"', idx+9);
                if (bHosted) {
                    xml = xml.substring(0, idx+8) + "1" + xml.substring(idx2);
                } else {
                    xml = xml.substring(0, idx+8) + "0" + xml.substring(idx2);
                }
            }
        }
        return xml;
    }
    
    private String createZip(String workDir) throws WorkflowException {
        final String zipName = workDir + "/Fedlet.zip";
        try {
            final List<String> files = ZipUtils.generateZip(workDir, zipName);
            deleteAllFiles(workDir, files);
        } catch (IOException | URISyntaxException e) {
            throw new WorkflowException(e.getMessage());
        }
        return zipName;
    }
    
    private void deleteAllFiles(String workDir, List<String> files) throws IOException {
        for (String fname : files) {
            new File(fname).delete();
        }
        
        List<String> dirs = getAllFiles(workDir, false);
        Collections.reverse(dirs);
        for (String dirName : dirs) {
            File test = new File(dirName);
            if (test.isDirectory()) {
                test.delete();
            }
        }
    }
    
    private List<String> getAllFiles(String dir, final boolean bFileOnly) throws IOException {
        final List<String> files = new ArrayList<>();

        Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                files.add(file.toAbsolutePath().toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                    throws IOException {
                if (!bFileOnly) {
                    files.add(dir.toAbsolutePath().toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }
    
    /**
     * Encodes special characters in a value.
     * percent to %25 and comma to %2C.
     */
    private String encodeVal(String v) {
        char[] chars = v.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length + 20);
        int i = 0, lastIdx = 0;
        for (i = 0; i < chars.length; i++) {
            if (chars[i] == '%') {
                if (lastIdx != i) {
                    sb.append(chars, lastIdx, i - lastIdx);
                }
                sb.append("%25");
                lastIdx = i + 1;
            } else if (chars[i] == ',') {
                if (lastIdx != i) {
                    sb.append(chars, lastIdx, i - lastIdx);
                }
                sb.append("%2C");
                lastIdx = i + 1;
            }
        }
        if (lastIdx != i) {
            sb.append(chars, lastIdx, i - lastIdx);
        }
        return sb.toString();
    }

    /**
     * Below method will add the AttributeQuery to the SP extended
     * meta data
     */
    private String addAttributeQueryTemplate(String extended, String cot) {
        String buff =
            "    <AttributeQueryConfig metaAlias=\"/attrQuery\">\n" +
            "        <Attribute name=\"signingCertAlias\">\n" +
            "            <Value>" + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"encryptionCertAlias\">\n" +
            "            <Value>" + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"wantNameIDEncrypted\">\n" +
            "            <Value>" + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"cotlist\">\n" +
            "            <Value>" + cot + "</Value>\n" +
            "        </Attribute>\n" +
            "    </AttributeQueryConfig>\n";
        int idx = extended.indexOf("</EntityConfig>");
        if (idx != -1) {
            extended = extended.substring(0, idx) + buff + "</EntityConfig>";
        }
	    return extended;
    }

    /**
     * Below method will add the XACMLAuthzDecisionQuery to the SP extended
     * meta data
     */
    private String addXACMLAuthzQueryTemplate(String extended, String cot) {
        String buff =
            "    <XACMLAuthzDecisionQueryConfig metaAlias=\"/pep\">\n" +
            "        <Attribute name=\"signingCertAlias\">\n" +
            "            <Value>" + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"encryptionCertAlias\">\n" +
            "            <Value>" + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"basicAuthOn\">\n" +
            "            <Value>" + false + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"basicAuthUser\">\n" +
            "            <Value>" + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"basicAuthPassword\">\n" +
            "            <Value>" + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"wantXACMLAuthzDecisionResponseSigned\">\n" +
            "            <Value>" + false + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"wantAssertionEncrypted\">\n" +
            "            <Value>" + false + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"cotlist\">\n" +
            "            <Value>" + cot + "</Value>\n" +
            "        </Attribute>\n" +
            "    </XACMLAuthzDecisionQueryConfig>\n";
        int idx = extended.indexOf("</EntityConfig>");
        if (idx != -1) {
            extended = extended.substring(0, idx) + buff + "</EntityConfig>";
        }
        return extended;
    }
}
