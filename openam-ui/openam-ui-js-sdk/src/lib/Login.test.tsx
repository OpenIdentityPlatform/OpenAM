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
 * Copyright 2026 3A Systems, LLC.
 */

import { render, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import Login from './Login';
import type { LoginService } from './loginService';
import type { AuthResponse } from './types';

const mockNavigate = vi.fn();

vi.mock('react-router', async (importOriginal) => ({
  ...await importOriginal<typeof import('react-router')>(),
  useNavigate: () => mockNavigate,
  useSearchParams: () => [new URLSearchParams()],
}));

function loginServiceReturning(response: AuthResponse): LoginService {
  return { init: vi.fn().mockResolvedValue(response) } as unknown as LoginService;
}

describe('Login', () => {
  beforeEach(() => {
    mockNavigate.mockReset();
  });

  it('completes a login whose response carries no tokenId (HttpOnly session cookie)', async () => {
    render(<Login loginService={loginServiceReturning({ successUrl: '/openam/console', realm: '/' })} />);

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'));
  });

  it('completes a login whose response carries a tokenId', async () => {
    render(<Login loginService={loginServiceReturning(
      { tokenId: 'AQIC5wM2LY4Sfczn-token', successUrl: '/openam/console', realm: '/' })} />);

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'));
  });
});
