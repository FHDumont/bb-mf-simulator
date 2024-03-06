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
import br.com.bb.iib.ComunicacaoExecutar;
import br.com.bb.iib.ContextoExecucao;
import br.com.bb.iib.Identificacao;
import br.com.bb.iib.InfoOperacao;

@RestController
@RequestMapping("/api")
public class ApiSimulator {

	@RequestMapping(value = "/executeFTW/{transporte}/{padrao}/{codigo}/{nome}/{servico}/{barramento}", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
	@ResponseBody
	public ResponseEntity<String> executeFTW(
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

	@RequestMapping(value = "/executeIIB/{transporte}/{protocolo}/{release}/{sistema}/{sysplex}/{operacao}/{servico}/{aplicacaoProvedora}/{versao}", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
	@ResponseBody
	public ResponseEntity<String> executeIIB(
			@PathVariable("transporte") String transporte,
			@PathVariable("protocolo") String protocolo,
			@PathVariable("release") String release,
			@PathVariable("sistema") String sistema,
			@PathVariable("sysplex") String sysplex,
			@PathVariable("operacao") String operacao,
			@PathVariable("servico") String servico,
			@PathVariable("aplicacaoProvedora") String aplicacaoProvedora,
			@PathVariable("versao") String versao) throws Exception {

		Identificacao identificacao = new Identificacao();
		identificacao.setAplicacaoProvedora(aplicacaoProvedora);
		identificacao.setOperacao(operacao);
		identificacao.setRelease(release);
		identificacao.setServico(servico);
		identificacao.setSistema(sistema);
		identificacao.setSysplex(sysplex);
		identificacao.setVersao(versao);

		InfoOperacao infoOperacao = new InfoOperacao();
		infoOperacao.setTransporte(transporte);
		infoOperacao.setProtocolo(protocolo);
		infoOperacao.setIdentificacao(identificacao);

		ContextoExecucao contextoExecucao = new ContextoExecucao();
		contextoExecucao.setInfoOperacao(infoOperacao);

		ComunicacaoExecutar comunicacaoExecutar = new ComunicacaoExecutar();
		comunicacaoExecutar.executar(contextoExecucao);

		Random rand = new Random();
		if (rand.nextInt(5) <= 1) {
			System.out.println("==> TWICE");
			comunicacaoExecutar.executar(contextoExecucao);
		}

		return new ResponseEntity<>("{ \"value\" : \"OK\" }", HttpStatus.OK);
	}

}
