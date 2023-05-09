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

let DATA_IIB = [
  {
    transporte: "EMS",
    protocolo: "IIB",
    release: "1",
    sistema: "CFE",
    sysplex: "1",
    operacao: "5888844",
    servico: "0",
    aplicacaoProvedora: "0",
    versao: "0",
  },
  {
    transporte: "EMS",
    protocolo: "IIB",
    release: "1",
    sistema: "ABC",
    sysplex: "1",
    operacao: "39965+",
    servico: "0",
    aplicacaoProvedora: "0",
    versao: "0",
  },
  {
    transporte: "Rendezvous",
    protocolo: "IIB",
    release: "1",
    sistema: "MOV",
    sysplex: "1",
    operacao: "593532",
    servico: "0",
    aplicacaoProvedora: "0",
    versao: "0",
  },
  {
    transporte: "Rendezvous",
    protocolo: "IIB",
    release: "1",
    sistema: "XYZ",
    sysplex: "1",
    operacao: "643234",
    servico: "0",
    aplicacaoProvedora: "0",
    versao: "0",
  },
  {
    transporte: "MQ",
    protocolo: "IIB",
    release: "1",
    sistema: "CCC",
    sysplex: "1",
    operacao: "12642",
    servico: "0",
    aplicacaoProvedora: "0",
    versao: "0",
  },
];

async function runFTW() {
  console.log(`====> Starting FTW`);

  while (true) {
    let example_ftw = DATA_FTW[lodash.random(0, DATA_FTW.length - 1)];
    let url = new URL(
      `${API_SERVER}/executeFTW/${example_ftw["transporte"]}/${example_ftw["padrao"]}/${example_ftw["codigo"]}/${example_ftw["nome"]}/${example_ftw["servico"]}/${example_ftw["barramento"]}`
    );

    // http://localhost:8080/api/executeFTW/Servcom/CICS/GTKA/VerificarComunicacaoComGTR/7463Basico/Transacional

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

async function runIIB() {
  console.log(`====> Starting IIB`);

  while (true) {
    let example_iib = DATA_IIB[lodash.random(0, DATA_IIB.length - 1)];
    let url = new URL(
      `${API_SERVER}/executeIIB/${example_iib["transporte"]}/${example_iib["protocolo"]}/${example_iib["release"]}/${example_iib["sistema"]}/${example_iib["sysplex"]}/${example_iib["operacao"]}/${example_iib["servico"]}/${example_iib["aplicacaoProvedora"]}/${example_iib["versao"]}`
    );

    // http://localhost:8080/api/executeIIB/EMS/IIB/1/CEF/1/5888844/0/0/0

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
  runIIB();
  runFTW();
};
