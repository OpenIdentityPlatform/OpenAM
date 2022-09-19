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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.shared.debug.Debug;

public abstract class QuotaExhaustionActionImpl implements QuotaExhaustionAction {
	final static  Debug debug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));
	
    public abstract boolean action(InternalSession is, Map<String, Long> existingSessions);

    static public class SetBlockingQueue<T> extends LinkedBlockingQueue<T> {

    	final private static long serialVersionUID = 1L;

    	private Set<T> set;
		public SetBlockingQueue(int capacity) {
			super(capacity);
			set = Collections.newSetFromMap(new ConcurrentHashMap<>(capacity));
		}
		
	    @Override
	    public synchronized boolean add(T t) {
            try {
            	if (set.contains(t)) {
            		return false;
            	}
            	set.add(t);
            	final boolean res=super.add(t);
            	if (!res) {
            		set.remove(t);
            	}
            	return res;
            }catch (IllegalStateException e) {
            	set.remove(t);
            	return false;
            }
	    }

	    @Override
	    public T take() throws InterruptedException {
	        T t = super.take();
	        set.remove(t);
	        return t;
	    }
    }
    
    final static SetBlockingQueue<String> queue=new SetBlockingQueue<String>(SystemProperties.getAsInt("org.openidentityplatform.openam.cts.quota.exhaustion.queue", 64000));
    
    static class Task implements Runnable {
    	@Override
		public void run() {
			String sessionId;
			try {
				while ((sessionId=queue.take())!=null) {
					final SessionID sid=new SessionID(sessionId);
					final String uid;
					try {
						final Session s=org.forgerock.openam.session.SessionCache.getInstance().getSession(sid,true,false);
						uid=s.getClientID();
						s.destroySession(s);
						debug.error("cts quota exhaustion destroy {} for {}: queue size {}", sessionId,uid,queue.size()+1);
					}catch (Exception e) {
						debug.error("error cts quota exhaustion destroy {}: queue size {}", sessionId,queue.size()+1,e.toString());
					}
				}
			}catch (InterruptedException e) {}
		}
    }
    
    final static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
	    		SystemProperties.getAsInt("org.openidentityplatform.openam.cts.quota.exhaustion.pool", 3),
	    		new ThreadFactoryBuilder().setNameFormat("QuotaExhaustionAction-%d")
	    		.build()
    		);
    
    static {
    	for(int i=1;i<=executor.getMaximumPoolSize();i++) {
    		executor.submit(new Task());
    	}
    }
    protected void destroy(String sessionId,Map<String, Long> sessions) {
    	try {
    		if (!queue.add(sessionId)) {
    			debug.error("cts quota exhaustion destroy full: queue size {}", queue.size());
    		}
    	}finally {
    		sessions.remove(sessionId);
		}
    }
}
