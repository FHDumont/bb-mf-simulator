package br.com.bb.iib;

import java.util.Random;

public class ComunicacaoExecutar implements Comunicacao {

    @Override
    public void executar(ContextoExecucao contextoExecucao) {
        try {

            System.out.println(contextoExecucao.getInfoOperacao().getTransporte() + " - "
                    + contextoExecucao.getInfoOperacao().getProtocolo() + " - "
                    + contextoExecucao.getInfoOperacao().getIdentificacao().getAplicacaoProvedora() + " - "
                    + contextoExecucao.getInfoOperacao().getIdentificacao().getOperacao() + " - "
                    + contextoExecucao.getInfoOperacao().getIdentificacao().getRelease() + " - "
                    + contextoExecucao.getInfoOperacao().getIdentificacao().getServico() + " - "
                    + contextoExecucao.getInfoOperacao().getIdentificacao().getSistema() + " - "
                    + contextoExecucao.getInfoOperacao().getIdentificacao().getSysplex() + " - "
                    + contextoExecucao.getInfoOperacao().getIdentificacao().getVersao());

            Random rand = new Random();
            int delayStart = 1000;
            int delay = rand.nextInt(5000 - delayStart) + delayStart;
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

        } catch (Exception errorEx) {
            errorEx.printStackTrace();
        }
    }

}
