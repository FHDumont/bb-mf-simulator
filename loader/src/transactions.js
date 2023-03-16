const fs = require("fs");
const yaml = require("js-yaml");
const puppeteer = require("puppeteer");
const needle = require("needle");
const lodash = require("lodash");
const chance = require("chance").Chance();
const url = require("url");
const path = require("path");
const { isError } = require("lodash");

const TIME_OUT_PAGE = 30000;
const API_SERVER = process.env.API_SERVER || "http://localhost:8080/api";

let DATA_FTW = [
  {
    transporte: "Servcom",
    padrao: "CICS",
    codigo: "GTKA",
    nome: "Verificar Comunicação com GTR",
    servico: "7463 Básico",
    barramento: "Transacional",
  },
  {
    transporte: "Servcom",
    padrao: "CICS",
    codigo: "ATOM",
    nome: "Identificação Celular - ID",
    servico: "7463 Básico",
    barramento: "Transacional",
  },
  {
    transporte: "Servcom",
    padrao: "CICS",
    codigo: "ATOM",
    nome: "Identificação Celular - ID",
    servico: "7463 Básico",
    barramento: "Transacional",
  },
  {
    transporte: "Rendezvous",
    padrao: "IIB",
    codigo: "CNL1",
    nome: "Gerar Ticket da sessão de atendimento",
    servico: "IIB IIB",
    barramento: "Transacional",
  },
  {
    transporte: "Rendezvous",
    padrao: "IIB",
    codigo: "IIBRL",
    nome: "Consulta Personalização",
    servico: "IIB IIB",
    barramento: "Transacional",
  },
  {
    transporte: "Rendezvous",
    padrao: "GRI",
    codigo: "8BX",
    nome: "CDC - Crédito Novo",
    servico: "GRI GRI",
    barramento: "Transacional",
  },
];

// FTW
// Class: br.com.bb.ftw.transacao.GerenteTransacao
// Method: processar
// => PARAM 01
// => getInfo().getTransporte()
// => getInfo().getPadrao()
// FTW - Rendezvous.IIB -> Rendezvous.GRI -> Servcom.CICS -> Servcom.GRI
// getInfo().getCodigo()
// getInfo().getNome()
// getInfo().getServico()
// getInfo().getBarramento()
// getInfo().getTransporte()
// getInfo().getPadrao()

// BARRAMENTO=TRANSACIONAL,TRANSPORTE=SERVCOM,SERVICO=7643_BÁSICO,NOME=VERIFICAR_COMUNICACAO_COM_GTR,PADRAO=CICS,CODIGO=GTKA
// BARRAMENTO=TRANSACIONAL,TRANSPORTE=SERVCOM,SERVICO=7643_BÁSICO,NOME=IDENTIFICACAÇÃO_CELULAR_-_ID,PADRAO=CICS,CODIGO=ATOM
// BARRAMENTO=TRANSACIONAL,TRANSPORTE=RENDEZVOUS,SERVICO=IIB_IIB,NOME=CONSULTA_PERSONALIZACAO,PADRAO=IIB,CODIGO=IIBRL
// BARRAMENTO=TRANSACIONAL,TRANSPORTE=RENDEZVOUS,SERVICO=IIB_IIB,NOME=GERAR_TICKET_DA_SESSAO_DE_ATENDIMENTO,PADRAO=IIB,CODIGO=CNL1

// IIB
// Interface: br.com.bb.iib.Comunicacao
// Method: executar
// => PARAM 00
// => getInfoOperacao().getTransporte()
// => getInfoOperacao().getProtocolo()
// IIB - EMS.IIB -> MQ.IIB -> Rendezvous.IIB
// getInfoOperacao().getIdentificacao().getRelease()
// getInfoOperacao().getIdentificacao().getSistema()
// getInfoOperacao().getIdentificacao().getSysplex()
// getInfoOperacao().getIdentificacao().getOperacao()
// getInfoOperacao().getIdentificacao().getServico()
// getInfoOperacao().getIdentificacao().getAplicacaoProvedora()

// APLICACAOPROVEDORA=0,OPERACAO=3042527,SERVICO=0,VERSAO=0,SYSPLEX=1,RELEASE=3,SISTEMA=MIV
// APLICACAOPROVEDORA=4307835.1,OPERACAO=3383065,SERVICO=0,VERSAO=0,SYSPLEX=1,RELEASE=2,SISTEMA=MOV
// APLICACAOPROVEDORA=4307795.1,OPERACAO=4687187,SERVICO=0,VERSAO=0,SYSPLEX=1,RELEASE=1,SISTEMA=IDH

async function run() {
  console.log(`[=> Starting`);

  while (true) {
    let example_ftw = DATA_FTW[lodash.random(0, DATA_FTW.length - 1)];
    let url = new URL(
      `${API_SERVER}/executeFTW/${example_ftw["transporte"]}/${example_ftw["padrao"]}/${example_ftw["codigo"]}/${example_ftw["nome"]}/${example_ftw["servico"]}/${example_ftw["barramento"]}`
    );
    console.log(" ==> Requestion ", url.href);
    let isError = false;
    try {
      needle.request(
        "get",
        url.href,
        {},
        { headers: { "Content-Type": "application/json" } },
        function (err, result) {
          if (result != undefined) {
            console.log("Return ", result.statusCode, JSON.parse(result.raw));
          }

          if (err != undefined) {
            isError = true;
          }
        }
      );
    } catch (error) {
    } finally {
      await new Promise((resolve) => setTimeout(resolve, isError ? 5000 : 500));
    }
  }
}

exports.main = async function () {
  await run();
};
