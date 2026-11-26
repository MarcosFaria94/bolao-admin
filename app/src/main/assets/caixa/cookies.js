// Os botões do app dentro da página da Caixa: o cadeado e o "Efetivar apostas", junto do ícone dos cookies do site (o
// botão flutuante do aviso de consentimento).
//
// História: na 0.38 o script passava o ícone dos cookies para a direita e o app punha o cadeado em cima; na 0.39 o
// script passou a procurar o ícone entre todos os elementos fixos da tela; na 0.41 o cadeado foi para dentro da página,
// copiando o disco do ícone; na 0.42 veio o "Efetivar apostas", em cima do cadeado.
//
// Desde a 0.44 (pedido do usuário: "jogue o cookie pra esquerda, ponha um fundo no cadeado do mesmo azul do cookie e com
// mesmo diâmetro e alinhe ele na mesma altura"; e "ao mudar de tela e voltar os botões somem"):
// - o ícone dos cookies fica onde o site põe (o script não mexe mais nele);
// - o cadeado fica na mesma altura do ícone, do outro lado da tela (com o ícone à direita, logo à esquerda dele), com o
//   diâmetro do desenho que aparece do ícone e o fundo da cor dele (sem conseguir ler a cor, o azul que se viu no site da
//   Caixa, #8AACD1); o "Efetivar apostas" fica logo acima do cadeado, igual a ele, com o desenho de uma prancheta com
//   o visto;
// - os botões aparecem sempre, com ou sem o ícone dos cookies (sem ele, no canto de baixo à direita, 47 px a 27 px da
//   borda), e voltam sozinhos se o site os tirar da página (a cada segundo);
// - sem o ícone, a busca continua sozinha (até a 0.43, uma busca que caía na espera de 3 s não voltava mais);
// - o toque na página diz ao app se ela está no alto (window.BolaoCadeado.toque(noTopo)): só assim puxar para baixo abre
//   o Painel de Jogos.
//
// window.__bolaoCookie = JSON {x, topo, largura, altura, direita, cor, opacidade, cadeado} (frações da tela; cor
// "rgb(...)"; cadeado = os botões estão na página) ou, sem o ícone, {x: 1, topo: 1, largura: 0, cadeado, semCookie: true}.
(function () {
  if (window.__bolaoCookies) { window.__bolaoCookies(); return 'ok'; }
  var NOMES = /cookie|lgpd|consent|privac|onetrust|ot-sdk|ot-floating|cookiebot|usercentrics|didomi|termly|iubenda|klaro|cmplz|cky-|cc-revoke|cc-btn|adopt/i;
  var CADEADO = 'bolao-cadeado', EFETIVAR = 'bolao-efetivar', VAO = 8;
  // Sem o ícone dos cookies: o que se viu no site da Caixa (47 px, a 27 px da borda e de baixo, azul-claro).
  var TAMANHO = 47, MARGEM = 27, COR = 'rgb(138, 172, 209)';
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
  function nosso(el) { return el && (el.id === CADEADO || el.id === EFETIVAR || (el.closest && el.closest('#' + CADEADO + ',#' + EFETIVAR))); }
  // O elemento fixo pequeno no canto de baixo que mais parece o botão dos cookies; null se não houver.
  function procurar(W, H) {
    var melhor = null, nota = -1, els = todos(document, []);
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      if (nosso(el)) continue;
      var cs = estilo(el);
      if (!cs || cs.position !== 'fixed' || !visivel(el, cs)) continue;
      var r = el.getBoundingClientRect();
      if (r.width < 20 || r.height < 20 || r.width > 120 || r.height > 120) continue;
      var razao = r.width / r.height;
      if (razao < 0.6 || razao > 1.7) continue;
      var cx = r.left + r.width / 2, cy = r.top + r.height / 2;
      if (cy < H * 0.55) continue;
      var nome = temNome(el);
      var esquerda = cx < W * 0.4, direita = cx > W * 0.6;
      // À direita, só o que tem nome de cookie (um "voltar ao topo" não).
      if (!(esquerda || (direita && nome))) continue;
      var redondo = parseFloat(cs.borderTopLeftRadius) >= Math.min(r.width, r.height) * 0.3 ? 3 : 0;
      var n = (nome ? 10 : 0) + redondo + (esquerda ? 1 : 0) + (cy / H);
      if (n > nota) { nota = n; melhor = el; }
    }
    return melhor;
  }
  function rgba(c) {
    var m = /rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+)\s*)?\)/.exec(c || '');
    return m ? [+m[1], +m[2], +m[3], m[4] === undefined ? 1 : +m[4]] : null;
  }
  function transparente(c) { var v = rgba(c); return !v || v[3] < 0.05; }
  // O desenho que aparece do ícone (desde a 0.44): o próprio botão fixo ou o de dentro que tem cor de fundo, imagem, SVG
  // ou foto e quase o tamanho dele. Até a 0.43 valia o primeiro com fundo; sem nenhum, o botão inteiro (às vezes maior que
  // o círculo, e sem cor: o cadeado saía de outro tamanho e sem fundo).
  function desenhoDoIcone(el) {
    var r0 = el.getBoundingClientRect(), fila = [el], vistos = 0, melhor = el, nota = -1;
    while (fila.length && vistos < 40) {
      var x = fila.shift(); vistos++;
      var cs = estilo(x), r = x.getBoundingClientRect();
      if (cs && r.width >= r0.width * 0.5 && r.height >= r0.height * 0.5 && r.width / r.height > 0.6 && r.width / r.height < 1.7) {
        var tag = String(x.tagName || '').toLowerCase();
        var n = (!transparente(cs.backgroundColor) ? 4 : 0) + (cs.backgroundImage && cs.backgroundImage !== 'none' ? 3 : 0) +
          (/^(svg|img|canvas|picture)$/.test(tag) ? 2 : 0);
        if (n > nota || (n === nota && n > 0 && r.width * r.height > melhor.getBoundingClientRect().width * melhor.getBoundingClientRect().height)) {
          nota = n; melhor = x;
        }
      }
      var filhos = x.children || [];
      for (var i = 0; i < filhos.length; i++) fila.push(filhos[i]);
      if (x.shadowRoot) for (var j = 0; j < x.shadowRoot.children.length; j++) fila.push(x.shadowRoot.children[j]);
    }
    return melhor;
  }
  // A cor do desenho: o fundo dele ou, num SVG, o preenchimento da maior forma; null = não deu para ler (uma imagem).
  function corDoIcone(d) {
    var cs = estilo(d);
    if (cs && !transparente(cs.backgroundColor)) return cs.backgroundColor;
    var svg = String(d.tagName || '').toLowerCase() === 'svg' ? d : (d.querySelector && d.querySelector('svg'));
    if (!svg) return null;
    var rs = svg.getBoundingClientRect(), formas = svg.querySelectorAll('circle, ellipse, rect, path');
    for (var i = 0; i < formas.length; i++) {
      var f = formas[i], r = f.getBoundingClientRect(), fs = estilo(f);
      if (!fs || r.width < rs.width * 0.5 || r.height < rs.height * 0.5) continue;
      if (fs.fill && fs.fill !== 'none' && !/url\(/.test(fs.fill) && !transparente(fs.fill)) {
        // A transparência do preenchimento (fill-opacity) entra na cor.
        var v = rgba(fs.fill), fo = parseFloat(fs.fillOpacity);
        if (v && !isNaN(fo) && fo < 1) return 'rgba(' + v[0] + ', ' + v[1] + ', ' + v[2] + ', ' + (Math.round(v[3] * fo * 1000) / 1000) + ')';
        return fs.fill;
      }
    }
    return null;
  }
  // A opacidade do desenho com a de todos os de fora (o que a tela mostra de verdade).
  function opacidade(el) {
    var o = 1;
    for (var x = el, i = 0; x && x.nodeType === 1 && i < 20; i++) {
      var cs = estilo(x);
      if (cs) { var v = parseFloat(cs.opacity); if (!isNaN(v)) o *= v; }
      x = x.parentElement || (x.getRootNode && x.getRootNode().host) || null;
    }
    return o;
  }
  // A primeira sombra que aparece, do desenho para fora (até o botão fixo).
  function sombra(d, el) {
    for (var x = d, i = 0; x && i < 8; i++) {
      var cs = estilo(x);
      if (cs && cs.boxShadow && cs.boxShadow !== 'none') return cs.boxShadow;
      if (x === el) break;
      x = x.parentElement || (x.getRootNode && x.getRootNode().host) || null;
    }
    return 'none';
  }
  // O ícone branco, como o dos cookies; num fundo claro (sobre o branco da página), azul-noite.
  function tintaPara(cor, o) {
    var fundo = rgba(cor);
    if (!fundo) return '#fff';
    var a = fundo[3] * o, l = [0, 1, 2].map(function (i) { var v = (fundo[i] * a + 255 * (1 - a)) / 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); });
    return 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2] > 0.75 ? '#13294B' : '#fff';
  }

  // ---------- o cadeado ----------
  // Tocar nele chama o app (window.BolaoCadeado.alternar()); o app avisa o estado com window.__bolaoCadeadoEstado(aberto).
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

  // Onde e como ficam os botões: copiados do ícone dos cookies [el] (na mesma altura, do outro lado) ou, sem ele, no canto.
  function geometria(el, W, H) {
    if (el && el.isConnected) {
      var d = desenhoDoIcone(el), r = d.getBoundingClientRect(), cs = estilo(d);
      if (cs && r.width > 0 && r.height > 0) {
        var tam = Math.round(Math.max(r.width, r.height));
        var cor = corDoIcone(d);
        var direita = r.left + r.width / 2 >= W / 2;
        var comFundo = !transparente(cs.backgroundColor);
        var raio = comFundo ? [cs.borderTopLeftRadius, cs.borderTopRightRadius, cs.borderBottomLeftRadius, cs.borderBottomRightRadius] : ['50%', '50%', '50%', '50%'];
        return {
          esquerda: direita ? r.left - tam - VAO : W - r.left - tam,
          topo: r.top + r.height / 2 - tam / 2,
          tam: tam, raio: raio, cor: cor || COR, opacidade: cor ? opacidade(d) : 1, sombra: cor ? sombra(d, el) : 'none',
          direita: direita, r: r, corLida: cor, d: d
        };
      }
    }
    return { esquerda: W - MARGEM - TAMANHO, topo: H - MARGEM - TAMANHO, tam: TAMANHO, raio: ['50%', '50%', '50%', '50%'], cor: COR, opacidade: 1, sombra: 'none' };
  }

  // Põe o cadeado e, logo acima, o "Efetivar apostas" (sempre, desde a 0.44).
  function largura() { var c = document.documentElement.clientWidth; return c > 0 && c <= window.innerWidth ? c : window.innerWidth; }
  function desenhar(el) {
    var W = largura(), H = window.innerHeight || document.documentElement.clientHeight;
    var g = geometria(el, W, H);
    var esquerda = Math.max(4, Math.min(g.esquerda, W - g.tam - 4));
    var topo = Math.max(4, Math.min(g.topo, H - g.tam - 4));
    var tinta = tintaPara(g.cor, g.opacidade);
    var c = cadeado(), s = c.style;
    var props = {
      position: 'fixed', left: esquerda + 'px', top: topo + 'px', right: 'auto', bottom: 'auto',
      width: g.tam + 'px', height: g.tam + 'px', margin: '0', padding: '0', 'box-sizing': 'border-box',
      'border-top-left-radius': g.raio[0], 'border-top-right-radius': g.raio[1],
      'border-bottom-left-radius': g.raio[2], 'border-bottom-right-radius': g.raio[3],
      'background-color': g.cor, 'background-image': 'none', border: '0 none transparent',
      'box-shadow': g.sombra, opacity: String(g.opacidade), display: 'flex', 'align-items': 'center', 'justify-content': 'center',
      cursor: 'pointer', 'z-index': '2147483647', transform: 'none', 'touch-action': 'manipulation',
      '-webkit-tap-highlight-color': 'transparent', visibility: 'visible', 'pointer-events': 'auto'
    };
    for (var k in props) s.setProperty(k, props[k], 'important');
    var chave = tinta + '|' + g.tam;
    if (c.getAttribute('data-chave') !== chave || !c.firstChild) { c.setAttribute('data-chave', chave); c.setAttribute('data-tinta', tinta); icone(c); }
    // O "Efetivar apostas" (desde a 0.42) logo acima do cadeado (sem lugar em cima, ao lado), igual a ele.
    var topoE = topo - g.tam - VAO, esquerdaE = esquerda;
    if (topoE < 4) { topoE = topo; esquerdaE = esquerda - g.tam - VAO; }
    try { colocarEfetivar(props, esquerdaE, topoE, g.tam, g.tam, tinta); } catch (e) {}
    return g;
  }

  // ---------- o "Efetivar apostas" (desde a 0.42) ----------
  // Pedido do usuário (0.42): "um botão com transparência igual à dos cookies e do cadeado, porém chama atenção se houver
  // pelo menos 1 aposta para efetivar". Com apostas esperando, o número num selo vermelho e um anel dourado que pulsa
  // ficam por cima. Desde a 0.44 o desenho é uma prancheta com o visto (era um carrinho). Tocar chama o app
  // (window.BolaoCadeado.efetivar()); o app avisa quantas esperam com window.__bolaoEfetivarEstado(n).
  var PRANCHETA = '<path d="M9 5h-2a2 2 0 0 0 -2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-12a2 2 0 0 0 -2 -2h-2"/>' +
    '<path d="M9 5a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2a2 2 0 0 1 -2 2h-2a2 2 0 0 1 -2 -2"/><path d="M9 14l2 2l4 -4"/>';
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
  // Põe o botão em ([esquerda], [topo]), do tamanho do cadeado, com o disco copiado do cadeado ([props]).
  function colocarEfetivar(props, esquerda, topo, largura, altura, tinta) {
    estiloDoEfetivar();
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
    if (c.getAttribute('data-chave') !== chave || !c.querySelector('.bolao-disco')) {
      c.setAttribute('data-chave', chave);
      c.setAttribute('aria-label', n > 0 ? 'Efetivar apostas: ' + n + ' para efetivar' : 'Efetivar apostas');
      var tam = Math.round(Math.min(largura, altura) * 0.5) || 22;
      c.innerHTML = (n > 0 ? '<div class="bolao-anel"></div>' : '') +
        '<div class="bolao-disco"><svg xmlns="http://www.w3.org/2000/svg" width="' + tam + '" height="' + tam + '" viewBox="0 0 24 24" fill="none" ' +
        'stroke="' + tinta + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="pointer-events:none;display:block">' +
        PRANCHETA + '</svg></div>' + (n > 0 ? '<div class="bolao-borda"></div><div class="bolao-selo">' + (n > 99 ? '99+' : n) + '</div>' : '');
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
    try { desenhar(achado); } catch (e) {}
    return 'ok';
  };
  window.__bolaoCadeadoEstado = function (a) {
    window.__bolaoCadeadoAberto = !!a;
    var c = document.getElementById(CADEADO);
    if (c) icone(c);
    return 'ok';
  };

  // ---------- o ícone dos cookies: achar e acompanhar ----------
  function semIcone() { return !achado || !achado.isConnected || !visivel(achado, estilo(achado)); }
  function mover() {
    var W = largura(), H = window.innerHeight;
    if (semIcone()) {
      achado = null;
      // A busca olha a página toda: no máximo a cada 3 s (depois de muitas sem achar, a cada 10 s). Desde a 0.44, a busca
      // que cai na espera fica marcada para depois (até a 0.43 ela não voltava sem outra mudança na página).
      var agora = Date.now(), espera = (window.__bolaoFalhas || 0) > 6 ? 10000 : 3000, desde = agora - (window.__bolaoBusca || 0);
      if (desde < espera) {
        if (!window.__bolaoDepois) window.__bolaoDepois = setTimeout(function () { window.__bolaoDepois = null; mover(); }, espera - desde + 50);
      } else {
        window.__bolaoBusca = agora;
        achado = procurar(W, H);
        window.__bolaoFalhas = achado ? 0 : (window.__bolaoFalhas || 0) + 1;
      }
    }
    var g = null;
    try { g = desenhar(achado); } catch (e) { g = null; }
    if (achado && g && g.r) {
      var rd = g.r;
      window.__bolaoCookie = JSON.stringify({
        x: (rd.left + rd.width / 2) / W, topo: rd.top / H, largura: rd.width / W, altura: rd.height / H,
        direita: rd.left + rd.width / 2 >= W / 2, cor: g.corLida, opacidade: g.corLida ? g.opacidade : 1, cadeado: true
      });
    } else {
      window.__bolaoCookie = JSON.stringify({ x: 1, topo: 1, largura: 0, cadeado: !!(g && document.getElementById(CADEADO)), semCookie: true });
    }
  }
  window.__bolaoCookies = mover;

  // O toque na página (desde a 0.44): ela está no alto? (a página e as partes roláveis em volta do dedo).
  function noTopo(alvo) {
    var y = window.scrollY || document.documentElement.scrollTop || (document.body && document.body.scrollTop) || 0;
    if (y > 2) return false;
    for (var x = alvo, i = 0; x && x.nodeType === 1 && i < 40; i++) {
      if (x.scrollTop > 2) {
        var cs = estilo(x);
        if (cs && /(auto|scroll|overlay)/.test(cs.overflowY)) return false;
      }
      x = x.parentElement || (x.getRootNode && x.getRootNode().host) || null;
    }
    return true;
  }
  try {
    document.addEventListener('touchstart', function (ev) {
      try { window.BolaoCadeado.toque(noTopo(ev.target)); } catch (e) {}
    }, { capture: true, passive: true });
  } catch (e) {}

  mover();
  // A cada segundo os botões acompanham o ícone (a animação de entrada, o zoom) e voltam se o site os tirou da página.
  try {
    setInterval(function () {
      try {
        if (semIcone()) mover(); else desenhar(achado);
      } catch (e) {}
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
