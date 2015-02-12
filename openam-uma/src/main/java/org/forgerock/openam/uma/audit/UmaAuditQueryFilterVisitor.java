package org.forgerock.openam.uma.audit;

import org.forgerock.json.fluent.JsonPointer;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryFilterVisitor;

import java.util.List;
import java.util.Map;

/**
 * A visitor that returns a Map containing fieldname -> value representing fieldname==value style filters
 * @since 13.0.0
 */
public class UmaAuditQueryFilterVisitor implements QueryFilterVisitor<Map<String, Object>, Map<String, Object>> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitAndFilter(Map<String, Object> attributeValuePairs, List<QueryFilter> subFilters) {
        for (QueryFilter queryFilter : subFilters) {
            attributeValuePairs.putAll(queryFilter.accept(this, attributeValuePairs));
        }

        return attributeValuePairs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitBooleanLiteralFilter(Map<String, Object> stringObjectMap, boolean value) {
        throw unsupportedFilterOperation("Boolean Literal");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitContainsFilter(Map<String, Object> stringObjectMap, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Contains");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitEqualsFilter(Map<String, Object> attributeValuePairs, JsonPointer field, Object valueAssertion) {
        final String fieldName = field.get(0);
        attributeValuePairs.put(fieldName, valueAssertion);
        return attributeValuePairs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitExtendedMatchFilter(Map<String, Object> stringObjectMap, JsonPointer field, String operator, Object valueAssertion) {
        throw unsupportedFilterOperation("Extended match");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitGreaterThanFilter(Map<String, Object> stringObjectMap, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Greater than");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitGreaterThanOrEqualToFilter(Map<String, Object> stringObjectMap, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Greater than or equal to");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitLessThanFilter(Map<String, Object> stringObjectMap, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Less than");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitLessThanOrEqualToFilter(Map<String, Object> stringObjectMap, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Less than or equal to");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitNotFilter(Map<String, Object> stringObjectMap, QueryFilter subFilter) {
        throw unsupportedFilterOperation("Not");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitOrFilter(Map<String, Object> stringObjectMap, List<QueryFilter> subFilters) {
        throw unsupportedFilterOperation("Or");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitPresentFilter(Map<String, Object> stringObjectMap, JsonPointer field) {
        throw unsupportedFilterOperation("Present");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> visitStartsWithFilter(Map<String, Object> stringObjectMap, JsonPointer field, Object valueAssertion) {
        throw unsupportedFilterOperation("Starts with");
    }

    private UnsupportedOperationException unsupportedFilterOperation(String filterType) {
        return new UnsupportedOperationException("'" + filterType + "' not supported");
    }
}
