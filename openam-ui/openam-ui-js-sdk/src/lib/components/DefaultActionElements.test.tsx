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

import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import DefaultActionElements from './DefaultActionElements';
import { mockAuthData } from '../__tests__/mocks';

describe('DefaultActionElements', () => {
    
    it('renders actions from confirmation callback', () => {
        render(<DefaultActionElements callbacks={mockAuthData.callbacks} />);
        const registerButton = screen.getByText('Register device')
        expect(registerButton).toBeInTheDocument();

        const skipButton = screen.getByText('Skip this step')
        expect(skipButton).toBeInTheDocument();
    });

    it('renders default action', () => {
        render(<DefaultActionElements callbacks={[]} />);
        const defaultButton = screen.getByText('Log In')
        expect(defaultButton).toBeInTheDocument();
    });

});