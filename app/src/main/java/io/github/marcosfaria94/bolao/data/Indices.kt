package io.github.marcosfaria94.bolao.data

import android.content.Context
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.indices.AuditoriaRegras
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.domain.indices.ParametrosK
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.net.CaixaParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * Índices k1 e k2 e odds de cada modalidade (manual dos índices, parâmetros v1.2), para a
 * lista de Jogos, a Nova aposta e o concurso em destaque.
 *
 * Para cada modalidade: o último concurso apurado (u) e o anterior a ele (a), da API da Caixa,
 * no máximo 3 pedidos ao mesmo tempo. As respostas ficam guardadas no celular: a tela abre com
 * o que já se sabe e se atualiza quando a Caixa responde. Concursos já sorteados (a) não mudam.
 *
 * Desde a 0.7: o cálculo usa o preço de Ajustes (não o do arquivo), avisa quando os dois são
 * diferentes e, a cada concurso novo, confere o rateio pago pela Caixa com as regras do arquivo
 * (AuditoriaRegras): 3 concursos seguidos fora das regras viram um aviso em Notificações.
 */
class Indices(private val context: Context) {

    /** Uma modalidade na lista. [k] = null quando a Caixa não respondeu ou é Loteca/Federal. */
    data class Item(
        val modalidade: Modalidade,
        /** Último concurso apurado, com o número, a data e o prêmio estimado do próximo. */
        val ultimo: Concurso?,
        val k: ResultadoK?,
        /** X das odds (1:X): chance de ganhar qualquer prêmio. */
        val odds: Double?,
        /** Preço da aposta simples usado no cálculo (o de Ajustes), em centavos. */
        val precoAjustes: Long? = null,
        /** Preço com que os parâmetros do arquivo foram calculados, em centavos (null = sem parâmetros). */
        val precoParametros: Long? = null,
    ) {
        val k1: Double? get() = k?.k1
        val k2: Double? get() = k?.k2
        val especial: Boolean get() = k?.especial == true
        val proximoNumero: Int? get() = k?.numero ?: ultimo?.proximoNumero ?: ultimo?.numero?.plus(1)

        /** O preço de Ajustes não é o dos parâmetros (a Caixa reajustou? os parâmetros envelheceram?). */
        val precoDiferente: Boolean
            get() = precoAjustes != null && precoParametros != null && precoAjustes != precoParametros
    }

    /** Aviso dos índices para a aba Notificações (e o topo da lista de Jogos). */
    data class Aviso(
        val chave: String,
        val modalidade: Modalidade,
        val titulo: String,
        val texto: String,
        /** Desde quando vale (ordena a lista de Notificações). */
        val desde: Long,
        /** Tela que explica (rota da navegação). */
        val destino: String,
    )

    /** Situação da auditoria das regras de uma modalidade (guardada no celular). */
    @Serializable
    data class Auditoria(
        /** Último concurso conferido. */
        val ultimo: Int = 0,
        /** Concursos seguidos com alguma faixa fora das regras. */
        val seguidas: Int = 0,
        /** A diferença mais recente: "concurso 3075, 5 acertos: R$ 45.000,00 em vez de R$ 38.295,71". */
        val detalhe: String? = null,
        /** Quando as divergências seguidas chegaram ao limite (o aviso vale desde então). */
        val avisoDesde: Long? = null,
    )

    @Serializable
    private data class ArquivoAuditoria(val versao: String = "", val modalidades: Map<String, Auditoria> = emptyMap())

    val parametros: ParametrosK? by lazy {
        runCatching { context.assets.open("parametros_k.json").use { ParametrosK.ler(it.readBytes().decodeToString()) } }.getOrNull()
    }

    private val pasta: File get() = File(context.filesDir, "indices").apply { mkdirs() }
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _itens = MutableStateFlow<Map<Modalidade, Item>>(emptyMap())
    val itens: StateFlow<Map<Modalidade, Item>> = _itens.asStateFlow()

    private val _atualizando = MutableStateFlow(false)
    val atualizando: StateFlow<Boolean> = _atualizando.asStateFlow()

    /** Falhou a última consulta (sem internet, Caixa fora do ar...). */
    private val _falhou = MutableStateFlow(false)
    val falhou: StateFlow<Boolean> = _falhou.asStateFlow()

    private val _auditorias = MutableStateFlow<Map<Modalidade, Auditoria>>(emptyMap())
    val auditorias: StateFlow<Map<Modalidade, Auditoria>> = _auditorias.asStateFlow()

    private val _avisos = MutableStateFlow<List<Aviso>>(emptyList())
    /** Preço diferente do dos parâmetros e regras que mudaram (3 concursos seguidos fora delas). */
    val avisos: StateFlow<List<Aviso>> = _avisos.asStateFlow()

    /** Último (u) e anterior (a) de cada modalidade, para refazer o cálculo quando o preço muda. */
    private val concursos = ConcurrentHashMap<Modalidade, Pair<Concurso, Concurso?>>()

    /** Preço de Ajustes de cada modalidade, em centavos. */
    @Volatile
    private var precos: Map<Modalidade, Long> = emptyMap()

    /** Desde quando cada preço está diferente do dos parâmetros (nesta abertura do app). */
    private val precoDiferenteDesde = ConcurrentHashMap<Modalidade, Long>()

    @Volatile
    private var ultimaAtualizacao = 0L

    init {
        parametros?.let { CaixaApi.configurar(it.paramsUrl, it.basesReserva) }
        escopo.launch {
            carregarAuditoria()
            carregarGuardados()
        }
        // O preço de Ajustes entra no cálculo: mudou lá, os índices são refeitos na hora.
        escopo.launch {
            context.bolao.repo.estado
                .filterNotNull()
                .map { e -> Modalidade.entries.associateWith { e.config.preco(it) } }
                .distinctUntilChanged()
                .collect { novos ->
                    precos = novos
                    recalcular()
                }
        }
    }

    private fun chave(m: Modalidade): String? = m.slugApi.takeIf { parametros?.modalidades?.containsKey(it) == true }

    private fun oddsDe(m: Modalidade): Double? = chave(m)?.let { c -> parametros?.modalidades?.get(c)?.let { IndiceK.odds(it).second } }

    private fun precoDosParametros(m: Modalidade): Long? =
        chave(m)?.let { c -> parametros?.modalidades?.get(c)?.preco?.let { (it * 100).roundToLong() } }

    /** Preço de Ajustes (centavos); a Federal tem preço variável e fica de fora. */
    private fun precoDeAjustes(m: Modalidade): Long? = if (m.precoVariavel) null else precos[m] ?: m.precoPadrao

    private fun arquivoUltimo(m: Modalidade) = File(pasta, "${m.slugApi}-ultimo.json")
    private fun arquivoConcurso(m: Modalidade, n: Int) = File(pasta, "${m.slugApi}-$n.json")
    private val arquivoAuditoria: File get() = File(pasta, "auditoria.json")

    private fun ler(m: Modalidade, f: File): Concurso? =
        if (f.isFile) runCatching { CaixaParser.ler(m, f.readText()) }.getOrNull() else null

    private fun guardar(f: File, texto: String) {
        runCatching {
            val temp = File(f.path + ".tmp")
            temp.writeText(texto)
            if (!temp.renameTo(f)) {
                f.delete()
                temp.renameTo(f)
            }
        }
    }

    private fun calcular(m: Modalidade, u: Concurso?, a: Concurso?): Item {
        val p = parametros
        val c = chave(m)
        val ru = u?.registro
        val ra = a?.registro
        val precoAjustes = precoDeAjustes(m)
        val k = if (p != null && c != null && ru != null && ra != null && ra.numero == ru.numero - 1) {
            runCatching { IndiceK.calcular(p, c, ru, ra, preco = precoAjustes?.let { it / 100.0 }) }.getOrNull()
        } else {
            null
        }
        return Item(m, u, k, oddsDe(m), precoAjustes = precoAjustes, precoParametros = precoDosParametros(m))
    }

    private fun carregarGuardados() {
        val mapa = Modalidade.entries.associateWith { m ->
            val u = ler(m, arquivoUltimo(m))
            val a = u?.let { ler(m, arquivoConcurso(m, it.numero - 1)) }
            if (u != null) concursos[m] = u to a
            calcular(m, u, a)
        }
        _itens.update { atual -> mapa + atual }
        publicarAvisos()
    }

    /** Refaz o cálculo das modalidades já conhecidas (o preço de Ajustes mudou). */
    private fun recalcular() {
        if (concursos.isEmpty()) {
            publicarAvisos()
            return
        }
        val novos = concursos.mapValues { (m, par) -> calcular(m, par.first, par.second) }
        _itens.update { it + novos }
        publicarAvisos()
    }

    /** Consulta a Caixa (no máximo a cada 10 minutos, a não ser que [forcar]). */
    fun atualizar(forcar: Boolean = false) {
        if (_atualizando.value) return
        if (!forcar && System.currentTimeMillis() - ultimaAtualizacao < VALIDADE_MS) return
        _atualizando.value = true
        escopo.launch {
            val limite = Semaphore(3)
            var algumaFalha = false
            try {
                Modalidade.entries.map { m ->
                    async {
                        try {
                            val (u, textoU) = limite.withPermit { CaixaApi.ultimoComTexto(m) }
                            guardar(arquivoUltimo(m), textoU)
                            val a = if (chave(m) != null && u.numero > 1) {
                                val f = arquivoConcurso(m, u.numero - 1)
                                ler(m, f) ?: limite.withPermit {
                                    runCatching { CaixaApi.concursoComTexto(m, u.numero - 1) }.getOrNull()
                                }?.let { (c, texto) ->
                                    guardar(f, texto)
                                    c
                                }
                            } else {
                                null
                            }
                            concursos[m] = u to a
                            val item = calcular(m, u, a)
                            _itens.update { it + (m to item) }
                            auditar(m, u, a)
                        } catch (t: Throwable) {
                            algumaFalha = true
                        }
                    }
                }.awaitAll()
                // Guarda só os concursos anteriores que ainda servem (o último de cada modalidade).
                val manter = _itens.value.values.mapNotNull { i -> i.ultimo?.let { arquivoConcurso(i.modalidade, it.numero - 1).name } }.toSet()
                pasta.listFiles()?.filter { it.name.matches(Regex(".+-\\d+\\.json")) && it.name !in manter }?.forEach { it.delete() }
                salvarAuditoria()
            } finally {
                publicarAvisos()
                _falhou.value = algumaFalha
                ultimaAtualizacao = System.currentTimeMillis()
                _atualizando.value = false
            }
        }
    }

    // ---------- auditoria das regras ----------

    /** Confere o concurso [u] com as regras (uma vez por concurso) e conta as divergências seguidas. */
    private fun auditar(m: Modalidade, u: Concurso, a: Concurso?) {
        val p = parametros ?: return
        val c = chave(m) ?: return
        val ru = u.registro ?: return
        val atual = _auditorias.value[m] ?: Auditoria()
        if (ru.numero <= atual.ultimo) return
        val r = runCatching { AuditoriaRegras.auditar(p, c, ru, a?.registro) }.getOrNull() ?: return
        val nova = if (r.confere) {
            Auditoria(ultimo = r.numero)
        } else {
            val seguidas = atual.seguidas + 1
            val f = r.divergentes.first()
            Auditoria(
                ultimo = r.numero,
                seguidas = seguidas,
                detalhe = "concurso ${r.numero}, ${f.nome}: ${Dinheiro.rs((f.pago * 100).roundToLong())} " +
                    "em vez de ${Dinheiro.rs((f.esperado * 100).roundToLong())}",
                avisoDesde = atual.avisoDesde ?: if (seguidas >= DIVERGENCIAS_PARA_AVISAR) System.currentTimeMillis() else null,
            )
        }
        _auditorias.update { it + (m to nova) }
    }

    private fun carregarAuditoria() {
        val versao = parametros?.versao.orEmpty()
        val lido = runCatching { json.decodeFromString(ArquivoAuditoria.serializer(), arquivoAuditoria.readText()) }.getOrNull()
        // Parâmetros novos (outra versão do arquivo): a contagem recomeça.
        if (lido == null || lido.versao != versao) return
        _auditorias.value = lido.modalidades.mapNotNull { (slug, a) -> Modalidade.entries.firstOrNull { it.slugApi == slug }?.let { it to a } }.toMap()
    }

    private fun salvarAuditoria() {
        val conteudo = ArquivoAuditoria(parametros?.versao.orEmpty(), _auditorias.value.mapKeys { it.key.slugApi })
        guardar(arquivoAuditoria, json.encodeToString(ArquivoAuditoria.serializer(), conteudo))
    }

    private fun publicarAvisos() {
        val agora = System.currentTimeMillis()
        val lista = mutableListOf<Aviso>()
        for (m in Modalidade.entries) {
            val item = _itens.value[m]
            if (item != null && item.precoDiferente) {
                val desde = precoDiferenteDesde.getOrPut(m) { agora }
                lista += Aviso(
                    chave = "indices-preco-${m.name}",
                    modalidade = m,
                    titulo = "Preço da ${m.nome} diferente do usado nos índices",
                    texto = "Ajustes: ${Dinheiro.rs(item.precoAjustes ?: 0)} · parâmetros dos índices: ${Dinheiro.rs(item.precoParametros ?: 0)}. " +
                        "Os índices já usam o preço de Ajustes, mas o modelo de vendas foi calibrado com o outro: " +
                        "confira o preço ou recalibre o parametros_k.json.",
                    desde = desde,
                    destino = "ajustes",
                )
            } else {
                precoDiferenteDesde.remove(m)
            }
            val aud = _auditorias.value[m]
            if (aud != null && aud.seguidas >= DIVERGENCIAS_PARA_AVISAR) {
                lista += Aviso(
                    chave = "indices-regras-${m.name}",
                    modalidade = m,
                    titulo = "Regras da ${m.nome} mudaram: os índices podem estar errados",
                    texto = "Nos últimos ${aud.seguidas} concursos, o rateio pago pela Caixa não bateu com as regras dos índices" +
                        (aud.detalhe?.let { " (último: $it)" } ?: "") + ". Recalibre o parametros_k.json.",
                    desde = aud.avisoDesde ?: agora,
                    destino = "modalidade/${m.name}",
                )
            }
        }
        _avisos.value = lista.sortedByDescending { it.desde }
    }

    /** Ordem da lista: maior k1 primeiro; sem k1 (Loteca, Federal, especial ou sem dados) no fim. */
    fun ordenados(mapa: Map<Modalidade, Item> = _itens.value): List<Item> =
        Modalidade.entries.map { mapa[it] ?: Item(it, null, null, oddsDe(it)) }
            .sortedWith(compareByDescending<Item> { it.k1 ?: Double.NEGATIVE_INFINITY }.thenBy { it.modalidade.ordinal })

    /** Concurso em destaque: o de maior k1 (o melhor retorno esperado), sem olhar o saldo. */
    fun destaque(mapa: Map<Modalidade, Item>): Item? = ordenados(mapa).firstOrNull { it.k1 != null }

    companion object {
        private const val VALIDADE_MS = 10 * 60 * 1000L

        /** Concursos seguidos fora das regras para avisar que elas mudaram. */
        const val DIVERGENCIAS_PARA_AVISAR = 3
    }
}
