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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.scripting.timeouts;


import java.util.concurrent.TimeUnit;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/**
 * This factory is used to provide contexts to the Rhino JavaScript engine which are responsible for
 * stopping the engine's execution of a script if the provided timeout is configured. The length of
 * time for a timeout is read from the {@link StandardScriptEngineManager}. As a singleton, it can
 * act to indicate to this factory when the server configuration for the scripted auth module
 * has been altered.
 *
 * By overriding the observeInstructionCount function, and setting the instructionObserverThreshold
 * low, the context is able to detect when it has gone over its timelimit. In this case, it will throw
 * a runtime exception, which will prevent any further execution in this engine.
 */
public class ObservedContextFactory extends ContextFactory {

    private static final int OBSERVER_THRESHOLD = 1000;
    private final StandardScriptEngineManager manager;

    public ObservedContextFactory(StandardScriptEngineManager manager) {
        this.manager = manager;
    }

    @Override
    protected Context makeContext() {
        return new ObservedJavaScriptContext(this);
    }

    /**
     * @see org.forgerock.openam.scripting.timeouts.ObservedContextFactory#makeContext()
     */
    @Override
    protected void observeInstructionCount(Context cx, int instructionCount) {
        final ObservedJavaScriptContext context = (ObservedJavaScriptContext) cx;
        final long timeout = TimeUnit.MILLISECONDS.convert(manager.getConfiguration().getScriptExecutionTimeout(),
                TimeUnit.SECONDS);
        if (timeout > 0 && System.currentTimeMillis() - context.getStartTime() > timeout) {
            throw new Error("Interrupt.");
        }
    }

    /**
     * This Context is configured in such a way that we are able to ensure we can determine the
     * length of time that the script has been running. We configure:
     *
     * - The time that the script begins
     * - The optimization level: -1 indicates interpreted mode, which allows us to observe the instructions
     * - The instruction observation threshold which is intentionally set low
     */
    protected static class ObservedJavaScriptContext extends Context {
        private final long startTime;

        public ObservedJavaScriptContext(final ObservedContextFactory factory) {
            super(factory);
            this.startTime = System.currentTimeMillis();
            this.setOptimizationLevel(-1);
            this.setInstructionObserverThreshold(OBSERVER_THRESHOLD);
        }

        public long getStartTime() {
            return startTime;
        }
    }

}