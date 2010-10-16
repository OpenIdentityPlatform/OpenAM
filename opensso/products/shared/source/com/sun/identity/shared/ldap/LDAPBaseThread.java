
package com.sun.identity.shared.ldap;


public class LDAPBaseThread extends Thread {
    
    private volatile boolean shouldTerminate;
    private Runnable task;
    
    public LDAPBaseThread(String name, boolean daemon) {
        setName(name);
        setDaemon(daemon);
        this.shouldTerminate = false;
        this.task = null;
    }
    
    public synchronized void run(Runnable task) {
        this.task = task;        
        this.notify();
    }
    
    public void run() {
        Runnable localTask = null;        
        boolean localShouldTerminate = false;
        while (true) {
            try {
                synchronized (this) {
                    if ((task == null) && (!shouldTerminate)){
                        this.wait();
                    }
                    localShouldTerminate = shouldTerminate;
                    localTask = task;
                    task = null;
                }
                if (localShouldTerminate) {
                    break;
                }
                if (localTask != null) {
                    localTask.run();
                }            
            } catch (RuntimeException ex) {
                localShouldTerminate = true;
                throw ex;
            } catch (Exception ex) {
            } catch (Throwable e) {
                localShouldTerminate = true;
                throw new Error(e);
            } finally {
                if (localShouldTerminate) {
                    break;
                }
            }
        }
    }
    
    public synchronized void shutdown() {
        this.shouldTerminate = true;
        this.notify();
    }
    
}
