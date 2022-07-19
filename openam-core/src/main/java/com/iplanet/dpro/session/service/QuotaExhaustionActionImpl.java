package com.iplanet.dpro.session.service;

import static org.forgerock.openam.session.SessionConstants.SESSION_DEBUG;

import java.util.Map;
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

    public abstract boolean action(InternalSession is, Map<String, Long> existingSessions);

    static public class SetBlockingQueue<T> extends LinkedBlockingQueue<T> {

    	final private static long serialVersionUID = 1L;

		public SetBlockingQueue(int capacity) {
			super(capacity);
		}

	    @Override
	    public boolean add(T t) {
            try {
            	if (contains(t)) {
            		return false;
            	}
            	return super.add(t);
            }catch (IllegalStateException e) {
            	return false;
            }
	    }

    }
    
    final static SetBlockingQueue<String> queue=new SetBlockingQueue<String>(SystemProperties.getAsInt("org.openidentityplatform.openam.cts.quota.exhaustion.queue", 64000));
    
    static class Task implements Runnable {
    	final static  Debug debug = InjectorHolder.getInstance(Key.get(Debug.class, Names.named(SESSION_DEBUG)));
		
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
					}finally {
						queue.remove(sessionId);
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
    		queue.add(sessionId);
    	}finally {
    		sessions.remove(sessionId);
		}
    }
}
