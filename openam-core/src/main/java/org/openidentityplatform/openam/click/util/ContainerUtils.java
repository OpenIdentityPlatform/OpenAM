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

package org.openidentityplatform.openam.click.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ognl.DefaultTypeConverter;
import ognl.Ognl;
import ognl.OgnlOps;

import org.openidentityplatform.openam.click.Control;
import org.openidentityplatform.openam.click.Page;
import org.apache.click.control.Button;
import org.openidentityplatform.openam.click.control.Container;
import org.openidentityplatform.openam.click.control.Field;
import org.apache.click.control.FieldSet;
import org.openidentityplatform.openam.click.control.Form;
import org.apache.click.control.Label;
import org.apache.click.service.LogService;
import org.apache.click.util.ClickUtils;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.click.util.PropertyUtils;
import org.apache.commons.lang.ClassUtils;

/**
 * Provides Container access and copy utilities.
 */
public class ContainerUtils {

    /**
     * Populate the given object attributes from the Containers field values.
     * <p/>
     * If a Field and object attribute matches, the object attribute is set to
     * the Object returned from the method
     * {@link org.apache.click.control.Field#getValueObject()}. If an object
     * attribute is a primitive, the Object returned from
     * {@link org.apache.click.control.Field#getValueObject()} will be converted
     * into the specific primitive e.g. Integer will become int and Boolean will
     * become boolean.
     * <p/>
     * The fieldList specifies which fields to copy to the object. This allows
     * one to include or exclude certain Container fields before populating the
     * object.
     * <p/>
     * The following example shows how to exclude disabled fields from
     * populating a customer object:
     * <pre class="prettyprint">
     * public void onInit() {
     *     List formFields = new ArrayList();
     *     for(Iterator it = form.getFieldList().iterator(); it.hasNext(); ) {
     *         Field field = (Field) formFields.next();
     *         // Exclude disabled fields
     *         if (!field.isDisabled()) {
     *             formFields.add(field);
     *         }
     *     }
     *     Customer customer = new Customer();
     *     ContainerUtils.copyContainerToObject(form, customer, formFields);
     * }
     * </pre>
     *
     * The specified Object can either be a POJO (plain old java object) or
     * a {@link java.util.Map}. If a POJO is specified, its attributes are
     * populated from  matching container fields. If a map is specified, its
     * key/value pairs are populated from matching container fields.
     *
     * @param container the fieldList Container
     * @param object the object to populate with field values
     * @param fieldList the list of fields to obtain values from
     *
     * @throws IllegalArgumentException if container, object or fieldList is
     * null
     */
    public static void copyContainerToObject(Container container,
                                             Object object, List<Field> fieldList) {

        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        if (object == null) {
            throw new IllegalArgumentException("Null object parameter");
        }

        if (fieldList == null) {
            throw new IllegalArgumentException("Null fieldList parameter");
        }

        if (fieldList.isEmpty()) {
            LogService logService = org.apache.click.util.ClickUtils.getLogService();
            if (logService.isDebugEnabled()) {
                String containerClassName =
                        ClassUtils.getShortClassName(container.getClass());
                logService.debug("   " + containerClassName
                        + " has no fields to copy from");
            }
            //Exit early.
            return;
        }

        String objectClassname = object.getClass().getName();
        objectClassname =
                objectClassname.substring(objectClassname.lastIndexOf(".") + 1);

        // If the given object is a map, its key/value pair is populated from
        // the fields name/value pair.
        if (object instanceof Map<?, ?>) {
            copyFieldsToMap(fieldList, (Map) object);
            // Exit after populating the map.
            return;
        }

        LogService logService = org.apache.click.util.ClickUtils.getLogService();

        Set<String> properties = getObjectPropertyNames(object);
        Map<?, ?> ognlContext = Ognl.createDefaultContext(
                object, null, new ContainerUtils.FixBigDecimalTypeConverter(), null);

        for (Field field : fieldList) {

            // Ignore disabled field as their values are not submitted in HTML
            // forms
            if (field.isDisabled()) {
                continue;
            }

            if (!hasMatchingProperty(field, properties)) {
                continue;
            }

            String fieldName = field.getName();

            ensureObjectPathNotNull(object, fieldName);

            try {
                PropertyUtils.setValueOgnl(object, fieldName, field.getValueObject(), ognlContext);

                if (logService.isDebugEnabled()) {
                    String containerClassName =
                            ClassUtils.getShortClassName(container.getClass());
                    String msg = "    " + containerClassName + " -> "
                            + objectClassname + "." + fieldName + " : "
                            + field.getValueObject();

                    logService.debug(msg);
                }

            } catch (Exception e) {
                String msg =
                        "Error incurred invoking " + objectClassname + "."
                                + fieldName + " with " + field.getValueObject()
                                + " error: " + e.toString();

                logService.debug(msg);
            }
        }
    }

    /**
     * Populate the given object attributes from the Containers field values.
     *
     * @see #copyContainerToObject(org.openidentityplatform.openam.click.control.Container, java.lang.Object, java.util.List)
     *
     * @param container the Container to obtain field values from
     * @param object the object to populate with field values
     */
    public static void copyContainerToObject(Container container,
                                             Object object) {
        List<Field> fieldList = getInputFields(container);
        copyContainerToObject(container, object, fieldList);
    }

    /**
     * Populate the given Container field values from the object attributes.
     * <p/>
     * If a Field and object attribute matches, the Field value is set to the
     * object attribute using the method
     * {@link org.apache.click.control.Field#setValueObject(java.lang.Object)}. If
     * an object attribute is a primitive it is first converted to its proper
     * wrapper class e.g. int will become Integer and boolean will become
     * Boolean.
     * <p/>
     * The fieldList specifies which fields to populate from the object. This
     * allows one to exclude or include specific fields.
     * <p/>
     * The specified Object can either be a POJO (plain old java object) or
     * a {@link java.util.Map}. If a POJO is specified, its attributes are
     * copied to matching container fields. If a map is specified, its key/value
     * pairs are copied to matching container fields.
     *
     * @param object the object to obtain attribute values from
     * @param container the Container to populate
     * @param fieldList the list of fields to populate from the object
     * attributes
     */
    public static void copyObjectToContainer(Object object,
                                             Container container, List<Field> fieldList) {
        if (object == null) {
            throw new IllegalArgumentException("Null object parameter");
        }

        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        if (container == null) {
            throw new IllegalArgumentException("Null fieldList parameter");
        }

        if (fieldList.isEmpty()) {
            LogService logService = org.apache.click.util.ClickUtils.getLogService();
            if (logService.isDebugEnabled()) {
                String containerClassName =
                        ClassUtils.getShortClassName(container.getClass());
                logService.debug("   " + containerClassName
                        + " has no fields to copy to");
            }
            //Exit early.
            return;
        }

        String objectClassname = object.getClass().getName();
        objectClassname =
                objectClassname.substring(objectClassname.lastIndexOf(".") + 1);

        //If the given object is a map, populate the fields name/value from
        //the maps key/value pair.
        if (object instanceof Map<?, ?>) {

            copyMapToFields((Map) object, fieldList);
            //Exit after populating the fields.
            return;
        }

        Set<String> properties = getObjectPropertyNames(object);

        LogService logService = org.apache.click.util.ClickUtils.getLogService();

        for (Field field : fieldList) {

            if (!hasMatchingProperty(field, properties)) {
                continue;
            }

            String fieldName = field.getName();
            try {
                Object result = PropertyUtils.getValue(object, fieldName);

                field.setValueObject(result);

                if (logService.isDebugEnabled()) {
                    String containerClassName =
                            ClassUtils.getShortClassName(container.getClass());
                    String msg = "    " + containerClassName + " <- "
                            + objectClassname + "." + fieldName + " : "
                            + result;
                    logService.debug(msg);
                }

            } catch (Exception e) {
                String msg = "Error incurred invoking " + objectClassname + "."
                        + fieldName + " error: " + e.toString();

                logService.debug(msg);
            }
        }
    }

    /**
     * Populate the given Container field values from the object attributes.
     *
     * @see #copyObjectToContainer(java.lang.Object, org.openidentityplatform.openam.click.control.Container, java.util.List)
     *
     * @param object the object to obtain attribute values from
     * @param container the Container to populate
     */
    public static void copyObjectToContainer(Object object,
                                             Container container) {

        List<Field> fieldList = getInputFields(container);
        copyObjectToContainer(object, container, fieldList);
    }

    /**
     * Find and return the first control with a matching name in the specified
     * container.
     * <p/>
     * If no matching control is found in the specified container, child
     * containers will be recursively scanned for a match.
     *
     * @param container the container that is searched for a control with a
     * matching name
     * @param name the name of the control to find
     * @return the control which name matched the given name
     */
    public static Control findControlByName(Container container, String name) {
        Control control = container.getControl(name);

        if (control != null) {
            return control;

        } else {
            for (Control childControl : container.getControls()) {

                if (childControl instanceof Container) {
                    Container childContainer = (Container) childControl;
                    Control found = findControlByName(childContainer, name);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find and return the specified controls parent Form or null
     * if no Form is present.
     *
     * @param control the control to check for Form
     * @return the controls parent Form or null if no parent is a Form
     */
    public static Form findForm(Control control) {
        while (control.getParent() != null && !(control.getParent() instanceof Page)) {
            control = (Control) control.getParent();
            if (control instanceof Form) {
                return (Form) control;
            }
        }
        return null;
    }

    /**
     * Return the list of Buttons for the given Container, recursively including
     * any Fields contained in child containers.
     *
     * @param container the container to obtain the buttons from
     * @return the list of contained buttons
     */
    public static List<Button> getButtons(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        List<Button> buttons = new ArrayList<Button>();
        addButtons(container, buttons);
        return buttons;
    }

    /**
     * Return a list of container fields which are not valid, not hidden and not
     * disabled.
     * <p/>
     * The list of returned fields will exclude any <tt>Button</tt> fields.
     *
     * @param container the container to obtain the invalid fields from
     * @return list of container fields which are not valid, not hidden and not
     * disabled
     */
    public static List<Field> getErrorFields(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        List<Field> fields = new ArrayList<Field>();
        addErrorFields(container, fields);
        return fields;
    }

    /**
     * Return a map of all Fields for the given Container, recursively including
     * any Fields contained in child containers.
     * <p/>
     * The map's key / value pair will consist of the control name and instance.
     *
     * @param container the container to obtain the fields from
     * @return the map of contained fields
     */
    public static Map<String, Field> getFieldMap(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        Map<String, Field> fields = new HashMap<String, Field>();
        addFields(container, fields);
        return fields;
    }

    /**
     * Return the list of Fields for the given Container, recursively including
     * any Fields contained in child containers.
     *
     * @param container the container to obtain the fields from
     * @return the list of contained fields
     */
    public static List<Field> getFields(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        List<Field> fields = new ArrayList<Field>();
        addFields(container, fields);
        return fields;
    }

    /**
     * Return the list of Fields for the given Container, recursively including
     * any Fields contained in child containers. The list of returned fields
     * will exclude any <tt>Button</tt> and <tt>FieldSet</tt> fields.
     *
     * @param container the container to obtain the fields from
     * @return the list of contained fields
     */
    public static List<Field> getFieldsAndLabels(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        List<Field> fields = new ArrayList<Field>();
        addFieldsAndLabels(container, fields);
        return fields;
    }

    /**
     * Return the list of hidden Fields for the given Container, recursively including
     * any Fields contained in child containers. The list of returned fields
     * will exclude any <tt>Button</tt>, <tt>FieldSet</tt> and <tt>Label</tt>
     * fields.
     *
     * @param container the container to obtain the fields from
     * @return the list of contained fields
     */
    public static List<Field> getHiddenFields(final Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        List<Field> fields = new ArrayList<Field>();
        addHiddenFields(container, fields);
        return fields;
    }

    /**
     * Return the list of input Fields (TextField, Select, Radio, Checkbox etc).
     * for the given Container, recursively including any Fields contained in
     * child containers. The list of returned fields will exclude any
     * <tt>Button</tt>, <tt>FieldSet</tt> and <tt>Label</tt> fields.
     *
     * @param container the container to obtain the fields from
     * @return the list of contained fields
     */
    public static List<Field> getInputFields(final Container container) {
        if (container == null) {
            throw new IllegalArgumentException("Null container parameter");
        }

        List<Field> fields = new ArrayList<Field>();
        addInputFields(container, fields);
        return fields;
    }

    /**
     * Add the given control to the container at the specified index, and return
     * the added instance.
     * <p/>
     * <b>Please note</b>: an exception is raised if the container contains a
     * control with the same name as the given control. It is the responsibility
     * of the caller to replace existing controls.
     * <p/>
     * <b>Also note</b> if the specified control already has a parent assigned,
     * it will automatically be removed from that parent and inserted as a child
     * of the container instead.
     * <p/>
     * This method is useful for developers needing to implement the
     * {@link org.apache.click.control.Container} interface but cannot for one
     * reason or another extend from {@link org.apache.click.control.AbstractContainer}.
     * For example if the Container already extends from an existing <tt>Control</tt>
     * such as a <tt>Field</tt>, it won't be possible to extend
     * <tt>AbstractContainer</tt> as well. In such scenarios instead of
     * reimplementing {@link org.apache.click.control.Container#insert(org.apache.click.Control, int) insert},
     * one can delegate to this method.
     * <p/>
     * For example, a custom Container that extends <tt>Field</tt> and
     * implements <tt>Container</tt> could implement the <tt>insert</tt> method
     * as follows:
     * <pre class="prettyprint">
     * public class MyContainer extends Field implements Container {
     *
     *     public Control insert(Control control, int index) {
     *         return ContainerUtils.insert(this, control, index, getControlMap());
     *     }
     *
     *     ...
     * } </pre>
     *
     * @param container the container to insert the given control into
     * @param control the control to add to the container
     * @param index the index at which the control is to be inserted
     * @param controlMap the container's map of controls keyed on control name
     * @return the control that was added to the container
     *
     * @throws IllegalArgumentException if the control is null or if the control
     * and container is the same instance
     *
     * @throws IndexOutOfBoundsException if index is out of range
     * <tt>(index &lt; 0 || index &gt; container.getControls().size())</tt>
     */
    public static Control insert(Container container, Control control, int index,
                                 Map<String, Control> controlMap) {

        // Pre conditions start
        if (control == null) {
            throw new IllegalArgumentException("Null control parameter");
        }
        if (control == container) {
            throw new IllegalArgumentException("Cannot add container to itself");
        }
        int size = container.getControls().size();
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: "
                    + size);
        }
        // Check if container already contains the control
        if (controlMap.containsKey(control.getName())
                && !(control instanceof Label)) {

            throw new IllegalArgumentException(
                    "Container already contains control named: " + control.getName());
        }

        // Pre conditions end

        // Check if control already has parent
        // If parent references the given container, there is no need to remove it
        Object currentParent = control.getParent();
        if (currentParent != null && currentParent != container) {

            // Remove control from parent Page or Container
            if (currentParent instanceof Page) {
                ((Page) currentParent).removeControl(control);

            } else if (currentParent instanceof Container) {
                ((Container) currentParent).remove(control);
            }

            // Create warning message to users that the parent has been reset
            logParentReset(container, control, currentParent);
        }

        // Note: set parent first since setParent might veto further processing
        control.setParent(container);
        container.getControls().add(index, control);

        String controlName = control.getName();
        if (controlName != null) {
            controlMap.put(controlName, control);
        }
        return control;
    }

    /**
     * Replace the current control in the container at the specified index, and
     * return the newly added control.
     * <p/>
     * <b>Please note</b> if the new control already has a parent assigned,
     * it will automatically be removed from that parent and inserted as a child
     * of the container instead.
     * <p/>
     * This method is useful for developers needing to implement the
     * {@link org.apache.click.control.Container} interface but cannot for one
     * reason or another extend from {@link org.apache.click.control.AbstractContainer}.
     * For example if the Container already extends from an existing <tt>Control</tt>
     * such as a <tt>Field</tt>, it won't be possible to extend
     * <tt>AbstractContainer</tt> as well. In such scenarios instead of
     * reimplementing {@link org.apache.click.control.Container#replace(org.apache.click.Control, org.apache.click.Control) replace},
     * one can delegate to this method.
     * <p/>
     * For example, a custom Container that extends <tt>Field</tt> and
     * implements <tt>Container</tt> could implement the <tt>replace</tt> method
     * as follows:
     *
     * <pre class="prettyprint">
     * public class MyContainer extends Field implements Container {
     *
     *     public Control replace(Control currentControl, Control newControl) {
     *         int controlIndex = getControls().indexOf(currentControl);
     *         return ContainerUtils.replace(this, currentControl, newControl,
     *             controlIndex, getControlMap());
     *     }
     *
     *     ...
     * } </pre>
     *
     * @param container the container to insert the new control into
     * @param currentControl the control currently contained in the container
     * @param newControl the control to replace the current control contained in
     * the container
     * @param controlIndex the index of the current control in the container
     * @param controlMap the container's map of controls keyed on control name
     * @return the new control that replaced the current control
     *
     * @deprecated this method was used for stateful pages, which have been deprecated
     *
     * @throws IllegalArgumentException if the currentControl or newControl is
     * null
     * @throws IllegalStateException if the controlIndex = -1
     */
    public static Control replace(Container container, Control currentControl,
                                  Control newControl, int controlIndex, Map<String, Control> controlMap) {

        // Pre conditions start

        // Current and new control is the same instance - exit early
        if (currentControl == newControl) {
            return newControl;
        }

        if (currentControl == null) {
            throw new IllegalArgumentException("Null current control parameter");
        }
        if (newControl == null) {
            throw new IllegalArgumentException("Null new control parameter");
        }

        if (controlIndex == -1) {
            throw new IllegalStateException("Cannot replace the given control"
                    + " because it is not present in the container");
        }

        // Pre conditions end

        // Check if control already has parent
        // If parent references the given container, there is no need to remove it
        Object currentParent = newControl.getParent();
        if (currentParent != null && currentParent != container) {

            // Remove new control from parent Page or Container
            if (currentParent instanceof Page) {
                ((Page) currentParent).removeControl(newControl);

            } else if (currentParent instanceof Container) {
                ((Container) currentParent).remove(newControl);
            }

            // Create warning message to users that the parent has been reset
            logParentReset(container, newControl, currentParent);
        }

        // Note: set parent first since setParent might veto further processing
        newControl.setParent(container);
        currentControl.setParent(null);

        // Replace currentControl with newControl
        container.getControls().set(controlIndex, newControl);

        // Update controlMap
        String controlName = newControl.getName();
        if (controlName != null) {
            controlMap.put(controlName, newControl);
        } else {
            controlName = currentControl.getName();

            if (controlName != null) {
                controlMap.remove(controlName);
            }
        }
        return newControl;
    }

    /**
     * Remove the given control from the container, returning <tt>true</tt> if
     * the control was found in the container and removed, or <tt>false</tt> if
     * the control was not found.
     * <p/>
     * This method is useful for developers needing to implement the
     * {@link org.apache.click.control.Container} interface but cannot for one
     * reason or another extend from {@link org.apache.click.control.AbstractContainer}.
     * For example if the Container already extends from an existing <tt>Control</tt>
     * such as a <tt>Field</tt>, it won't be possible to extend
     * <tt>AbstractContainer</tt> as well. In such scenarios instead of
     * reimplementing {@link org.apache.click.control.Container#remove(org.apache.click.Control) remove},
     * one can delegate to this method.
     * <p/>
     * For example, a custom Container that extends <tt>Field</tt> and
     * implements <tt>Container</tt> could implement the <tt>remove</tt> method
     * as follows:
     * <pre class="prettyprint">
     * public class MyContainer extends Field implements Container {
     *
     *     public boolean remove (Control control) {
     *         return ContainerUtils.remove(this, control, getControlMap());
     *     }
     *
     *     ...
     * } </pre>
     *
     * @param container the container to remove the given control from
     * @param control the control to remove from the container
     * @param controlMap the container's map of controls keyed on control name
     *
     * @return true if the control was removed from the container
     * @throws IllegalArgumentException if the control is null
     */
    public static boolean remove(Container container, Control control,
                                 Map<String, Control> controlMap) {

        if (control == null) {
            throw new IllegalArgumentException("Control cannot be null");
        }

        boolean contains = container.getControls().remove(control);

        if (contains) {
            // Only nullify if the container is parent. This check is for the
            // case where a Control has two parents e.g. Page and Form.
            // NOTE the current #insert logic does not allow Controls to have
            // two parents so this check might be redundant.
            if (control.getParent() == container) {
                control.setParent(null);
            }

            String controlName = control.getName();

            if (controlName != null) {
                controlMap.remove(controlName);
            }
        }

        return contains;
    }

    // -------------------------------------------------------- Private Methods

    /**
     * Extract and return the specified object property names.
     * <p/>
     * If the object is a Map instance, this method returns the maps key set.
     *
     * @param object the object to extract property names from
     * @return the unique set of property names
     */
    private static Set<String> getObjectPropertyNames(Object object) {
        if (object instanceof Map) {
            return ((Map) object).keySet();
        }

        Set<String> hashSet = new TreeSet<String>();

        Method[] methods = object.getClass().getMethods();

        for (Method method : methods) {
            String methodName = method.getName();

            if (methodName.startsWith("get") && methodName.length() > 3) {
                String propertyName =
                        Character.toLowerCase(methodName.charAt(3))
                                + methodName.substring(4);
                hashSet.add(propertyName);
            }
            if (methodName.startsWith("is") && methodName.length() > 2) {
                String propertyName =
                        Character.toLowerCase(methodName.charAt(2))
                                + methodName.substring(3);
                hashSet.add(propertyName);
            }
            if (methodName.startsWith("set") && methodName.length() > 3) {
                String propertyName =
                        Character.toLowerCase(methodName.charAt(3))
                                + methodName.substring(4);
                hashSet.add(propertyName);
            }
        }

        return hashSet;
    }

    /**
     * Return true if the specified field name is contained within the
     * specified set of properties.
     *
     * @param field the field which name should be checked
     * @param properties set of properties to check
     * @return true if the specified field name is contained in the properties,
     * false otherwise
     */
    private static boolean hasMatchingProperty(Field field, Set<String> properties) {
        String fieldName = field.getName();
        if (fieldName.indexOf(".") != -1) {
            fieldName = fieldName.substring(0, fieldName.indexOf("."));
        }
        return properties.contains(fieldName);
    }

    /**
     * This method ensures that the object can safely be navigated according
     * to the specified path.
     * <p/>
     * If any object in the graph is null, a new instance of that object class
     * is instantiated.
     *
     * @param object the object which path must be navigable without
     * encountering null values
     * @param path the navigation path
     */
    private static void ensureObjectPathNotNull(Object object, String path) {

        final int index = path.indexOf('.');

        if (index == -1) {
            return;
        }

        String property = path.substring(0, index);
        Method getterMethod = findGetter(object, property, path);
        Object result = invokeGetter(getterMethod, object, property, path);

        if (result == null) {
            // Find the target class of the object in the path to create
            Class<?> targetClass = getterMethod.getReturnType();

            Constructor<?> constructor = null;
            try {
                // Lookup default no-arg constructor
                constructor = targetClass.getConstructor((Class[]) null);

            } catch (NoSuchMethodException e) {
                // Log detailed error message of looking up constructor failed
                org.apache.click.util.HtmlStringBuffer buffer = new org.apache.click.util.HtmlStringBuffer();
                logBasicDescription(buffer, object, path, property);
                buffer.append("Attempt to construct instance of class '");
                buffer.append(targetClass.getName()).append("' resulted in error: '");
                buffer.append(targetClass.getName()).append("' does not seem");
                buffer.append(" to have a default no argument constructor.");
                buffer.append(" Please note another common problem is that the");
                buffer.append(" class is either not public or not static.");
                throw new RuntimeException(buffer.toString(), e);
            }

            try {
                // Create target object instance
                result = constructor.newInstance(new Object[]{});

            } catch (Exception e) {
                // Log detailed error message of why creating target failed
                org.apache.click.util.HtmlStringBuffer buffer = new org.apache.click.util.HtmlStringBuffer();
                logBasicDescription(buffer, object, path, property);
                buffer.append("Result: could not create");
                buffer.append(" object with constructor '");
                buffer.append(constructor.getName()).append("'.");
                throw new RuntimeException(buffer.toString(), e);
            }

            Method setterMethod = findSetter(object, property, targetClass, path);
            invokeSetter(setterMethod, object, result, property, path);
        }

        String remainingPath = path.substring(index + 1);

        ensureObjectPathNotNull(result, remainingPath);
    }

    /**
     * Find the object getter method for the given property.
     * <p/>
     * If this method cannot find a 'get' property it tries to lookup an 'is'
     * property.
     *
     * @param object the object to find the getter method on
     * @param property the getter property name specifying the getter to lookup
     * @param path the full expression path (used for logging purposes)
     * @return the getter method
     */
    private static Method findGetter(Object object, String property,
                                     String path) {

        // Find the getter for property
        String getterName = org.apache.click.util.ClickUtils.toGetterName(property);

        Method method = null;
        Class<?> sourceClass = object.getClass();

        try {
            method = sourceClass.getMethod(getterName, (Class[]) null);
        } catch (Exception e) {
        }

        if (method == null) {
            String isGetterName = org.apache.click.util.ClickUtils.toIsGetterName(property);
            try {
                method = sourceClass.getMethod(isGetterName, (Class[]) null);
            } catch (Exception e) {
                org.apache.click.util.HtmlStringBuffer buffer = new org.apache.click.util.HtmlStringBuffer();
                logBasicDescription(buffer, object, path, property);
                buffer.append("Result: neither getter methods '");
                buffer.append(getterName).append("()' nor '");
                buffer.append(isGetterName).append("()' was found on class: '");
                buffer.append(object.getClass().getName()).append("'.");
                throw new RuntimeException(buffer.toString(), e);
            }
        }
        return method;
    }

    /**
     * Invoke the getterMethod for the given source object.
     *
     * @param getterMethod the getter method to invoke
     * @param source the source object to invoke the getter method on
     * @param property the getter method property name (used for logging)
     * @param path the full expression path (used for logging)
     * @return the getter result
     */
    private static Object invokeGetter(Method getterMethod, Object source,
                                       String property, String path) {

        try {
            // Retrieve target object from getter
            return getterMethod.invoke(source, new Object[0]);

        } catch (Exception e) {
            // Log detailed error message of why getter failed
            org.apache.click.util.HtmlStringBuffer buffer = new org.apache.click.util.HtmlStringBuffer();
            logBasicDescription(buffer, source, path, property);
            buffer.append("Result: error occurred while trying to get");
            buffer.append(" instance of '");
            buffer.append(getterMethod.getReturnType().getName());
            buffer.append("' using method: '");
            buffer.append(getterMethod.getName()).append("()' of class '");
            buffer.append(source.getClass().getName()).append("'.");
            throw new RuntimeException(buffer.toString(), e);
        }
    }

    /**
     * Find the source object setter method for the given property.
     *
     * @param source the source object to find the setter method on
     * @param property the property which setter needs to be looked up
     * @param targetClass the setter parameter type
     * @param path the full expression path (used for logging purposes)
     * @return the setter method
     */
    private static Method findSetter(Object source,
                                     String property, Class<?> targetClass, String path) {
        Method method = null;

        // Find the setter for property
        String setterName = org.apache.click.util.ClickUtils.toSetterName(property);

        Class<?> sourceClass = source.getClass();
        Class<?>[] classArgs = { targetClass };
        try {
            method = sourceClass.getMethod(setterName, classArgs);
        } catch (Exception e) {
            // Log detailed error message of why setter lookup failed
            org.apache.click.util.HtmlStringBuffer buffer = new org.apache.click.util.HtmlStringBuffer();
            logBasicDescription(buffer, source, path, property);
            buffer.append("Result: setter method '");
            buffer.append(setterName).append("(").append(targetClass.getName());
            buffer.append(")' was not found on class '");
            buffer.append(source.getClass().getName()).append("'.");
            throw new RuntimeException(buffer.toString(), e);
        }
        return method;
    }

    /**
     * Invoke the setter method for the given source and target object.
     *
     * @param setterMethod the setter method to invoke
     * @param source the source object to invoke the setter method on
     * @param target the target object to set
     * @param property the setter method property name (used for logging)
     * @param path the full expression path (used for logging)
     */
    private static void invokeSetter(Method setterMethod, Object source,
                                     Object target, String property, String path) {

        try {
            Object[] objectArgs = {target};
            setterMethod.invoke(source, objectArgs);

        } catch (Exception e) {
            // Log detailed error message of why setter failed
            org.apache.click.util.HtmlStringBuffer buffer = new org.apache.click.util.HtmlStringBuffer();
            logBasicDescription(buffer, source, path, property);
            buffer.append("Result: error occurred while trying to set an");
            buffer.append(" instance of '");
            buffer.append(target.getClass().getName()).append("' using method '");
            buffer.append(setterMethod.getName()).append("(");
            buffer.append(target.getClass());
            buffer.append(")' of class '").append(source.getClass()).append("'.");
            throw new RuntimeException(buffer.toString(), e);
        }
    }

    /**
     * Log a generic error message to the specified buffer for the given object,
     * path and property.
     *
     * @param buffer the buffer to append log message to
     * @param object the active object when the exception occurred
     * @param path the current expression path
     * @param property the current property being processed
     */
    private static void logBasicDescription(org.apache.click.util.HtmlStringBuffer buffer, Object object,
                                            String path, String property) {
        buffer.append("Invoked ensureObjectPathNotNull");
        buffer.append(" for class: '").append(object.getClass().getName());
        buffer.append("', path: '").append(path).append("' and property: '");
        buffer.append(property).append("'. ");
    }

    /**
     * Populate the given map from the values of the specified fieldList. The
     * map's key/value pairs are populated from the fields name/value. The keys
     * of the map are matched against each field name. If a key matches a field
     * name, the value of the field will be copied to the map.
     *
     * @param fieldList the forms list of fields to obtain field values from
     * @param map the map to populate with field values
     */
    private static void copyFieldsToMap(List<Field> fieldList, Map<String, Object> map) {

        LogService logService = org.apache.click.util.ClickUtils.getLogService();

        String objectClassname = map.getClass().getName();
        objectClassname =
                objectClassname.substring(objectClassname.lastIndexOf(".") + 1);

        for (Field field : fieldList) {

            // Check if the map contains the fields name. The fields name can
            // also be a path for example 'foo.bar'
            String fieldName = field.getName();
            if (map.containsKey(fieldName)) {

                map.put(fieldName, field.getValueObject());

                if (logService.isDebugEnabled()) {
                    String msg = "   Form -> " + objectClassname + "."
                            + fieldName + " : " + field.getValueObject();

                    logService.debug(msg);
                }
            }
        }
    }

    /**
     * Copy the map values to the specified fieldList. For every field in the
     * field list, a lookup is done in the map for a matching value. A match is
     * found if a field name matches against a key in the map. The matching
     * value is then copied to the field.
     *
     * @param map the map containing values to populate the fields with
     * @param fieldList the forms list of fields to be populated
     */
    private static void copyMapToFields(Map<String, Object> map, List<Field> fieldList) {

        LogService logService = org.apache.click.util.ClickUtils.getLogService();

        String objectClassname = map.getClass().getName();
        objectClassname =
                objectClassname.substring(objectClassname.lastIndexOf(".") + 1);

        for (Field field : fieldList) {
            String fieldName = field.getName();

            // Check if the fieldName is contained in the map. For
            // example if a field has the name 'user.address', check if
            // 'user.address' is contained in the map.
            if (map.containsKey(fieldName)) {

                Object result = map.get(fieldName);

                field.setValueObject(result);

                if (logService.isDebugEnabled()) {
                    String msg = "   Form <- " + objectClassname + "."
                            + fieldName + " : " + result;
                    logService.debug(msg);
                }
            }
        }
    }

    /**
     * Add buttons for the given Container to the specified buttons list,
     * recursively including any Fields contained in child containers. The list
     * of returned buttons will exclude any <tt>Button</tt> or <tt>Label</tt>
     * fields.
     *
     * @param container the container to obtain the fields from
     * @param buttons the list of contained fields
     */
    private static void addButtons(final Container container, final List<Button> buttons) {
        for (Control control : container.getControls()) {

            if (control instanceof Container) {
                // Include buttons that are containers
                if (control instanceof Button) {
                    buttons.add((Button) control);
                }
                Container childContainer = (Container) control;
                addButtons(childContainer, buttons);

            } else if (control instanceof Button) {
                buttons.add((Button) control);
            }
        }
    }

    /**
     * Add fields for the given Container to the specified field list,
     * recursively including any Fields contained in child containers.
     *
     * @param container the container to obtain the fields from
     * @param fields the list of contained fields
     */
    private static void addFields(final Container container, final List<Field> fields) {
        for (Control control : container.getControls()) {

            if (control instanceof Container) {
                // Include fields that are containers
                if (control instanceof Field) {
                    fields.add((Field) control);
                }
                Container childContainer = (Container) control;
                addFields(childContainer, fields);

            } else if (control instanceof Field) {
                fields.add((Field) control);
            }
        }
    }

    /**
     * Add input fields (TextField, TextArea, Select, Radio, Checkbox etc.) for
     * the given Container to the specified field list, recursively including
     * any Fields contained in child containers. The list of returned fields
     * will exclude any <tt>Button</tt>, <tt>FieldSet</tt> and <tt>Label</tt>
     * fields.
     *
     * @param container the container to obtain the fields from
     * @param fields the list of contained fields
     */
    private static void addInputFields(final Container container, final List<Field> fields) {
        for (Control control : container.getControls()) {

            if (control instanceof Label || control instanceof Button) {
                // Skip buttons and labels
                continue;

            } else if (control instanceof Container) {
                // Include fields but skip fieldSets
                if (control instanceof Field && !(control instanceof FieldSet)) {
                    fields.add((Field) control);
                }
                Container childContainer = (Container) control;
                addInputFields(childContainer, fields);

            } else if (control instanceof Field) {
                fields.add((Field) control);
            }
        }
    }

    /**
     * Add hidden fields for the given Container to the specified field list,
     * recursively including any Fields contained in child containers. The list
     * of returned fields will exclude any <tt>Button</tt>, <tt>FieldSet</tt>
     * and <tt>Label</tt> fields.
     *
     * @param container the container to obtain the hidden fields from
     * @param fields the list of contained fields
     */
    private static void addHiddenFields(final Container container, final List<Field> fields) {
        for (Control control : container.getControls()) {

            if (control instanceof Label || control instanceof Button) {
                // Skip buttons and labels
                continue;

            } else if (control instanceof Container) {
                // Include fields but skip fieldSets
                if (control instanceof Field && !(control instanceof FieldSet)) {
                    Field field = (Field) control;
                    if (field.isHidden()) {
                        fields.add((Field) control);
                    }
                }

                Container childContainer = (Container) control;
                addHiddenFields(childContainer, fields);

            } else if (control instanceof Field) {
                Field field = (Field) control;
                if (field.isHidden()) {
                    fields.add((Field) control);
                }
            }
        }
    }

    /**
     * Add fields for the container to the specified field list, recursively
     * including any Fields contained in child containers. The list
     * of returned fields will exclude any <tt>Button</tt> and <tt>FieldSet</tt>
     * fields.
     *
     * @param container the container to obtain the fields from
     * @param fields the list of contained fields
     */
    private static void addFieldsAndLabels(final Container container, final List<Field> fields) {
        for (Control control : container.getControls()) {

            if (control instanceof Button) {
                // Skip buttons
                continue;

            } else if (control instanceof Container) {
                // Include fields but skip fieldSets
                if (control instanceof Field && !(control instanceof FieldSet)) {
                    fields.add((Field) control);
                }
                Container childContainer = (Container) control;
                addFieldsAndLabels(childContainer, fields);

            } else if (control instanceof Field) {
                fields.add((Field) control);
            }
        }
    }

    /**
     * Add all the Fields for the given Container to the specified map,
     * recursively including any Fields contained in child containers.
     * <p/>
     * The map's key / value pair will consist of the control name and instance.
     *
     * @param container the container to obtain the fields from
     * @param fields the map of contained fields
     */
    private static void addFields(final Container container, final Map<String, Field> fields) {
        for (Control control : container.getControls()) {

            if (control instanceof Container) {
                // Include fields that are containers
                if (control instanceof Field) {
                    fields.put(control.getName(), (Field) control);
                }
                Container childContainer = (Container) control;
                addFields(childContainer, fields);

            } else if (control instanceof Field) {
                fields.put(control.getName(), (Field) control);
            }
        }
    }

    /**
     * Add the list of container fields to the specified list of fields, which
     * are not valid, not hidden and not disabled.
     * <p/>
     * The list of returned invalid fields will exclude any <tt>Button</tt>
     * fields.
     *
     * @param container the container to obtain the fields from
     * @param fields the map of contained fields
     */
    private static void addErrorFields(final Container container, final List<Field> fields) {
        for (Control control : container.getControls()) {

            if (control instanceof Button) {
                // Skip buttons
                continue;

            } else if (control instanceof Container) {
                if (control instanceof Field) {
                    Field field = (Field) control;
                    if (!field.isValid()
                            && !field.isHidden()
                            && !field.isDisabled()) {

                        fields.add((Field) control);
                    }
                }
                Container childContainer = (Container) control;
                addErrorFields(childContainer, fields);

            } else if (control instanceof Field) {
                Field field = (Field) control;
                if (!field.isValid()
                        && !field.isHidden()
                        && !field.isDisabled()) {

                    fields.add((Field) control);
                }
            }
        }
    }

    /**
     * Log a warning that the parent of the given control will be set to
     * the specified container.
     *
     * @param container the parent container
     * @param control the control which parent is being reset
     * @param currentParent the control current parent
     */
    private static void logParentReset(Container container, Control control,
                                       Object currentParent) {
        org.apache.click.util.HtmlStringBuffer message = new HtmlStringBuffer();

        message.append("Changed ");
        message.append(ClassUtils.getShortClassName(control.getClass()));
        String controlId = control.getId();
        if (controlId != null) {
            message.append("[");
            message.append(controlId);
            message.append("]");
        } else {
            message.append("#");
            message.append(control.hashCode());
        }
        message.append(" parent from ");

        if (currentParent instanceof Page) {
            message.append(ClassUtils.getShortClassName(currentParent.getClass()));

        } else if (currentParent instanceof Container) {
            Container parentContainer = (Container) currentParent;

            message.append(ClassUtils.getShortClassName(parentContainer.getClass()));
            String parentId = parentContainer.getId();
            if (parentId != null) {
                message.append("[");
                message.append(parentId);
                message.append("]");
            } else {
                message.append("#");
                message.append(parentContainer.hashCode());
            }
        }

        message.append(" to ");
        message.append(ClassUtils.getShortClassName(container.getClass()));
        String id = container.getId();
        if (id != null) {
            message.append("[");
            message.append(id);
            message.append("]");
        } else {
            message.append("#");
            message.append(container.hashCode());
        }

        ClickUtils.getLogService().warn(message);
    }

    /**
     * This class fix an error in ognl's conversion of double->BigDecimal. The
     * default conversion uses BigDecimal(double), the fix is to use
     * BigDecimal.valueOf(double)
     *
     */
    private static class FixBigDecimalTypeConverter extends DefaultTypeConverter {
        @SuppressWarnings("unchecked")
        @Override
        public Object convertValue(Map context, Object value, Class toType) {
            if (value != null && toType == BigDecimal.class) {
                return bigDecValue(value);
            }
            return OgnlOps.convertValue(value, toType);
        }

        /**
         * Convert the given value into a BigDecimal.
         *
         * @param value the object to convert into a BigDecimal
         * @return the converted BigDecimal value
         */
        private BigDecimal bigDecValue(Object value) {
            if (value == null) {
                return BigDecimal.valueOf(0L);
            }
            Class<?> c = value.getClass();
            if (c == BigDecimal.class) {
                return (BigDecimal) value;
            }
            if (c == BigInteger.class) {
                return new BigDecimal((BigInteger) value);
            }
            if (c == Boolean.class) {
                return BigDecimal.valueOf(((Boolean) value).booleanValue() ? 1 : 0);
            }
            if (c == Character.class) {
                return BigDecimal.valueOf(((Character) value).charValue());
            }
            return new BigDecimal(value.toString().trim());
        }
    }
}
