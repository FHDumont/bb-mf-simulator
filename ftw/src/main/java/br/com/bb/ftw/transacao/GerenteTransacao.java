package br.com.bb.ftw.transacao;

import java.util.Random;

import br.com.bb.ftw.BBException;
import br.com.bb.ftw.sessao.Sessao;

public abstract class GerenteTransacao {

    // private final Propriedades mConfiguracao;

    // public GerenteTransacao(Propriedades configuracao) {
    // this.mConfiguracao = configuracao;
    // }

    public GerenteTransacao() {
    }

    // Instrumentar este método
    protected byte[] processar(Sessao sessao, Transacao transacao, byte[] requisicao) throws BBException {
        try {

            System.out.println(transacao.getInfo().getCodigo() + " - " + transacao.getInfo().getNome() + " - "
                    + transacao.getInfo().getServico()
                    + " - " + transacao.getInfo().getBarramento() + " - " + transacao.getInfo().getTransporte() + " - "
                    + transacao.getInfo().getPadrao());

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
        return new byte[1];
    }

    public void processar(Sessao sessao, Transacao transacao) throws BBException {
        processar(sessao, transacao, new byte[1]);
    }

}
