package com.cumulocity.microservice.context;

import com.cumulocity.microservice.context.credentials.Credentials;
import com.cumulocity.microservice.context.credentials.UserCredentials;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.LinkedList;
import java.util.concurrent.Callable;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.reverse;

public class ContextServiceImpl<C> implements ContextService<C> {

    private static final Logger log = LoggerFactory.getLogger(ContextServiceImpl.class);

    private static final String TENANT_LOG_FLAG = "tenant";
    private static final String DEVICE_LOG_FLAG = "device";

    private static ThreadLocal<LinkedList<Object>> localContext = new ThreadLocal<LinkedList<Object>>() {
        @Override
        protected LinkedList<Object> initialValue() {
            return new LinkedList<>();
        }
    };
    private final Class<C> clazz;

    public ContextServiceImpl(Class<C> c) {
        clazz = c;
    }

    @Override
    public C getContext() {
        C context = doGetContext();
        if (context == null) {
            throw new IllegalStateException("Not within any context!");
        }
        return context;
    }

    @Override
    public boolean isInContext() {
        return doGetContext() != null;
    }

	private C doGetContext() {
        return from(reverse(localContext.get())).firstMatch(new Predicate<Object>() {
            public boolean apply(Object o) {
                return clazz.isInstance(o);
            }
        }).transform(new Function<Object, C>() {
            public C apply(Object o) {
                return (C) o;
            }
        }).orNull();
    }

    @Override
    public void runWithinContext(C context, Runnable task) {
        try {
            callWithinContext(context, new CallableRunnableWrapper(task));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <V> V callWithinContext(C context, Callable<V> task) {
        enterContext(context);
        try {
            return task.call();
        } catch (Exception e) {
            log.warn("execution of task failed within tenant: {} - {} " , getContextTenant(context), e.getMessage());
            log.debug("execution of task failed within tenant : {} ", getContextTenant(context), e);
            throw Throwables.propagate(e);
        } finally {
            leaveContext();
        }
    }

    private void enterContext(C newContext) {
        // Add flags for logging framework
        MDC.put(TENANT_LOG_FLAG, getContextTenant(newContext));
        MDC.put(DEVICE_LOG_FLAG, getContextDevice(newContext));
        log.debug("entering to  {} ", newContext);
        localContext.get().add(newContext);
    }

    private void leaveContext() {
        // Remove logging flags
        MDC.remove(TENANT_LOG_FLAG);
        MDC.remove(DEVICE_LOG_FLAG);
        localContext.get().removeLast();

        final C previousContext = doGetContext();
        if (previousContext != null) {
            MDC.put(TENANT_LOG_FLAG, getContextTenant(previousContext));
            MDC.put(DEVICE_LOG_FLAG, getContextDevice(previousContext));
        }
    }

    private static class CallableRunnableWrapper implements Callable<Void> {

        private final Runnable runnable;

        public CallableRunnableWrapper(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public Void call() {
            runnable.run();
            return null;
        }
    }

    @Override
    public Runnable withinContext(final C context, final Runnable task) {
        return new Runnable() {
            @Override
            public void run() {
                runWithinContext(context, task);
            }
        };
    }

    @Override
    public <V> Callable<V> withinContext(final C context, final Callable<V> task) {
        return new Callable<V>() {
            @Override
            public V call() {
                return callWithinContext(context, task);
            }
        };
    }

	private static String getContextDevice(Object credentials) {
        if (credentials instanceof UserCredentials) {
            return ((UserCredentials) credentials).getIdentifier();
        }
        return null;
	}

    private static String getContextTenant(Object credentials) {
        if (credentials instanceof Credentials) {
            return ((Credentials) credentials).getTenant();
        }
        return null;
    }
}
