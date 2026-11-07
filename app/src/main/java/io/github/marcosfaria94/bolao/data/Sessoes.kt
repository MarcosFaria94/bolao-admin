package io.github.marcosfaria94.bolao.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Uma sessão: um bolão (ou o Individual) com os seus dados, separados dos das outras (desde a 0.21). */
@Serializable
data class Sessao(
    val id: String,
    /** O nome que identifica a sessão ("Bolão 1", o nome escolhido no cadastro ou "Individual"). Vazio = cadastro não concluído. */
    val nome: String = "",
    val criadaEm: Long = 0,
    /** Bolão ou Individual (null = cadastro não concluído). */
    val modo: Modo? = null,
) {
    val individual: Boolean get() = modo == Modo.INDIVIDUAL
}

/**
 * As sessões do app (desde a 0.21): mais de um bolão ativo no mesmo celular, cada um com os seus
 * dados, anexos e ajustes (um [Repositorio] por sessão). Uma sessão está em uso de cada vez (a que
 * aparece nas telas); o Vigia cuida de todas.
 *
 * - A sessão das versões anteriores é a [PRINCIPAL] (as pastas de sempre: "dados" e "arquivos");
 *   as novas ficam em "sessoes/<id>".
 * - Individual: no máximo uma sessão, sempre com o nome "Individual" (não muda).
 * - Bolão: o nome é opcional no cadastro; sem ele, o nome genérico "Bolão 1", "Bolão 2"...
 * - O modo de teste é uma sessão à parte ([TESTE], a pasta "teste"), fora da lista: entrar cria um
 *   exemplo novo; sair (ou trocar de sessão) apaga o exemplo e volta à sessão de antes.
 *
 * O registro (a lista e a sessão em uso) fica nas preferências do app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Sessoes(private val context: Context) {

    private val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val repos = ConcurrentHashMap<String, Repositorio>()
    private val trava = Any()
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Serializable
    private data class Registro(val sessoes: List<Sessao> = emptyList(), val ativa: String = PRINCIPAL, val antesDoTeste: String? = null)

    private var registro: Registro = lerRegistro()

    private val _lista = MutableStateFlow(registro.sessoes)
    /** As sessões (sem o modo de teste), na ordem em que foram criadas. */
    val lista: StateFlow<List<Sessao>> = _lista.asStateFlow()

    private val _ativa = MutableStateFlow(registro.ativa)
    /** A sessão em uso (pode ser o [TESTE]). */
    val ativa: StateFlow<String> = _ativa.asStateFlow()

    private val _recomecos = MutableStateFlow(0)
    /**
     * Quantas vezes o app foi para um cadastro novo nesta abertura (sessão criada, sessão apagada).
     * O cadastro que vem depois começa no "Como vai usar o app?" (as boas-vindas são do app recém-instalado).
     */
    val recomecos: StateFlow<Int> = _recomecos.asStateFlow()

    /** Os dados da sessão em uso (null enquanto carregam). */
    val estado: Flow<Estado?> = ativa.flatMapLatest { repo(it).estado }

    init {
        // A sessão das versões anteriores (ou a de agora, sem nome): o nome sai do cadastro.
        escopo.launch { runCatching { completarNomes() } }
    }

    /** O repositório (os dados) da sessão [id]. */
    fun repo(id: String): Repositorio = repos.getOrPut(id) { Repositorio(context, id) }

    /** O repositório da sessão em uso. */
    val repoAtivo: Repositorio get() = repo(ativa.value)

    val emTeste: Boolean get() = ativa.value == TESTE

    fun sessao(id: String): Sessao? = lista.value.firstOrNull { it.id == id }

    /** O nome da sessão [id] ("Modo de teste" para o teste; "Nova sessão" antes do cadastro). */
    fun nome(id: String): String = when (id) {
        TESTE -> NOME_DO_TESTE
        else -> sessao(id)?.nome?.takeIf { it.isNotBlank() } ?: NOME_SEM_CADASTRO
    }

    /** Já existe uma sessão Individual (fora a [exceto])? Só pode haver uma. */
    fun temIndividual(exceto: String? = null): Boolean = lista.value.any { it.individual && it.id != exceto }

    /** O nome genérico para o próximo bolão sem nome: "Bolão N", com o menor N livre. */
    fun nomeGenerico(exceto: String? = null): String = nomeGenerico(lista.value.filter { it.id != exceto }.map { it.nome })

    /** Troca a sessão em uso (o teste, se estiver em uso, é apagado: use [sairDoTeste]). */
    fun trocarPara(id: String) {
        synchronized(trava) {
            require(id == TESTE || registro.sessoes.any { it.id == id }) { "Esta sessão não existe mais." }
            gravar(registro.copy(ativa = id))
        }
    }

    /** Cria uma sessão vazia (vai para o cadastro) e passa a usá-la. Devolve o id. */
    fun criar(): String {
        val id = UUID.randomUUID().toString().replace("-", "").take(10)
        synchronized(trava) {
            gravar(registro.copy(sessoes = registro.sessoes + Sessao(id, criadaEm = System.currentTimeMillis()), ativa = id))
        }
        _recomecos.value++
        return id
    }

    /** O nome e o modo da sessão [id], no fim do cadastro (ou de um backup restaurado). Nome vazio: o genérico. */
    fun nomear(id: String, nome: String, modo: Modo) {
        if (id == TESTE) return
        synchronized(trava) {
            val limpo = nome.trim().take(TAMANHO_DO_NOME)
            val final = when {
                modo == Modo.INDIVIDUAL -> NOME_INDIVIDUAL
                limpo.isBlank() || limpo.equals(NOME_INDIVIDUAL, ignoreCase = true) -> nomeGenerico(registro.sessoes.filter { it.id != id }.map { it.nome })
                else -> limpo
            }
            gravar(registro.copy(sessoes = registro.sessoes.map { if (it.id == id) it.copy(nome = final, modo = modo) else it }))
        }
    }

    /** Muda o nome de um bolão (o Individual não muda de nome). Vazio: o nome genérico. */
    fun renomear(id: String, nome: String) {
        val s = sessao(id) ?: return
        require(!s.individual) { "A sessão Individual não muda de nome." }
        nomear(id, nome, s.modo ?: Modo.BOLAO)
    }

    /**
     * Apaga a sessão [id] (os dados e os anexos dela): fim da extinção, "Apagar tudo", extinção forçada
     * ou um cadastro desistido. Se era a sessão em uso, passa para outra (a criada por último) ou, sem
     * nenhuma, para uma sessão nova (o cadastro). O teste: [sairDoTeste].
     */
    suspend fun excluir(id: String) {
        if (id == TESTE) {
            sairDoTeste()
            return
        }
        // Primeiro a lista (as telas passam para a outra sessão), depois os dados.
        var nova = false
        synchronized(trava) {
            val resto = registro.sessoes.filter { it.id != id }
            val ativa = when {
                registro.ativa != id -> registro.ativa
                resto.isNotEmpty() -> resto.maxBy { it.criadaEm }.id
                else -> null
            }
            if (ativa == null) {
                val outra = UUID.randomUUID().toString().replace("-", "").take(10)
                gravar(Registro(listOf(Sessao(outra, criadaEm = System.currentTimeMillis())), outra))
                nova = true
            } else {
                gravar(registro.copy(sessoes = resto, ativa = ativa, antesDoTeste = registro.antesDoTeste?.takeIf { it != id }))
            }
        }
        if (nova) _recomecos.value++
        repos.remove(id)?.apagarDados() ?: apagarPasta(id)
    }

    // ---------- o modo de teste (uma sessão à parte) ----------

    /** A sessão de onde se entrou no modo de teste (a de agora, se não for o teste). */
    fun sessaoReal(): String = if (emTeste) registro.antesDoTeste?.takeIf { s -> registro.sessoes.any { it.id == s } } ?: registro.sessoes.last().id else ativa.value

    /**
     * Entra no modo de teste com o [exemplo] (um teste anterior é apagado antes). [prepararAnexos]
     * grava os anexos fictícios na pasta recebida e devolve o estado final.
     */
    suspend fun entrarNoTeste(exemplo: Estado, prepararAnexos: (File, Estado) -> Estado) {
        require(exemplo.exemplo) { "Só dados de exemplo podem ir para o modo de teste." }
        val voltarPara = sessaoReal()
        repos.remove(TESTE)?.apagarDados() ?: apagarPasta(TESTE)
        repo(TESTE).substituir { pasta ->
            prepararAnexos(pasta, exemplo).also { require(it.exemplo) { "Só dados de exemplo podem ir para o modo de teste." } }
        }
        synchronized(trava) { gravar(registro.copy(ativa = TESTE, antesDoTeste = voltarPara)) }
    }

    /** Sai do modo de teste: apaga o exemplo e vai para a sessão [para] (sem ela, a de antes do teste). */
    suspend fun sairDoTeste(para: String? = null) {
        val destino = para?.takeIf { p -> registro.sessoes.any { it.id == p } } ?: sessaoReal()
        synchronized(trava) {
            if (registro.ativa == TESTE) gravar(registro.copy(ativa = destino, antesDoTeste = null))
        }
        repos.remove(TESTE)?.apagarDados() ?: apagarPasta(TESTE)
    }

    /** Existe a sessão do teste (os dados de exemplo)? */
    val temTeste: Boolean get() = File(pasta(context, TESTE), "dados/estado.json").isFile || repos.containsKey(TESTE)

    // ---------- para o Vigia e o despertador ----------

    /** As sessões com cadastro concluído (e o teste, se existir), com os dados de agora. */
    suspend fun todasComDados(): List<Pair<String, Estado>> {
        val ids = lista.value.map { it.id } + (if (temTeste) listOf(TESTE) else emptyList())
        return ids.mapNotNull { id -> runCatching { id to repo(id).atual() }.getOrNull() }.filter { it.second.cadastroConcluido }
    }

    /** Há mais de uma sessão (com cadastro): os avisos do celular levam o nome da sessão. */
    val varias: Boolean get() = lista.value.count { it.nome.isNotBlank() } > 1

    // ---------- registro ----------

    private fun lerRegistro(): Registro {
        val texto = prefs.getString(CHAVE_REGISTRO, null)
        val lido = texto?.let { runCatching { json.decodeFromString(Registro.serializer(), it) }.getOrNull() }
        if (lido != null && lido.sessoes.isNotEmpty()) {
            val ativa = lido.ativa.takeIf { a -> a == TESTE || lido.sessoes.any { it.id == a } } ?: lido.sessoes.last().id
            return lido.copy(ativa = ativa)
        }
        // Primeira abertura da 0.21 (ou app novo): a sessão de sempre; o modo de teste ligado continua.
        val teste = prefs.getBoolean(CHAVE_MODO_TESTE_ANTIGO, false)
        val novo = Registro(
            sessoes = listOf(Sessao(PRINCIPAL, criadaEm = System.currentTimeMillis())),
            ativa = if (teste) TESTE else PRINCIPAL,
            antesDoTeste = if (teste) PRINCIPAL else null,
        )
        prefs.edit().putString(CHAVE_REGISTRO, json.encodeToString(Registro.serializer(), novo)).remove(CHAVE_MODO_TESTE_ANTIGO).commit()
        return novo
    }

    /** Grava o registro (dentro da [trava]) e avisa as telas. */
    private fun gravar(novo: Registro) {
        registro = novo
        prefs.edit().putString(CHAVE_REGISTRO, json.encodeToString(Registro.serializer(), novo)).commit()
        _lista.value = novo.sessoes
        _ativa.value = novo.ativa
    }

    /** As sessões sem nome com cadastro concluído (a das versões anteriores) recebem o nome: "Individual" ou "Bolão N". */
    private suspend fun completarNomes() {
        for (s in lista.value.filter { it.nome.isBlank() }) {
            val e = repo(s.id).atual()
            if (e.cadastroConcluido) nomear(s.id, "", e.modo)
        }
    }

    private fun apagarPasta(id: String) {
        if (id == PRINCIPAL) {
            File(context.filesDir, "dados").deleteRecursively()
            File(context.filesDir, "arquivos").deleteRecursively()
        } else {
            pasta(context, id).deleteRecursively()
        }
    }

    companion object {
        /** A sessão das versões anteriores (as pastas de sempre). */
        const val PRINCIPAL = "principal"

        /** O modo de teste (os dados de exemplo), fora da lista das sessões. */
        const val TESTE = "teste"

        const val NOME_INDIVIDUAL = "Individual"
        const val NOME_DO_TESTE = "Modo de teste"
        const val NOME_SEM_CADASTRO = "Nova sessão"
        const val TAMANHO_DO_NOME = 30

        private const val CHAVE_REGISTRO = "sessoes"

        /** Até a 0.20, o modo de teste era uma chave nas preferências. */
        private const val CHAVE_MODO_TESTE_ANTIGO = "modo_teste"

        /** A pasta da sessão [id]. */
        fun pasta(context: Context, id: String): File = when (id) {
            PRINCIPAL -> context.filesDir
            TESTE -> File(context.filesDir, Repositorio.PASTA_TESTE)
            else -> File(File(context.filesDir, "sessoes"), id)
        }

        /** "Bolão N" com o menor N que nenhum dos [usados] tem. */
        fun nomeGenerico(usados: Collection<String>): String =
            generateSequence(1) { it + 1 }.map { "Bolão $it" }.first { n -> usados.none { it.equals(n, ignoreCase = true) } }
    }
}
