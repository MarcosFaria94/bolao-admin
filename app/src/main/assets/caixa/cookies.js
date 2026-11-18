// O ícone dos cookies do site (o botão flutuante do aviso de consentimento) e onde ele ficou, para o app pôr o cadeado
// junto dele (desde a 0.38; refeito na 0.39). Na 0.38 o script procurava o botão só pelo nome (cookie, lgpd) e não o
// achou no site da Caixa. Desde a 0.39 ele olha todos os elementos fixos na tela (também os de dentro das "shadow
// roots" abertas e as molduras de outros sites): pequenos, quase quadrados, no canto de baixo; os que têm nome de
// cookie/consentimento contam mais. Achado no canto de baixo à esquerda, o script tenta passá-lo para a direita (com a
// mesma margem); se não der (o site o prende de outro jeito), o app espelha o cadeado do outro lado.
// window.__bolaoCookie = JSON {x, topo, largura, altura, direita, cor, opacidade} (frações da tela; cor "rgba(...)"), ou null.
(function () {
  if (window.__bolaoCookies) { window.__bolaoCookies(); return 'ok'; }
  var NOMES = /cookie|lgpd|consent|privac|onetrust|ot-sdk|ot-floating|cookiebot|usercentrics|didomi|termly|iubenda|klaro|cmplz|cky-|cc-revoke|cc-btn|adopt/i;
  var achado = null;

  function rotulos(el) {
    if (!el || !el.getAttribute) return '';
    var c = el.getAttribute('class') || '';
    return (el.id || '') + ' ' + c + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('title') || '') + ' ' +
      (el.getAttribute('alt') || '') + ' ' + (el.getAttribute('src') || '') + ' ' + (el.getAttribute('name') || '');
  }
  function temNome(el) {
    if (NOMES.test(rotulos(el))) return true;
    var d = el.querySelectorAll ? el.querySelectorAll('*') : [];
    for (var i = 0; i < d.length && i < 40; i++) if (NOMES.test(rotulos(d[i]))) return true;
    if (el.shadowRoot) {
      var s = el.shadowRoot.querySelectorAll('*');
      for (var j = 0; j < s.length && j < 40; j++) if (NOMES.test(rotulos(s[j]))) return true;
    }
    return false;
  }
  function todos(raiz, lista) {
    var els = raiz.querySelectorAll('*');
    for (var i = 0; i < els.length; i++) {
      lista.push(els[i]);
      if (els[i].shadowRoot) todos(els[i].shadowRoot, lista);
    }
    return lista;
  }
  function estilo(el) { try { return window.getComputedStyle(el); } catch (e) { return null; } }
  function visivel(el, cs) {
    if (!cs || cs.display === 'none' || cs.visibility === 'hidden' || parseFloat(cs.opacity) === 0) return false;
    var r = el.getBoundingClientRect();
    return r.width > 0 && r.height > 0;
  }
  // O elemento fixo pequeno no canto de baixo que mais parece o botão dos cookies; null se não houver.
  function procurar(W, H) {
    var melhor = null, nota = -1, els = todos(document, []);
    for (var i = 0; i < els.length; i++) {
      var el = els[i], cs = estilo(el);
      if (!cs || cs.position !== 'fixed' || !visivel(el, cs)) continue;
      var r = el.getBoundingClientRect();
      if (r.width < 20 || r.height < 20 || r.width > 120 || r.height > 120) continue;
      var razao = r.width / r.height;
      if (razao < 0.6 || razao > 1.7) continue;
      var cx = r.left + r.width / 2, cy = r.top + r.height / 2;
      if (cy < H * 0.55) continue;
      var nosso = el.getAttribute && el.getAttribute('data-bolao') === '1';
      var nome = temNome(el);
      var esquerda = cx < W * 0.4, direita = cx > W * 0.6;
      // À direita, só o que o próprio script já passou para lá ou o que tem nome de cookie (um "voltar ao topo" não).
      if (!(esquerda || (direita && (nosso || nome)))) continue;
      var redondo = parseFloat(cs.borderTopLeftRadius) >= Math.min(r.width, r.height) * 0.3 ? 3 : 0;
      var n = (nosso ? 20 : 0) + (nome ? 10 : 0) + redondo + (esquerda ? 1 : 0) + (cy / H);
      if (n > nota) { nota = n; melhor = el; }
    }
    return melhor;
  }
  // A cor de fundo que aparece (a do próprio botão ou a do círculo de dentro) e a opacidade dele (com a dos de fora).
  function cor(el) {
    var fila = [el], vistos = 0;
    while (fila.length && vistos < 12) {
      var x = fila.shift(); vistos++;
      var cs = estilo(x);
      if (cs) {
        var c = cs.backgroundColor;
        if (c && c !== 'transparent' && !/rgba\([^)]*,\s*0\)$/.test(c)) return c;
      }
      var filhos = x.children || [];
      for (var i = 0; i < filhos.length; i++) fila.push(filhos[i]);
      if (x.shadowRoot) for (var j = 0; j < x.shadowRoot.children.length; j++) fila.push(x.shadowRoot.children[j]);
    }
    return null;
  }
  function opacidade(el) {
    var o = 1;
    for (var x = el, i = 0; x && x.nodeType === 1 && i < 12; i++) {
      var cs = estilo(x);
      if (cs) o *= parseFloat(cs.opacity) || 1;
      x = x.parentElement || (x.getRootNode && x.getRootNode().host) || null;
    }
    return o;
  }
  function passarParaADireita(el, W) {
    var r = el.getBoundingClientRect();
    var margem = Math.max(8, r.left);
    el.style.setProperty('left', 'auto', 'important');
    el.style.setProperty('inset-inline-start', 'auto', 'important');
    el.style.setProperty('right', margem + 'px', 'important');
    el.style.setProperty('inset-inline-end', margem + 'px', 'important');
    el.setAttribute('data-bolao', '1');
    var depois = el.getBoundingClientRect();
    // Não foi (o site prende de outro jeito, ex.: transform): volta como estava; o app espelha o cadeado.
    if (depois.left + depois.width / 2 < W / 2) {
      ['left', 'inset-inline-start', 'right', 'inset-inline-end'].forEach(function (p) { el.style.removeProperty(p); });
      el.setAttribute('data-bolao', '2');
    }
  }
  function mover() {
    var W = window.innerWidth, H = window.innerHeight;
    if (!achado || !achado.isConnected || !visivel(achado, estilo(achado))) {
      // Sem o botão, a busca (que olha a página toda) roda no máximo a cada 3 s.
      var agora = Date.now();
      if (window.__bolaoBusca && agora - window.__bolaoBusca < 3000) { achado = null; window.__bolaoCookie = null; return; }
      window.__bolaoBusca = agora;
      achado = procurar(W, H);
    }
    if (!achado) { window.__bolaoCookie = null; return; }
    var r = achado.getBoundingClientRect();
    if (achado.getAttribute('data-bolao') !== '1' && achado.getAttribute('data-bolao') !== '2' && r.left + r.width / 2 < W / 2) {
      passarParaADireita(achado, W);
      r = achado.getBoundingClientRect();
    } else if (achado.getAttribute('data-bolao') === '1' && r.left + r.width / 2 < W / 2) {
      // O site redesenhou o botão e ele voltou para a esquerda: de novo.
      achado.removeAttribute('data-bolao');
      passarParaADireita(achado, W);
      r = achado.getBoundingClientRect();
    }
    window.__bolaoCookie = JSON.stringify({
      x: (r.left + r.width / 2) / W, topo: r.top / H, largura: r.width / W, altura: r.height / H,
      direita: r.left + r.width / 2 >= W / 2, cor: cor(achado), opacidade: opacidade(achado)
    });
  }
  window.__bolaoCookies = mover;
  mover();
  try {
    new MutationObserver(function () {
      clearTimeout(window.__bolaoEspera);
      window.__bolaoEspera = setTimeout(mover, 600);
    }).observe(document.documentElement, { childList: true, subtree: true });
  } catch (e) {}
  return 'ok';
})()
