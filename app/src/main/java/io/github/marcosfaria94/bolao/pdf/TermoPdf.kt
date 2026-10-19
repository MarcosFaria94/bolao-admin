package io.github.marcosfaria94.bolao.pdf

import io.github.marcosfaria94.bolao.data.Estado
import io.github.marcosfaria94.bolao.domain.Dinheiro
import java.io.OutputStream

/**
 * O termo do bolão em PDF, sem assinaturas ("cru"), para baixar e assinar digitalmente.
 *
 * O texto é o modelo do termo; os dados pessoais (nomes, CPFs, chaves Pix, grupo) vêm do
 * cadastro deste celular. Assim o termo com os dados das pessoas não fica no repositório
 * do app: só o modelo.
 */
object TermoPdf {

    const val TITULO = "Termo de Constituição de Bolão"

    /** "Fulano de Tal – ***.099.906-**" (sem CPF, só o nome). */
    private fun pessoaComCpf(nome: String, cpf: String, administrador: Boolean): String =
        nome + (if (cpf.isNotBlank()) " – $cpf" else "") + (if (administrador) " (Administrador)" else "")

    fun gerar(e: Estado, saida: OutputStream) {
        val doc = Documento(rodape = TITULO)
        doc.pdf.titulo = TITULO
        doc.pdf.assunto = e.nomeExibicao
        val aporte = Dinheiro.rs(e.config.aporteMensal)
        val chaveAdmin = e.administrador?.chavePix.orEmpty()

        doc.titulo(TITULO, 21f)
        doc.espaco(4f)
        doc.paragrafo(
            "Os participantes abaixo, doravante chamados **Integrantes**, reunidos em um grupo de WhatsApp, constituem, " +
                "por prazo indeterminado, um bolão de apostas nas Loterias Caixa, com participação igual de **1/3 (um terço)** " +
                "em qualquer obrigação e prêmio.",
        )

        doc.secao("Parte 1 – Identificação e Deveres")
        doc.subsecao("Pessoas")
        doc.lista(e.pessoas.map { pessoaComCpf(it.nome, it.cpf, it.administrador) }, marcacao = false)
        doc.subsecao("Chave PIX")
        doc.lista(e.pessoas.map { "${it.nome} – ${it.chavePix}" }, marcacao = false)
        doc.subsecao("Grupo do WhatsApp")
        doc.lista(listOfNotNull("Nome: ${e.nomeExibicao}", e.grupoWhatsapp.takeIf { it.isNotBlank() }?.let { "Group ID: $it" }), marcacao = false)
        doc.paragrafo("É dever do Administrador:")
        doc.lista(
            listOf(
                "Receber, guardar e prestar contas do dinheiro do grupo.",
                "Fazer as apostas, publicar os comprovantes e as apurações dos concursos.",
                "Receber o prêmio antes do prazo de prescrição e efetuar a transferência dos rateios.",
            ),
        )
        doc.paragrafo("É dever dos Integrantes:")
        doc.lista(listOf("Fazer o PIX de $aporte todo mês na chave $chaveAdmin"), marcacao = false)

        doc.secao("Parte 2 – Financiamento das Apostas")
        doc.paragrafo(
            "Todos os integrantes do bolão devem contribuir com **$aporte por mês** para participar dos jogos. " +
                "Este valor poderá ser alterado posteriormente, de modo a compensar o reajuste de preço das apostas, " +
                "desde que haja a expressa concordância de todos os 3 (três) integrantes, devidamente registrado no grupo " +
                "de comunicação oficial do bolão.",
        )
        doc.paragrafo(
            "O PIX do aporte mensal deverá ser feito no dia 1º de todo mês, através da conta bancária de mesma " +
                "titularidade do Integrante. Sugere-se, a critério de cada Integrante, fazer PIX agendado automático para não esquecer.",
        )
        doc.paragrafo(
            "Além do aporte mensal obrigatório, poderão ser realizados **aportes adicionais**, desde que haja a " +
                "**expressa concordância de todos os 3 (três) integrantes**, devidamente registrada no grupo de comunicação oficial do bolão.",
        )
        doc.paragrafo(
            "O dinheiro do bolão fica separado em uma caixinha Nubank **exclusiva**, em nome do Administrador, e não será " +
                "misturado com o seu dinheiro pessoal, nem será usado para outra finalidade. O rendimento da caixinha pertence " +
                "ao bolão, nas mesmas proporções de 1/3.",
        )

        doc.secao("Parte 3 – Apostas e Premiações")
        doc.paragrafo(
            "O bolão fará pelo menos **1 (uma) aposta nos dias 1, 4, 7, 10, 13, 16, 19, 22, 25 e 28** de todos os meses do ano, " +
                "totalizando no mínimo 10 apostas mensais ou 120 apostas anuais. As apostas realizadas fora dos dias programados " +
                "ou para concursos especiais serão denominadas como **eventuais**, e sua realização ficará condicionada à " +
                "existência de **saldo extraordinário**.",
        )
        doc.paragrafo(
            "Não havendo interesse de rateio, os pequenos prêmios e o rendimento da caixinha Nubank ficarão no caixa do grupo " +
                "para novas apostas, nas modalidades e dias escolhidos pelo Administrador.",
        )
        doc.paragrafo(
            "Havendo interesse, o rateio ocorrerá, desde que o caixa do grupo tenha saldo líquido maior que o valor " +
                "provisionado para os jogos do mês corrente, ou seja:",
        )
        doc.paragrafo("**Rateio = Saldo Líquido - Provisionado**", tamanho = 11f, centralizado = true, depois = 2f)
        doc.paragrafo("**Cota = Rateio ÷ 3**", tamanho = 11f, centralizado = true)
        doc.paragrafo("Onde,")
        doc.lista(
            listOf(
                "Rateio = Valor total a ser dividido para os Integrantes.",
                "Saldo Líquido = Total de dinheiro da caixinha Nubank (aportes + rendimento líquido + prêmios).",
                "Provisionado = Valor ainda necessário para as apostas restantes do mês, nunca superior aos aportes do mês.",
                "Cota = Valor devido a cada Integrante.",
            ),
        )
        doc.paragrafo(
            "Obs.: Se o rateio resultar em divisão inexata, arredondar-se-á a segunda casa decimal **para baixo**, " +
                "permanecendo como saldo do grupo as sobras daí decorrentes.",
        )
        doc.paragrafo(
            "Os números apostados em cada concurso serão, salvo pedido de algum Integrante, escolhidos randomicamente através " +
                "de um gerador de números. A modalidade lotérica será escolhida pelo Administrador, mas observará 2 critérios: " +
                "o preço da aposta em relação ao saldo do grupo e o cálculo matemático do melhor retorno esperado.",
        )
        doc.paragrafo(
            "**Todo prêmio de aposta do bolão pertence aos três Integrantes, na proporção de 1/3 (um terço) para cada um, " +
                "independentemente de quem fez a aposta, a registrou ou resgatou o prêmio na Caixa.** **Qualquer aposta cujo " +
                "comprovante não tenha sido publicado neste grupo de WhatsApp não faz parte do bolão e, portanto, não dá direito a prêmio.**",
        )

        doc.secao("Parte 4 – Prestação de Contas")
        doc.paragrafo("O Administrador organizará os jogos utilizando a seguinte nomenclatura:")
        doc.lista(
            listOf(
                "APOSTA Nº1 = Primeira aposta do mês corrente",
                "010626 = Data da aposta",
                "LF-3060 = Lotofácil, Concurso 3060",
                "2M = Prêmio de 2 milhões",
                "030626 = Data do sorteio",
            ),
        )
        doc.paragrafo("Siglas:")
        doc.lista(
            listOf(
                "DP = Dupla Sena", "DS = Dia de Sorte", "FE = Loteria Federal", "LF = Lotofácil", "LM = Lotomania",
                "LO = Loteca", "MI = +Milionária", "MS = Mega-Sena", "QN = Quina", "SS = Super Sete", "TM = Timemania",
            ),
        )
        MODELOS.forEach { (titulo, mensagem, bloco) ->
            doc.subsecao(titulo)
            doc.paragrafo(mensagem, depois = 3f)
            doc.codigo(bloco)
        }

        doc.secao("Parte 5 – Da extinção do grupo")
        doc.paragrafo(
            "Basta que um dos Integrantes manifeste no grupo a decisão de sair para que este bolão seja encerrado. " +
                "O encerramento obedecerá aos seguintes passos:",
        )
        doc.lista(
            listOf(
                "Nenhuma aposta mais será realizada.",
                "Todos os jogos não sorteados aguardarão apuração.",
                "O saldo total do grupo será repartido em 3 partes e transferido aos Integrantes.",
            ),
            numerada = true,
        )
        doc.paragrafo(
            "Obs.: Caso a última divisão seja inexata, se arredondará a segunda casa decimal **para cima** e a diferença será " +
                "coberta pelo Administrador, como sinal de boa-fé.",
        )
        doc.paragrafo("Feito isso, toda obrigação deste bolão se extinguirá, sem nada devido entre as partes.")

        // Título, frase e as linhas de assinatura ficam juntos na mesma página.
        doc.reservar(e.pessoas.size * 78f + 110f)
        doc.secao("Parte 6 – Assinaturas")
        doc.paragrafo("Termos acordados e assinados eletronicamente por todas as partes:")
        doc.assinaturas(e.pessoas.map { it.nome })
        doc.gravar(saida)
    }

    /** Modelos de mensagem da Parte 4: título da seção, texto do WhatsApp e o bloco (com os anexos indicados). */
    val MODELOS: List<Triple<String, String, String>> = listOf(
        Triple(
            "1. Registro de aporte",
            "APORTE - 01/06/2026",
            "- APORTE 010626 -\n\nSaldo Inicial:   R$ 10,02\nAportes......: + R$ 30,00\nSaldo Final..: = R$ 40,02",
        ),
        Triple(
            "2. Registro do Jogo (dias programados ou eventuais)",
            "APOSTA Nº 1 -  01/06/2026",
            "- APOSTA Nº1 010626 LF-3060 2M 030626 -\n\nSaldo Inicial:   R$ 40,02\nAposta.......: - R$  3,50\n" +
                "Saldo Final..: = R$ 36,52\n\n[PDF DO COMPROVANTE ELETRÔNICO DA CAIXA]",
        ),
        Triple(
            "3. Apuração (dia seguinte ao sorteio)",
            "APURAÇÃO Nº 1 - 04/06/26",
            "- APURAÇÃO Nº1 010626 LF-3060 2M 030626 -\n\nAcertos: 12/15 (Aposta Premiada!)\nSaldo Inicial:   R$ 36,52\n" +
                "Prêmio.......: + R$ 14,00\nSaldo Final..: = R$ 50,52\n\n" +
                "[IMAGEM COM OS NÚMEROS SORTEADOS E OS NÚMEROS APOSTADOS COM MARCAÇÃO DE ACERTOS]",
        ),
        Triple(
            "4. Rateio (após prêmio grande ou quando houver interesse)",
            "RATEIO - 14/06/26",
            "- RATEIO 140626 -\n\nSaldo Líquido:   R$ 39,02\nRateio.......: - R$ 24,00\nSaldo Final    = R$ 15,02\n" +
                "[COMPROVANTE PIX DAS 3 TRANSFERÊNCIAS]",
        ),
        Triple(
            "5. Prestação de Contas (último dia do mês)",
            "EXTRATO - 30/06/26",
            "- EXTRATO JUN/2026 -\n\nRESUMO\nSaldo Líq. Anterior.:   R$10,00\nRendimento Líquido..: + R$ 0,02\n" +
                "Aportes.............: + R$30,00\nPremiações..........: + R$14,00\nApostas.............: - R$30,00\n" +
                "Rateio..............: - R$24,00\nSaldo Final.........: = R$ 0,02\n\nMOVIMENTAÇÕES\n" +
                "00  01/06/26 SALDO LÍQUIDO.........:   R$10,02\n01  01/06/26 APORTE ...............: + R$30,00\n" +
                "02  01/06/26 APOSTA Nº01 LF .......: - R$ 3,50\n03  03/06/26 PRÊMIO Nº01 LF .......: + R$14,00\n" +
                "04  04/06/26 APOSTA Nº02 QN .......: - R$ 3,00\n05  07/06/26 APOSTA Nº03 QN .......: - R$ 3,00\n" +
                "06  10/06/26 APOSTA Nº04 SS .......: - R$ 2,50\n07  13/06/26 APOSTA Nº05 LM .......: - R$ 3,00\n" +
                "08  14/06/26 RATEIO ...............: - R$24,00\n09  16/06/26 APOSTA Nº06 LM .......: - R$ 3,00\n" +
                "10  19/06/26 APOSTA Nº07 QN .......: - R$ 3,00\n11  22/06/26 APOSTA Nº08 SS .......: - R$ 2,50\n" +
                "12  25/06/26 APOSTA Nº09 LM .......: - R$ 3,00\n13  28/06/26 APOSTA Nº10 TM .......: - R$ 3,50",
        ),
    )
}
