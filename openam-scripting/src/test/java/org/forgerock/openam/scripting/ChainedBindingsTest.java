package org.forgerock.openam.scripting;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import static org.fest.assertions.Assertions.assertThat;

public class ChainedBindingsTest {

    private Bindings currentScope;
    private Bindings parentScope;

    private ChainedBindings chainedBindings;

    @BeforeMethod
    public void setupTestObjects() {
        currentScope = new SimpleBindings();
        parentScope = new SimpleBindings();

        chainedBindings = new ChainedBindings(parentScope, currentScope);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullCurrentScope() {
        new ChainedBindings(parentScope, null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullParentScope() {
        new ChainedBindings(null, currentScope);
    }

    @Test
    public void shouldSeeVariablesInCurrentScope() {
        // Given
        String varName = "var";
        String value = "test value";
        currentScope.put(varName, value);

        // When
        Object result = chainedBindings.get(varName);

        // Then
        assertThat(result).isEqualTo(value);
    }

    @Test
    public void shouldSeeUnshadowedVariablesInParentScope() {
        // Given
        String varName = "var";
        String value = "test value";
        parentScope.put(varName, value);

        // When
        Object result = chainedBindings.get(varName);

        // Then
        assertThat(result).isEqualTo(value);
    }

    @Test
    public void shouldSeeCurrentScopeVariablesThatShadowParentScope() {
        // Given
        String varName = "var";
        String currentValue = "current value";
        String parentValue = "parent value";
        currentScope.put(varName, currentValue);
        parentScope.put(varName, parentValue);

        // When
        Object result = chainedBindings.get(varName);

        // Then
        assertThat(result).isEqualTo(currentValue);
    }

    @Test
    public void shouldUpdatePreExistingVariablesInCurrentScope() {
        // Given
        String varName = "var";
        currentScope.put(varName, "initial");
        String newValue = "new value";

        // When
        chainedBindings.put(varName, newValue);

        // Then
        assertThat(currentScope.get(varName)).isEqualTo(newValue);
    }

    @Test
    public void shouldUpdateNonShadowedVariablesInParentScope() {
        // Given
        String varName = "var";
        parentScope.put(varName, "initial");
        String newValue = "new value";

        // When
        chainedBindings.put(varName, newValue);

        // Then
        assertThat(parentScope.get(varName)).isEqualTo(newValue);
    }

    @Test
    public void shouldUpdateShadowedVariablesInCurrentScope() {
        // Given
        String varName = "var";
        String newValue = "new value";
        String oldValue = "old value";
        parentScope.put(varName, oldValue);
        currentScope.put(varName, oldValue);

        // When
        chainedBindings.put(varName, newValue);

        // Then - should update variable in *both* scopes
        assertThat(currentScope.get(varName)).isEqualTo(newValue);
        assertThat(parentScope.get(varName)).isEqualTo(newValue);
    }
}