package br.com.bb.iib;

public class InfoOperacao {

    String transporte;
    String protocolo;
    Identificacao identificacao;

    /**
     * @param transporte the transporte to set
     */
    public void setTransporte(String transporte) {
        this.transporte = transporte;
    }

    /**
     * @param protocolo the protocolo to set
     */
    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    /**
     * @param identificacao the identificacao to set
     */
    public void setIdentificacao(Identificacao identificacao) {
        this.identificacao = identificacao;
    }

    public String getTransporte() {
        return transporte;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public Identificacao getIdentificacao() {
        return identificacao;
    }

}
