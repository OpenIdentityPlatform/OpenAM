/**
 * @author sevani
 *
 * Configures JBoss server instance's server.policy
 * with permissions for agent codebase.
 */

package com.sun.identity.agents.tools.jboss;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.OSChecker;
import com.sun.identity.install.tools.util.ReplaceTokens;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

public class CopyAgentClasspathFileBase implements IConstants, IConfigKeys {
    
    protected String _setAgentClasspathFile;
    private String _agentLocaleDir;
    private String _agentLibPath;
    private String _agentInstanceConfigDirPath;
    
    public CopyAgentClasspathFileBase() {
    }
    
    protected String getAgentClasspathScriptName() {
        StringBuffer buff = new StringBuffer();
        
        if (OSChecker.isWindows()) {
            buff.append(STR_SET_AGENT_CLASSPATH_FILE);
            buff.append(STR_SET_AGENT_CLASSPATH_FILE_WIN_SERVER_PARAM);
            buff.append(STR_SET_AGENT_CLASSPATH_FILE_WIN_EXTN);
            
        } else {
            buff.append(STR_SET_AGENT_CLASSPATH_FILE);
            buff.append(STR_SET_AGENT_CLASSPATH_FILE_UNIX_SERVER_PARAM);
            buff.append(STR_SET_AGENT_CLASSPATH_FILE_UNIX_EXTN);
        }
        
        Debug.log("CopyAgentClasspathFileBase.getAgentClasspathScriptName(): "
                + buff.toString());
        
        return buff.toString();
    }
    
    protected void getAgentClasspathScriptFile(IStateAccess stateAccess) {
        String jbHomeDir = (String) stateAccess.get(STR_KEY_JB_HOME_DIR);
        String serverInstancename =
                (String) stateAccess.get(STR_KEY_JB_INST_NAME);
        
        String temp = jbHomeDir + STR_FORWARD_SLASH
                + STR_BIN_DIRECTORY + STR_FORWARD_SLASH;
        
        if (OSChecker.isWindows()) {
            _setAgentClasspathFile = temp
                    + STR_SET_AGENT_CLASSPATH_FILE
                    + serverInstancename
                    + STR_SET_AGENT_CLASSPATH_FILE_WIN_EXTN;
        } else {
            _setAgentClasspathFile = temp
                    + STR_SET_AGENT_CLASSPATH_FILE
                    + serverInstancename
                    + STR_SET_AGENT_CLASSPATH_FILE_UNIX_EXTN;
        }
        
        Debug.log("CopyAgentClasspathFileBase.getAgentClasspathScriptFile(): "
                + "agent script name = " + _setAgentClasspathFile);
        
        return;
    }
    
    protected boolean copyAgentClasspathFile(IStateAccess stateAccess) {
        
        boolean status = false;
        String srcFile = null;
        String destFile = null;
        try { 
            srcFile = createTagSwapFile(stateAccess);
            destFile = _setAgentClasspathFile;
            stateAccess.put(STR_KEY_JB_AGENT_ENV_FILE_PATH, destFile);
        
            Map tokens = new HashMap();
            tokens.put("AGENT_LOCALE_DIR", _agentLocaleDir);
            tokens.put("AGENT_CONFIG_DIR", _agentInstanceConfigDirPath);
        
            ReplaceTokens filter = new ReplaceTokens(srcFile, destFile, tokens);
            filter.tagSwapAndCopyFile();
       
            status = new File(srcFile).delete();
            Debug.log("CopyAgentClasspathFileBase.copyAgentClasspathFile() - " 
                + srcFile + " : " + destFile);
        } catch (Exception e) {
            Debug.log("CopyAgentClasspathFileBase." +
                "copyAgentClasspathFile() - Failed to copy  " + srcFile
                + " : " + destFile, e);

        }
        return status;
    }
    
    private String createTagSwapFile(IStateAccess stateAccess)
    throws Exception {
        
        Map tokens = new HashMap();
        String srcFile;
        
        if (OSChecker.isWindows()) {
            srcFile = ConfigUtil.getEtcDirPath() + STR_FORWARD_SLASH
                    + STR_AGENT_ENV_CMD_TEMPLATE;
        } else {
            srcFile = ConfigUtil.getEtcDirPath() + STR_FORWARD_SLASH
                    + STR_AGENT_ENV_SH_TEMPLATE;
        }
        
        String destFile = ConfigUtil.getEtcDirPath() + STR_FORWARD_SLASH
                + "tokens.txt";
        
        ReplaceTokens filter = new ReplaceTokens(srcFile, destFile, tokens);
        filter.tagSwapAndCopyFile();
        Debug.log("CopyAgentClasspathFileBase." +
            "createTagSwapFile() - after filter tag swap " + srcFile
            + " : " + destFile);
        
        return destFile;
    }
    
    protected void getAgentClasspathDirectories(IStateAccess stateAccess) {
        String homeDir = ConfigUtil.getHomePath();
        _agentLibPath = ConfigUtil.getLibPath();
        _agentLocaleDir = ConfigUtil.getLocaleDirPath();
        
        String instanceName = stateAccess.getInstanceName();
        StringBuffer sb = new StringBuffer();
        sb.append(homeDir).append(STR_FORWARD_SLASH);
        sb.append(instanceName).append(STR_FORWARD_SLASH);
        sb.append(STR_INSTANCE_CONFIG_DIR_NAME);
        _agentInstanceConfigDirPath = sb.toString();
    }
    
    protected boolean removeAgentClasspathFile(IStateAccess state) {
        boolean status = false;
        getAgentClasspathScriptFile(state);
        
        try { 
            File file = new File(_setAgentClasspathFile);
            status = file.delete();
            Debug.log("UpdateServerClasspathBase.removeAgentClasspathFile(): "
                + " Removed file " + _setAgentClasspathFile);
        } catch (Exception e) {
            Debug.log(
                "CopyAgentClasspathFileBase.removeAgentClasspathFile(): "
                + " Removing file " + _setAgentClasspathFile, e);
        }

        return status;
    }
    
    
}
