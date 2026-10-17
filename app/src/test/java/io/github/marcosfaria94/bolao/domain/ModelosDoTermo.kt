package io.github.marcosfaria94.bolao.domain

/**
 * Modelos de mensagem do termo (Parte 4), copiados do documento.
 * As linhas entre colchetes indicam o anexo e não fazem parte do texto.
 * Os nomes foram trocados por outros do mesmo tamanho (o alinhamento é o mesmo).
 */
object ModelosDoTermo {
    const val APORTE = "- APORTE 010626 -\nSaldo Líquido:     R\$ 10,02\nAportes......:   + R\$ 30,00\nSaldo Final    =   R\$ 40,02"
    const val APOSTA = "- APOSTA #132 010626 LF-3060 2M 030626 -\nSaldo Líquido:   R\$ 40,02\nAposta.......: - R\$  3,50\nSaldo Final    = R\$ 36,52"
    const val APURACAO = "- APURAÇÃO #132 010626 LF-3060 2M 030626 -\nContagem de acertos: 12/15\nSaldo Líquido:   R\$ 36,52\nPrêmio.......: + R\$ 14,00\nSaldo Final    = R\$ 50,52"
    const val RATEIO = "- RATEIO Nº1 140626 -\n\nSaldo Líquido:     R\$ 39,02\nRateio.......:   - R\$ 24,00\nSaldo Final    =   R\$ 15,02"
    const val EXTRATO = "- EXTRATO JUN/2026 -\n\nRESUMO\nSaldo Líq. Anterior.:   R\$10,00\nRendimento Líquido..: + R\$ 0,02\nAportes.............: + R\$30,00\nPremiações..........: + R\$14,00\nApostas.............: - R\$30,00\nRateio..............: - R\$24,00\nSaldo Final           = R\$ 0,02\n\nMOVIMENTAÇÕES\n00  01/06/26 SALDO + RENDIMENTO...:   R\$10,02\n01           APORTE (Fulano)......: + R\$10,00\n02           APORTE (Beto)........: + R\$10,00\n03           APORTE (Beltrano)....: + R\$10,00\n04           APOSTA #132 LF ......: - R\$ 3,50\n05  03/06/26 PRÊMIO #132 LF ......: + R\$14,00\n06  04/06/26 APOSTA #133 QN ......: - R\$ 3,00\n07  07/06/26 APOSTA #134 QN ......: - R\$ 3,00\n08  10/06/26 APOSTA #135 SS ......: - R\$ 2,50\n09  13/06/26 APOSTA #136 LM ......: - R\$ 3,00\n10  14/06/26 RATEIO Nº1 (Fulano)..: - R\$ 8,00\n11           RATEIO Nº1 (Beto)....: - R\$ 8,00\n12           RATEIO Nº1 (Beltrano): - R\$ 8,00\n13  16/06/26 APOSTA #137 LM ......: - R\$ 3,00\n14  19/06/26 APOSTA #138 QN ......: - R\$ 3,00\n15  22/06/26 APOSTA #139 SS ......: - R\$ 2,50\n16  25/06/26 APOSTA #140 LM ......: - R\$ 3,00\n17  28/06/26 APOSTA #141 TM ......: - R\$ 3,50"
}
