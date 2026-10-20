package io.github.marcosfaria94.bolao.domain

/**
 * Modelos do termo (Parte 4), copiados do documento: o título (texto do WhatsApp) e o bloco
 * (conteúdo do comprovante). As linhas entre colchetes indicam o anexo e não fazem parte do texto.
 * No termo, o bloco da APOSTA traz "NÚMEROS: ..." no lugar dos números apostados.
 */
object ModelosDoTermo {
    const val TITULO_APORTE = "APORTE - 01/06/2026"
    const val APORTE = "TIPO DE APORTE: OBRIGATÓRIO\nDATA: 01/06/2026\n\nCONTABILIDADE DO GRUPO\n" +
        "SALDO ATUAL:   R\$ 10,02\nAPORTES....: + R\$ 30,00\nSALDO FINAL: = R\$ 40,02"

    const val TITULO_APOSTA = "APOSTA Nº 1 -  01/06/2026"
    const val APOSTA = "APOSTA: Nº1/JUN\nDATA: 01/06/2026\nMODALIDADE: LOTOFÁCIL\nCONCURSO: 3060\nPRÊMIO PRINCIPAL: 2 MILHÕES\n" +
        "SORTEIO: 03/06/2026\nNÚMEROS: ...\n\nCONTABILIDADE DO GRUPO\n" +
        "SALDO ATUAL:   R\$ 40,02\nAPOSTA.....: - R\$  3,50\nSALDO FINAL: = R\$ 36,52"

    const val TITULO_APURACAO = "APURAÇÃO Nº 1 - 04/06/26"
    const val APURACAO = "APOSTA: Nº1/JUN\nDATA: 01/06/2026\n\nCONTABILIDADE DO GRUPO\n" +
        "SALDO ATUAL:   R\$ 36,52\nPRÊMIO.....: + R\$ 14,00\nSALDO FINAL: = R\$ 50,52"

    const val TITULO_RATEIO = "RATEIO - 14/06/26"
    const val RATEIO = "- RATEIO 140626 -\n\nSALDO ATUAL:   R\$ 39,02\nRATEIO.....: - R\$ 24,00\nSALDO FINAL: = R\$ 15,02"

    const val TITULO_EXTRATO = "EXTRATO - 30/06/26"
    const val EXTRATO = "- EXTRATO JUN/2026 -\n\nRESUMO\nSALDO LÍQ. ANTERIOR.:   R\$10,00\nRENDIMENTO LÍQUIDO..: + R\$ 0,02\nAPORTES.............: + R\$30,00\nPREMIAÇÕES..........: + R\$14,00\nAPOSTAS.............: - R\$30,00\nRATEIO..............: - R\$24,00\nSALDO FINAL.........: = R\$ 0,02\n\nMOVIMENTAÇÕES\n00  01/06/26 SALDO LÍQUIDO.........:   R\$10,02\n01  01/06/26 APORTE ...............: + R\$30,00\n02  01/06/26 APOSTA Nº01 LF .......: - R\$ 3,50\n03  03/06/26 PRÊMIO Nº01 LF .......: + R\$14,00\n04  04/06/26 APOSTA Nº02 QN .......: - R\$ 3,00\n05  07/06/26 APOSTA Nº03 QN .......: - R\$ 3,00\n06  10/06/26 APOSTA Nº04 SS .......: - R\$ 2,50\n07  13/06/26 APOSTA Nº05 LM .......: - R\$ 3,00\n08  14/06/26 RATEIO ...............: - R\$24,00\n09  16/06/26 APOSTA Nº06 LM .......: - R\$ 3,00\n10  19/06/26 APOSTA Nº07 QN .......: - R\$ 3,00\n11  22/06/26 APOSTA Nº08 SS .......: - R\$ 2,50\n12  25/06/26 APOSTA Nº09 LM .......: - R\$ 3,00\n13  28/06/26 APOSTA Nº10 TM .......: - R\$ 3,50"
}
