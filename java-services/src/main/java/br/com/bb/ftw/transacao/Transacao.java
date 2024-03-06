package br.com.bb.ftw.transacao;

public class Transacao implements Cloneable {

    private InfoTransacao mInfo;

    public Transacao(InfoTransacao info) {
        this.mInfo = info;
    }

    public InfoTransacao getInfo() {
        return this.mInfo;
    }

    public String getNome() {
        return this.mInfo.getNome();
    }

    public String getCodigo() {
        return this.mInfo.getCodigo();
    }

    public String getPadrao() {
        return this.mInfo.getPadrao();
    }
}
