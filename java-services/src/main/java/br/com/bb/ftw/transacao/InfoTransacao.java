package br.com.bb.ftw.transacao;

public class InfoTransacao {

    private String nome;
    private String codigo;
    private String padrao;

    private String servico;
    private String barramento;
    private String transporte;

    public InfoTransacao(String nome, String codigo, String padrao) {
        this.nome = nome;
        this.codigo = codigo;
        this.padrao = padrao;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getPadrao() {
        return padrao;
    }

    /**
     * @return the servico
     */
    public String getServico() {
        return servico;
    }

    /**
     * @param servico the servico to set
     */
    public void setServico(String servico) {
        this.servico = servico;
    }

    /**
     * @return the barramento
     */
    public String getBarramento() {
        return barramento;
    }

    /**
     * @param barramento the barramento to set
     */
    public void setBarramento(String barramento) {
        this.barramento = barramento;
    }

    /**
     * @return the transporte
     */
    public String getTransporte() {
        return transporte;
    }

    /**
     * @param transporte the transporte to set
     */
    public void setTransporte(String transporte) {
        this.transporte = transporte;
    }

}
