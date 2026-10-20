package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.FaixaPremio
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
    /** Nome curto usado na tela, ex.: "Marcos". */
    val apelido: String,
    val chavePix: String,
    val administrador: Boolean = false,
    /** CPF só com os dígitos do meio, como no termo: "***.099.906-**". Vazio = não informado. */
    val cpf: String = "",
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
    /** SHA-256 do conteúdo (entra na autenticação do comprovante). */
    val sha256: String? = null,
)

@Serializable
enum class TipoLancamento { SALDO_INICIAL, APORTE, APOSTA, PREMIO, RATEIO, RENDIMENTO, COMPLEMENTO, DIVISAO_FINAL }

/**
 * Toda entrada e saída de dinheiro. O saldo é a soma de todos os lançamentos.
 * Lançamentos nunca são alterados nem apagados (registro só cresce).
 */
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
    /** Aporte adicional (fora do aporte mensal do dia 1º). */
    val eventual: Boolean = false,
    val obs: String? = null,
)

@Serializable
data class Apuracao(
    /** Texto dos acertos, ex.: "12/15". */
    val acertos: String,
    val premio: Long,
    val dezenasSorteadas: List<Int> = emptyList(),
    val segundoSorteio: List<Int> = emptyList(),
    val trevosSorteados: List<Int> = emptyList(),
    val extraSorteado: String? = null,
    val resultadosLoteca: List<String> = emptyList(),
    val premiosFederal: List<String> = emptyList(),
    val faixa: String? = null,
    /** Números das faixas em que a aposta ganhou (para destacar no rateio oficial). */
    val faixasPremiadas: List<Int> = emptyList(),
    /** Rateio oficial do concurso (faixa, ganhadores e prêmio), como a Caixa divulga. */
    val rateioOficial: List<FaixaPremio> = emptyList(),
    /** O prêmio principal acumulou? null = a Caixa não informou. */
    val acumulou: Boolean? = null,
    /**
     * A conferência não basta para registrar sozinha: falta o valor de uma faixa premiada,
     * o Time do Coração/Mês de Sorte, um placar... O Administrador confere e confirma.
     */
    val precisaConfirmar: Boolean = false,
    val conferidaEm: Long,
    val confirmadaEm: Long? = null,
)

@Serializable
data class Aposta(
    val id: String,
    /** Número da aposta no mês (Nº1 = primeira aposta do mês, como no termo). 0 enquanto rascunho. */
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
    /** Loteca: "TIME A x TIME B" de cada jogo, quando a Caixa informa a programação. */
    val jogosLoteca: List<String> = emptyList(),
    val bilheteFederal: String? = null,
    val fracoesFederal: Int? = null,
    val comprovante: Arquivo? = null,
    /** Registrada no bolão (valor descontado do saldo). Depois disso nada muda, só a apuração entra. */
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
enum class TipoComprovante { APORTE, APOSTA, APURACAO, RATEIO, EXTRATO, EXTINCAO }

/**
 * Comprovante único de cada registro: o texto do termo vira uma imagem (estilo comprovante
 * térmico) e os anexos originais vão junto. Nunca é alterado depois de criado.
 */
@Serializable
data class Comprovante(
    val id: String,
    val tipo: TipoComprovante,
    /** O que vai escrito no WhatsApp: só o título, ex.: "APURAÇÃO Nº 1 - 04/06/26". */
    val titulo: String,
    /** Bloco do termo (o conteúdo do comprovante), linha a linha. */
    val texto: String,
    /** Data da operação (aporte, aposta, apuração...). */
    val data: Data,
    val criadoEm: Long,
    /** Imagem gerada pelo app (PNG). */
    val imagem: Arquivo,
    /** Anexos originais (PDF da Caixa, Pix, imagem dos acertos), sem nenhuma mudança. */
    val anexos: List<Arquivo> = emptyList(),
    /** Código de autenticação (SHA-256 encadeado com o comprovante anterior). */
    val autenticacao: String,
    /** Aporte mensal: só é registrado, sem mensagem para o grupo. */
    val envioDispensado: Boolean = false,
    val apostaId: String? = null,
    val rateioNumero: Int? = null,
    /** Extrato: "2026-10". */
    val mes: String? = null,
)

/** Cada vez que um comprovante foi aberto no WhatsApp para enviar ao grupo. */
@Serializable
data class Envio(val comprovanteId: String, val em: Long)

/** Extinção do bolão em andamento (tela que não deixa sair até terminar). */
@Serializable
data class Extincao(
    val iniciadaEm: Long,
    /** Comprovantes da divisão final (Pix de cada integrante), na ordem dos integrantes. */
    val comprovantes: Map<String, Arquivo> = emptyMap(),
    /** Divisão final registrada (lançamentos e comprovante). */
    val registradaEm: Long? = null,
    /** PDF da auditoria completa. */
    val auditoria: Arquivo? = null,
    /** O PDF foi aberto no WhatsApp: ao voltar, o app é limpo. */
    val auditoriaEnviadaEm: Long? = null,
)

@Serializable
data class Estado(
    val versao: Int = 2,
    val cadastroConcluido: Boolean = false,
    /** Dia do cadastro inicial (o aporte de um mês já começado não é cobrado). */
    val cadastradoEm: Data? = null,
    val nomeGrupo: String = "",
    /** ID do grupo no WhatsApp (só aparece no termo). */
    val grupoWhatsapp: String = "",
    val pessoas: List<Pessoa> = emptyList(),
    val config: Config = Config(),
    /** Próximo número de rateio (controle interno). */
    val proximoRateio: Int = 1,
    val lancamentos: List<Lancamento> = emptyList(),
    val apostas: List<Aposta> = emptyList(),
    val rateios: List<Rateio> = emptyList(),
    val extratos: List<Extrato> = emptyList(),
    val comprovantes: List<Comprovante> = emptyList(),
    val envios: List<Envio> = emptyList(),
    val extincao: Extincao? = null,
    /** Termo assinado pelos 3 (anexado pelo Administrador). */
    val termo: Arquivo? = null,
    /** Termo sem assinaturas (anexado pelo Administrador). */
    val termoSemAssinatura: Arquivo? = null,
    /** Dados fictícios do modo de teste (nunca entram nos dados reais). */
    val exemplo: Boolean = false,
    /**
     * Registros feitos até este instante vieram de versões sem comprovante (até a 0.2.6):
     * o verificador de integridade não cobra comprovante deles.
     */
    val legadoAte: Long? = null,
    /** Primeiro mês com aporte mensal ("2026-11"). Vazio = regra do dia do cadastro. */
    val inicioAportes: String? = null,
) {
    val saldo: Long get() = lancamentos.sumOf { it.valor }
    val administrador: Pessoa? get() = pessoas.firstOrNull { it.administrador } ?: pessoas.firstOrNull()
    fun pessoa(id: String?): Pessoa? = pessoas.firstOrNull { it.id == id }
    fun comprovante(id: String?): Comprovante? = comprovantes.firstOrNull { it.id == id }
    fun enviado(c: Comprovante): Boolean = envios.any { it.comprovanteId == c.id }
    val nomeExibicao: String get() = nomeGrupo.ifBlank { "Bolão dos Amigos" }
}
