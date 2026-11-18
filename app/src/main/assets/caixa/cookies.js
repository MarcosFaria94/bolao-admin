// O ícone dos cookies do site para o lado direito (desde a 0.38, pedido do usuário) e onde ele ficou (para o app pôr o
// cadeado logo acima dele). O site é da Caixa: o script procura o botão flutuante dos cookies pelo nome (cookie, lgpd,
// os ids das ferramentas de consentimento mais comuns), sobe até o elemento fixo na tela e, se ele estiver no canto
// de baixo à esquerda, passa para o canto de baixo à direita, com a mesma margem. Os avisos grandes (faixas de ponta a
// ponta) ficam como estão. window.__bolaoCookie = JSON {x, topo, largura} em frações da tela, ou null.
(function () {
  if (window.__bolaoCookies) { window.__bolaoCookies(); return 'ok'; }
  var SELETOR = '[id*="cookie" i],[class*="cookie" i],[aria-label*="cookie" i],[title*="cookie" i],' +
    '[id*="lgpd" i],[class*="lgpd" i],#ot-sdk-btn-floating,.ot-floating-button,#CookiebotWidget';
  function fixo(el) {
    for (var i = 0; el && el !== document.body && el !== document.documentElement && i < 6; i++, el = el.parentElement) {
      var cs = window.getComputedStyle(el);
      if (cs.position === 'fixed' || cs.position === 'sticky') return el;
    }
    return null;
  }
  function mover() {
    var W = window.innerWidth, H = window.innerHeight, achado = null, vistos = [];
    var lista = [];
    try { lista = Array.prototype.slice.call(document.querySelectorAll(SELETOR)); } catch (e) { lista = []; }
    lista.forEach(function (c) {
      var f = fixo(c);
      if (!f || vistos.indexOf(f) >= 0) return;
      vistos.push(f);
      var r = f.getBoundingClientRect();
      if (r.width <= 0 || r.height <= 0) return;
      if (r.width > W * 0.5 || r.height > H * 0.3) return;
      if (r.bottom < H * 0.5) return;
      if (f.getAttribute('data-bolao') !== '1' && r.left + r.width / 2 < W / 2) {
        var margem = Math.max(8, r.left);
        f.style.setProperty('left', 'auto', 'important');
        f.style.setProperty('inset-inline-start', 'auto', 'important');
        f.style.setProperty('right', margem + 'px', 'important');
        f.setAttribute('data-bolao', '1');
        r = f.getBoundingClientRect();
      }
      if (r.left + r.width / 2 >= W / 2) achado = r;
    });
    window.__bolaoCookie = achado ? JSON.stringify({ x: (achado.left + achado.width / 2) / W, topo: achado.top / H, largura: achado.width / W }) : null;
  }
  window.__bolaoCookies = mover;
  mover();
  try {
    new MutationObserver(function () {
      clearTimeout(window.__bolaoEspera);
      window.__bolaoEspera = setTimeout(mover, 300);
    }).observe(document.documentElement, { childList: true, subtree: true });
  } catch (e) {}
  return 'ok';
})()
