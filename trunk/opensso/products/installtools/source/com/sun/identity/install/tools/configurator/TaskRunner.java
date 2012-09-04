/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TaskRunner.java,v 1.3 2008/09/09 16:59:33 leiming Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class TaskRunner implements InstallConstants {

    public TaskRunner(InstallRunInfo installRunInfo,
            PersistentStateAccess stateAccess, InstallLogger installLog,
            boolean silentMode, boolean installMode) throws InstallException {
        setTaskInfoList(new ArrayList());
        setTaskHandlers(new HashMap());
        setInstallMode(installMode);
        setStateAccess(stateAccess);
        initalizeTasks(installRunInfo);
        setSilentModeFlag(silentMode);
        setInstallLogger(installLog);
    }

    public void runTasks() throws InstallAbortException, InstallException {

        boolean exitStatus = false;
        boolean continueForward = true;
        int index = 0;
        int taskCount = getTaskInfoList().size();

        Debug.log("TaskRunner.runTasks() - Starting to execute tasks...");
        while (!getTaskInfoList().isEmpty() && !exitStatus) {

            TaskInfo taskInfo = (TaskInfo) getTaskInfoList().get(index);
            String handlerClass = taskInfo.getClassName();
            ITask task = (ITask) getTaskHandlers().get(handlerClass);

            // Set the PersistentStateAccess marker
            PersistentStateAccess stateAccess = getStateAccess();
            markStateAccess(stateAccess, index);

            if (continueForward) { // Task Execution in progress
                continueForward = executeTask(taskInfo, task, stateAccess);
            } else { // Undo operation in progress
                rollBackTask(taskInfo, task, stateAccess);
            }

            index = (continueForward) ? index + 1 : index - 1;
            if ((index >= taskCount) || (index < 0)) {
                exitStatus = true;
            }
        }

        if (!continueForward) {
            // We have successully done Roll back for all the tasks. So, exit
            Debug.log("TaskRunner.promptForUserAction() - Successfully "
                    + "completed RollBack for all the tasks. Now Exiting..");
            LocalizedMessage message = LocalizedMessage
                    .get(LOC_DR_MSG_TASK_ABORT_REQUESTED);
            throw new InstallAbortException(message);
        }
    }

    private boolean executeTask(TaskInfo taskInfo, ITask task,
            IStateAccess stateAccess) throws InstallAbortException,
            InstallException {
        boolean continueForward = true;

        LocalizedMessage message = task.getExecutionMessage(stateAccess,
                taskInfo.getPropertiesMap());
        printMessage(message);

        Debug.log("TaskRunner.executeTask() - task: " + taskInfo.getName()
                + " for class: " + task.getClass().getName());

        boolean result = task.execute(taskInfo.getName(), stateAccess, taskInfo
                .getPropertiesMap());
        if (!result) { // Task Execution failed
            printError();
            writeLogError(message);
            continueForward = promptForUserAction();
            if (!continueForward) { // Need to roll back!
                rollBackTask(taskInfo, task, stateAccess);
            }
        } else {
            printDone();
            writeLogSuccess(message);
        }

        return continueForward;
    }

    private void rollBackTask(TaskInfo taskInfo, ITask task,
            IStateAccess stateAccess) throws InstallException {
        LocalizedMessage message = task.getRollBackMessage(stateAccess,
                taskInfo.getPropertiesMap());
        printMessage(message);

        Debug.log("TaskRunner.rollBackTask() - task: " + taskInfo.getName()
                + " for class: " + task.getClass().getName());

        boolean result = task.rollBack(taskInfo.getName(), stateAccess,
                taskInfo.getPropertiesMap());
        if (!result) { // Task Undo failed
            printError();
            writeLogError(message);
        } else { // Task Undo Succeeded
            printDone();
            writeLogSuccess(message);
        }
    }

    private void markStateAccess(PersistentStateAccess stateAccess, int index) 
    {
        if (index < getInstanceTasksOffset()) {
            stateAccess.setCommonDataFlag(true);
        } else {
            stateAccess.setCommonDataFlag(false);
        }
    }

    private void initalizeTasks(InstallRunInfo installRunInfo)
            throws InstallException {
        if ((isInstallMode() && InstallState.isFreshInstall())
                || (!isInstallMode() && InstallState.getInstanceCount() == 1)) 
        { 
            // Include common tasks if fresh install or last install
            ArrayList commonTasks = installRunInfo.getCommonTasks();
            initTaskHandlers(commonTasks);
            setInstanceTasksOffset(commonTasks.size());
        } else {
            setInstanceTasksOffset(0);
        }
        initTaskHandlers(installRunInfo.getInstanceTasks());
    }

    private void initTaskHandlers(ArrayList list) throws InstallException {
        Debug.log("TaskRunner.initTaskHandlers() - Initializing Task "
                + "Handlers");
        int cCount = list.size();
        for (int i = 0; i < cCount; i++) {
            TaskInfo taskInfo = (TaskInfo) list.get(i);
            getTaskInfoList().add(taskInfo);
            // Initialize the handler class if is not present.
            String handlerClass = taskInfo.getClassName();
            if (getTaskHandlers().get(handlerClass) == null) {
                // Handler not present. Intialize and add it
                try {
                    ITask task = (ITask) Class.forName(handlerClass)
                            .newInstance();
                    getTaskHandlers().put(handlerClass, task);
                } catch (Exception e) {
                    Debug.log("TaskRunner.initTaskHandlers() - ERROR: Failed "
                            + "to intialize Task Handler for task: "
                            + taskInfo.getName(), e);
                    Object args[] = { taskInfo.getName() };
                    LocalizedMessage lMessage = LocalizedMessage.get(
                            LOC_DR_ERR_TASK_INIT_FAILED, args);
                    throw new InstallException(lMessage);
                }
            }
        }
    }

    private void printMessage(LocalizedMessage message) {
        if (!isSilentMode() && message != null) {
            Console.println();
            Console.print(message);
        }
    }

    private void printError() {
        if (!isSilentMode()) {
            LocalizedMessage message = LocalizedMessage
                    .get(LOC_DR_ERR_TASK_EXECUTE_FAIL);
            Console.println(message);
        }
    }

    private void writeLogError(LocalizedMessage message1) {
        if (message1 != null) {
            LocalizedMessage message2 = LocalizedMessage
                    .get(LOC_DR_ERR_TASK_EXECUTE_FAIL);
            Object[] args = { message1.getMessage(), message2.getMessage() };
            LocalizedMessage combinedMessage = LocalizedMessage.get(
                    LOC_DR_MSG_TASK_LOG_COMBINED_MSG, args);
            logInstall(combinedMessage);
        }
    }

    private void printDone() {
        if (!isSilentMode()) {
            LocalizedMessage message = LocalizedMessage
                    .get(LOC_DR_MSG_TASK_EXECUTE_DONE);
            Console.println(message);
        }
    }

    private void writeLogSuccess(LocalizedMessage message1) {
        if (message1 != null) {
            LocalizedMessage message2 = LocalizedMessage
                    .get(LOC_DR_MSG_TASK_EXECUTE_SUCCESS);
            Object[] args = { message1.getMessage(), message2.getMessage() };
            LocalizedMessage combinedMessage = LocalizedMessage.get(
                    LOC_DR_MSG_TASK_LOG_COMBINED_MSG, args);
            logInstall(combinedMessage);
        }
    }

    private boolean promptForUserAction() throws InstallAbortException,
            InstallException {
        boolean continueForward = true;
        if (!isSilentMode() && isInstallMode()) {
            TaskInteraction tInteraction = new TaskInteraction();
            InteractionResultStatus status = tInteraction
                    .interact(getStateAccess());
            if (status.getIntValue() == 
                InteractionResultStatus.INT_STATUS_BACK) {
                Debug.log("TaskRunner.promptForUserAction() - User choose "
                        + "to roll back previous Tasks");
                continueForward = false;
            } else if (status.getIntValue() == 
                InteractionResultStatus.INT_STATUS_ABORT) {
                Debug.log("TaskRunner.promptForUserAction() - User choose "
                        + "ABORT the installation.");
                LocalizedMessage message = LocalizedMessage
                        .get(LOC_DR_MSG_TASK_ABORT_REQUESTED);
                throw new InstallAbortException(message);
            }
        }
        return continueForward;
    }

    private PersistentStateAccess getStateAccess() {
        return stateAccess;
    }

    private boolean isSilentMode() {
        return isSilentMode;
    }

    private int getInstanceTasksOffset() {
        return instanceTasksOffset;
    }

    private ArrayList getTaskInfoList() {
        return taskInfoList;
    }

    private Map getTaskHandlers() {
        return taskHandlers;
    }

    private void logInstall(LocalizedMessage mess) {
        instLogger.getLogger().log(mess);
    }

    private boolean isInstallMode() {
        return installMode;
    }

    private void setTaskInfoList(ArrayList list) {
        taskInfoList = list;
    }

    private void setTaskHandlers(Map handlers) {
        taskHandlers = handlers;
    }

    private void setStateAccess(PersistentStateAccess stateAccess) {
        this.stateAccess = stateAccess;
    }

    private void setSilentModeFlag(boolean mode) {
        isSilentMode = mode;
    }

    private void setInstanceTasksOffset(int offset) {
        instanceTasksOffset = offset;
    }

    private void setInstallLogger(InstallLogger logger) {
        instLogger = logger;
    }

    private void setInstallMode(boolean installMode) {
        this.installMode = installMode;
    }

    private int instanceTasksOffset;

    private ArrayList taskInfoList;

    private Map taskHandlers;

    private PersistentStateAccess stateAccess;

    private InstallLogger instLogger;

    private boolean isSilentMode;

    private boolean installMode; // true => Install; false => Uninstall

    private static final String LOC_DR_ERR_TASK_INIT_FAILED = 
        "DR_ERR_TASK_INIT_FAILED";

    private static final String LOC_DR_MSG_TASK_ABORT_REQUESTED = 
        "DR_MSG_TASK_ABORT_REQUESTED";

    private static final String LOC_DR_ERR_TASK_EXECUTE_FAIL = 
        "DR_ERR_TASK_EXECUTE_FAIL";

    private static final String LOC_DR_MSG_TASK_EXECUTE_DONE = 
        "DR_MSG_TASK_EXECUTE_DONE";

    private static final String LOC_DR_MSG_TASK_EXECUTE_SUCCESS = 
        "DR_MSG_TASK_EXECUTE_SUCCESS";

    private static final String LOC_DR_MSG_TASK_LOG_COMBINED_MSG = 
        "DR_MSG_TASK_LOG_COMBINED_MSG";
}
