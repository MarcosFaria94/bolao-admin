// Bolão dos Amigos: a sessão do site da Caixa está aberta? (desde a 0.42, pedido do usuário: "faz verificação se a sessão
// não expirou antes, para os jogos não irem para o limbo").
//
// Olha o que a página mostra: o "Sair", o "Olá, NOME" ou o "Minha conta" (a conta aberta) contra o "Entrar"/"Acessar", o
// campo da senha ou um aviso de sessão expirada (a conta fechada). Devolve 'logado', 'deslogado' ou 'desconhecido' (a
// página não diz: o app segue e confere o carrinho de cada aposta, que só conta quando o contador do site sobe).
(function () {
  'use strict';
  function visivel(el) {
    if (!el || !el.getBoundingClientRect) return false;
    var cs = window.getComputedStyle(el);
    if (!cs || cs.display === 'none' || cs.visibility === 'hidden' || parseFloat(cs.opacity) === 0) return false;
    var r = el.getBoundingClientRect();
    return r.width > 0 && r.height > 0;
  }
  function texto(el) { return String(el.innerText || el.textContent || '').replace(/\s+/g, ' ').trim(); }
  function todos(s) { return Array.prototype.slice.call(document.querySelectorAll(s)).filter(visivel); }

  // Um aviso aberto falando da sessão (expirou, faça o login de novo).
  var avisos = todos('.modal, [role="dialog"], .alert, .toast');
  for (var i = 0; i < avisos.length; i++) {
    if (/sess[aã]o (expirou|expirada|encerrada|finalizada)|fa[cç]a (o )?login|efetue (o )?login|entre novamente|acesse novamente|n[aã]o autenticad/i.test(texto(avisos[i]))) {
      return 'deslogado';
    }
  }
  var clicaveis = todos('a, button, [role="button"], input[type="button"], input[type="submit"]');
  function tem(re) {
    for (var j = 0; j < clicaveis.length; j++) {
      var el = clicaveis[j];
      var t = texto(el) || el.value || el.getAttribute('aria-label') || el.getAttribute('title') || '';
      if (re.test(String(t).trim())) return true;
    }
    return false;
  }
  var href = todos('a[href]').map(function (a) { return a.getAttribute('href') || ''; }).join(' ');
  var corpo = texto(document.body || document.documentElement).slice(0, 4000);
  var logado = tem(/^(sair|logout|encerrar sess[aã]o|minha conta|meus dados|minhas apostas)$/i) ||
    /logout|\/sair\b|encerrarsessao/i.test(href) || /\bol[aá],\s+\S+/i.test(corpo);
  if (logado) return 'logado';
  var senha = todos('input[type="password"]').length > 0;
  var entrar = tem(/^(entrar|acessar|acesse|login|fazer login|acessar conta|acesse sua conta|entre na sua conta|identifique-se)$/i);
  if (senha || entrar) return 'deslogado';
  return 'desconhecido';
})()
