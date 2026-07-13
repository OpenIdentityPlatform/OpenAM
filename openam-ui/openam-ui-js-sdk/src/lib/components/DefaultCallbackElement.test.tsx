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

import { fireEvent, render, screen } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import DefaultCallbackElement from './DefaultCallbackElement';
import { mockAuthData } from '../__tests__/mocks';

describe('DefaultCallbackElement', () => {
  const setCallbackValue = vi.fn();

  it('renders login input', () => {
    render(<DefaultCallbackElement callback={mockAuthData.callbacks[0]} setCallbackValue={setCallbackValue} />);

    const input = screen.getByRole('textbox')
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('type', 'text');
    expect(input).toHaveAttribute('value', 'demo');
  });

  it('renders password input', () => {
    const { container } = render(<DefaultCallbackElement callback={mockAuthData.callbacks[1]} setCallbackValue={setCallbackValue} />);
    
    const input = container.querySelector("#IDToken2");
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute('type', 'password');
  });

  it('changes callback value', () => {
    const newLogin = 'newLogin';
    render(<DefaultCallbackElement callback={mockAuthData.callbacks[0]} setCallbackValue={setCallbackValue} />);
    const input = screen.getByRole('textbox')
    expect(input).toBeInTheDocument();
    fireEvent.change(input, {target: {value: newLogin}});
    expect(setCallbackValue).toHaveBeenCalledTimes(1);
    expect(setCallbackValue).toHaveBeenCalledWith(newLogin)
  });
});