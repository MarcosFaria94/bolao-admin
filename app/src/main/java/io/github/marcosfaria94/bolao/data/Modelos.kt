package io.github.marcosfaria94.bolao.data

import io.github.marcosfaria94.bolao.domain.Calendario
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
    /** Os parâmetros da sessão (Ajustes > Parâmetros, desde a 0.22). */
    val parametros: Parametros = Parametros(),
) {
    fun preco(m: Modalidade): Long = precos[m.name] ?: m.precoPadrao
}

/**
 * Parâmetros (Ajustes > Parâmetros, desde a 0.22): escolhas de como a sessão funciona. Todos começam
 * desligados (o app fica como era até a 0.21).
 */
@Serializable
data class Parametros(
    /** Registrar a aposta direto, sem a janela "Deseja registrar aposta?". */
    val semConfirmacao: Boolean = false,
    /** Deixar marcar mais números que a aposta simples (o preço acompanha as combinações). */
    val maisNumeros: Boolean = false,
    /** Mostrar a teimosinha no volante (a mesma aposta em vários concursos seguidos). */
    val teimosinha: Boolean = false,
    /**
     * O calendário de apostas: os dias do mês com aposta. null = os do termo (1, 4, 7... 28);
     * vazio = sem dia definido.
     */
    val diasDeAposta: List<Int>? = null,
) {
    /** Os dias de aposta que valem, em ordem (vazio = sem dia definido). */
    val dias: List<Int> get() = (diasDeAposta ?: Calendario.DIAS_PROGRAMADOS).filter { it in 1..31 }.distinct().sorted()

    val semDiaDefinido: Boolean get() = dias.isEmpty()
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
    /**
     * Desde a 0.10 a aposta é registrada sem o comprovante da Caixa: ela fica esperando o comprovante
     * (aba Comprovantes) e a conferência dele. Enquanto isso, pode ser descartada (o valor volta ao
     * saldo). Na conferência sai o comprovante do bolão e a aposta passa a valer como as outras.
     */
    val pendenteDeAnexo: Boolean = false,
    /** O que a leitura do comprovante da Caixa mudou ou não conseguiu ler (para a conferência). */
    val notasDoAnexo: List<String> = emptyList(),
    /**
     * Lotomania (desde a 0.20): registrar junto a aposta espelho, uma segunda aposta com os 50 números
     * que ficaram de fora desta (como a opção "Aposta espelho" do volante da Caixa).
     */
    val espelho: Boolean = false,
    /** O k1, o k2 e as odds do concurso, guardados quando a aposta foi registrada (desde a 0.21). */
    val indices: IndicesDaAposta? = null,
    /**
     * Teimosinha (desde a 0.22): em quantos concursos seguidos a aposta se repete (0 ou 1 = só neste).
     * No registro vira uma aposta por concurso, todas com este total.
     */
    val teimosinha: Int = 0,
    /** Teimosinha registrada: a posição desta aposta (1 = a do primeiro concurso). 0 = não é teimosinha. */
    val teimosinhaOrdem: Int = 0,
    /** Teimosinha registrada: o id da aposta do primeiro concurso (o grupo). */
    val teimosinhaDe: String? = null,
    /**
     * Como a aposta estava antes de o comprovante da Caixa mudá-la (desde a 0.22): tirar o comprovante,
     * na conferência, devolve a aposta a isto. Some na aprovação.
     */
    val antesDoAnexo: JogoDaAposta? = null,
) {
    /** Faz parte de uma teimosinha já registrada. */
    val ehTeimosinha: Boolean get() = teimosinhaDe != null && teimosinha > 1
}

/**
 * Um jogo favorito (desde a 0.28, a estrelinha das apostas no Painel de Jogos): os números da aposta (com os
 * trevos, o Time do Coração ou o Mês de Sorte), sem o concurso, para apostar de novo em qualquer concurso.
 */
@Serializable
data class Favorito(
    val id: String,
    val modalidade: Modalidade,
    val numeros: List<Int> = emptyList(),
    val trevos: List<Int> = emptyList(),
    /** Time do Coração (Timemania) ou Mês de Sorte (Dia de Sorte). */
    val extra: String? = null,
    val criadoEm: Long,
)

/** O jogo de uma aposta (o que o comprovante da Caixa pode mudar), para desfazer a mudança. */
@Serializable
data class JogoDaAposta(
    val concurso: Int,
    val dataSorteio: Data? = null,
    val premioEstimado: Long? = null,
    val numeros: List<Int> = emptyList(),
    val trevos: List<Int> = emptyList(),
    val extra: String? = null,
    val palpitesLoteca: List<String> = emptyList(),
    val bilheteFederal: String? = null,
    val valor: Long = 0,
)

/**
 * Os índices do concurso da aposta (desde a 0.21), como estavam no app quando ela foi registrada: o
 * Painel de Jogos mostra estes, mesmo quando os Jogos já passaram para o concurso seguinte.
 */
@Serializable
data class IndicesDaAposta(
    val k1: Double? = null,
    val k2: Double? = null,
    /** X das odds (1:X). */
    val odds: Double? = null,
    /** Concurso especial (sem k1 e k2). */
    val especial: Boolean = false,
)

@Serializable
data class Rateio(
    /** Chave interna do rateio (corrida, nunca se repete). O número do comprovante é [numeroNoMes]. */
    val numero: Int,
    val data: Data,
    val saldoLiquido: Long,
    val provisionado: Long,
    val cota: Long,
    val comprovantes: List<Arquivo> = emptyList(),
    val criadoEm: Long,
    /** Nº do rateio no mês (Nº1 = primeiro rateio do mês), como no termo. 0 = registrado antes da 0.7. */
    val numeroNoMes: Int = 0,
    /** Registrado sem os Pix (desde a 0.10): espera os comprovantes de cada integrante e a conferência. */
    val pendenteDeAnexo: Boolean = false,
    /** Comprovantes dos Pix já anexados, por integrante (id da pessoa), enquanto o rateio espera a conferência. */
    val pix: Map<String, Arquivo> = emptyMap(),
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
    /**
     * Extinção forçada começada na 0.6 (com pendências). Desde a 0.7 a forçada é a formatação do
     * app e não cria extinção: uma dessas que ficou pela metade só pode ser concluída apagando tudo.
     */
    val forcada: Boolean = false,
    /** Comprovantes da divisão final (Pix de cada integrante), na ordem dos integrantes. */
    val comprovantes: Map<String, Arquivo> = emptyMap(),
    /** Divisão final registrada (lançamentos e comprovante). */
    val registradaEm: Long? = null,
    /** PDF da auditoria completa. */
    val auditoria: Arquivo? = null,
    /** O PDF foi aberto no WhatsApp: ao voltar, o app é limpo. */
    val auditoriaEnviadaEm: Long? = null,
)

/** Nome do app (cabeçalho dos comprovantes, PDFs e lembretes). */
const val NOME_DO_APP = "Bolão dos Amigos"

/**
 * Como o app é usado. [BOLAO]: o grupo (2 ou mais integrantes), com comprovantes, termo, aportes,
 * rateios e extrato. [INDIVIDUAL] (desde a 0.10): só uma pessoa, bem enxuto: registra as apostas e
 * os prêmios, o saldo começa em zero e cai a cada aposta (mostra o quanto se gastou), sem
 * comprovante, sem termo, sem autenticação; tudo pode ser excluído e o app pode ser apagado a
 * qualquer momento.
 */
@Serializable
enum class Modo { BOLAO, INDIVIDUAL }

/** Integrantes de um bolão: no mínimo 2. */
const val MINIMO_DE_INTEGRANTES = 2
const val MAXIMO_DE_INTEGRANTES = 20

@Serializable
data class Estado(
    val versao: Int = 3,
    val cadastroConcluido: Boolean = false,
    /** Dia do cadastro inicial (o aporte de um mês já começado não é cobrado). */
    val cadastradoEm: Data? = null,
    /** Nome do grupo (até a 0.6, no cadastro). Não é mais usado: o bolão não precisa de nome. */
    val nomeGrupo: String = "",
    /** ID do grupo no WhatsApp (só aparece no termo). */
    val grupoWhatsapp: String = "",
    val pessoas: List<Pessoa> = emptyList(),
    val config: Config = Config(),
    /** Próximo número interno de rateio (chave corrida; o comprovante usa o número no mês). */
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
    /** Bolão (o grupo) ou Individual. */
    val modo: Modo = Modo.BOLAO,
    /**
     * Bolão informal (desde a 0.22, escolhido no cadastro): sem o termo e sem nada para enviar ao grupo
     * (os comprovantes do bolão ficam só no app). O comprovante da Caixa de cada aposta continua.
     */
    val informal: Boolean = false,
    /** Os jogos favoritos (desde a 0.28): os números guardados para apostar de novo. */
    val favoritos: List<Favorito> = emptyList(),
) {
    val individual: Boolean get() = modo == Modo.INDIVIDUAL
    /** O bolão formal: com o termo e os comprovantes enviados ao grupo do WhatsApp. */
    val enviaAoGrupo: Boolean get() = !individual && !informal
    val parametros: Parametros get() = config.parametros
    val saldo: Long get() = lancamentos.sumOf { it.valor }
    val administrador: Pessoa? get() = pessoas.firstOrNull { it.administrador } ?: pessoas.firstOrNull()
    fun pessoa(id: String?): Pessoa? = pessoas.firstOrNull { it.id == id }
    fun comprovante(id: String?): Comprovante? = comprovantes.firstOrNull { it.id == id }
    fun enviado(c: Comprovante): Boolean = envios.any { it.comprovanteId == c.id }
    /** Nome que vai no cabeçalho dos comprovantes e dos PDFs: o nome do app. */
    val nomeExibicao: String get() = NOME_DO_APP
}
