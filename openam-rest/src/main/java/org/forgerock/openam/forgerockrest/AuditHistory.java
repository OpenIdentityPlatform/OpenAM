package org.forgerock.openam.forgerockrest;

import com.google.inject.Inject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.RouterContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.uma.audit.UmaAuditLogger;

public class AuditHistory implements CollectionResourceProvider {

    private final UmaAuditLogger auditLogger;

    @Inject
    public AuditHistory(UmaAuditLogger datastore) {
        this.auditLogger = datastore;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        AMIdentity identity = getIdentity(context);
        handler.handleResult(new JsonValue(auditLogger.getHistory(identity, null)));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        handler.handleError(new NotSupportedException("Not supported."));
//        AMIdentity identity = getIdentity(context);
//        List<UmaAuditEntry> history = auditLogger.getHistory(identity, request);
//
//        final QueryResultHandler resultHandler = new QueryResultHandlerBuilder(handler)
//                .withPaging(request.getPageSize(), request.getPagedResultsOffset()).build();

        //TODO - finish this..
//        for (UmaAuditEntry entry : history) {
//            resultHandler.handleResource(new Resource(entry.getId(), null, new JsonValue()))
//        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException("Not supported."));
    }

    private AMIdentity getIdentity(ServerContext context) {
        String realm = context.asContext(RealmContext.class).getResolvedRealm();
        final String user = context.asContext(RouterContext.class).getUriTemplateVariables().get("user");
        return IdUtils.getIdentity(user, realm);
    }
}
