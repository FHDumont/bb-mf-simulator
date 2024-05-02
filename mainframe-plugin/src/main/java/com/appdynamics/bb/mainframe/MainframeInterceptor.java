package com.appdynamics.bb.mainframe;

import java.net.InetAddress;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.ExitCall;
import com.appdynamics.agent.api.ExitTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.apm.appagent.api.DataScope;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;
import com.singularity.ee.agent.appagent.services.transactionmonitor.TransactionMonitoringService;

public class MainframeInterceptor extends AGenericInterceptor {

    private IReflector reflectionGetTransporte, reflectionInfoOperacao, reflectionInfo, reflectionGetPadrao, reflectionGetCodigo, reflectionGetCodigoServico,
            reflectionGetNome, reflectionGetServico, reflectionGetBarramento, reflectionGetProtocolo, reflectionOperacao, reflectionIdentificacao,
            reflectionSistema;

    private Set<DataScope> dataScopes;
    private boolean initialized = false;

    public MainframeInterceptor() {
        super();

        // FTW & IIB
        this.reflectionGetTransporte = makeInvokeInstanceMethodReflector("getTransporte");
        this.reflectionGetServico = makeInvokeInstanceMethodReflector("getServico");
        this.reflectionGetCodigoServico = makeInvokeInstanceMethodReflector("getCodigoServico");

        // FTW
        this.reflectionInfo = makeInvokeInstanceMethodReflector("getInfo");
        this.reflectionGetPadrao = makeInvokeInstanceMethodReflector("getPadrao");
        this.reflectionGetCodigo = makeInvokeInstanceMethodReflector("getCodigo");
        this.reflectionGetNome = makeInvokeInstanceMethodReflector("getNome");
        this.reflectionGetBarramento = makeInvokeInstanceMethodReflector("getBarramento");

        // IIB
        this.reflectionInfoOperacao = makeInvokeInstanceMethodReflector("getInfoOperacao");
        this.reflectionIdentificacao = makeInvokeInstanceMethodReflector("getIdentificacao");
        this.reflectionGetProtocolo = makeInvokeInstanceMethodReflector("getProtocolo");
        this.reflectionSistema = makeInvokeInstanceMethodReflector("getSistema");
        this.reflectionOperacao = makeInvokeInstanceMethodReflector("getOperacao");

        initialize();
        getLogger().info(String.format("Initialized plugin class %s version %s build date %s", getClass().getCanonicalName(), MetaData.VERSION,
                MetaData.BUILDTIMESTAMP));

    }

    @Override
    public Object onMethodBegin(Object invokedObject, String className, String methodName, Object[] paramValues) {

        Transaction transaction = AppdynamicsAgent.getTransaction();

        if (Common.isFakeTransaction(transaction)) {
            return null;
        }

        Object result = null;

        try {
            if (className.equalsIgnoreCase("br.com.bb.ftw.transacao.GerenteTransacao")) {
                result = mainframeFtw(transaction, invokedObject, className, methodName, paramValues);
            } else if (className.indexOf("br.com.bb.iib.Comunicacao") != -1) {
                result = mainframeIIB(transaction, invokedObject, className, methodName, paramValues);
            }
        } catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    public void onMethodEnd(Object state, Object invokedObject, String className, String methodName, Object[] paramValues, Throwable thrownException,
            Object returnValue) {

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

            basicMetricName = Normalizer.normalize(basicMetricName, Normalizer.Form.NFD);
            basicMetricName = basicMetricName.replaceAll("\\p{M}", "");

            metricName = Normalizer.normalize(metricName, Normalizer.Form.NFD);
            metricName = metricName.replaceAll("\\p{M}", "");

            if (thrownException != null) {
                transaction.markAsError(thrownException.getMessage());
                isError = true;
            }

            exitCall.end();

            String componentIdString = TransactionMonitoringService.getComponentIDString();

            String environmentFirst = "";
            String environmentSecond = "";
            String[] bbEnvironemnts = getBBEnvironment();
            if (!bbEnvironemnts[0].equals("")) {
                environmentFirst = bbEnvironemnts[0];
                environmentSecond = bbEnvironemnts[1];
            }

            this.getLogger()
                    .debug(String.format("environmentFirst [%s] environmentSecond [%s] metricName [%s]", environmentFirst, environmentSecond, metricName));

            String customName = String.format("Server|Component:%s|Custom Metrics|", componentIdString) + basicMetricName;

            // FINAL METRICS (EX: FTW - Servcom.CICS)
            reportMetric(customName + "|Average Response Time (ms)", endTime - startTime, "AVERAGE", "CURRENT", "COLLECTIVE");
            reportMetric(customName + "|Calls per Minute", 1, "SUM", "CURRENT", "COLLECTIVE");
            reportMetric(customName + "|Errors per Minute", isError ? 1 : 0, "SUM", "CURRENT", "COLLECTIVE");

            if (!"".equals(environmentFirst)) {
                // SUMMARY FOR FIRST BB ENVIRONMENT LEVEL (EX: CCT1)
                reportMetric(customName + "|" + environmentFirst + "|Average Response Time (ms)", endTime - startTime, "AVERAGE", "CURRENT", "COLLECTIVE");
                reportMetric(customName + "|" + environmentFirst + "|Calls per Minute", 1, "SUM", "CURRENT", "COLLECTIVE");
                reportMetric(customName + "|" + environmentFirst + "|Errors per Minute", isError ? 1 : 0, "SUM", "CURRENT", "COLLECTIVE");
                // SUMMARY FOR SECOND BB ENVIRONMENT LEVEL (EX: CCT1|BLUE)
                reportMetric(customName + "|" + environmentFirst + "|" + environmentSecond + "|Average Response Time (ms)", endTime - startTime, "AVERAGE",
                        "CURRENT", "COLLECTIVE");
                reportMetric(customName + "|" + environmentFirst + "|" + environmentSecond + "|Calls per Minute", 1, "SUM", "CURRENT", "COLLECTIVE");
                reportMetric(customName + "|" + environmentFirst + "|" + environmentSecond + "|Errors per Minute", isError ? 1 : 0, "SUM", "CURRENT",
                        "COLLECTIVE");

                // SUMMARY FOR TYPE CUSTOM BACKEND AND BB ENVIRONMENT (EX: CCT1|BLUE|OPERATION)
                reportMetric(customName + "|" + environmentFirst + "|" + environmentSecond + "|" + metricName + "|Average Response Time (ms)",
                        endTime - startTime, "AVERAGE", "CURRENT", "COLLECTIVE");
                reportMetric(customName + "|" + environmentFirst + "|" + environmentSecond + "|" + metricName + "|Calls per Minute", 1, "SUM", "CURRENT",
                        "COLLECTIVE");
                reportMetric(customName + "|" + environmentFirst + "|" + environmentSecond + "|" + metricName + "|Errors per Minute", isError ? 1 : 0, "SUM",
                        "CURRENT", "COLLECTIVE");
            } else {
                // SUMMARY FOR TYPE CUSTOM BACKEND AND WITHOUT BB ENVIRONMENT (EX: OPERATION)
                reportMetric(customName + "|" + metricName + "|Average Response Time (ms)", endTime - startTime, "AVERAGE", "CURRENT", "COLLECTIVE");
                reportMetric(customName + "|" + metricName + "|Calls per Minute", 1, "SUM", "CURRENT", "COLLECTIVE");
                reportMetric(customName + "|" + metricName + "|Errors per Minute", isError ? 1 : 0, "SUM", "CURRENT", "COLLECTIVE");
            }

        } catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }

    }

    @Override
    public List<Rule> initializeRules() {

        List<Rule> rules = new ArrayList<Rule>();

        rules.add(new Rule.Builder("br.com.bb.ftw.transacao.GerenteTransacao").classMatchType(SDKClassMatchType.MATCHES_CLASS).methodMatchString("processar")
                .withParams("br.com.bb.ftw.sessao.Sessao", "br.com.bb.ftw.transacao.Transacao", "byte[]").build());

        rules.add(new Rule.Builder("br.com.bb.iib.Comunicacao").classMatchType(SDKClassMatchType.IMPLEMENTS_INTERFACE).methodMatchString("executar")
                .withParams("br.com.bb.iib.ContextoExecucao").build());

        return rules;
    }

    protected void initialize() {
        this.dataScopes = new HashSet<DataScope>();

        try {
            this.dataScopes = new HashSet<DataScope>();
            this.dataScopes.add(DataScope.SNAPSHOTS);
            this.dataScopes.add(DataScope.ANALYTICS);
            this.initialized = true;
        } catch (Exception e) {
            getLogger().error("Failed to initialize data scopes: " + e.getMessage(), e);
        }
    }

    protected Object mainframeFtw(Transaction transaction, Object objectIntercepted, String className, String methodName, Object[] params) throws Exception {

        Map<String, String> map = new HashMap<>();
        String exitCallName = "";
        long startTime = new Date().getTime();
        ExitCall exitCall = null;

        if (!className.equalsIgnoreCase("br.com.bb.ftw.transacao.GerenteTransacao"))
            return null;

        // Params
        Object paramTransaction = params[1];

        // Fields
        Object infoTransacao = getReflectiveObject(paramTransaction, reflectionInfo);

        String transporte = getReflectiveString(infoTransacao, reflectionGetTransporte, "UNKNOW-TRANSPORTE");
        String padrao = getReflectiveString(infoTransacao, reflectionGetPadrao, "UNKNOW-PADRAO");
        String codigo = getReflectiveString(infoTransacao, reflectionGetCodigo, "UNKNOW-CODIGO");
        String nome = getReflectiveString(infoTransacao, reflectionGetNome, "UNKNOW-NOME");
        String barramento = getReflectiveString(infoTransacao, reflectionGetBarramento, "UNKNOW-BARRAMENTO");

        Object servico = getReflectiveObject(infoTransacao, reflectionGetServico);
        String codigoServico = getReflectiveString(servico, reflectionGetCodigoServico, "UNKNOW-CODIGO-SERVICO");

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
            return new State(exitCall, String.format("FTW - %s.%s", transporte, padrao), String.format("%s - %s", codigo, nome), startTime);
        } else {
            return null;
        }
    }

    protected Object mainframeIIB(Transaction transaction, Object objectIntercepted, String className, String methodName, Object[] params) throws Exception {

        Map<String, String> map = new HashMap<>();
        String exitCallName = "";
        long startTime = new Date().getTime();
        ExitCall exitCall = null;

        if (className.indexOf("br.com.bb.iib.Comunicacao") == -1)
            return null;

        // Params
        Object contextoExecucao = params[0];

        // Fields
        Object infoOperacao = getReflectiveObject(contextoExecucao, reflectionInfoOperacao);
        String operacaoNome = infoOperacao.getClass().getSimpleName();

        Object transporte = getReflectiveObject(infoOperacao, reflectionGetTransporte);
        Object protocolo = getReflectiveObject(infoOperacao, reflectionGetProtocolo);
        Object identificacao = getReflectiveObject(infoOperacao, reflectionIdentificacao);

        String sistema = getReflectiveString(identificacao, reflectionSistema, "UNKNOW-SISTEMA");
        String operacao = getReflectiveString(identificacao, reflectionOperacao, "UNKNOW-OPERACAO");

        // Custom Backend
        exitCallName = String.format("IIB - %s", transporte);
        map.put("TRANSPORTE", String.valueOf(transporte));
        map.put("PROTOCOLO", String.valueOf(protocolo));
        exitCall = transaction.startExitCall(map, exitCallName, ExitTypes.CUSTOM, false);

        // Data Collectors
        collectSnapshotData(transaction, "Trnasporte", String.valueOf(transporte));
        collectSnapshotData(transaction, "Protocolo", String.valueOf(protocolo));
        collectSnapshotData(transaction, "Operacao", operacao);
        collectSnapshotData(transaction, "Operacao Nome", operacaoNome);
        collectSnapshotData(transaction, "Sistema", sistema);

        if (exitCall != null) {
            return new State(exitCall, String.format("IIB - %s", transporte), String.format("%s - %s", operacao, operacaoNome), startTime);
        } else {
            return null;
        }
    }

    protected class State {
        public State(ExitCall exitCall, String basicMetricName, String metricName, long startTime) {

            this.exitCall = exitCall;
            this.basicMetricName = basicMetricName;
            this.metricName = metricName;
            this.startTime = startTime;
        }

        public ExitCall exitCall;
        public String basicMetricName;
        public String metricName;
        public long startTime;
    }

    protected IReflector makeInvokeInstanceMethodReflector(String method, String... args) {
        try {
            return args.length > 0 ? getNewReflectionBuilder().invokeInstanceMethod(method, true, args).build()
                    : getNewReflectionBuilder().invokeInstanceMethod(method, true).build();
        } catch (Exception e) {
            getLogger().error("Failed makeInvokeInstanceMethodReflector: " + method, e);
            return null;
        }
    }

    protected void collectSnapshotData(Transaction transaction, String name, String value) {
        if (transaction == null)
            return;
        if (!this.initialized) {
            initialize();
        }
        transaction.collectData(name, value, this.dataScopes);
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
            this.getLogger().debug("Error in reflection call, method: " + method.getClass().getCanonicalName() + " object: "
                    + object.getClass().getCanonicalName() + " exception: " + e.getMessage(), e);
        }
        return value;
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

    protected String[] getBBEnvironment() {
        String[] environmentValues = { "", "" };

        try

        {
            // O nodename poderá vir de vários lugares diferentes, vai depender de como foi configurado, a ser testado na seguinte sequencia:
            // 1) normalmente o ZFI faz o insert do parâmetro -D
            // 2) variável de ambiente
            // 3) hostname
            // senão encontrar nenhum deles então não criar a hierárquia com estes nomes

            String nodeName = "";

            // property
            nodeName = System.getProperty("appdynamics.agent.nodeName");
            if (nodeName == null || "".equals(nodeName)) {
                nodeName = System.getenv("APPDYNAMICS_AGENT_NODE_NAME");
                if (nodeName == null || "".equals(nodeName)) {
                    nodeName = InetAddress.getLocalHost().getHostName();
                }
            }

            getLogger().debug(String.format("[getBBEnvironment] System.getProperty [%s]", System.getProperty("appdynamics.agent.nodeName")));
            getLogger().debug(String.format("[getBBEnvironment] System.getenv [%s]", System.getenv("APPDYNAMICS_AGENT_NODE_NAME")));
            getLogger().debug(String.format("[getBBEnvironment] getHostName [%s]", InetAddress.getLocalHost().getHostName()));
            getLogger().debug(String.format("[getBBEnvironment] final result [%s]", nodeName));

            if (nodeName != null && !"".equals(nodeName)) {
                if ("p".equalsIgnoreCase(nodeName.substring(0, 1))) {
                    // O split só pode ser feito quando for produção, neste caso o servidor começa com P
                    // pxl1mov2b036-ee85-8ad2-2d4e316e1bafe74
                    // position 7 = CCT,
                    // position 8 = BLUE or GREEN
                    String envNumber = nodeName.substring(7, 8);
                    String envType = nodeName.substring(8, 9);

                    envNumber = "CCT" + envNumber;
                    envType = envType.equalsIgnoreCase("b") ? "BLUE" : envType.equalsIgnoreCase("g") ? "GREEN" : "UNKNOWN";

                    environmentValues[0] = envNumber;
                    environmentValues[1] = envType;
                } else {
                    getLogger().debug("[getBBEnvironment] Hierarquia paddrão das métricas, ambiente não é de produção");
                }

            }

            getLogger().info("[getBBEnvironment] [" + environmentValues[0] + "] [" + environmentValues[1] + "]");

        } catch (Exception e) {
            getLogger().error("[getBBEnvironment] ", e);
            environmentValues[0] = "";
            environmentValues[1] = "";
        }

        return environmentValues;

    }

    protected void reportMetric(String metricName, long metricValue, String aggregationType, String timeRollupType, String clusterRollupType) {
        this.getLogger().debug("Begin reportMetric name: " + metricName + " = " + metricValue + " aggregation type: " + aggregationType + " time rollup type: "
                + timeRollupType + " cluster rollup type: " + clusterRollupType);
        try {
            AppdynamicsAgent.getMetricPublisher().reportMetric(metricName, metricValue, aggregationType, timeRollupType, clusterRollupType);
        } catch (Exception e) {
            getLogger().error("Failed to report metric: " + e.getMessage(), e);
        }
    }
}
