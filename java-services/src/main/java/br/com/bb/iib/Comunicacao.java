package br.com.bb.iib;

import java.io.Serializable;

public interface Comunicacao extends Serializable {

    public void executar(ContextoExecucao contextoExecucao);
}
