package org.forgerock.openam.uma.audit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.openam.cts.adapters.JavaBeanAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.sm.datalayer.api.Task;
import org.forgerock.openam.sm.datalayer.api.TaskExecutor;
import org.forgerock.openam.sm.datalayer.impl.tasks.CreateTask;
import org.forgerock.openam.sm.datalayer.impl.tasks.TaskFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Singleton
public class UmaAuditLogger {
    private final TaskFactory taskFactory;
    private TaskExecutor taskDispatcher;
    private JavaBeanAdapter<UmaAuditEntry> umaAuditEntryAdapter;

    @Inject
    public UmaAuditLogger(@DataLayer(ConnectionType.RESOURCE_SETS) TaskExecutor taskDispatcher, @DataLayer
            (ConnectionType.RESOURCE_SETS) TaskFactory taskFactory, JavaBeanAdapter<UmaAuditEntry>
            umaAuditEntryAdapter) {
        this.taskDispatcher = taskDispatcher;
        this.taskFactory = taskFactory;
        this.umaAuditEntryAdapter = umaAuditEntryAdapter;
    }

    public void log(String resourceSetId, String resourceOwnerId, String requestAuthorisation) {
        persist(new UmaAuditEntry(resourceSetId, resourceOwnerId, requestAuthorisation));
    }

    public void persist(UmaAuditEntry auditEntry) {
        Token token = umaAuditEntryAdapter.toToken(auditEntry);
        BlockingQueue<Task> blockingQueue = new ArrayBlockingQueue<Task>(1);
        blockingQueue.add(new CreateTask(token, null));

        try {
            taskDispatcher.execute(token.getTokenId(), taskFactory.create(token, new ResultHandler<Token, Exception>() {
                @Override
                public Token getResults() throws CoreTokenException {
                    return null;
                }

                @Override
                public void processResults(Token result) {

                }

                @Override
                public void processError(Exception error) {

                }
            }));
        } catch (DataLayerException e) {
            e.printStackTrace();
        }
    }

    public List<UmaAuditEntry> getHistory(AMIdentity identity, QueryRequest request) {
        if (request != null) {
            Map<String, Object> filter = request.getQueryFilter().accept(new UmaAuditQueryFilterVisitor(), new
                    HashMap<String, Object>());

        } else {
            //TODO: Get all for this user
        }

        return null;
    }

}
