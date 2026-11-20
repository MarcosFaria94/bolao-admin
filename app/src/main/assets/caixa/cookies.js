// O ícone dos cookies do site (o botão flutuante do aviso de consentimento) e onde ele ficou, para o app pôr o cadeado
// junto dele (desde a 0.38; refeito na 0.39). Na 0.38 o script procurava o botão só pelo nome (cookie, lgpd) e não o
// achou no site da Caixa. Desde a 0.39 ele olha todos os elementos fixos na tela (também os de dentro das "shadow
// roots" abertas e as molduras de outros sites): pequenos, quase quadrados, no canto de baixo; os que têm nome de
// cookie/consentimento contam mais. Achado no canto de baixo à esquerda, o script tenta passá-lo para a direita (com a
// mesma margem); se não der (o site o prende de outro jeito), o app espelha o cadeado do outro lado.
// window.__bolaoCookie = JSON {x, topo, largura, altura, direita, cor, opacidade, cadeado} (frações da tela; cor "rgba(...)";
// cadeado = o cadeado foi posto na própria página, desde a 0.41, e com ele o "Efetivar apostas", desde a 0.42), ou null.
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
      if (el.id === CADEADO || el.id === EFETIVAR || !cs || cs.position !== 'fixed' || !visivel(el, cs)) continue;
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
  // O disco que aparece (desde a 0.41): o próprio botão ou o primeiro de dentro com cor de fundo (ou imagem) e quase do
  // tamanho dele. O tamanho, a cor, a sombra e a transparência do cadeado saem dele.
  function temFundo(cs) {
    if (!cs) return false;
    var c = cs.backgroundColor;
    var cor = c && c !== 'transparent' && !/rgba\([^)]*,\s*0\)$/.test(c);
    return cor || (cs.backgroundImage && cs.backgroundImage !== 'none');
  }
  function disco(el) {
    var r0 = el.getBoundingClientRect(), fila = [el], vistos = 0;
    while (fila.length && vistos < 16) {
      var x = fila.shift(); vistos++;
      var cs = estilo(x);
      if (cs && temFundo(cs)) {
        var r = x.getBoundingClientRect();
        if (r.width >= r0.width * 0.5 && r.height >= r0.height * 0.5) return x;
      }
      var filhos = x.children || [];
      for (var i = 0; i < filhos.length; i++) fila.push(filhos[i]);
      if (x.shadowRoot) for (var j = 0; j < x.shadowRoot.children.length; j++) fila.push(x.shadowRoot.children[j]);
    }
    return el;
  }
  // A opacidade do disco com a de todos os de fora (o que a tela mostra de verdade).
  function opacidade(el) {
    var o = 1;
    for (var x = el, i = 0; x && x.nodeType === 1 && i < 20; i++) {
      var cs = estilo(x);
      if (cs) { var v = parseFloat(cs.opacity); if (!isNaN(v)) o *= v; }
      x = x.parentElement || (x.getRootNode && x.getRootNode().host) || null;
    }
    return o;
  }
  // A primeira sombra que aparece, do disco para fora (até o botão fixo).
  function sombra(d, el) {
    for (var x = d, i = 0; x && i < 8; i++) {
      var cs = estilo(x);
      if (cs && cs.boxShadow && cs.boxShadow !== 'none') return cs.boxShadow;
      if (x === el) break;
      x = x.parentElement || (x.getRootNode && x.getRootNode().host) || null;
    }
    return 'none';
  }
  function rgba(c) {
    var m = /rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+)\s*)?\)/.exec(c || '');
    return m ? [+m[1], +m[2], +m[3], m[4] === undefined ? 1 : +m[4]] : null;
  }

  // ---------- o cadeado dentro da página (desde a 0.41) ----------
  // Na 0.39 o app desenhava o cadeado por cima da página, com o tamanho e a cor lidos aqui; com a página ampliada ou
  // reduzida (o site cabe na tela pelo zoom) e com o botão ainda aparecendo (a animação de entrada), ele saía maior e mais
  // claro que o dos cookies. Agora o cadeado é um elemento da própria página: o navegador o desenha com o mesmo zoom, e
  // ele copia do disco dos cookies o tamanho, os cantos, a cor, a sombra e a transparência, de novo a cada segundo.
  // Tocar nele chama o app (window.BolaoCadeado.alternar()); o app avisa o estado com window.__bolaoCadeadoEstado(aberto).
  var CADEADO = 'bolao-cadeado', VAO = 8;
  var ABERTO = '<path d="M5 13a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-6z"/><path d="M11 16a1 1 0 1 0 2 0a1 1 0 0 0 -2 0"/><path d="M8 11v-5a4 4 0 0 1 8 0"/>';
  var FECHADO = '<path d="M5 13a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-6z"/><path d="M11 16a1 1 0 1 0 2 0a1 1 0 0 0 -2 0"/><path d="M8 11v-4a4 4 0 1 1 8 0v4"/>';
  function aberto() {
    if (typeof window.__bolaoCadeadoAberto === 'boolean') return window.__bolaoCadeadoAberto;
    try { return !!window.BolaoCadeado.aberto(); } catch (e) { return true; }
  }
  function icone(c) {
    var a = aberto();
    c.setAttribute('aria-label', a ? 'Cadeado aberto' : 'Cadeado fechado');
    var tam = Math.round(Math.min(c.offsetWidth, c.offsetHeight) * 0.5) || 22;
    c.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="' + tam + '" height="' + tam + '" viewBox="0 0 24 24" fill="none" ' +
      'stroke="' + (c.getAttribute('data-tinta') || '#fff') + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" ' +
      'style="pointer-events:none;display:block">' + (a ? ABERTO : FECHADO) + '</svg>';
  }
  function cadeado() {
    var c = document.getElementById(CADEADO);
    if (c) return c;
    c = document.createElement('div');
    c.id = CADEADO;
    c.setAttribute('role', 'button');
    c.setAttribute('data-bolao', 'cadeado');
    var tocar = function (ev) {
      ev.preventDefault(); ev.stopPropagation();
      if (ev.type !== 'click') return;
      var a = aberto();
      try { a = !!window.BolaoCadeado.alternar(); } catch (e) { a = !a; }
      window.__bolaoCadeadoAberto = a;
      icone(c);
    };
    c.addEventListener('click', tocar, true);
    c.addEventListener('mousedown', tocar, true);
    c.addEventListener('touchstart', function (ev) { ev.stopPropagation(); }, { capture: true, passive: true });
    (document.body || document.documentElement).appendChild(c);
    return c;
  }
  // Copia do disco dos cookies [d] (dentro do botão fixo [el]) tudo o que aparece e põe o cadeado logo acima dele (com o
  // botão à direita) ou espelhado, do outro lado, na mesma altura (com o botão preso à esquerda).
  function desenhar(el) {
    var d = disco(el), r = d.getBoundingClientRect(), cs = estilo(d);
    if (!cs || r.width <= 0 || r.height <= 0) return null;
    var W = document.documentElement.clientWidth || window.innerWidth;
    var c = cadeado(), s = c.style;
    var direita = r.left + r.width / 2 >= W / 2;
    var esquerda = direita ? r.left : W - r.left - r.width;
    var topo = direita ? r.top - r.height - VAO : r.top;
    if (topo < 4) topo = r.top + r.height + VAO;
    var o = opacidade(d);
    var fundo = rgba(cs.backgroundColor);
    // O ícone branco, como o dos cookies; num disco claro (sobre o branco da página), azul-noite.
    var tinta = '#fff';
    if (fundo) {
      var a = fundo[3] * o, l = [0, 1, 2].map(function (i) { var v = (fundo[i] * a + 255 * (1 - a)) / 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); });
      if (0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2] > 0.75) tinta = '#13294B';
    }
    var props = {
      position: 'fixed', left: esquerda + 'px', top: topo + 'px', right: 'auto', bottom: 'auto',
      width: r.width + 'px', height: r.height + 'px', margin: '0', padding: '0', 'box-sizing': 'border-box',
      'border-top-left-radius': cs.borderTopLeftRadius, 'border-top-right-radius': cs.borderTopRightRadius,
      'border-bottom-left-radius': cs.borderBottomLeftRadius, 'border-bottom-right-radius': cs.borderBottomRightRadius,
      'background-color': cs.backgroundColor, 'background-image': cs.backgroundImage,
      border: cs.borderTopWidth + ' ' + cs.borderTopStyle + ' ' + cs.borderTopColor,
      'box-shadow': sombra(d, el), opacity: String(o), display: 'flex', 'align-items': 'center', 'justify-content': 'center',
      cursor: 'pointer', 'z-index': '2147483647', transform: 'none', 'touch-action': 'manipulation',
      '-webkit-tap-highlight-color': 'transparent', visibility: 'visible', 'pointer-events': 'auto'
    };
    for (var k in props) s.setProperty(k, props[k], 'important');
    if (c.getAttribute('data-tinta') !== tinta || !c.firstChild) { c.setAttribute('data-tinta', tinta); icone(c); }
    // O "Efetivar apostas" (desde a 0.42) logo acima do cadeado (ou ao lado, sem lugar em cima), com o mesmo disco.
    var topoE = topo <= r.top ? topo - r.height - VAO : topo + r.height + VAO, esquerdaE = esquerda;
    if (topoE < 4) { topoE = topo; esquerdaE = esquerda - r.width - VAO; }
    try { colocarEfetivar(props, esquerdaE, topoE, r.width, r.height, tinta); } catch (e) {}
    return { cadeado: true, direita: direita };
  }

  // ---------- o "Efetivar apostas" dentro da página (desde a 0.42) ----------
  // Pedido do usuário: "o 'Transcrever em lote' agora é 'Efetivar aposta' e, em vez de ficar em cima, vira um botão com
  // transparência igual à dos cookies e do cadeado, porém chama atenção se houver pelo menos 1 aposta para efetivar". O
  // disco copia o dos cookies (a mesma transparência); com apostas esperando, o número num selo vermelho e um anel dourado
  // que pulsa ficam por cima, sem a transparência. Tocar chama o app (window.BolaoCadeado.efetivar()); o app avisa quantas
  // esperam com window.__bolaoEfetivarEstado(n).
  var EFETIVAR = 'bolao-efetivar';
  var CARRINHO = '<path d="M4 19a2 2 0 1 0 4 0a2 2 0 0 0 -4 0"/><path d="M15 19a2 2 0 1 0 4 0a2 2 0 0 0 -4 0"/>' +
    '<path d="M17 17h-11v-14h-2"/><path d="M6 5l7 .5"/><path d="M20 13l-.5 2h-13.5"/><path d="M16 6h6"/><path d="M19 3v6"/>';
  function pendentes() {
    if (typeof window.__bolaoEfetivarN === 'number') return window.__bolaoEfetivarN;
    try { return Number(window.BolaoCadeado.pendentes()) || 0; } catch (e) { return 0; }
  }
  function estiloDoEfetivar() {
    if (document.getElementById('bolao-efetivar-estilo')) return;
    var st = document.createElement('style');
    st.id = 'bolao-efetivar-estilo';
    st.textContent = '@keyframes bolaoPulsa{0%{transform:scale(1);opacity:.85}100%{transform:scale(1.35);opacity:0}}' +
      '#bolao-efetivar>.bolao-disco{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;box-sizing:border-box}' +
      '#bolao-efetivar>.bolao-anel,#bolao-efetivar>.bolao-borda{position:absolute;inset:0;border-radius:inherit;border:2px solid #F2B630;pointer-events:none;box-sizing:border-box}' +
      '#bolao-efetivar>.bolao-anel{animation:bolaoPulsa 1.1s linear infinite}' +
      '#bolao-efetivar>.bolao-selo{position:absolute;top:-6px;right:-6px;min-width:20px;height:20px;padding:0 5px;box-sizing:border-box;' +
      'border-radius:10px;background:#E53935;color:#fff;font:700 12px/17px sans-serif;text-align:center;border:1.5px solid #fff;pointer-events:none}';
    (document.head || document.documentElement).appendChild(st);
  }
  function efetivar() {
    var c = document.getElementById(EFETIVAR);
    if (c) return c;
    estiloDoEfetivar();
    c = document.createElement('div');
    c.id = EFETIVAR;
    c.setAttribute('role', 'button');
    c.setAttribute('data-bolao', 'efetivar');
    var tocar = function (ev) {
      ev.preventDefault(); ev.stopPropagation();
      if (ev.type !== 'click') return;
      try { window.BolaoCadeado.efetivar(); } catch (e) {}
    };
    c.addEventListener('click', tocar, true);
    c.addEventListener('mousedown', tocar, true);
    c.addEventListener('touchstart', function (ev) { ev.stopPropagation(); }, { capture: true, passive: true });
    (document.body || document.documentElement).appendChild(c);
    return c;
  }
  // Põe o botão em ([esquerda], [topo]), do tamanho do disco, com o disco copiado do cadeado ([props]).
  function colocarEfetivar(props, esquerda, topo, largura, altura, tinta) {
    var c = efetivar(), n = pendentes();
    var fora = {
      position: 'fixed', left: esquerda + 'px', top: topo + 'px', right: 'auto', bottom: 'auto', width: largura + 'px', height: altura + 'px',
      margin: '0', padding: '0', 'box-sizing': 'border-box', 'border-top-left-radius': props['border-top-left-radius'],
      'border-top-right-radius': props['border-top-right-radius'], 'border-bottom-left-radius': props['border-bottom-left-radius'],
      'border-bottom-right-radius': props['border-bottom-right-radius'], background: 'transparent', opacity: '1', cursor: 'pointer',
      'z-index': '2147483647', transform: 'none', 'touch-action': 'manipulation', '-webkit-tap-highlight-color': 'transparent',
      visibility: 'visible', 'pointer-events': 'auto', display: 'block'
    };
    for (var k in fora) c.style.setProperty(k, fora[k], 'important');
    var chave = n + '|' + tinta + '|' + Math.round(largura);
    if (c.getAttribute('data-chave') !== chave) {
      c.setAttribute('data-chave', chave);
      c.setAttribute('aria-label', n > 0 ? 'Efetivar apostas: ' + n + ' para efetivar' : 'Efetivar apostas');
      var tam = Math.round(Math.min(largura, altura) * 0.5) || 22;
      c.innerHTML = (n > 0 ? '<div class="bolao-anel"></div>' : '') +
        '<div class="bolao-disco"><svg xmlns="http://www.w3.org/2000/svg" width="' + tam + '" height="' + tam + '" viewBox="0 0 24 24" fill="none" ' +
        'stroke="' + tinta + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="pointer-events:none;display:block">' +
        CARRINHO + '</svg></div>' + (n > 0 ? '<div class="bolao-borda"></div><div class="bolao-selo">' + (n > 99 ? '99+' : n) + '</div>' : '');
    }
    var d = c.querySelector('.bolao-disco');
    if (d) {
      ['border-top-left-radius', 'border-top-right-radius', 'border-bottom-left-radius', 'border-bottom-right-radius', 'background-color',
        'background-image', 'border', 'box-shadow', 'opacity'].forEach(function (p) { d.style.setProperty(p, props[p], 'important'); });
    }
  }
  window.__bolaoEfetivarEstado = function (n) {
    window.__bolaoEfetivarN = Number(n) || 0;
    var c = document.getElementById(EFETIVAR);
    if (c) c.removeAttribute('data-chave');
    if (achado && achado.isConnected) { try { desenhar(achado); } catch (e) {} }
    return 'ok';
  };
  window.__bolaoCadeadoEstado = function (a) {
    window.__bolaoCadeadoAberto = !!a;
    var c = document.getElementById(CADEADO);
    if (c) icone(c);
    return 'ok';
  };

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
      if (window.__bolaoBusca && agora - window.__bolaoBusca < 3000) {
        achado = null;
        window.__bolaoCookie = document.getElementById(CADEADO) ? JSON.stringify({ x: 1, topo: 1, largura: 0, cadeado: true }) : null;
        return;
      }
      window.__bolaoBusca = agora;
      achado = procurar(W, H);
    }
    if (!achado) {
      window.__bolaoCookie = document.getElementById(CADEADO) ? JSON.stringify({ x: 1, topo: 1, largura: 0, cadeado: true }) : null;
      return;
    }
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
    var d = disco(achado), rd = d.getBoundingClientRect(), csd = estilo(d);
    var feito = null;
    try { feito = desenhar(achado); } catch (e) { feito = null; }
    window.__bolaoCookie = JSON.stringify({
      x: (rd.left + rd.width / 2) / W, topo: rd.top / H, largura: rd.width / W, altura: rd.height / H,
      direita: rd.left + rd.width / 2 >= W / 2, cor: csd ? csd.backgroundColor : null, opacidade: opacidade(d),
      cadeado: !!(feito && feito.cadeado)
    });
  }
  window.__bolaoCookies = mover;
  mover();
  // A cada segundo o cadeado acompanha o disco dos cookies (a animação de entrada, o toque, o zoom).
  try {
    setInterval(function () {
      if (achado && achado.isConnected && visivel(achado, estilo(achado))) {
        try { desenhar(achado); } catch (e) {}
      }
    }, 1000);
  } catch (e) {}
  try {
    new MutationObserver(function () {
      clearTimeout(window.__bolaoEspera);
      window.__bolaoEspera = setTimeout(mover, 600);
    }).observe(document.documentElement, { childList: true, subtree: true });
  } catch (e) {}
  return 'ok';
})()
