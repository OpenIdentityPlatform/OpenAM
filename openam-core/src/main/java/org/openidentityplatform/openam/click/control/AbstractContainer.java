/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.openidentityplatform.openam.click.control;

import org.openidentityplatform.openam.click.Control;
import org.openidentityplatform.openam.click.util.ClickUtils;
import org.openidentityplatform.openam.click.util.ContainerUtils;
import org.openidentityplatform.openam.click.util.HtmlStringBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a default implementation of the {@link org.openidentityplatform.openam.click.control.Container} interface
 * to make it easier for developers to create their own containers.
 * <p/>
 * Subclasses can override {@link #getTag()} to return a specific HTML element.
 * <p/>
 * The following example shows how to create an HTML <tt>div</tt> element:
 *
 * <pre class="prettyprint">
 * public class Div extends AbstractContainer {
 *
 *     public String getTag() {
 *         // Return the HTML tag
 *         return "div";
 *     }
 * } </pre>
 */
public abstract class AbstractContainer extends AbstractControl implements
        Container {

    // Constants --------------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // Instance Variables -----------------------------------------------------

    /** The list of controls. */
    protected List<Control> controls;

    /** The map of controls keyed by field name. */
    protected Map<String, Control> controlMap;

    // Constructors -----------------------------------------------------------

    /**
     * Create a container with no name defined.
     */
    public AbstractContainer() {
    }

    /**
     * Create a container with the given name.
     *
     * @param name the container name
     */
    public AbstractContainer(String name) {
        super(name);
    }

    // Public Methods ---------------------------------------------------------

    /**
     * @see org.openidentityplatform.openam.click.control.Container#add(Control).
     * <p/>
     * <b>Please note</b>: if the container contains a control with the same name
     * as the given control, that control will be
     * {@link #replace(Control, Control) replaced}
     * by the given control. If a control has no name defined it cannot be replaced.
     *
     * @param control the control to add to the container
     * @return the control that was added to the container
     * @throws IllegalArgumentException if the control is null
     */
    public Control add(Control control) {
        return insert(control, getControls().size());
    }

    /**
     * Add the control to the container at the specified index, and return the
     * added instance.
     * <p/>
     * <b>Please note</b>: if the container contains a control with the same name
     * as the given control, that control will be
     * {@link #replace(Control, Control) replaced}
     * by the given control. If a control has no name defined it cannot be replaced.
     * <p/>
     * <b>Also note</b> if the specified control already has a parent assigned,
     * it will automatically be removed from that parent and inserted into this
     * container.
     *
     * @see org.openidentityplatform.openam.click.control.Container#insert(Control, int)
     *
     * @param control the control to add to the container
     * @param index the index at which the control is to be inserted
     * @return the control that was added to the container
     *
     * @throws IllegalArgumentException if the control is null or if the control
     * and container is the same instance
     *
     * @throws IndexOutOfBoundsException if index is out of range
     * <tt>(index &lt; 0 || index &gt; getControls().size())</tt>
     */
    public Control insert(Control control, int index) {
        // Check if panel already contains the control
        String controlName = control.getName();
        if (controlName != null) {
            // Check if container already contains the control
            Control currentControl = getControlMap().get(control.getName());

            // If container already contains the control do a replace
            if (currentControl != null) {

                // Current control and new control are referencing the same object
                // so we exit early
                if (currentControl == control) {
                    return control;
                }

                // If the two controls are different objects, we remove the current
                // control and add the given control
                return replace(currentControl, control);
            }
        }

        return ContainerUtils.insert(this, control, index, getControlMap());
    }

    /**
     * @seeorg.openidentityplatform.openam.click.control.Container#remove(Control).
     *
     * @param control the control to remove from the container
     * @return true if the control was removed from the container
     * @throws IllegalArgumentException if the control is null
     */
    public boolean remove(Control control) {
        return ContainerUtils.remove(this, control, getControlMap());
    }

    /**
     * Replace the control in the container at the specified index, and return
     * the newly added control.
     *
     * @see org.openidentityplatform.openam.click.control.Container#replace(Control, Control)
     *
     * @param currentControl the control currently contained in the container
     * @param newControl the control to replace the current control contained in
     * the container
     * @return the new control that replaced the current control
     *
     * @deprecated this method was used for stateful pages, which have been deprecated
     *
     * @throws IllegalArgumentException if the currentControl or newControl is
     * null
     * @throws IllegalStateException if the currentControl is not contained in
     * the container
     */
    public Control replace(Control currentControl, Control newControl) {
        int controlIndex = getControls().indexOf(currentControl);
        return ContainerUtils.replace(this, currentControl, newControl,
            controlIndex, getControlMap());
    }

    /**
     * @see org.apache.click.control.Container#getControls().
     *
     * @return the sequential list of controls held by the container
     */
    public List<Control> getControls() {
        if (controls == null) {
            controls = new ArrayList<Control>();
        }
        return controls;
    }

    /**
     * @see org.apache.click.control.Container#getControl(String)
     *
     * @param controlName the name of the control to get from the container
     * @return the named control from the container if found or null otherwise
     */
    public Control getControl(String controlName) {
        if (hasControls()) {
            return getControlMap().get(controlName);
        }
        return null;
    }

    /**
     * @see Container#contains(Control)
     *
     * @param control the control whose presence in this container is to be tested
     * @return true if the container contains the specified control
     */
    public boolean contains(Control control) {
        return getControls().contains(control);
    }

    /**
     * Returns true if this container has existing controls, false otherwise.
     *
     * @return true if the container has existing controls, false otherwise.
     */
    public boolean hasControls() {
        return (controls != null) && !controls.isEmpty();
    }

    /**
     * Return the map of controls where each map's key / value pair will consist
     * of the control name and instance.
     * <p/>
     * Controls added to the container that did not specify a {@link #name},
     * will not be included in the returned map.
     *
     * @return the map of controls
     */
    public Map<String, Control> getControlMap() {
        if (controlMap == null) {
            controlMap = new HashMap<String, Control>();
        }
        return controlMap;
    }

    /**
     * @see org.openidentityplatform.openam.click.control.AbstractControl#getControlSizeEst().
     *
     * @return the estimated rendered control size in characters
     */
    @Override
    public int getControlSizeEst() {
        int size = 20;

        if (getTag() != null && hasAttributes()) {
            size += 20 * getAttributes().size();
        }

        if (hasControls()) {
            size += getControls().size() * size;
        }

        return size;
    }

    /**
     * @see Control#onProcess().
     *
     * @return true to continue Page event processing or false otherwise
     */
    @Override
    public boolean onProcess() {

        boolean continueProcessing = true;

        for (Control control : getControls()) {
            if (!control.onProcess()) {
                continueProcessing = false;
            }
        }

        dispatchActionEvent();

        return continueProcessing;
    }

    /**
     * @see Control#onDestroy()
     */
    @Override
    public void onDestroy() {
        for (Control control : getControls()) {
            try {
                control.onDestroy();
            } catch (Throwable t) {
                ClickUtils.getLogService().error("onDestroy error", t);
            }
        }
    }

   /**
    * @see Control#onInit()
    */
    @Override
    public void onInit() {
        super.onInit();
        for (Control control : getControls()) {
            control.onInit();
        }
    }

   /**
    * @see Control#onRender()
    */
    @Override
    public void onRender() {
        for (Control control : getControls()) {
            control.onRender();
        }
    }

    /**
     * Render the HTML representation of the container and all its child
     * controls to the specified buffer.
     * <p/>
     * If {@link #getTag()} returns null, this method will render only its
     * child controls.
     * <p/>
     * @see org.openidentityplatform.openam.click.control.AbstractControl#render(HtmlStringBuffer)
     *
     * @param buffer the specified buffer to render the control's output to
     */
    @Override
    public void render(HtmlStringBuffer buffer) {

        //If tag is set, render it
        if (getTag() != null) {
            renderTagBegin(getTag(), buffer);
            buffer.closeTag();
            if (hasControls()) {
                buffer.append("\n");
            }
            renderContent(buffer);
            renderTagEnd(getTag(), buffer);

        } else {

            //render only content because no tag is specified
            renderContent(buffer);
        }
    }

    /**
     * Returns the HTML representation of this control.
     * <p/>
     * This method delegates the rendering to the method
     * {@link #render(HtmlStringBuffer)}. The size of buffer
     * is determined by {@link #getControlSizeEst()}.
     *
     * @see Object#toString()
     *
     * @return the HTML representation of this control
     */
    @Override
    public String toString() {
        HtmlStringBuffer buffer = new HtmlStringBuffer(getControlSizeEst());
        render(buffer);
        return buffer.toString();
    }

    // Protected Methods ------------------------------------------------------

    /**
     * @see AbstractControl#renderTagEnd(String, HtmlStringBuffer).
     *
     * @param tagName the name of the tag to close
     * @param buffer the buffer to append the output to
     */
    @Override
    protected void renderTagEnd(String tagName, HtmlStringBuffer buffer) {
        buffer.elementEnd(tagName);
    }

    /**
     * Render this container content to the specified buffer.
     *
     * @param buffer the buffer to append the output to
     */
    protected void renderContent(HtmlStringBuffer buffer) {
        renderChildren(buffer);
    }

    /**
     * Render this container children to the specified buffer.
     *
     * @see #getControls()
     *
     * @param buffer the buffer to append the output to
     */
    protected void renderChildren(HtmlStringBuffer buffer) {
        for (Control control : getControls()) {

            int before = buffer.length();
            control.render(buffer);

            int after = buffer.length();
            if (before != after) {
                buffer.append("\n");
            }
        }
    }
}
