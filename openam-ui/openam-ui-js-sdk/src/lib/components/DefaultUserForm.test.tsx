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
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DefaultUserForm from './DefaultUserForm';
import type { UserService } from '../userService';
import { mockUserAuthData, mockUserData } from '../__tests__/mocks';
import type { AuthError, UserAuthData } from '../types';

describe('DefaultUserForm', () => {


    let mockUserService: UserService;
    let mockErrorAuthHandler: (authError: any) => void;

    let defaultProps: {
        userAuthData: UserAuthData,
        userService: UserService,
        errorAuthHandler: (authError: AuthError) => void
    }

    beforeEach(() => {
        vi.clearAllMocks();
        mockUserService = {
            getUserData: vi.fn().mockResolvedValue(mockUserData),
            saveUserData: vi.fn().mockResolvedValue(mockUserData)
        } as unknown as UserService;
        mockErrorAuthHandler = vi.fn();
        defaultProps = {
            userAuthData: mockUserAuthData,
            userService: mockUserService,
            errorAuthHandler: mockErrorAuthHandler
        };

    });

    describe('Initialization and Loading', () => {
        it('should display loading state initially', () => {
            render(
                <DefaultUserForm {...defaultProps} />
            );

            expect(screen.getByText('Loading user data...')).toBeInTheDocument();
        });

        it('should fetch user data on mount', async () => {
            render(
                <DefaultUserForm {...defaultProps} />
            );

            await waitFor(() => {
                expect(mockUserService.getUserData).toHaveBeenCalledWith(
                    mockUserAuthData.id,
                    mockUserAuthData.realm
                );
                expect(mockUserService.getUserData).toHaveBeenCalledTimes(1);
            });
        });

        it('should render form after data is fetched', async () => {
            render(
                 <DefaultUserForm {...defaultProps} />
            );
            await waitFor(() => {
                expect(mockUserService.getUserData).toHaveBeenCalledTimes(1);
                expect(screen.getByText('User Profile')).toBeInTheDocument();
                expect(screen.getByLabelText('Username:')).toBeInTheDocument();
            }, { timeout: 1000 });


        });
    });

    describe('Form Rendering', () => {
        it('should render all form fields correctly', async () => {
            render(
                 <DefaultUserForm {...defaultProps} />
            );

            await waitFor(() => {
                expect(screen.getByLabelText('Username:')).toBeInTheDocument();
                expect(screen.getByLabelText('First Name:')).toBeInTheDocument();
                expect(screen.getByLabelText('Last Name:')).toBeInTheDocument();
                expect(screen.getByLabelText('Mail:')).toBeInTheDocument();
                expect(screen.getByLabelText('Phone number:')).toBeInTheDocument();
                expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
            });
        });

        it('should populate form fields with user data', async () => {
            render(
                <DefaultUserForm {...defaultProps} />
            );

            await waitFor(() => {
                expect(screen.getByDisplayValue('demo')).toBeInTheDocument();
                expect(screen.getByDisplayValue('John')).toBeInTheDocument();
            });
        });
    });

    describe('Form Interaction', () => {
        const testCases = [ 
            { label: 'First Name:', field: 'givenName', newValue: 'Jane' },
            { label: 'Last Name:', field: 'sn', newValue: 'Smith' },
            { label: 'Mail:', field: 'mail', newValue: 'jane.smith@example.com' },
            { label: 'Phone number:', field: 'telephoneNumber', newValue: '+9876543210' },
        ]
        testCases.forEach(({ label, field, newValue }) => {
            it(`should update ${field} field on input change`, async () => {
                const user = userEvent.setup();

                render(
                    <DefaultUserForm {...defaultProps} />
                );

                await waitFor(() => {
                    expect(screen.getByLabelText(label)).toBeInTheDocument();
                });

                const input = screen.getByLabelText(label) as HTMLInputElement;
                await user.clear(input);
                await user.type(input, newValue);

                expect(input.value).toBe(newValue);
            });
        });
    });

    describe('Form Submission', () => {

        it('should prevent default form submission', async () => {

            render(
                 <DefaultUserForm {...defaultProps} />
            );

            await waitFor(() => {
                expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
            });

            const form = screen.getByRole('button', { name: 'Save' }).closest('form')!;
            const submitEvent = new Event('submit', { bubbles: true, cancelable: true });
            const preventDefaultSpy = vi.spyOn(submitEvent, 'preventDefault');

            try {
                fireEvent(form, submitEvent);
            } catch (error) {
            }

            expect(preventDefaultSpy).toHaveBeenCalled();
        });

        it('should submit updated user data', async () => {
            const user = userEvent.setup();
            render(
                 <DefaultUserForm {...defaultProps} />
            );

            await waitFor(() => {
                expect(screen.getByLabelText('First Name:')).toBeInTheDocument();
            });

            const givenNameInput = screen.getByLabelText('First Name:') as HTMLInputElement;
            await user.clear(givenNameInput);
            await user.type(givenNameInput, 'UpdatedName');

            const form = screen.getByRole('button', { name: 'Save' }).closest('form')!;

            try {
                fireEvent.submit(form);
            } catch (error) {
                // Ignore the "not implemented" error
            }

            expect(mockUserService.saveUserData).toHaveBeenCalledTimes(1)
            expect(mockUserService.saveUserData).toHaveBeenCalledWith(
                mockUserAuthData.id,
                mockUserAuthData.realm,
                expect.objectContaining({
                    username: 'demo',
                    givenName: ['UpdatedName'],
                })
            );
        });
    });
});