package org.forgerock.openam.uma.audit;

import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.Field;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.tokens.Type;

import java.util.Calendar;
import java.util.Date;

@Type(TokenType.UMA_AUDIT_ENTRY)
public class UmaAuditEntry {
    @Field(field = CoreTokenField.TOKEN_ID, generated = true)
    private String id;
    @Field(field = CoreTokenField.STRING_ONE)
    private String resourceSetId;
    @Field(field = CoreTokenField.STRING_TWO)
    private String userId;
    @Field(field = CoreTokenField.STRING_THREE)
    private String type;
    @Field(field = CoreTokenField.DATE_ONE)
    private Calendar eventTime;

    public UmaAuditEntry() {
    }

    public UmaAuditEntry(String resourceSetId, String userId, String type) {
        this.resourceSetId = resourceSetId;
        this.userId = userId;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceSetId() {
        return resourceSetId;
    }

    public void setResourceSetId(String resourceSetId) {
        this.resourceSetId = resourceSetId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Calendar getEventTime() {
        return eventTime;
    }

    public void setEventTime(Calendar eventTime) {
        this.eventTime = eventTime;
    }

}
