package com.iplanet.dpro.session.service;

import static org.forgerock.openam.session.SessionConstants.SESSION_DEBUG;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.SessionCache;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.shared.debug.Debug;

public abstract class QuotaExhaustionActionImpl implements QuotaExhaustionAction {

    public abstract boolean action(InternalSession is, Map<String, Long> existingSessions);

    static public class SetBlockingQueue<T> extends LinkedBlockingQueue<T> {

    	final private static long serialVersionUID = 1L;

		final private Set<T> set = Collections.newSetFromMap(new ConcurrentHashMap<>());

		public SetBlockingQueue(int capacity) {
			super(capacity);
		}

	    @Override
	    public synchronized boolean add(T t) {
	        if (set.contains(t)) {
	            return false;
	        } else {
	            try {
	            	final Boolean res=super.add(t);
	            	if (res) {
	            		set.add(t);
	            	}
	            	return res;
	            }catch (IllegalStateException e) {
	            	return false;
	            }
	        }
	    }

	    @Override
	    public T take() throws InterruptedException {
	        final T t = super.take();
	        set.remove(t);
	        return t;
	    }
    }
    
    final static SetBlockingQueue<String> queue=new SetBlockingQueue<String>(SystemProperties.getAsInt("org.openidentityplatform.openam.cts.quota.exhaustion.queue", 64000));

    static class Task implements Runnable {
    	final static  SessionCache sessionCache = InjectorHolder.getInstance(SessionCache.class);
    	final static  Debug debug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));
		
    	@Override
		public void run() {
			String sessionId;
			try {
				while ((sessionId=queue.take())!=null) {
					try {
						final Session s=sessionCache.getSession(new SessionID(sessionId), true, false);
						s.logout();
						debug.error("cts quota exhaustion destroy {} for {}: queue size {}", sessionId,s.getClientID(),queue.size()+1);
					}catch (Throwable e) {}
				}
			}catch (InterruptedException e) {}
		}
    }
    
    final static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1,new ThreadFactoryBuilder().setNameFormat("QuotaExhaustionAction-%d").build());
    static {
    	final Integer poolSize=SystemProperties.getAsInt("org.openidentityplatform.openam.cts.quota.exhaustion.pool", 1);
    	executor.setMaximumPoolSize(poolSize);
    	executor.setCorePoolSize(poolSize);
    	for(int i=1;i<=poolSize;i++) {
    		executor.submit(new Task());
    	}
    }
    protected void destroy(String sessionId,Map<String, Long> sessions) {
    	queue.add(sessionId);
    	sessions.remove(sessionId);
    }
}
