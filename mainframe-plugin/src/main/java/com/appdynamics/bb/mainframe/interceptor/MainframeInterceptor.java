package com.appdynamics.bb.mainframe.interceptor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.ExitCall;
import com.appdynamics.agent.api.ExitTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.singularity.ee.agent.appagent.services.transactionmonitor.TransactionMonitoringService;

public class MainframeInterceptor extends AgentSideInterceptor {

    IReflector getInfo, getInfoOperacao, getTransporte, getPadrao, getCodigo, getCodigoServico, getNome, getServico,
            getBarramento, getProtocolo, getOperacao, getIdentificacao, getSistema;

    public MainframeInterceptor() {
        super();
        try {
            // FTW & IIB
            getTransporte = makeInvokeInstanceMethodReflector("getTransporte");
            getServico = makeInvokeInstanceMethodReflector("getServico");
            getCodigoServico = makeInvokeInstanceMethodReflector("getCodigoServico");

            // FTW
            getInfo = makeInvokeInstanceMethodReflector("getInfo");
            getPadrao = makeInvokeInstanceMethodReflector("getPadrao");
            getCodigo = makeInvokeInstanceMethodReflector("getCodigo");
            getNome = makeInvokeInstanceMethodReflector("getNome");
            getBarramento = makeInvokeInstanceMethodReflector("getBarramento");

            // IIB
            getInfoOperacao = makeInvokeInstanceMethodReflector("getInfoOperacao");
            getIdentificacao = makeInvokeInstanceMethodReflector("getIdentificacao");
            getProtocolo = makeInvokeInstanceMethodReflector("getProtocolo");
            getSistema = makeInvokeInstanceMethodReflector("getSistema");
            getOperacao = makeInvokeInstanceMethodReflector("getOperacao");
        } catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }

    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();

        rules.add(new Rule.Builder(
                "br.com.bb.ftw.transacao.GerenteTransacao")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("processar")
                .withParams("br.com.bb.ftw.sessao.Sessao", "br.com.bb.ftw.transacao.Transacao", "byte[]")
                .build());

        rules.add(new Rule.Builder(
                "br.com.bb.iib.Comunicacao")
                .classMatchType(SDKClassMatchType.IMPLEMENTS_INTERFACE)
                .methodMatchString("executar")
                .withParams("br.com.bb.iib.ContextoExecucao")
                .build());

        return rules;
    }

    private Object mainframeFtw(Transaction transaction, Object objectIntercepted, String className, String methodName,
            Object[] params)
            throws Exception {

        Map<String, String> map = new HashMap<>();
        String exitCallName = "";
        long startTime = new Date().getTime();
        ExitCall exitCall = null;

        if (!className.equalsIgnoreCase("br.com.bb.ftw.transacao.GerenteTransacao"))
            return null;

        // Params
        Object paramTransaction = params[1];

        // Fields
        Object info = getReflectiveObject(paramTransaction, getInfo);
        String transporte = getReflectiveString(info, getTransporte, "UNKNOW-TRANSPORTE");
        String padrao = getReflectiveString(info, getPadrao, "UNKNOW-PADRAO");

        String codigo = getReflectiveString(info, getCodigo, "UNKNOW-CODIGO");
        String nome = getReflectiveString(info, getNome, "UNKNOW-NOME");
        Object servico = getReflectiveObject(info, getServico);
        String codigoServico = getReflectiveString(servico, getCodigoServico, "UNKNOW-CODIGO-SERVICOI");
        String barramento = getReflectiveString(info, getBarramento, "UNKNOW-BARRAMENTO");

        // Custom Backend
        exitCallName = String.format("FTW - %s.%s", transporte, padrao);
        map.put("TRANSPORTE", transporte);
        map.put("PADRAO", padrao);
        exitCall = transaction.startExitCall(map, exitCallName, ExitTypes.CUSTOM, false);

        // Data Collectors
        collectSnapshotData(transaction, "Transporte", transporte);
        collectSnapshotData(transaction, "Padrao", padrao);
        collectSnapshotData(transaction, "Codigo", codigo);
        collectSnapshotData(transaction, "Nome", nome);
        collectSnapshotData(transaction, "Servico", codigoServico);
        collectSnapshotData(transaction, "Barramento", barramento);

        if (exitCall != null) {
            return new State(exitCall, String.format("FTW - %s.%s", transporte, padrao), codigo, startTime);
        } else {
            return null;
        }
    }

    private Object mainframeIIB(Transaction transaction, Object objectIntercepted, String className, String methodName,
            Object[] params)
            throws Exception {

        Map<String, String> map = new HashMap<>();
        String exitCallName = "";
        long startTime = new Date().getTime();
        ExitCall exitCall = null;

        if (className.indexOf("br.com.bb.iib.Comunicacao") == -1)
            return null;

        // Params
        Object paramTransaction = params[0];

        // Fields
        Object infoOperacao = getReflectiveObject(paramTransaction, getInfoOperacao);
        Object transporte = getReflectiveObject(infoOperacao, getTransporte);
        Object protocolo = getReflectiveObject(infoOperacao, getProtocolo);
        Object identificacao = getReflectiveObject(infoOperacao, getIdentificacao);
        String sistema = getReflectiveString(identificacao, getSistema, "UNKNOW-SISTEMA");
        String operacao = getReflectiveString(identificacao, getOperacao, "UNKNOW-OPERACAO");

        // Custom Backend
        exitCallName = String.format("IIB - %s", transporte);
        map.put("TRANSPORTE", String.valueOf(transporte));
        map.put("PROTOCOLO", String.valueOf(protocolo));
        exitCall = transaction.startExitCall(map, exitCallName, ExitTypes.CUSTOM, false);

        // Data Collectors
        collectSnapshotData(transaction, "Trnasporte", String.valueOf(transporte));
        collectSnapshotData(transaction, "Protocolo", String.valueOf(protocolo));
        collectSnapshotData(transaction, "Operacao", operacao);
        collectSnapshotData(transaction, "Sistema", sistema);

        if (exitCall != null) {
            return new State(exitCall, String.format("IIB - %s", transporte), operacao, startTime);
        } else {
            return null;
        }
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] params) {
        Transaction transaction = AppdynamicsAgent.getTransaction();

        if (isFakeTransaction(transaction))
            return null;

        Object result = null;

        try {
            if (className.equalsIgnoreCase("br.com.bb.ftw.transacao.GerenteTransacao")) {
                result = mainframeFtw(transaction, objectIntercepted, className, methodName, params);
            } else if (className.indexOf("br.com.bb.iib.Comunicacao") != -1) {
                result = mainframeIIB(transaction, objectIntercepted, className, methodName, params);
            }

            // getLogger().info("===> Application: " +
            // getSystemProperties("APPDYNAMICS_AGENT_APPLICATION_NAME"));
            // getLogger().info("===> Tier: " +
            // getSystemProperties("APPDYNAMICS_AGENT_TIER_NAME"));
            // getLogger().info("===> Node: " +
            // getSystemProperties("APPDYNAMICS_AGENT_NODE_NAME"));

        } catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
            result = null;
        }

        return result;

    }

    private String[] getBBEnvironment() {
        String[] environmentValues = { "", "" };

        try

        {
            String propertiesValue = System.getenv("APPDYNAMICS_AGENT_NODE_NAME");
            if (propertiesValue != null) {
                // pxl1mov2b036-ee85-8ad2-2d4e316e1bafe74
                // position 7 = CCT,
                // position 8 = BLUE or GREEN
                String envNumber = propertiesValue.substring(7, 8);
                String envType = propertiesValue.substring(8, 9);

                envNumber = "CCT" + envNumber;
                envType = envType.equalsIgnoreCase("b") ? "BLUE" : "GREEN";

                environmentValues[0] = envNumber;
                environmentValues[1] = envType;

            }

            getLogger().info("[getSystemProperties] [" + environmentValues[0] + "] [" + environmentValues[1] + "]");

        } catch (Exception e) {
            getLogger().error("[getSystemProperties] ", e);
            environmentValues[0] = "";
            environmentValues[1] = "";
        }

        return environmentValues;

    }

    public String getSystemProperties(String properties) {
        String propertiesValue = "";

        try {
            propertiesValue = System.getenv(properties);
            getLogger().info("[getSystemProperties] property [" + properties + "] value [" + propertiesValue + "]");
            if (propertiesValue == null)
                propertiesValue = "";
        } catch (Exception e) {
            getLogger().error("[getSystemProperties] ", e);
        }

        return propertiesValue;
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params,
            Throwable exception, Object returnVal) {

        if (state == null)
            return;

        boolean isError = false;
        long endTime = new Date().getTime();

        try {
            Transaction transaction = AppdynamicsAgent.getTransaction();

            ExitCall exitCall = ((State) state).exitCall;
            String basicMetricName = ((State) state).basicMetricName;
            String metricName = ((State) state).metricName;
            long startTime = ((State) state).startTime;

            if (exception != null) {
                transaction.markAsError(exception.getMessage());
                isError = true;
            }

            exitCall.end();

            String componentIdString = TransactionMonitoringService.getComponentIDString();

            String environment = "";
            String[] bbEnvironemnts = getBBEnvironment();
            if (!bbEnvironemnts[0].equals("")) {
                environment = bbEnvironemnts[0] + "|" + bbEnvironemnts[1];
            }

            String customName = String.format("Server|Component:%s|Custom Metrics|", componentIdString)
                    + basicMetricName;

            // FINAL METRICS
            reportMetric(customName + "|" + environment + "|" + metricName + "|Average Response Time (ms)",
                    endTime - startTime, "AVERAGE", "CURRENT", "COLLECTIVE");
            reportMetric(customName + "|" + environment + "|" + metricName + "|Calls per Minute",
                    1, "SUM", "CURRENT", "COLLECTIVE");
            reportMetric(customName + "|" + environment + "|" + metricName + "|Errors per Minute",
                    isError ? 1 : 0, "SUM", "CURRENT", "COLLECTIVE");

            // SUMMARY FOR TYPE CUSTOM BACKEND
            reportMetric(customName + "|Average Response Time (ms)",
                    endTime - startTime, "AVERAGE", "CURRENT", "COLLECTIVE");
            reportMetric(customName + "|Calls per Minute",
                    1, "SUM", "CURRENT", "COLLECTIVE");
            reportMetric(customName + "|Errors per Minute",
                    isError ? 1 : 0, "SUM", "CURRENT", "COLLECTIVE");

            // SUMMARY FOR TYPE CUSTOM BACKEND AND BB ENVIRONMENT
            reportMetric(customName + "|" + environment + "|Average Response Time (ms)",
                    endTime - startTime, "AVERAGE", "CURRENT", "COLLECTIVE");
            reportMetric(customName + "|" + environment + "|Calls per Minute",
                    1, "SUM", "CURRENT", "COLLECTIVE");
            reportMetric(customName + "|" + environment + "|Errors per Minute",
                    isError ? 1 : 0, "SUM", "CURRENT", "COLLECTIVE");

        } catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }

    }

    public class State {
        public State(ExitCall exitCall,
                String basicMetricName,
                String metricName,
                long startTime) {

            this.exitCall = exitCall;
            this.basicMetricName = basicMetricName;
            this.metricName = metricName;
            this.startTime = startTime;
        }

        public ExitCall exitCall;
        String basicMetricName;
        public String metricName;
        public long startTime;
    }
}
