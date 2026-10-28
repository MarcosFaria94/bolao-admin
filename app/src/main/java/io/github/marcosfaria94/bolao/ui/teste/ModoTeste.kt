package io.github.marcosfaria94.bolao.ui.teste

import android.content.Context
import io.github.marcosfaria94.bolao.bolao
import io.github.marcosfaria94.bolao.data.Aposta
import io.github.marcosfaria94.bolao.data.Arquivo
import io.github.marcosfaria94.bolao.data.DadosDeExemplo
import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.data.Operacoes
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Volante
import io.github.marcosfaria94.bolao.domain.Autenticacao
import io.github.marcosfaria94.bolao.data.Pessoa
import io.github.marcosfaria94.bolao.domain.Concurso
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.net.CaixaApi
import io.github.marcosfaria94.bolao.notificacoes.Vigia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.random.Random

/**
 * Liga e desliga o modo de teste. Os dados de exemplo ficam num espaço separado
 * (ver Repositorio): nada do que se faz no teste chega aos dados reais.
 */
object ModoTeste {

    private val _trabalhando = MutableStateFlow(false)
    /** true enquanto o exemplo está sendo preparado (ou apagado). */
    val trabalhando: StateFlow<Boolean> = _trabalhando.asStateFlow()

    private const val ESPERA_CAIXA_MS = 8_000L

    /** Consulta a Caixa (sem esperar muito), cria um mês de exemplo e liga o modo de teste. */
    fun entrar(context: Context) {
        val app = context.bolao
        if (_trabalhando.value) return
        _trabalhando.value = true
        app.escopo.launch {
            try {
                val hoje = LocalDate.now()
                val agora = System.currentTimeMillis()
                val reais = concursosRecentes(hoje)
                val real = app.repo.estadoReal()
                val exemplo = withContext(Dispatchers.Default) {
                    DadosDeExemplo.gerar(
                        hoje = hoje,
                        agora = agora,
                        fuso = ZoneId.systemDefault(),
                        reais = reais,
                        pessoas = real.pessoas.takeIf { real.cadastroConcluido },
                        semente = System.nanoTime() xor agora,
                        modo = if (real.cadastroConcluido) real.modo else io.github.marcosfaria94.bolao.data.Modo.BOLAO,
                    )
                }
                app.repo.entrarNoTeste(exemplo) { pasta, e ->
                    ImagensDeExemplo.desenharTodos(app, pasta, e)
                    // Com os anexos desenhados, as impressões digitais entram e a autenticação é refeita.
                    DadosDeExemplo.comImpressoesDigitais(e) { a ->
                        File(pasta, a.nome).takeIf { it.isFile }?.let { Autenticacao.sha256(it.readBytes()) }
                    }
                }
                Vigia.esquecerAvisosDeTeste(app)
                // Confere o último jogo na Caixa já agora: o aviso de "jogo sorteado" chega em instantes.
                Vigia.rodarAgora(app)
                app.repo.avisar(
                    if (reais.isEmpty()) "Exemplo pronto. A Caixa não respondeu agora: o último jogo vai pedir o resultado à mão."
                    else "Exemplo pronto: um mês fictício, separado dos dados reais.",
                )
            } catch (t: Throwable) {
                app.repo.avisar("Não foi possível criar o exemplo: ${t.message}")
            } finally {
                _trabalhando.value = false
            }
        }
    }

    /** Desliga o modo de teste e apaga os dados de exemplo. */
    fun sair(context: Context) {
        val app = context.bolao
        if (_trabalhando.value) return
        _trabalhando.value = true
        app.escopo.launch {
            try {
                app.repo.sairDoTeste()
                Vigia.esquecerAvisosDeTeste(app)
                app.repo.avisar("Modo de teste desligado. Os dados de exemplo foram apagados.")
            } catch (t: Throwable) {
                app.repo.avisar("Não foi possível sair do modo de teste: ${t.message}")
            } finally {
                _trabalhando.value = false
            }
        }
    }

    /**
     * Último concurso de cada modalidade do exemplo. Quem não responder em poucos segundos
     * fica de fora (o exemplo usa números fictícios para ela).
     */
    private suspend fun concursosRecentes(hoje: LocalDate): Map<Modalidade, Concurso> {
        val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val pedidos = DadosDeExemplo.MODALIDADES.associateWith { m ->
            escopo.async {
                runCatching {
                    val ultimo = CaixaApi.ultimo(m, usarCache = false)
                    // O sorteio de hoje ainda aparece como "sorteio hoje": para apurar, serve o anterior.
                    if (ultimo.data == hoje && ultimo.numero > 1) {
                        runCatching { CaixaApi.concurso(m, ultimo.numero - 1) }.getOrNull() ?: ultimo
                    } else {
                        ultimo
                    }
                }.getOrNull()
            }
        }
        val achados = mutableMapOf<Modalidade, Concurso>()
        withTimeoutOrNull(ESPERA_CAIXA_MS) {
            for ((m, pedido) in pedidos) pedido.await()?.let { achados[m] = it }
        }
        escopo.cancel()
        return achados
    }

    /** Comprovante fictício para uma aposta feita no modo de teste. */
    suspend fun comprovante(context: Context, a: Aposta): Arquivo = withContext(Dispatchers.IO) {
        val nome = "${DadosDeExemplo.PREFIXO}comprovante-${UUID.randomUUID().toString().take(8)}.png"
        val arquivo = File(context.bolao.repo.pastaArquivos, nome)
        ImagensDeExemplo.comprovanteAposta(arquivo, a)
        Arquivo(
            nome = nome,
            nomeOriginal = "comprovante-exemplo-${a.modalidade.sigla}.png",
            mime = "image/png",
            guardadoEm = System.currentTimeMillis(),
            sha256 = Autenticacao.sha256(arquivo.readBytes()),
        )
    }

    /**
     * Comprovantes fictícios de tudo o que espera comprovante (as apostas registradas e os Pix dos
     * rateios), com o texto de cada um, para a aba Comprovantes. Para variar, às vezes um deles vem
     * com outros números (a aposta é alterada pelo comprovante) e às vezes um vem repetido.
     */
    suspend fun comprovantesDeExemplo(context: Context, e: Estado): List<Pair<Arquivo, String>> = withContext(Dispatchers.IO) {
        val r = Random(System.nanoTime())
        val pasta = context.bolao.repo.pastaArquivos
        fun novo(prefixo: String, nomeOriginal: String, desenhar: (File) -> String): Pair<Arquivo, String> {
            val nome = "${DadosDeExemplo.PREFIXO}$prefixo-${UUID.randomUUID().toString().take(8)}.png"
            val arquivo = File(pasta, nome)
            val texto = desenhar(arquivo)
            return Arquivo(nome, nomeOriginal, "image/png", System.currentTimeMillis(), Autenticacao.sha256(arquivo.readBytes())) to texto
        }
        val apostas = Operacoes.apostasSemComprovante(e)
        val alterar = if (apostas.isNotEmpty() && r.nextInt(100) < 35) apostas.random(r).id else null
        val lista = mutableListOf<Pair<Arquivo, String>>()
        apostas.forEach { a ->
            val jogo = if (a.id == alterar && a.modalidade.formato == Formato.NUMEROS) {
                a.copy(numeros = Volante.sortear(a.modalidade, r).numeros)
            } else {
                a
            }
            lista += novo("comprovante", "comprovante-caixa-${a.modalidade.sigla}-${a.concurso}.png") { f -> ImagensDeExemplo.comprovanteAposta(f, jogo) }
        }
        Operacoes.rateiosSemPix(e).forEach { rt ->
            e.pessoas.filter { !rt.pix.containsKey(it.id) }.forEach { p ->
                lista += novo("pix", "pix-${p.apelido}.png") { f -> ImagensDeExemplo.pix(f, p.apelido, p.chavePix, rt.cota, rt.data) }
            }
        }
        // De vez em quando, o mesmo arquivo escolhido duas vezes (o app descarta a repetição).
        if (lista.isNotEmpty() && r.nextInt(100) < 25) {
            val (arq, texto) = lista.random(r)
            val copia = "${DadosDeExemplo.PREFIXO}copia-${UUID.randomUUID().toString().take(8)}.png"
            File(pasta, arq.nome).copyTo(File(pasta, copia))
            lista += arq.copy(nome = copia) to texto
        }
        lista.shuffled(r)
    }

    /** Comprovante fictício de um Pix (rateio ou divisão final), no modo de teste. */
    suspend fun pix(context: Context, p: Pessoa, valor: Long): Arquivo = withContext(Dispatchers.IO) {
        val nome = "${DadosDeExemplo.PREFIXO}pix-${UUID.randomUUID().toString().take(8)}.png"
        val arquivo = File(context.bolao.repo.pastaArquivos, nome)
        ImagensDeExemplo.pix(arquivo, p.apelido, p.chavePix, valor, LocalDate.now())
        Arquivo(
            nome = nome,
            nomeOriginal = "pix-exemplo-${p.apelido}.png",
            mime = "image/png",
            guardadoEm = System.currentTimeMillis(),
            sha256 = Autenticacao.sha256(arquivo.readBytes()),
        )
    }
}
