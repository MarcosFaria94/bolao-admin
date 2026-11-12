// Bolão dos Amigos: o "Transcrever" da tela da Caixa (desde a 0.31).
//
// Roda dentro da página de uma modalidade do Loterias Online da Caixa (o app chama esta função com o plano da aposta,
// feito em domain/Transcricao.kt): limpa o volante do site, marca a aposta, confere o que o site marcou e o valor, e
// clica em "Colocar no Carrinho". NUNCA vai para o pagamento: o usuário confere o carrinho e paga sozinho.
//
// A resposta fica em window.__bolaoTranscricao.fim (o app pergunta de tempos em tempos):
//   { ok, noCarrinho, parcial, semEspelho, mensagem }
// parcial = o volante ficou marcado, mas parou antes do carrinho para o usuário conferir (o que faltou vai na mensagem).
//
// O que se sabe do site (levantado por quem já automatizou o volante; a Caixa muda o site de vez em quando):
// - números em a#n01... (na Lotomania o 00 é a#n00), a marcada com a classe "selected"; acima da aposta simples, um
//   clique em #aumentarnumero por número a mais, antes de marcar; #limparvolante, #colocarnocarrinho (o site limpa o
//   volante quando aceita), #valoraposta ("R$ 6,00"), o contador do carrinho em #carrinho;
// - Loteca: a.loteca_palpite (14 jogos x 3 colunas, na ordem), marcado com "loteca_palpite_selecionado", valor em
//   .valor-aposta;
// - Timemania: img.data-selecionar-time-do-coracao com o nome em .nomeTime ao lado, o escolhido com "active"; a busca
//   #txtTimeCoracao filtra a lista (tem de estar vazia);
// - +Milionária: os trevos em #trevo1 a #trevo6; Dia de Sorte: os meses em #carrossel_diadesorte; Lotomania: a
//   "Aposta espelho" em input#apostaEspelho; teimosinha em #aumentarteimosinha;
// - avisos em .modal (#alert, #confirm-cancel...): "aposta idêntica" responde Cancelar (nunca confirma sozinho).
// O que não dá para garantir (o botão de mais trevos, o mês, o Super Sete) é conferido depois do clique; sem conseguir
// conferir, o script para antes do carrinho e diz o que o usuário precisa olhar.
(function (plano) {
  'use strict';
  var atual = window.__bolaoTranscricao;
  if (atual && atual.rodando) return 'ocupado';
  var estado = (window.__bolaoTranscricao = { rodando: true, fim: null });

  var SEL = {
    limpar: '#limparvolante',
    carrinho: '#colocarnocarrinho',
    contador: '#carrinho',
    aumentar: '#aumentarnumero',
    valor: '#valoraposta, .valor-aposta',
    modais: '.modal',
    cancelar: '.data-cancelar-modal-confirmacao',
    fechar: '.data-fechar-modal-alerta, #fecharModalAlerta, .data-fechar-modal-confirmacao, button.close, [data-dismiss="modal"]',
    times: 'img.data-selecionar-time-do-coracao',
    buscaDoTime: '#txtTimeCoracao',
    palpites: 'a.loteca_palpite',
  };

  var avisos = [];
  var semEspelho = false;
  var teimosinhaConferida = false;

  function Parar(mensagem, parcial) {
    this.mensagem = mensagem;
    this.parcial = !!parcial;
  }

  function qs(s, raiz) { return (raiz || document).querySelector(s); }
  function qsa(s, raiz) { return Array.prototype.slice.call((raiz || document).querySelectorAll(s)); }
  function id(i) { return document.getElementById(i); }
  function clicar(el) { el.click(); }
  function textoDe(el) { return el ? String(el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim() : ''; }
  function classeDe(el) { return el ? String(el.getAttribute('class') || '') : ''; }
  function cede() { return new Promise(function (ok) { setTimeout(ok, 0); }); }
  function pausa(ms) { return new Promise(function (ok) { setTimeout(ok, ms); }); }
  function ordenados(a) { return a.slice().sort(function (x, y) { return x < y ? -1 : x > y ? 1 : 0; }); }
  function iguais(a, b) { return JSON.stringify(ordenados(a)) === JSON.stringify(ordenados(b)); }
  function soLetras(t) {
    return String(t || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '');
  }
  function reais(c) {
    if (c === null || c === undefined) return '?';
    var r = (c / 100).toFixed(2).split('.');
    return 'R$ ' + r[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.') + ',' + r[1];
  }

  // Espera a condição valer (olhando as mudanças da página e, por garantia, a cada 150 ms), até [ms].
  function esperaAte(condicao, ms) {
    function vale() { try { return !!condicao(); } catch (e) { return false; } }
    return new Promise(function (ok) {
      if (vale()) return ok(true);
      var feito = false;
      var obs = new MutationObserver(function () { if (vale()) fim(true); });
      var intervalo = setInterval(function () { if (vale()) fim(true); }, 150);
      var limite = setTimeout(function () { fim(vale()); }, ms || 4000);
      function fim(v) {
        if (feito) return;
        feito = true;
        obs.disconnect();
        clearInterval(intervalo);
        clearTimeout(limite);
        ok(v);
      }
      obs.observe(document.documentElement, { attributes: true, childList: true, subtree: true, characterData: true });
    });
  }

  // Marcado: a classe do site ("selected", "active"...), o aria ou a caixa marcada.
  var MARCAS = /(^|\s)(selected|active|ativo|ativa|marcado|marcada|checked|selecionado|selecionada)(\s|$)/i;
  function marcado(el) {
    if (!el || !el.getAttribute) return false;
    if (el.type === 'checkbox' || el.type === 'radio') return !!el.checked;
    if (el.getAttribute('aria-pressed') === 'true' || el.getAttribute('aria-checked') === 'true' || el.getAttribute('aria-selected') === 'true') return true;
    var c = classeDe(el);
    if (/-remove(\s|$)/.test(c)) return false;
    return MARCAS.test(c) || /[_-](selected|selecionad[oa])(\s|$)/i.test(c);
  }
  function marcadoPerto(el) {
    if (!el) return false;
    if (marcado(el) || marcado(el.parentElement)) return true;
    return qsa('*', el).some(marcado);
  }
  // A "assinatura" das classes do elemento, do pai e dos filhos (para ver se o clique mudou alguma coisa).
  function marcasDe(el) {
    if (!el) return '';
    return [classeDe(el), classeDe(el.parentElement)].concat(qsa('*', el).map(classeDe)).join('|');
  }

  // ---------- avisos (modais) do site ----------

  function modalAberto() {
    return qsa(SEL.modais).filter(function (m) {
      var st = getComputedStyle(m);
      return st.display !== 'none' && st.visibility !== 'hidden';
    })[0] || null;
  }
  function textoDoModal(m) {
    return textoDe(m)
      .replace(/^[×x]\s*/i, '')
      .replace(/^Loterias? Online\s*/i, '')
      .replace(/\s*(Fechar|Confirmar Cancelar|Cancelar Confirmar|OK)$/i, '')
      .trim();
  }
  async function fecharModal(m) {
    var botao = qs(SEL.cancelar, m) ||
      qsa('button, a', m).filter(function (b) { return /^cancelar$/i.test(textoDe(b)); })[0] ||
      qs(SEL.fechar, m);
    if (botao) clicar(botao);
    if (!(await esperaAte(function () { return !modalAberto(); }, 4000))) {
      throw new Parar('O site abriu um aviso que não consegui fechar. Feche o aviso e toque em Transcrever de novo.');
    }
  }
  function semAvisoAberto() {
    var m = modalAberto();
    if (m) throw new Parar('O site está com um aviso aberto: "' + textoDoModal(m) + '". Feche o aviso e toque em Transcrever de novo.');
  }

  // ---------- o carrinho e o valor ----------

  function itensNoCarrinho() {
    var n = parseInt(textoDe(qs(SEL.contador)), 10);
    return isNaN(n) ? null : n;
  }
  function valorNaTela() {
    var el = qsa(SEL.valor)[0];
    var m = textoDe(el).match(/R\$\s*([\d.]+),(\d{2})/);
    return m ? Number(m[1].replace(/\./g, '')) * 100 + Number(m[2]) : null;
  }

  async function limpar(marcados) {
    var b = qs(SEL.limpar);
    if (b) clicar(b);
    if (await esperaAte(function () { return marcados().length === 0; }, 3000)) return;
    // Sem o botão (ou ele não limpou tudo): desmarca um por um.
    marcados().forEach(function (el) { if (el && el.click) clicar(el); });
    if (await esperaAte(function () { return marcados().length === 0; }, 2000)) return;
    throw new Parar('Não consegui limpar o volante do site. Toque em "Limpar Volante" e depois em Transcrever de novo.');
  }

  async function conferirValor() {
    var esperado = semEspelho ? Math.round(plano.valor / 2) : plano.valor;
    if (!esperado || valorNaTela() === null) return;
    if (await esperaAte(function () { return valorNaTela() === esperado; }, 2500)) return;
    var lido = valorNaTela();
    // Na teimosinha o site pode mostrar o valor de um concurso só.
    if (plano.teimosinha > 1 && teimosinhaConferida && lido === Math.round(esperado / plano.teimosinha)) return;
    throw new Parar(
      'O site mostra ' + reais(lido) + ' e, no app, ' + plano.titulo + ' vale ' + reais(esperado) + '. ' +
      'O volante ficou marcado: confira e, se estiver certo, toque em "Colocar no Carrinho".', true);
  }

  async function teimosinha() {
    if (!(plano.teimosinha > 1)) return;
    var mais = id('aumentarteimosinha');
    if (!mais) {
      throw new Parar('O volante ficou marcado, mas não achei a teimosinha no site: ajuste para ' + plano.teimosinha +
        ' concursos e toque em "Colocar no Carrinho".', true);
    }
    function lida() {
      var p = mais.parentElement;
      if (!p) return null;
      var t = textoDe(p).replace(textoDe(mais), '');
      var menos = id('diminuirteimosinha');
      if (menos && p.contains(menos)) t = t.replace(textoDe(menos), '');
      var m = t.match(/\d+/);
      return m ? parseInt(m[0], 10) : null;
    }
    if (lida() !== null) {
      for (var i = 0; i < 40 && lida() !== null && lida() < plano.teimosinha; i++) { clicar(mais); await cede(); }
      await pausa(100);
      teimosinhaConferida = lida() === plano.teimosinha;
      if (!teimosinhaConferida) {
        throw new Parar('O volante ficou marcado, mas o site não deixou a teimosinha em ' + plano.teimosinha +
          ' concursos (ficou em ' + lida() + '): ajuste e toque em "Colocar no Carrinho".', true);
      }
    } else {
      for (var j = 1; j < plano.teimosinha; j++) { clicar(mais); await cede(); }
    }
  }

  // Clica em "Colocar no Carrinho" e espera o site aceitar (o volante limpa, o contador sobe) ou avisar.
  async function carrinho(marcados) {
    var botao = qs(SEL.carrinho);
    if (!botao) throw new Parar('O volante ficou marcado, mas não achei o botão "Colocar no Carrinho": toque nele.', true);
    var antes = itensNoCarrinho();
    function entrou() { return antes !== null && itensNoCarrinho() === antes + 1; }
    clicar(botao);
    var resolvido = await esperaAte(function () { return marcados().length === 0 || !!modalAberto() || entrou(); }, 8000);
    var m = modalAberto();
    if (m) {
      var t = textoDoModal(m);
      if (/id[eê]ntica/i.test(t)) {
        await fecharModal(m);
        await limpar(marcados).catch(function () {});
        return { noCarrinho: true, mensagem: 'O carrinho já tinha uma aposta igual a ' + plano.titulo + ' (não repeti).' };
      }
      await esperaAte(entrou, 1500);
      await fecharModal(m);
      if (!entrou()) {
        await limpar(marcados).catch(function () {});
        throw new Parar('O site não aceitou ' + plano.titulo + ': "' + t + '"');
      }
    } else if (!resolvido) {
      throw new Parar('O site não pôs a aposta no carrinho e não deu nenhuma mensagem. O volante ficou marcado: confira e toque em "Colocar no Carrinho".', true);
    }
    if (antes !== null) await esperaAte(entrou, 3000);
    return { noCarrinho: true, mensagem: plano.titulo + ' da ' + plano.modalidade + ' está no carrinho.' };
  }

  // ---------- volantes de números ----------

  function numerosMarcados() {
    return qsa('[id^="n"]').filter(function (el) { return /^n\d\d$/.test(el.id) && marcado(el); }).map(function (el) { return el.id; });
  }
  function elementosMarcados() { return numerosMarcados().map(id); }

  async function trevos() {
    function el(t) { return id('trevo' + t); }
    if (!el(1) || !el(6)) {
      throw new Parar('Os números foram marcados, mas não achei os trevos no site: marque os trevos ' + plano.trevos.join(' e ') +
        ' e toque em "Colocar no Carrinho".', true);
    }
    var todos = [1, 2, 3, 4, 5, 6];
    function marcadosT() { return todos.filter(function (t) { return marcado(el(t)); }); }
    // Tira os que não são da aposta (o site guarda a escolha anterior?).
    marcadosT().forEach(function (t) { if (plano.trevos.indexOf(t) < 0) clicar(el(t)); });
    await esperaAte(function () { return marcadosT().every(function (t) { return plano.trevos.indexOf(t) >= 0; }); }, 1500);
    var base = todos.map(function (t) { return classeDe(el(t)); });
    function escolhidos() {
      var porClasse = marcadosT();
      if (porClasse.length) return porClasse;
      return todos.filter(function (t, i) { return classeDe(el(t)) !== base[i]; });
    }
    function marcarQueFaltam() {
      plano.trevos.forEach(function (t) { if (escolhidos().indexOf(t) < 0) clicar(el(t)); });
    }
    marcarQueFaltam();
    var ok = await esperaAte(function () { return iguais(escolhidos(), plano.trevos); }, 2000);
    if (!ok && plano.trevos.length > Math.max(plano.trevosSimples || 2, 2)) {
      // Mais trevos que a aposta simples: o site pede para aumentar a quantidade antes.
      var mais = botaoMaisTrevos();
      if (mais) {
        for (var i = escolhidos().length; i < plano.trevos.length; i++) { clicar(mais); await cede(); }
        marcarQueFaltam();
        ok = await esperaAte(function () { return iguais(escolhidos(), plano.trevos); }, 2500);
      }
    }
    if (!ok) {
      throw new Parar('Os números foram marcados, mas o site ficou com os trevos ' + (escolhidos().join(' e ') || 'sem marcar') +
        ' em vez de ' + plano.trevos.join(' e ') + '. Acerte os trevos e toque em "Colocar no Carrinho".', true);
    }
  }

  function botaoMaisTrevos() {
    var diretos = ['#aumentartrevo', '#aumentarTrevo', '#aumentartrevos', '#aumentarTrevos'];
    for (var i = 0; i < diretos.length; i++) { var b = qs(diretos[i]); if (b) return b; }
    var porNome = qsa('[id], [ng-click]').filter(function (e) {
      var s = ((e.id || '') + ' ' + (e.getAttribute('ng-click') || '')).toLowerCase();
      return s.indexOf('trevo') >= 0 && (s.indexOf('aument') >= 0 || s.indexOf('mais') >= 0);
    });
    if (porNome.length) return porNome[0];
    var doPasso = qs('#step6 > div:nth-child(2) > button:first-child');
    if (doPasso) return doPasso;
    var p = id('trevo1');
    for (var k = 0; p && k < 6; k++, p = p.parentElement) {
      var mais = qsa('button, a', p).filter(function (b) {
        return textoDe(b) === '+' && b.id !== 'aumentarnumero' && b.id !== 'aumentarteimosinha';
      });
      if (mais.length) return mais[0];
    }
    return null;
  }

  // Timemania: "VASCO DA GAMA/RJ" no app e na lista do site, que às vezes escreve diferente ("BARRA FC/SC").
  function partesDoTime(t) {
    var n = String(t || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toUpperCase().replace(/[^A-Z0-9/ ]/g, ' ').replace(/\s+/g, ' ').trim();
    var i = n.lastIndexOf('/');
    return i < 0 ? { nome: n, uf: '' } : { nome: n.slice(0, i).trim(), uf: n.slice(i + 1).trim() };
  }
  function pontosDoTime(a, b) {
    if (!a.nome || !b.nome) return 0;
    if (a.uf && b.uf && a.uf !== b.uf) return 0;
    if (a.nome === b.nome) return 100;
    if (b.nome.indexOf(a.nome + ' ') === 0 || a.nome.indexOf(b.nome + ' ') === 0) return 80;
    var ta = a.nome.split(' '), tb = b.nome.split(' ');
    var n = Math.min(ta.length, tb.length), todos = true;
    for (var i = 0; i < n; i++) {
      if (!(ta[i].indexOf(tb[i]) === 0 || tb[i].indexOf(ta[i]) === 0)) { todos = false; break; }
    }
    if (todos) return 70;
    if (ta[0] === tb[0] && ta[0].length >= 4) return 50;
    return 0;
  }
  function timeEscolhido(img) { return marcado(img) || marcado(img.parentElement) || marcado(img.closest && img.closest('li')); }

  async function time() {
    var busca = qs(SEL.buscaDoTime);
    if (busca && busca.value) {
      busca.value = '';
      ['input', 'change', 'keyup'].forEach(function (t) { busca.dispatchEvent(new Event(t, { bubbles: true })); });
      await pausa(400);
    }
    var lista = qsa(SEL.times).map(function (img) {
      var li = (img.closest && img.closest('li')) || img.parentElement;
      var nome = qs('.nomeTime', img.parentElement) || (li && qs('.nomeTime', li));
      return { el: img, nome: nome ? textoDe(nome) : (img.getAttribute('alt') || img.getAttribute('title') || '') };
    });
    if (!lista.length) {
      throw new Parar('Os números foram marcados, mas não achei a lista dos times no site: escolha o ' + plano.time +
        ' e toque em "Colocar no Carrinho".', true);
    }
    var alvo = partesDoTime(plano.time), melhor = null, maior = 0, empate = false;
    lista.forEach(function (it) {
      var p = pontosDoTime(alvo, partesDoTime(it.nome));
      if (p > maior) { maior = p; melhor = it; empate = false; } else if (p === maior && p > 0) empate = true;
    });
    if (!melhor || maior < 50 || empate) {
      throw new Parar('Os números foram marcados, mas não achei o time ' + plano.time + ' na lista do site: escolha o time e toque em "Colocar no Carrinho".', true);
    }
    if (!timeEscolhido(melhor.el)) clicar(melhor.el);
    if (!(await esperaAte(function () { return timeEscolhido(melhor.el); }, 3000))) {
      throw new Parar('Os números foram marcados, mas o site não ficou com o time ' + melhor.nome + ' escolhido: escolha o time e toque em "Colocar no Carrinho".', true);
    }
  }

  // Dia de Sorte: o mês pelo nome (ou pela sigla) no carrossel; sem achar pelo nome, pela posição.
  async function mes() {
    var raiz = id('carrossel_diadesorte') || qsa('[id]').filter(function (e) { return soLetras(e.id).indexOf('diadesorte') >= 0; })[0] || document.body;
    var nome = soLetras(plano.nomeDoMes), sigla = nome.slice(0, 3);
    var porNome = qsa('*', raiz).filter(function (el) {
      var t = soLetras(el.getAttribute('alt') || el.getAttribute('title') || (el.children.length === 0 ? textoDe(el) : ''));
      return t && (t === nome || t === sigla || t.indexOf(nome) === 0);
    });
    var alvo = null;
    if (porNome.length) {
      alvo = (porNome[0].closest && porNome[0].closest('a, button, li, [ng-click]')) || porNome[0];
      if (!raiz.contains(alvo)) alvo = porNome[0];
    } else if (raiz !== document.body && raiz.children[0]) {
      var itens = Array.prototype.slice.call(raiz.children[0].children);
      if (itens.length >= 23) alvo = itens[2 * (plano.mes - 1)];
      else if (itens.length >= 12) alvo = itens[plano.mes - 1];
    }
    if (!alvo) {
      throw new Parar('Os números foram marcados, mas não achei o mês ' + plano.nomeDoMes + ' no site: escolha o mês e toque em "Colocar no Carrinho".', true);
    }
    if (marcadoPerto(alvo)) return;
    var antes = marcasDe(alvo);
    clicar(alvo);
    if (!(await esperaAte(function () { return marcadoPerto(alvo) || marcasDe(alvo) !== antes; }, 2500))) {
      throw new Parar('Os números foram marcados, mas não consegui conferir o mês ' + plano.nomeDoMes + ': confira o mês e toque em "Colocar no Carrinho".', true);
    }
  }

  async function espelho() {
    var caixa = id('apostaEspelho');
    if (!plano.espelho) {
      if (caixa && caixa.checked) { clicar(caixa); await esperaAte(function () { return !caixa.checked; }, 1500); }
      return;
    }
    if (!caixa) {
      semEspelho = true;
      avisos.push('Não achei a opção "Aposta espelho" no site: a aposta espelho continua para transcrever.');
      return;
    }
    if (!caixa.checked) clicar(caixa);
    if (!(await esperaAte(function () { return caixa.checked; }, 2000))) {
      throw new Parar('Os números foram marcados, mas a "Aposta espelho" não ficou marcada: marque e toque em "Colocar no Carrinho".', true);
    }
  }

  async function dezenas() {
    var pronta = await esperaAte(function () { return id(plano.primeiro) && id(plano.ultimo); }, 6000);
    if (!pronta) {
      throw new Parar('O volante da ' + plano.modalidade + ' não apareceu na página. Espere a página carregar (e entre na sua conta, se o site pedir) e toque em Transcrever de novo.');
    }
    semAvisoAberto();
    await limpar(elementosMarcados);
    var caixaEspelho = id('apostaEspelho');
    if (caixaEspelho && caixaEspelho.checked && !plano.espelho) { clicar(caixaEspelho); await esperaAte(function () { return !caixaEspelho.checked; }, 1500); }
    if (plano.aumentos > 0) {
      var mais = qs(SEL.aumentar);
      if (!mais) throw new Parar('Não achei no site o botão de aumentar a quantidade de números (' + plano.titulo + ' tem ' + plano.numeros.length + ').');
      for (var i = 0; i < plano.aumentos; i++) { clicar(mais); await cede(); }
    }
    for (var j = 0; j < plano.numeros.length; j++) {
      var el = id(plano.numeros[j]);
      if (!el) throw new Parar('Não achei o número ' + plano.numeros[j].slice(1) + ' no volante do site.');
      if (!marcado(el)) clicar(el);
      await cede();
    }
    if (!(await esperaAte(function () { return iguais(numerosMarcados(), plano.numeros); }, 5000))) {
      var aviso = modalAberto();
      if (aviso) {
        var t = textoDoModal(aviso);
        await fecharModal(aviso);
        throw new Parar('O site avisou: "' + t + '"');
      }
      var obtidos = numerosMarcados().length;
      await limpar(elementosMarcados).catch(function () {});
      throw new Parar('O volante do site ficou com ' + obtidos + ' números marcados em vez de ' + plano.numeros.length + '. Toque em Transcrever de novo.');
    }
    if (plano.trevos && plano.trevos.length) await trevos();
    if (plano.time) await time();
    if (plano.mes) await mes();
    await espelho();
    await teimosinha();
    await conferirValor();
    return carrinho(elementosMarcados);
  }

  // ---------- Loteca ----------

  async function loteca() {
    if (!(await esperaAte(function () { return qsa(SEL.palpites).length === 42; }, 6000))) {
      throw new Parar('O volante da Loteca não apareceu na página. Espere a página carregar (e entre na sua conta, se o site pedir) e toque em Transcrever de novo.');
    }
    semAvisoAberto();
    function indices() {
      var r = [];
      qsa(SEL.palpites).forEach(function (a, i) {
        var c = classeDe(a);
        if (/(^|\s)loteca_palpite_selecionado(\s|$)/.test(c) && !/loteca_palpite_selecionado-remove/.test(c)) r.push(i);
      });
      return r;
    }
    function elementos() { var links = qsa(SEL.palpites); return indices().map(function (i) { return links[i]; }); }
    if (plano.loteca.length !== 14 || plano.loteca.some(function (j) { return !j.length; })) {
      throw new Parar(plano.titulo + ' não tem os palpites dos 14 jogos.');
    }
    await limpar(elementos);
    var alvo = [];
    plano.loteca.forEach(function (colunas, jogo) { colunas.forEach(function (c) { alvo.push(jogo * 3 + c); }); });
    var links = qsa(SEL.palpites);
    alvo.forEach(function (i) { clicar(links[i]); });
    await pausa(300);
    if (!(await esperaAte(function () { return iguais(indices(), alvo); }, 5000))) {
      var aviso = modalAberto();
      if (aviso) {
        var t = textoDoModal(aviso);
        await fecharModal(aviso);
        throw new Parar('O site avisou: "' + t + '"');
      }
      await limpar(elementos).catch(function () {});
      throw new Parar('O volante da Loteca não ficou igual à aposta. Toque em Transcrever de novo.');
    }
    await conferirValor();
    return carrinho(elementos);
  }

  // ---------- Super Sete ----------

  // As 7 colunas, cada uma com os elementos dos números de 0 a 9: pelos ids conhecidos ou pela posição na tela.
  function gradeSuperSete() {
    var padroes = [
      function (c, d) { return 'c' + c + 'n' + d; },
      function (c, d) { return 'n' + d + 'c' + c; },
      function (c, d) { return 'col' + c + 'n' + d; },
      function (c, d) { return 'coluna' + c + '_' + d; },
      function (c, d) { return 'n' + c + d; },
    ];
    for (var p = 0; p < padroes.length; p++) {
      var grade = [], completo = true;
      for (var c = 1; c <= 7 && completo; c++) {
        var col = [];
        for (var d = 0; d <= 9; d++) { var el = id(padroes[p](c, d)); if (!el) { completo = false; break; } col.push(el); }
        grade.push(col);
      }
      if (completo) return grade;
    }
    var seletor = 'a, button, li, span, div, td, label';
    var folhas = qsa(seletor).filter(function (el) {
      return /^\d$/.test(textoDe(el)) && !qsa(seletor, el).some(function (f) { return /^\d$/.test(textoDe(f)); });
    });
    var itens = folhas.map(function (el) {
      var r = el.getBoundingClientRect();
      return { el: el, x: r.left + r.width / 2, w: r.width, d: Number(textoDe(el)) };
    }).filter(function (o) { return o.w > 0; }).sort(function (a, b) { return a.x - b.x; });
    var grupos = [];
    itens.forEach(function (o) {
      var g = grupos[grupos.length - 1];
      if (g && Math.abs(g.x - o.x) < Math.max(6, o.w / 2)) g.itens.push(o); else grupos.push({ x: o.x, itens: [o] });
    });
    var colunas = grupos.filter(function (g) {
      return g.itens.length === 10 && g.itens.map(function (o) { return o.d; }).sort().join('') === '0123456789';
    });
    if (colunas.length !== 7) return null;
    return colunas.map(function (g) { var porDigito = []; g.itens.forEach(function (o) { porDigito[o.d] = o.el; }); return porDigito; });
  }

  async function superSete() {
    var grade = null;
    await esperaAte(function () { return (grade = gradeSuperSete()) !== null; }, 6000);
    if (!grade) {
      throw new Parar('Não reconheci o volante da Super Sete na página. Marque à mão: ' + plano.colunas.join(' · ') + '.');
    }
    if (plano.colunas.length !== 7 || plano.colunas.some(function (d) { return !(d >= 0 && d <= 9); })) {
      throw new Parar(plano.titulo + ' não tem um número em cada uma das 7 colunas.');
    }
    semAvisoAberto();
    var todos = [].concat.apply([], grade);
    await limpar(function () { return todos.filter(marcado); });
    var base = todos.map(classeDe);
    var alvos = plano.colunas.map(function (d, c) { return grade[c][d]; });
    function escolhidos() {
      var porClasse = todos.filter(marcado);
      return porClasse.length ? porClasse : todos.filter(function (el, i) { return classeDe(el) !== base[i]; });
    }
    alvos.forEach(function (el) { clicar(el); });
    if (!(await esperaAte(function () {
      var e = escolhidos();
      return e.length === 7 && alvos.every(function (a) { return e.indexOf(a) >= 0; });
    }, 4000))) {
      throw new Parar('Marquei as colunas, mas não consegui conferir o volante da Super Sete. Confira (' + plano.colunas.join(' · ') +
        ') e toque em "Colocar no Carrinho".', true);
    }
    await teimosinha();
    await conferirValor();
    return carrinho(escolhidos);
  }

  // ---------- começo ----------

  (async function () {
    var r;
    try {
      if (plano.tipo === 'loteca') r = await loteca();
      else if (plano.tipo === 'colunas') r = await superSete();
      else r = await dezenas();
      r.ok = true;
    } catch (e) {
      if (e instanceof Parar) r = { ok: false, noCarrinho: false, parcial: e.parcial, mensagem: e.mensagem };
      else r = { ok: false, noCarrinho: false, parcial: false, mensagem: 'Algo deu errado no site: ' + (e && e.message ? e.message : String(e)) };
    }
    r.semEspelho = semEspelho;
    if (avisos.length) r.mensagem = (r.mensagem ? r.mensagem + ' ' : '') + avisos.join(' ');
    estado.fim = r;
    estado.rodando = false;
  })();
  return 'iniciado';
})
