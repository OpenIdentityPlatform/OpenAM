package com.sun.identity.cli;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.http.Client;
import org.forgerock.openam.shared.guice.CloseableHttpClientProvider;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

@GuiceModule
public class WebCliGuiceModule implements Module {

	@Override
	public void configure(Binder binder) {
		binder.bind(Client.class).toProvider(CloseableHttpClientProvider.class).in(Scopes.SINGLETON);
	}
	
}
