package io.github.marcosfaria94.bolao.data

import android.content.Context
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.domain.AgendaDaCaixa
import io.github.marcosfaria94.bolao.domain.AvisosDeEspeciais
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Dinheiro
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Especiais
import io.github.marcosfaria94.bolao.domain.ConcursoEspecial
import io.github.marcosfaria94.bolao.domain.indices.AuditoriaRegras
import io.github.marcosfaria94.bolao.domain.indices.IndiceEspecial
import io.github.marcosfaria94.bolao.domain.indices.IndiceK
import io.github.marcosfaria94.bolao.domain.indices.ParametrosK
import io.github.marcosfaria94.bolao.domain.indices.ResultadoK
import io.github.marcosfaria94.bolao.domain.PremiosTipicos
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.net.CaixaParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * Índices k1 e k2 e odds de cada modalidade (manual dos índices, parâmetros v1.2), para a
 * lista de Jogos e o concurso em destaque.
 *
 * Para cada modalidade: o último concurso apurado (u) e o anterior a ele (a), da API da Caixa,
 * no máximo 3 pedidos ao mesmo tempo. As respostas ficam guardadas no celular: a tela abre com
 * o que já se sabe e se atualiza quando a Caixa responde. Concursos já sorteados (a) não mudam.
 *
 * Desde a 0.20 a Caixa só é consultada quando a agenda dos sorteios pede ([AgendaDaCaixa]): depois
 * de cada sorteio (desde a 0.25, 5 minutos depois) e de novo a cada 5 minutos enquanto o resultado não
 * chega; o despertador acorda o app para isso, mesmo fechado ([proximaConsulta]). Os dados guardados valem até o próximo sorteio e nunca voltam para um concurso mais velho
 * (servidores da Caixa às vezes respondem com dados atrasados). A última consulta de cada modalidade
 * (e o erro, se deu) fica guardada no celular ([consultas]), para a tela mostrar se os dados são
 * certos ou estão esperando o resultado.
 *
 * Desde a 0.7: o cálculo usa o preço de Ajustes (não o do arquivo), avisa quando os dois são
 * diferentes e, a cada concurso novo, confere o rateio pago pela Caixa com as regras do arquivo
 * (AuditoriaRegras): 3 concursos seguidos fora das regras viram um aviso no resumo do Painel de Jogos.
 *
 * Desde a 0.33: o concurso anterior (do k1) vem do histórico ([Historico]) antes de ir à Caixa, e os concursos
 * especiais (o próximo concurso, quando é um, e os anunciados à parte nos Jogos) têm o k1 e o k2 da média ponderada
 * dos últimos especiais já sorteados ([IndiceEspecial], [especiais]).
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

    /** Aviso dos índices para o resumo do Painel de Jogos e o topo da lista de Jogos. */
    data class Aviso(
        val chave: String,
        val modalidade: Modalidade,
        val titulo: String,
        val texto: String,
        /** Desde quando vale (ordena as pendências). */
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

    /** A última consulta à Caixa de uma modalidade (guardada no celular). */
    @Serializable
    data class Consulta(
        /** Última vez que o app perguntou à Caixa (deu certo ou não), em ms. */
        val tentativa: Long = 0,
        /** Última resposta da Caixa, em ms. */
        val sucesso: Long = 0,
        /** O erro da última tentativa (null = a Caixa respondeu). */
        val erro: String? = null,
    ) {
        val tentativaEm: Instant? get() = tentativa.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }
    }

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

    private val _consultas = MutableStateFlow<Map<Modalidade, Consulta>>(emptyMap())
    /** A última consulta de cada modalidade (quando foi e o erro, se deu). */
    val consultas: StateFlow<Map<Modalidade, Consulta>> = _consultas.asStateFlow()

    /** Uma consulta de cada vez. */
    private val trava = Mutex()

    /** Os dados guardados no celular já foram lidos (a agenda depende deles). */
    private lateinit var carregamento: Job

    private val _auditorias = MutableStateFlow<Map<Modalidade, Auditoria>>(emptyMap())
    val auditorias: StateFlow<Map<Modalidade, Auditoria>> = _auditorias.asStateFlow()

    private val _avisos = MutableStateFlow<List<Aviso>>(emptyList())
    /** Preço diferente do dos parâmetros e regras que mudaram (3 concursos seguidos fora delas). */
    val avisos: StateFlow<List<Aviso>> = _avisos.asStateFlow()

    private val _especiais = MutableStateFlow<Map<Modalidade, IndiceEspecial.Estimativa>>(emptyMap())

    /** O k1 e o k2 estimados para o próximo concurso especial de cada modalidade que tem um (desde a 0.33). */
    val especiais: StateFlow<Map<Modalidade, IndiceEspecial.Estimativa>> = _especiais.asStateFlow()

    private val historico: Historico get() = context.bolao.historico

    /** Último (u) e anterior (a) de cada modalidade, para refazer o cálculo quando o preço muda. */
    private val concursos = ConcurrentHashMap<Modalidade, Pair<Concurso, Concurso?>>()

    /** Preço de Ajustes de cada modalidade, em centavos. */
    @Volatile
    private var precos: Map<Modalidade, Long> = emptyMap()

    /** Desde quando cada preço está diferente do dos parâmetros (nesta abertura do app). */
    private val precoDiferenteDesde = ConcurrentHashMap<Modalidade, Long>()

    init {
        parametros?.let { CaixaApi.configurar(it.paramsUrl, it.basesReserva) }
        // O último concurso consultado em outras telas (o volante, o registro) também entra aqui.
        CaixaApi.aoReceberUltimo = { m, c, texto -> escopo.launch { carregamento.join(); receber(m, c, texto, null) } }
        // Todo concurso completo que chega da Caixa entra no histórico do celular (desde a 0.33).
        CaixaApi.aoReceberConcurso = { m, c, texto -> escopo.launch { historico.acrescentar(m, c, texto) } }
        carregamento = escopo.launch {
            carregarAuditoria()
            carregarConsultas()
            carregarGuardados()
        }
        // O k1 e o k2 dos concursos especiais (desde a 0.33): dos especiais já sorteados, no histórico; um especial novo
        // no histórico refaz a conta.
        escopo.launch {
            carregamento.join()
            estimarEspeciais()
            historico.novosEspeciais.drop(1).collect { estimarEspeciais() }
        }
        // O preço de Ajustes entra no cálculo: mudou lá, os índices são refeitos na hora.
        escopo.launch {
            context.bolao.sessoes.estado
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
    private val arquivoConsultas: File get() = File(pasta, "consultas.json")

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
        val calculado = if (p != null && c != null && ru != null && ra != null && ra.numero == ru.numero - 1) {
            runCatching { IndiceK.calcular(p, c, ru, ra, preco = precoAjustes?.let { it / 100.0 }) }.getOrNull()
        } else {
            null
        }
        // O próximo concurso é um especial (pela conta dos acumulados ou pela lista dos anunciados): o k1 e o k2 são
        // os da média dos últimos especiais (desde a 0.33; antes, nenhum).
        val k = calculado?.let { k ->
            if (k.especial || Especiais.doConcurso(m, k.numero) != null) comEstimativaDoEspecial(k.copy(especial = true), m) else k
        }
        return Item(m, u, k, oddsDe(m), precoAjustes = precoAjustes, precoParametros = precoDosParametros(m))
    }

    private fun comEstimativaDoEspecial(k: ResultadoK, m: Modalidade): ResultadoK {
        val e = _especiais.value[m]
        return k.copy(k1 = e?.k1, k2 = e?.k2, k1PorFaixa = emptyMap())
    }

    /** As modalidades com concurso especial (as que têm o nome dele). */
    private val comEspecial: List<Modalidade> get() = Modalidade.ATIVAS.filter { Especiais.nomeDoEspecial(it) != null && chave(it) != null }

    /** Refaz a estimativa do k1 e do k2 dos especiais, a partir dos especiais do histórico. */
    private suspend fun estimarEspeciais() {
        val p = parametros ?: return
        val novas = comEspecial.mapNotNull { m ->
            val c = chave(m) ?: return@mapNotNull null
            runCatching { IndiceEspecial.estimar(p, c, historico.especiais(m), epoca = Especiais.epoca(m)) }.getOrNull()?.let { m to it }
        }.toMap()
        if (novas == _especiais.value) return
        // Publica na linha principal (quem lê os índices está nas telas).
        withContext(Dispatchers.Main) {
            _especiais.value = novas
            recalcular()
        }
    }

    /**
     * Os índices de um concurso especial anunciado à parte nos Jogos (desde a 0.33): o k1 e o k2 da média dos
     * últimos especiais e as odds da modalidade (sem a estimativa, o k1 e o k2 ficam vazios).
     */
    fun doEspecial(e: ConcursoEspecial, mapa: Map<Modalidade, Item> = _itens.value): Item {
        val m = e.modalidade
        val base = mapa[m] ?: Item(m, null, null, oddsDe(m))
        return base.copy(k = resultadoDoEspecial(m, e.concurso) ?: ResultadoK(e.concurso, true, 0.0, 0, emptyMap(), 0.0))
    }

    /**
     * Os concursos especiais que podem ser avisados (desde a 0.34): os anunciados ([Especiais.CONHECIDOS], com o
     * sorteio ainda por vir) e o próximo concurso de cada modalidade quando ele é especial; com o k1 da média dos
     * últimos especiais.
     */
    fun alvosDosEspeciais(agora: Instant = Instant.now(), mapa: Map<Modalidade, Item> = _itens.value): List<AvisosDeEspeciais.Alvo> {
        val anunciados = Especiais.CONHECIDOS.filter { it.momento().isAfter(agora) }.map { e ->
            AvisosDeEspeciais.Alvo(
                e.modalidade, e.nome, e.concurso, e.dia, e.hora, e.premioEstimado,
                k1 = _especiais.value[e.modalidade]?.k1,
                destino = "especial/${e.modalidade.name}/${e.concurso}?detalhes=false",
            )
        }
        val proximos = mapa.values.filter { it.especial && it.proximoNumero != null }.mapNotNull { item ->
            val m = item.modalidade
            val n = item.proximoNumero ?: return@mapNotNull null
            if (anunciados.any { it.modalidade == m && it.concurso == n }) return@mapNotNull null
            val dia = item.ultimo?.proximaData
            AvisosDeEspeciais.Alvo(
                m, Especiais.nomeDoEspecial(m) ?: "Concurso especial da ${m.nome}", n, dia,
                dia?.let { AgendaDaCaixa.horaDoSorteio(m, it) }, item.ultimo?.premioEstimadoProximo, item.k1,
                destino = "modalidade/${m.name}",
            )
        }
        return anunciados + proximos
    }

    /** O k1 e o k2 do concurso [numero] quando ele é um especial anunciado (null = não é, ou sem a estimativa). */
    fun resultadoDoEspecial(m: Modalidade, numero: Int): ResultadoK? {
        if (Especiais.doConcurso(m, numero) == null) return null
        val e = _especiais.value[m] ?: return null
        return ResultadoK(numero, true, 0.0, 0, emptyMap(), 0.0, k1 = e.k1, k2 = e.k2)
    }

    /**
     * Os prêmios típicos do próximo concurso da modalidade (desde a 0.32, nos Detalhes): a estimativa de cada faixa,
     * em centavos e antes do IR ([PremiosTipicos.estimar], o mesmo cálculo do k1, com o preço de Ajustes). null sem
     * os dados dos dois últimos concursos, sem os parâmetros (Loteca) ou num concurso especial.
     */
    fun premiosTipicos(m: Modalidade): Map<Int, Long>? {
        val p = parametros ?: return null
        val c = chave(m) ?: return null
        val (u, a) = concursos[m] ?: return null
        val ru = u.registro ?: return null
        val ra = a?.registro?.takeIf { it.numero == ru.numero - 1 } ?: return null
        return PremiosTipicos.estimar(p, c, ru, ra, precoDeAjustes(m)?.let { it / 100.0 })
    }

    private fun carregarGuardados() {
        val mapa = Modalidade.ATIVAS.associateWith { m ->
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

    /** A situação dos dados da modalidade [m] agora: em dia (certos) ou esperando o resultado. */
    fun situacao(
        m: Modalidade,
        agora: Instant = Instant.now(),
        mapa: Map<Modalidade, Item> = _itens.value,
        ultimas: Map<Modalidade, Consulta> = _consultas.value,
    ): AgendaDaCaixa.Situacao = AgendaDaCaixa.situacao(m, mapa[m]?.ultimo, agora, ultimas[m]?.tentativaEm)

    /** Quando a agenda pede a próxima consulta depois de um sorteio (o despertador do Vigia acorda o app para ela). */
    fun proximaConsulta(agora: Instant = Instant.now()): Instant? =
        AgendaDaCaixa.proximaConsulta(Modalidade.ATIVAS.map { situacao(it, agora) }, agora)

    /**
     * Consulta a Caixa, só para os jogos que a agenda pede (o sorteio passou e o resultado ainda não
     * chegou). [forcar] = "Tentar agora": todos os que não estão em dia, sem esperar o intervalo entre
     * as tentativas.
     */
    fun atualizar(forcar: Boolean = false) {
        if (_atualizando.value) return
        escopo.launch { consultar(forcar) }
    }

    /** Ao entrar nos Jogos (ou na página de um jogo): a agenda decide se precisa consultar. */
    fun atualizarAoEntrar() = atualizar()

    /** O mesmo que [atualizar], esperando terminar (o [io.github.marcosfaria94.bolao.notificacoes.Vigia], com o app fechado). */
    suspend fun consultar(forcar: Boolean = false) {
        carregamento.join()
        if (!trava.tryLock()) return
        try {
            val agora = Instant.now()
            val devidas = Modalidade.ATIVAS.filter { m ->
                val s = situacao(m, agora)
                // Sem o concurso anterior (o k1 precisa dele): pede de novo, respeitando o intervalo.
                val semAnterior = concursos[m]?.let { (u, a) -> a == null && chave(m) != null && u.numero > 1 } == true
                val podeTentar = _consultas.value[m]?.tentativaEm?.let { !agora.isBefore(it.plus(AgendaDaCaixa.INTERVALO_DAS_TENTATIVAS)) } ?: true
                if (forcar) !s.certa || semAnterior else s.precisaConsultar(agora) || (semAnterior && podeTentar)
            }
            if (devidas.isEmpty()) return
            _atualizando.value = true
            val limite = Semaphore(3)
            try {
                devidas.map { m -> escopo.async { consultarModalidade(m, limite) } }.awaitAll()
                // Guarda só os concursos anteriores que ainda servem (o último de cada modalidade).
                val manter = concursos.map { (m, par) -> arquivoConcurso(m, par.first.numero - 1).name }.toSet()
                pasta.listFiles()?.filter { it.name.matches(Regex(".+-\\d+\\.json")) && it.name !in manter }?.forEach { it.delete() }
                salvarAuditoria()
                salvarConsultas()
            } finally {
                publicarAvisos()
                _atualizando.value = false
            }
        } finally {
            trava.unlock()
        }
    }

    /** Pergunta à Caixa o último concurso da modalidade [m] e guarda a tentativa (e o erro, se deu). */
    private suspend fun consultarModalidade(m: Modalidade, limite: Semaphore) {
        val agora = System.currentTimeMillis()
        try {
            val (u, texto) = limite.withPermit { CaixaApi.ultimoComTexto(m) }
            receber(m, u, texto, limite)
            _consultas.update { it + (m to Consulta(tentativa = agora, sucesso = agora)) }
        } catch (t: Throwable) {
            val antes = _consultas.value[m] ?: Consulta()
            _consultas.update { it + (m to antes.copy(tentativa = agora, erro = t.message ?: "Não foi possível consultar a Caixa.")) }
        }
    }

    /**
     * Um último concurso da modalidade [m] que a Caixa mandou: fica no lugar do guardado só se for mais
     * novo (ou o mesmo, mais completo); o anterior (do k1) vem do celular ou da Caixa.
     */
    private suspend fun receber(m: Modalidade, novo: Concurso, texto: String, limite: Semaphore?) {
        val guardado = concursos[m]?.first
        val u = if (AgendaDaCaixa.aceitar(guardado, novo)) {
            guardar(arquivoUltimo(m), texto)
            novo
        } else {
            guardado ?: return
        }
        val antes = concursos[m]?.takeIf { it.first.numero == u.numero }?.second
        val a = antes ?: anterior(m, u, limite)
        if (u === guardado && a == antes) return
        concursos[m] = u to a
        _itens.update { it + (m to calcular(m, u, a)) }
        auditar(m, u, a)
        if (limite == null) {
            salvarAuditoria()
            publicarAvisos()
        }
    }

    /** O concurso anterior ao [u] (para o k1): do celular, do histórico ou da Caixa (null = Loteca, Federal ou sem resposta). */
    private suspend fun anterior(m: Modalidade, u: Concurso, limite: Semaphore?): Concurso? {
        if (chave(m) == null || u.numero <= 1) return null
        val f = arquivoConcurso(m, u.numero - 1)
        ler(m, f)?.let { return it }
        runCatching { historico.concurso(m, u.numero - 1) }.getOrNull()?.takeIf { it.registro != null }?.let { return it }
        suspend fun buscar() = runCatching { CaixaApi.concursoComTexto(m, u.numero - 1) }.getOrNull()
        val (c, texto) = (if (limite != null) limite.withPermit { buscar() } else buscar()) ?: return null
        guardar(f, texto)
        return c
    }

    private fun carregarConsultas() {
        val lido = runCatching { json.decodeFromString(SERIALIZADOR_CONSULTAS, arquivoConsultas.readText()) }.getOrNull() ?: return
        _consultas.value = lido.mapNotNull { (slug, c) -> Modalidade.entries.firstOrNull { it.slugApi == slug }?.let { it to c } }.toMap()
    }

    private fun salvarConsultas() {
        guardar(arquivoConsultas, json.encodeToString(SERIALIZADOR_CONSULTAS, _consultas.value.mapKeys { it.key.slugApi }))
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
        for (m in Modalidade.ATIVAS) {
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
                    destino = "ajustes/parametros",
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

    /** Ordem da lista: maior k1 primeiro; sem k1 (Loteca, especial ou sem dados) no fim. Sem a Federal (desde a 0.27). */
    fun ordenados(mapa: Map<Modalidade, Item> = _itens.value): List<Item> =
        Modalidade.ATIVAS.map { mapa[it] ?: Item(it, null, null, oddsDe(it)) }
            .sortedWith(compareByDescending<Item> { it.k1 ?: Double.NEGATIVE_INFINITY }.thenBy { it.modalidade.ordinal })

    /** O jogo de maior k1 (o melhor retorno esperado), sem olhar o saldo: o concurso em destaque quando não há especial. */
    fun maiorK1(mapa: Map<Modalidade, Item>): Item? = ordenados(mapa).firstOrNull { it.k1 != null }

    /**
     * O concurso em destaque: a modalidade e, quando é um, o concurso especial. Desde a 0.34 (pedido do usuário) é
     * sempre o de maior k1 entre todas as opções dos Jogos ([opcoes]: as modalidades e os especiais à parte, com o k1
     * da média dos últimos especiais); de 0.28 a 0.33, o especial com apostas abertas era sempre o destaque.
     */
    data class Destaque(val modalidade: Modalidade, val especial: ConcursoEspecial? = null)

    /** Uma opção da lista dos Jogos (desde a 0.34): o cartão de uma modalidade ou um concurso especial à parte. */
    sealed interface Opcao {
        val item: Item
        val k1: Double? get() = item.k1

        /** O cartão da modalidade (o próximo concurso dela, especial ou não). */
        data class DoJogo(override val item: Item) : Opcao

        /** Um concurso especial anunciado, à parte (ainda não é o próximo da modalidade). */
        data class Especial(val especial: ConcursoEspecial, override val item: Item) : Opcao
    }

    /**
     * As opções dos Jogos em ordem de k1, do maior para o menor (desde a 0.34): os especiais à parte entram na mesma
     * ordem que as modalidades (até a 0.33 ficavam sempre no alto); sem k1 (Loteca, sem dados), no fim. No empate, o
     * especial primeiro e depois a ordem das modalidades.
     */
    fun opcoes(mapa: Map<Modalidade, Item>, agora: Instant = Instant.now()): List<Opcao> {
        val especiais = Especiais.aParte(agora, { mapa[it]?.proximoNumero }).map { Opcao.Especial(it, doEspecial(it, mapa)) }
        val jogos = ordenados(mapa).map { Opcao.DoJogo(it) }
        return emOrdem(especiais + jogos)
    }

    /** O destaque de uma opção: a modalidade e o especial (o à parte, ou o próximo concurso quando é um especial). */
    fun destaqueDa(o: Opcao): Destaque = destaqueDe(o)

    fun destaque(mapa: Map<Modalidade, Item>, agora: Instant = Instant.now()): Destaque? =
        opcoes(mapa, agora).firstOrNull { it.k1 != null }?.let(::destaqueDa)

    companion object {
        /** A ordem das opções dos Jogos (desde a 0.34): maior k1 primeiro; sem k1 no fim; no empate, o especial e a ordem das modalidades. */
        fun emOrdem(opcoes: List<Opcao>): List<Opcao> = opcoes.sortedWith(
            compareByDescending<Opcao> { it.k1 ?: Double.NEGATIVE_INFINITY }
                .thenBy { if (it is Opcao.Especial) 0 else 1 }
                .thenBy { it.item.modalidade.ordinal },
        )

        /** O destaque de uma opção (ver [destaqueDa]). */
        fun destaqueDe(o: Opcao): Destaque = when (o) {
            is Opcao.Especial -> Destaque(o.especial.modalidade, o.especial)
            is Opcao.DoJogo -> Destaque(o.item.modalidade, Especiais.doConcurso(o.item.modalidade, o.item.proximoNumero))
        }

        private val SERIALIZADOR_CONSULTAS = MapSerializer(String.serializer(), Consulta.serializer())

        /** Concursos seguidos fora das regras para avisar que elas mudaram. */
        const val DIVERGENCIAS_PARA_AVISAR = 3
    }
}
