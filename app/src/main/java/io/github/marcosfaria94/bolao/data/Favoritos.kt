package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Conferencia
import io.github.marcosfaria94.bolao.domain.Formato
import io.github.marcosfaria94.bolao.domain.Modalidade
import io.github.marcosfaria94.bolao.domain.Palpite
import io.github.marcosfaria94.bolao.domain.Volante
import java.time.LocalDate

/** O palpite do jogo favorito (para o volante e para a conferência das regras). */
val Favorito.palpite: Palpite get() = Palpite(numeros, trevos, extra)

/**
 * Os jogos favoritos (desde a 0.28): a estrelinha de cada aposta no Painel de Jogos guarda os números dela
 * (não o concurso). Na seção "Jogos favoritos" do Painel dá para apostar um favorito no próximo concurso, um
 * por um ou vários de uma vez ([consolidar]); no volante, o botão "Jogo favorito" preenche os números.
 */
object Favoritos {

    /**
     * As modalidades com jogo favorito: as de números e a Super Sete. A Loteca muda os jogos a cada concurso
     * e a Federal saiu do app.
     */
    fun aceita(m: Modalidade): Boolean = m.ativa && (m.formato == Formato.NUMEROS || m.formato == Formato.COLUNAS)

    /** A aposta pode virar favorita: a modalidade aceita e o volante está completo. */
    fun podeFavoritar(a: Aposta): Boolean = aceita(a.modalidade) && Volante.pendencias(a.modalidade, a.palpite).isEmpty()

    /** O jogo sem o que não importa: os números em ordem (na Super Sete, as colunas como estão), os trevos em ordem. */
    private fun mesmos(m: Modalidade, a: List<Int>, b: List<Int>): Boolean =
        if (m.formato == Formato.COLUNAS) a == b else a.sorted() == b.sorted()

    /** O favorito [f] é o jogo da aposta [a] (mesma modalidade, números, trevos, time ou mês). */
    fun mesmoJogo(f: Favorito, a: Aposta): Boolean =
        f.modalidade == a.modalidade && mesmos(f.modalidade, f.numeros, a.numeros) && f.trevos.sorted() == a.trevos.sorted() &&
            Conferencia.normalizar(f.extra.orEmpty()) == Conferencia.normalizar(a.extra.orEmpty())

    /** O favorito com o jogo da aposta [a] (null = a aposta não é favorita). */
    fun doJogo(e: Estado, a: Aposta): Favorito? = e.favoritos.firstOrNull { mesmoJogo(it, a) }

    /** Guarda o jogo da aposta [a] nos favoritos (se já estiver, nada muda). */
    fun favoritar(e: Estado, a: Aposta, agora: Long): Estado {
        require(podeFavoritar(a)) { "Complete o jogo antes de guardar nos favoritos." }
        if (doJogo(e, a) != null) return e
        val f = Favorito(
            id = novoId(), modalidade = a.modalidade,
            numeros = if (a.modalidade.formato == Formato.COLUNAS) a.numeros else a.numeros.sorted(),
            trevos = a.trevos.sorted(), extra = a.extra?.takeIf { it.isNotBlank() }, criadoEm = agora,
        )
        return e.copy(favoritos = e.favoritos + f)
    }

    /** A estrelinha: guarda o jogo da aposta [a] nos favoritos ou, se já estiver, tira. */
    fun alternar(e: Estado, a: Aposta, agora: Long): Estado =
        doJogo(e, a)?.let { f -> remover(e, f.id) } ?: favoritar(e, a, agora)

    fun remover(e: Estado, id: String): Estado = e.copy(favoritos = e.favoritos.filterNot { it.id == id })

    /** Devolve um favorito tirado (o "Desfazer"), no lugar dele (pela ordem em que foi guardado). */
    fun devolver(e: Estado, f: Favorito): Estado =
        if (e.favoritos.any { it.id == f.id }) e else e.copy(favoritos = (e.favoritos + f).sortedBy { it.criadoEm })

    /** Os favoritos da modalidade [m], na ordem em que foram guardados. */
    fun daModalidade(e: Estado, m: Modalidade): List<Favorito> = e.favoritos.filter { it.modalidade == m }

    /**
     * O favorito cabe no volante com os Parâmetros de agora: um jogo com mais números que a aposta simples só
     * com as apostas com mais números ligadas.
     */
    fun cabe(e: Estado, f: Favorito): Boolean {
        val m = f.modalidade
        if (!aceita(m) || Volante.pendencias(m, f.palpite).isNotEmpty()) return false
        val comMais = m.formato == Formato.NUMEROS && (f.numeros.size > m.qtdAposta || f.trevos.size > m.trevos)
        return !comMais || (e.parametros.maisNumeros && Volante.aceitaMais(m))
    }

    /** Os favoritos da modalidade [m] que cabem no volante agora (o botão "Jogo favorito" do volante). */
    fun paraOVolante(e: Estado, m: Modalidade): List<Favorito> = daModalidade(e, m).filter { cabe(e, it) }

    /** Quantas apostas simples o favorito faz (o preço é o da simples vezes isto). */
    fun apostasSimples(f: Favorito): Long =
        if (f.modalidade.formato == Formato.NUMEROS) Volante.apostasSimples(f.modalidade, f.numeros.size, f.trevos.size) else 1

    /** O preço de apostar o favorito, com o preço da aposta simples de Ajustes. */
    fun preco(e: Estado, f: Favorito): Long = e.config.preco(f.modalidade) * apostasSimples(f)

    /** "6 números", "7 números e 3 trevos", "7 colunas". */
    fun descricao(f: Favorito): String = when {
        f.modalidade.formato == Formato.COLUNAS -> "7 colunas"
        f.trevos.isNotEmpty() -> "${f.numeros.size} números e ${f.trevos.size} trevos"
        else -> "${f.numeros.size} números"
    }

    /**
     * A aposta do favorito [f]: a [base] (uma aposta nova da modalidade, já no próximo concurso) com os números
     * do favorito e o preço que acompanha a quantidade de números.
     */
    fun aposta(e: Estado, f: Favorito, base: Aposta): Aposta = base.copy(
        numeros = f.numeros, trevos = f.trevos, extra = f.extra, valor = preco(e, f), teimosinha = 0, espelho = false,
    )

    /**
     * As apostas já registradas no concurso da aposta [a] com o mesmo jogo (desde a 0.29: apostar um favorito
     * que já foi apostado naquele concurso avisa que a aposta é repetida). Sem o concurso, nenhuma.
     */
    fun repetidas(e: Estado, a: Aposta): List<Aposta> = if (a.concurso <= 0) emptyList() else e.apostas.filter { b ->
        b.id != a.id && b.registrada && b.modalidade == a.modalidade && b.concurso == a.concurso &&
            mesmos(a.modalidade, a.numeros, b.numeros) && a.trevos.sorted() == b.trevos.sorted() &&
            Conferencia.normalizar(a.extra.orEmpty()) == Conferencia.normalizar(b.extra.orEmpty())
    }

    /**
     * Apostar os favoritos (consolidar em aposta, desde a 0.28): registra as [apostas] (feitas por [aposta]),
     * uma por uma, como o "Registrar" do volante; no bolão, cada uma fica esperando o comprovante da Caixa.
     * Devolve o estado e as apostas registradas.
     */
    fun consolidar(e: Estado, apostas: List<Aposta>, hoje: LocalDate, agora: Long): Pair<Estado, List<Aposta>> {
        var est = e
        val feitas = mutableListOf<Aposta>()
        apostas.forEachIndexed { i, a ->
            val (novo, lista) = Operacoes.registrarTudo(Operacoes.salvarRascunho(est, a), a.id, hoje, agora + i * 3)
            est = novo
            feitas += lista
        }
        return est to feitas
    }
}
