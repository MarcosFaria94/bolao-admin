package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Modalidade
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

/** Datas gravadas como "2026-10-17". */
object DataSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

typealias Data = @Serializable(with = DataSerializer::class) LocalDate

@Serializable
data class Pessoa(
    val id: String,
    val nome: String,
    /** Nome curto usado nas mensagens, ex.: "Marcos". */
    val apelido: String,
    val chavePix: String,
    val administrador: Boolean = false,
)

@Serializable
data class Config(
    /** Aporte mensal de cada integrante, em centavos. */
    val aporteMensal: Long = 1000,
    /** Preço da aposta simples por modalidade (nome do enum -> centavos). Ausente = preço padrão. */
    val precos: Map<String, Long> = emptyMap(),
) {
    fun preco(m: Modalidade): Long = precos[m.name] ?: m.precoPadrao
}

/** Arquivo guardado na pasta interna do app (comprovantes, termo, imagens). */
@Serializable
data class Arquivo(
    /** Nome do arquivo dentro da pasta "arquivos". */
    val nome: String,
    /** Nome original, como veio do celular. */
    val nomeOriginal: String,
    val mime: String,
    val guardadoEm: Long,
)

@Serializable
enum class TipoLancamento { SALDO_INICIAL, APORTE, APOSTA, PREMIO, RATEIO, RENDIMENTO }

/** Toda entrada e saída de dinheiro. O saldo é a soma de todos os lançamentos. */
@Serializable
data class Lancamento(
    val id: String,
    val data: Data,
    val criadoEm: Long,
    val tipo: TipoLancamento,
    /** Centavos, positivo para entrada e negativo para saída. */
    val valor: Long,
    val pessoaId: String? = null,
    val apostaId: String? = null,
    val rateioNumero: Int? = null,
    val eventual: Boolean = false,
    val obs: String? = null,
)

@Serializable
data class Apuracao(
    /** Texto da "Contagem de acertos", ex.: "12/15". */
    val acertos: String,
    val premio: Long,
    val dezenasSorteadas: List<Int> = emptyList(),
    val segundoSorteio: List<Int> = emptyList(),
    val trevosSorteados: List<Int> = emptyList(),
    val extraSorteado: String? = null,
    val resultadosLoteca: List<String> = emptyList(),
    val premiosFederal: List<String> = emptyList(),
    val faixa: String? = null,
    val conferidaEm: Long,
    val confirmadaEm: Long? = null,
)

@Serializable
data class Aposta(
    val id: String,
    /** Número do jogo no bolão (#132). */
    val numero: Int,
    val modalidade: Modalidade,
    val concurso: Int,
    val dataAposta: Data,
    val dataSorteio: Data? = null,
    val premioEstimado: Long? = null,
    /** Valor pago, em centavos. */
    val valor: Long,
    val eventual: Boolean = false,
    val numeros: List<Int> = emptyList(),
    val trevos: List<Int> = emptyList(),
    /** Time do Coração (Timemania) ou Mês de Sorte (Dia de Sorte). */
    val extra: String? = null,
    /** Loteca: "1", "X" ou "2" para cada um dos 14 jogos. */
    val palpitesLoteca: List<String> = emptyList(),
    val bilheteFederal: String? = null,
    val fracoesFederal: Int? = null,
    val comprovante: Arquivo? = null,
    /** Mensagem enviada e valor descontado do saldo. */
    val registrada: Boolean = false,
    val criadaEm: Long,
    val apuracao: Apuracao? = null,
)

@Serializable
data class Rateio(
    val numero: Int,
    val data: Data,
    val saldoLiquido: Long,
    val provisionado: Long,
    val cota: Long,
    val comprovantes: List<Arquivo> = emptyList(),
    val criadoEm: Long,
)

@Serializable
data class Extrato(
    /** "2026-10". */
    val mes: String,
    val saldoInformado: Long,
    val rendimento: Long,
    val criadoEm: Long,
)

@Serializable
data class Estado(
    val versao: Int = 1,
    val cadastroConcluido: Boolean = false,
    val nomeGrupo: String = "",
    val pessoas: List<Pessoa> = emptyList(),
    val config: Config = Config(),
    /** Próximo número de jogo (#) e de rateio (Nº). */
    val proximoJogo: Int = 1,
    val proximoRateio: Int = 1,
    val lancamentos: List<Lancamento> = emptyList(),
    val apostas: List<Aposta> = emptyList(),
    val rateios: List<Rateio> = emptyList(),
    val extratos: List<Extrato> = emptyList(),
    /** PDF do termo assinado. */
    val termo: Arquivo? = null,
) {
    val saldo: Long get() = lancamentos.sumOf { it.valor }
    val administrador: Pessoa? get() = pessoas.firstOrNull { it.administrador } ?: pessoas.firstOrNull()
    fun pessoa(id: String?): Pessoa? = pessoas.firstOrNull { it.id == id }
}
