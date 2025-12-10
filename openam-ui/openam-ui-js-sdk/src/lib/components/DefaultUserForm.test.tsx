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

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import DefaultUserForm from './DefaultUserForm';
import { mockUserData } from '../__tests__/mocks';
import type { UserData } from '../types';

describe('DefaultUserForm', () => {


    let defaultProps: {
        userData: UserData,
        setUserData: (userData: UserData) => void,
        saveHandler: () => void,
        savePasswordHandler: (password: string) => void
    }

    beforeEach(() => {
        vi.clearAllMocks();

        window.alert = vi.fn();

        const mockSetUserData = vi.fn();
        const mockSaveHandler = vi.fn();
        const mockSavePasswordHandler = vi.fn();
        defaultProps = {
            userData: mockUserData,
            setUserData: mockSetUserData,
            saveHandler: mockSaveHandler,
            savePasswordHandler: mockSavePasswordHandler
        };

    });

    it('displays user data correctly in input fields', () => {
        render(
            <DefaultUserForm {...defaultProps} />
        );

        expect(screen.getByDisplayValue('demo')).toBeInTheDocument(); // username (readonly)
        expect(screen.getByDisplayValue('John')).toBeInTheDocument();
        expect(screen.getByDisplayValue('john.doe@example.org')).toBeInTheDocument();
        expect(screen.getByDisplayValue('+1234567890')).toBeInTheDocument();
    });

    it('username field is readonly', () => {
        render(
            <DefaultUserForm {...defaultProps} />
        );

        const usernameInput = screen.getByLabelText(/username/i) as HTMLInputElement;
        expect(usernameInput).toHaveAttribute('readonly');
        expect(usernameInput.readOnly).toBe(true);
    });

    it('calls setUserData with updated array value when editable fields change', () => {
        render(
            <DefaultUserForm {...defaultProps} />
        );

        const firstNameInput = screen.getByLabelText(/first name/i);
        fireEvent.change(firstNameInput, { target: { value: 'Jane' } });

        expect(defaultProps.setUserData).toHaveBeenCalledWith({
            ...mockUserData,
            givenName: ['Jane'],
        });

        const emailInput = screen.getByLabelText(/mail/i);
        fireEvent.change(emailInput, { target: { value: 'jane.doe@example.org' } });

        expect(defaultProps.setUserData).toHaveBeenCalledWith({
            ...mockUserData,
            mail: ['jane.doe@example.org'],
        });
    });

    it('calls saveHandler and prevents default on form submit', () => {
        render(
            <DefaultUserForm {...defaultProps} />
        );
        const submitButton = screen.getByRole('button', { name: /save/i });
        fireEvent.click(submitButton);
        expect(defaultProps.saveHandler).toHaveBeenCalledTimes(1);
    });

    it('opens set password modal on click', () => {
        render(
            <DefaultUserForm {...defaultProps} />
        );
        const changePasswordButton = screen.getByText('Change Password');
        expect(changePasswordButton).toBeInTheDocument();

        fireEvent.click(changePasswordButton);

        expect(screen.getByLabelText("New:")).toBeInTheDocument();
        expect(screen.getByLabelText("Confirm:")).toBeInTheDocument();
        expect(screen.getByText("Update Password")).toBeInTheDocument();
    });

    it('validates new and confirm password', () => {
        render(
            <DefaultUserForm {...defaultProps} />
        );
        const changePasswordButton = screen.getByText('Change Password');
        expect(changePasswordButton).toBeInTheDocument();

        fireEvent.click(changePasswordButton);

        fireEvent.change(screen.getByLabelText('New:'), { target: { value: 'newPass123' } });
        fireEvent.change(screen.getByLabelText('Confirm:'), { target: { value: 'mismatch123' } });

        fireEvent.click(screen.getByText('Update Password'));

        expect(window.alert).toHaveBeenCalledWith('Passwords do not match.');

        expect(screen.getByLabelText('New:')).toBeInTheDocument();
    });

    it('saves password', () => {
        render(
            <DefaultUserForm {...defaultProps} />
        );
        const changePasswordButton = screen.getByText('Change Password');
        expect(changePasswordButton).toBeInTheDocument();

        fireEvent.click(changePasswordButton);

        fireEvent.change(screen.getByLabelText('New:'), { target: { value: 'newPass123' } });
        fireEvent.change(screen.getByLabelText('Confirm:'), { target: { value: 'newPass123' } });
        fireEvent.click(screen.getByText('Update Password'));


         expect(defaultProps.savePasswordHandler).toHaveBeenCalledTimes(1);
    });



});