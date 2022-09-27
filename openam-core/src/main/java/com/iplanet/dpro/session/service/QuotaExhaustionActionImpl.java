package com.iplanet.dpro.session.service;

import static org.forgerock.openam.session.SessionConstants.SESSION_DEBUG;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.forgerock.guice.core.InjectorHolder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.sun.identity.shared.debug.Debug;

public abstract class QuotaExhaustionActionImpl implements QuotaExhaustionAction {
	final static  Debug debug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));
	
    public abstract boolean action(InternalSession is, Map<String, Long> existingSessions);

    static public class SetBlockingQueue<T> extends LinkedBlockingQueue<T> {

    	final private static long serialVersionUID = 1L;

    	final Cache<T, Boolean> cache;
    	
		public SetBlockingQueue(int capacity) {
			super(capacity);
			cache = CacheBuilder.newBuilder()
	    			.maximumSize(capacity)
	    			.expireAfterWrite(SystemProperties.getAsInt("org.openidentityplatform.openam.cts.quota.exhaustion.queue.expire", 60), TimeUnit.SECONDS)
	    			.build();
		}
		
	    @Override
	    public boolean add(T t) {
	    	boolean res=false;
            try {
            	if (cache.getIfPresent(t)==null) {
            		res=super.add(t);
            	}
            	cache.put(t,true);
            	return res;
            }catch (IllegalStateException e) {
            	debug.error("cts quota exhaustion destroy full: queue size {}", queue.size());
           		return false;
            }
	    }

	    @Override
	    public T take() throws InterruptedException {
	        T t = super.take();
	        return t;
	    }
    }
    
    final static SetBlockingQueue<String> queue=new SetBlockingQueue<String>(SystemProperties.getAsInt("org.openidentityplatform.openam.cts.quota.exhaustion.queue", 32000));
    
    static class Task implements Runnable {
    	@Override
		public void run() {
			String sessionId;
			try {
				while (true) {
					while ((sessionId=queue.take())!=null) {
						try {
							final SessionID sid=new SessionID(sessionId);
							final Session s=org.forgerock.openam.session.SessionCache.getInstance().getSession(sid,true,false);
							final String uid=s.getPropertyWithoutValidation("sun.am.UniversalIdentifier");
							s.destroySession(s);
							debug.error("cts quota exhaustion destroy {} for {}: queue size {}", sessionId,uid,queue.size()+1);
						}catch (SessionException e) {
							debug.warning("error cts quota exhaustion destroy {}: queue size {} {}", sessionId,queue.size()+1,e.toString());
						}catch (Throwable e) {
							debug.error("error cts quota exhaustion destroy {}: queue size {} {}", sessionId,queue.size()+1,e.toString(),e);
						}
					}
				}
			}catch (InterruptedException e) {}
		}
    }
    
    final static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
	    		SystemProperties.getAsInt("org.openidentityplatform.openam.cts.quota.exhaustion.pool", 6),
	    		new ThreadFactoryBuilder().setNameFormat("QuotaExhaustionAction-%d")
	    		.build()
    		);
    
    static {
    	for(int i=1;i<=executor.getMaximumPoolSize();i++) {
    		final Task task=new Task();
    		executor.submit(task);
    	}
    }
    protected void destroy(String sessionId,Map<String, Long> sessions) {
    	try {
    		queue.add(sessionId);
    	}finally {
    		sessions.remove(sessionId);
		}
    }
}
