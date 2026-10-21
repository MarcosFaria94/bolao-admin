package io.github.marcosfaria94.bolao.data

import android.content.Context
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

/**
 * Índices k1 e k2 e odds de cada modalidade (manual dos índices, parâmetros v1.2), para a
 * lista de Jogos, a Nova aposta e o concurso em destaque.
 *
 * Para cada modalidade: o último concurso apurado (u) e o anterior a ele (a), da API da Caixa,
 * no máximo 3 pedidos ao mesmo tempo. As respostas ficam guardadas no celular: a tela abre com
 * o que já se sabe e se atualiza quando a Caixa responde. Concursos já sorteados (a) não mudam.
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
    ) {
        val k1: Double? get() = k?.k1
        val k2: Double? get() = k?.k2
        val especial: Boolean get() = k?.especial == true
        val proximoNumero: Int? get() = k?.numero ?: ultimo?.proximoNumero ?: ultimo?.numero?.plus(1)
    }

    val parametros: ParametrosK? by lazy {
        runCatching { context.assets.open("parametros_k.json").use { ParametrosK.ler(it.readBytes().decodeToString()) } }.getOrNull()
    }

    private val pasta: File get() = File(context.filesDir, "indices").apply { mkdirs() }
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _itens = MutableStateFlow<Map<Modalidade, Item>>(emptyMap())
    val itens: StateFlow<Map<Modalidade, Item>> = _itens.asStateFlow()

    private val _atualizando = MutableStateFlow(false)
    val atualizando: StateFlow<Boolean> = _atualizando.asStateFlow()

    /** Falhou a última consulta (sem internet, Caixa fora do ar...). */
    private val _falhou = MutableStateFlow(false)
    val falhou: StateFlow<Boolean> = _falhou.asStateFlow()

    @Volatile
    private var ultimaAtualizacao = 0L

    init {
        parametros?.let { CaixaApi.configurar(it.paramsUrl, it.basesReserva) }
        escopo.launch { carregarGuardados() }
    }

    private fun chave(m: Modalidade): String? = m.slugApi.takeIf { parametros?.modalidades?.containsKey(it) == true }

    private fun oddsDe(m: Modalidade): Double? = chave(m)?.let { c -> parametros?.modalidades?.get(c)?.let { IndiceK.odds(it).second } }

    private fun arquivoUltimo(m: Modalidade) = File(pasta, "${m.slugApi}-ultimo.json")
    private fun arquivoConcurso(m: Modalidade, n: Int) = File(pasta, "${m.slugApi}-$n.json")

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
        val k = if (p != null && c != null && ru != null && ra != null && ra.numero == ru.numero - 1) {
            runCatching { IndiceK.calcular(p, c, ru, ra) }.getOrNull()
        } else {
            null
        }
        return Item(m, u, k, oddsDe(m))
    }

    private fun carregarGuardados() {
        val mapa = Modalidade.entries.associateWith { m ->
            val u = ler(m, arquivoUltimo(m))
            val a = u?.let { ler(m, arquivoConcurso(m, it.numero - 1)) }
            calcular(m, u, a)
        }
        _itens.update { atual -> mapa + atual }
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
                            val item = calcular(m, u, a)
                            _itens.update { it + (m to item) }
                        } catch (t: Throwable) {
                            algumaFalha = true
                        }
                    }
                }.awaitAll()
                // Guarda só os concursos anteriores que ainda servem (o último de cada modalidade).
                val manter = _itens.value.values.mapNotNull { i -> i.ultimo?.let { arquivoConcurso(i.modalidade, it.numero - 1).name } }.toSet()
                pasta.listFiles()?.filter { it.name.matches(Regex(".+-\\d+\\.json")) && it.name !in manter }?.forEach { it.delete() }
            } finally {
                _falhou.value = algumaFalha
                ultimaAtualizacao = System.currentTimeMillis()
                _atualizando.value = false
            }
        }
    }

    /** Ordem da lista: maior k1 primeiro; sem k1 (Loteca, Federal, especial ou sem dados) no fim. */
    fun ordenados(mapa: Map<Modalidade, Item> = _itens.value): List<Item> =
        Modalidade.entries.map { mapa[it] ?: Item(it, null, null, oddsDe(it)) }
            .sortedWith(compareByDescending<Item> { it.k1 ?: Double.NEGATIVE_INFINITY }.thenBy { it.modalidade.ordinal })

    /** Concurso em destaque: o de maior k1 (o melhor retorno esperado), sem olhar o saldo. */
    fun destaque(mapa: Map<Modalidade, Item>): Item? = ordenados(mapa).firstOrNull { it.k1 != null }

    companion object {
        private const val VALIDADE_MS = 10 * 60 * 1000L
    }
}
