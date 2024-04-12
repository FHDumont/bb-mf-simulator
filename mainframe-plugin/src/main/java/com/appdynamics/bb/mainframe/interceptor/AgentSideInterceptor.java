package com.appdynamics.bb.mainframe.interceptor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.ExitCall;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.apm.appagent.api.DataScope;
import com.appdynamics.bb.mainframe.MetaData;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;

public abstract class AgentSideInterceptor extends AGenericInterceptor {

    protected static final Object CORRELATION_HEADER_KEY = AppdynamicsAgent.TRANSACTION_CORRELATION_HEADER;
    protected boolean initialized = false;
    protected Set<DataScope> dataScopes = null;

    public AgentSideInterceptor() {
        super();
        initialize();
        getLogger().info(String.format("Initialized plugin class %s version %s build date %s",
                getClass().getCanonicalName(), MetaData.VERSION, MetaData.BUILDTIMESTAMP));
    }

    abstract public Object onMethodBegin(Object objectIntercepted, String className, String methodName,
            Object[] params);

    abstract public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params,
            Throwable exception, Object returnVal);

    abstract public List<Rule> initializeRules();

    protected boolean isInitialized() {
        return this.initialized;
    }

    protected void initialize() {
        dataScopes = new HashSet<DataScope>();
        dataScopes.add(DataScope.SNAPSHOTS);
        dataScopes.add(DataScope.ANALYTICS);
        this.initialized = true;
    }

    protected Map<String, String> getListOfCustomProperties() {
        return new HashMap<>();
    }

    protected boolean isFakeTransaction(Transaction transaction) {
        return "".equals(transaction.getUniqueIdentifier());
    }

    protected boolean isFakeExitCall(ExitCall exitCall) {
        return "".equals(exitCall.getCorrelationHeader());
    }

    protected String getUrlWithoutParameters(String url) {
        try {
            URI uri = new URI(url);
            return new URI(uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    null, // Ignore the query part of the input url
                    uri.getFragment()).toString();
        } catch (URISyntaxException e) {
            return url; // just give back the original input on error
        }
    }

    protected IReflector makeAccessFieldValueReflector(String field) {
        return getNewReflectionBuilder().accessFieldValue(field, true).build();
    }

    protected IReflector makeInvokeInstanceMethodReflector(String method, String... args) {
        if (args.length > 0)
            return getNewReflectionBuilder().invokeInstanceMethod(method, true, args).build();
        return getNewReflectionBuilder().invokeInstanceMethod(method, true).build();
    }

    protected String getReflectiveString(Object object, IReflector method, String defaultString) {
        String value = defaultString;
        if (object == null || method == null)
            return defaultString;
        try {
            value = (String) method.execute(object.getClass().getClassLoader(), object);
            if (value == null)
                return defaultString;
        } catch (ReflectorException e) {
            this.getLogger().debug("Error in reflection call, exception: " + e.getMessage(), e);
        }
        return value;
    }

    protected Integer getReflectiveInteger(Object object, IReflector method, Integer defaultInteger) {
        Integer value = defaultInteger;
        if (object == null || method == null)
            return defaultInteger;
        try {
            value = (Integer) method.execute(object.getClass().getClassLoader(), object);
            if (value == null)
                return defaultInteger;
        } catch (ReflectorException e) {
            this.getLogger().debug("Error in reflection call, exception: " + e.getMessage(), e);
        }
        return value;
    }

    protected Long getReflectiveLong(Object object, IReflector method) {
        if (object == null || method == null)
            return null;
        Object rawValue = getReflectiveObject(object, method);
        if (rawValue instanceof Long)
            return (Long) rawValue;
        if (rawValue instanceof Integer)
            return ((Integer) rawValue).longValue();
        if (rawValue instanceof Double)
            return ((Double) rawValue).longValue();
        if (rawValue instanceof Number)
            return ((Number) rawValue).longValue();
        return null;
    }

    protected Object getReflectiveObject(Object object, IReflector method, Object... args) {
        Object value = null;
        if (object == null || method == null)
            return value;
        try {
            if (args.length > 0) {
                value = method.execute(object.getClass().getClassLoader(), object, args);
            } else {
                value = method.execute(object.getClass().getClassLoader(), object);
            }
        } catch (ReflectorException e) {
            this.getLogger().debug("Error in reflection call, method: " + method.getClass().getCanonicalName()
                    + " object: " + object.getClass().getCanonicalName() + " exception: " + e.getMessage(), e);
        }
        return value;
    }

    protected void collectData(Transaction transaction, String name, String value) {
        collectData(transaction, null, name, value);
    }

    protected void collectData(Transaction transaction, String className, String name, String value) {
        if (transaction == null)
            return;
        if (!isInitialized()) {
            initialize();
        }
        transaction.collectData(name, value, this.dataScopes);
    }

    protected void collectSnapshotData(Transaction transaction, String name, String value) {
        if (transaction == null)
            return;
        if (!isInitialized()) {
            initialize();
        }
        transaction.collectData(name, value, this.dataScopes);
    }

    protected void publishEvent(String eventSummary, String severity, String eventType, Map<String, String> details) {
        this.getLogger().debug("Begin publishEvent event summary: " + eventSummary + " severity: " + severity
                + " event type: " + eventType);
        AppdynamicsAgent.getEventPublisher().publishEvent(eventSummary, severity, eventType, details);
    }

    protected void reportMetric(String metricName, long metricValue, String aggregationType, String timeRollupType,
            String clusterRollupType) {
        this.getLogger()
                .debug("Begin reportMetric name: " + metricName + " = " + metricValue + " aggregation type: "
                        + aggregationType + " time rollup type: " + timeRollupType + " cluster rollup type: "
                        + clusterRollupType);
        AppdynamicsAgent.getMetricPublisher().reportMetric(metricName, metricValue, aggregationType, timeRollupType,
                clusterRollupType);
    }
}
