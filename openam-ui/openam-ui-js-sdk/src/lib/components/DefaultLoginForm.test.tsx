/**
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
 * Copyright 2025 3A Systems LLC.
 */

import { render, fireEvent } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import DefaultLoginForm from './DefaultLoginForm';
import { mockAuthData } from '../__tests__/mocks';
import { setConfig } from '../config';
import type { ActionElements, CallbackElement } from './types';

describe('DefaultLoginForm', () => {
    const mockCallbackElement: CallbackElement = vi.fn();
    const mockActionElements: ActionElements = vi.fn();

    const mockSetCallbackValue = vi.fn();
    const mockDoLogin = vi.fn()

    setConfig({
        CallbackElement: mockCallbackElement,
        ActionElements: mockActionElements,
    })
   

    const defaultProps = {
        authData: mockAuthData,
        setCallbackValue: mockSetCallbackValue,
        doLogin: mockDoLogin
    }

    it('renders login form', () => {
        render(<DefaultLoginForm {...defaultProps} />);
        
        expect(mockCallbackElement).toHaveBeenCalledTimes(2);
        expect(mockActionElements).toHaveBeenCalledTimes(1);        
    });

    it('calls doLogin on form submit', () => {
        const {container} = render(<DefaultLoginForm {...defaultProps} />);
        
        const form = container.querySelector('form');
        if(!form) {
            expect(form).not.toBeNull();
            return;
        }
        
        expect(form).toBeInTheDocument();
        fireEvent.submit(form);
        expect(mockDoLogin).toHaveBeenCalledTimes(1)
    });
});