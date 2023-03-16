package br.com.bb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.EntryTypes;
import com.appdynamics.agent.api.ExitCall;
import com.appdynamics.agent.api.ExitTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;

public class MainframeInterceptor extends MyBaseInterceptor {

    IReflector getInfo, getTransporte, getPadrao, getCodigo, getNome, getServico, getBarramento;

    public MainframeInterceptor() {
        super();

        getInfo = makeInvokeInstanceMethodReflector("getInfo");
        getTransporte = makeInvokeInstanceMethodReflector("getTransporte");
        getPadrao = makeInvokeInstanceMethodReflector("getPadrao");

        getCodigo = makeInvokeInstanceMethodReflector("getCodigo");
        getNome = makeInvokeInstanceMethodReflector("getNome");
        getServico = makeInvokeInstanceMethodReflector("getServico");
        getBarramento = makeInvokeInstanceMethodReflector("getBarramento");
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

        return rules;
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] params) {
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if (isFakeTransaction(transaction))
            return null;

        // Param 01
        // => getInfo().getTransporte()
        // => getInfo().getPadrao()

        Object paramTransaction = params[1];

        Object info = getReflectiveObject(paramTransaction, getInfo);
        String transporte = getReflectiveString(info, getTransporte, "UNKNOW-TRANSPORTE");
        String padrao = getReflectiveString(info, getPadrao, "UNKNOW-PADRAO");

        String codigo = getReflectiveString(info, getCodigo, "UNKNOW-CODIGO");
        String nome = getReflectiveString(info, getNome, "UNKNOW-NOME");
        String servico = getReflectiveString(info, getServico, "UNKNOW-SERVICO");
        String barramento = getReflectiveString(info, getBarramento, "UNKNOW-BARRAMENTO");

        String exitCallName = String.format("FTW - %s.%s", transporte, padrao);

        Map<String, String> map = new HashMap<>();
        map.put("TRANSPORTE", transporte);
        map.put("PADRAO", padrao);

        ExitCall exitCall = transaction.startExitCall(map, exitCallName,
                ExitTypes.CUSTOM, false);

        Transaction transactionSED = AppdynamicsAgent.startTransactionAndServiceEndPoint("XXX", null,
                String.format("SED %s.%s.%s", transporte, padrao, codigo), EntryTypes.POJO, false);

        // getLogger().info("==> SED begin: " + transactionSED.getUniqueIdentifier() + "
        // ==> " + String.format(
        // "SED %s.%s.%s", transporte, padrao,
        // codigo));

        // Data Collectors
        collectSnapshotData(transaction, "Transporte", transporte);
        collectSnapshotData(transaction, "Padrao", padrao);
        collectSnapshotData(transaction, "Codigo", codigo);
        collectSnapshotData(transaction, "Nome", nome);
        collectSnapshotData(transaction, "Servico", servico);
        collectSnapshotData(transaction, "Barramento", barramento);

        return new State(transaction, transactionSED, exitCall, transporte, padrao);
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params,
            Throwable exception, Object returnVal) {

        if (state == null)
            return;

        Transaction transaction = ((State) state).transaction;
        Transaction transactionSED = ((State) state).transactionSED;
        ExitCall exitCall = ((State) state).exitCall;
        String transporte = ((State) state).transporte;
        String padrao = ((State) state).padrao;

        if (exception != null) {
            transaction.markAsError(String.format("FTW - %s.%s Exception: %s", transporte, padrao, exception));
            transactionSED.markAsError(String.format("FTW - %s.%s Exception: %s", transporte, padrao, exception));
        }

        // getLogger().info("==> SED end: " + transactionSED.getUniqueIdentifier());
        exitCall.end();
        transactionSED.end();

        // MetricPublisher metricPublisher = AppdynamicsAgent.getMetricPublisher();
        // metricPublisher.reportObservedMetric(exitCallName + "|" + subject + "|Days To
        // Expiration", daysToExpiration);

    }

    public class State {
        public State(Transaction transaction, Transaction transactionSED, ExitCall exitCall,
                String transporte,
                String padrao) {

            this.transaction = transaction;
            this.exitCall = exitCall;
            this.transporte = transporte;
            this.padrao = padrao;
            this.transactionSED = transactionSED;

        }

        public Transaction transaction;
        public Transaction transactionSED;
        public ExitCall exitCall;
        public String transporte;
        public String padrao;
    }
}
