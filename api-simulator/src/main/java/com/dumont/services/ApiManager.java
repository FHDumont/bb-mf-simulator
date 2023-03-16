package com.dumont.services;

import java.util.Random;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.bb.ftw.transacao.GerenteTransacao;
import br.com.bb.ftw.transacao.InfoTransacao;
import br.com.bb.ftw.transacao.Transacao;

@RestController
@RequestMapping("/api")
public class ApiManager {

    @RequestMapping(value = "/executeFTW/{transporte}/{padrao}/{codigo}/{nome}/{servico}/{barramento}", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<String> executeTransaction(
            @PathVariable("transporte") String transporte,
            @PathVariable("padrao") String padrao,
            @PathVariable("codigo") String codigo,
            @PathVariable("nome") String nome,
            @PathVariable("servico") String servico,
            @PathVariable("barramento") String barramento) throws Exception {

        InfoTransacao infoTransacao = new InfoTransacao(nome, codigo, padrao);
        infoTransacao.setBarramento(barramento);
        infoTransacao.setTransporte(transporte);
        infoTransacao.setServico(servico);
        Transacao transacao = new Transacao(infoTransacao);

        GerenteTransacao gt = new GerenteTransacao() {
        };

        gt.processar(null, transacao);

        Random rand = new Random();
        if (rand.nextInt(5) <= 1) {
            System.out.println("==> TWICE");
            gt.processar(null, transacao);
        }

        return new ResponseEntity<>("{ \"value\" : \"OK\" }", HttpStatus.OK);
    }

}
