package io.github.marcosfaria94.bolao.domain

/**
 * Times do Coração da Timemania, como a Caixa escreve (biênio de maio/2026 a maio/2028,
 * Portaria do Ministério do Esporte). A lista muda a cada dois anos: na tela de escolha
 * dá para digitar outro time, se faltar algum.
 */
val TIMES_DO_CORACAO: List<String> = listOf(
    "ABC/RN", "ÁGUIA DE MARABÁ/PA", "ALTOS/PI", "AMAZONAS/AM", "AMÉRICA/MG", "AMÉRICA/RN", "ANÁPOLIS/GO",
    "APARECIDENSE/GO", "ASA/AL", "ATHLETIC CLUB/MG", "ATHLETICO/PR", "ATLÉTICO/GO", "ATLÉTICO/MG", "AVAÍ/SC",
    "BAHIA/BA", "BARRA/SC", "BOTAFOGO/PB", "BOTAFOGO/RJ", "BOTAFOGO/SP", "BRAGANTINO/SP", "BRASIL/RS",
    "BRASILIENSE/DF", "BRUSQUE/SC", "CASCAVEL/PR", "CAXIAS/RS", "CEARÁ/CE", "CHAPECOENSE/SC", "CONFIANÇA/SE",
    "CORINTHIANS/SP", "CORITIBA/PR", "CRB/AL", "CRICIÚMA/SC", "CRUZEIRO/MG", "CSA/AL", "CUIABÁ/MT",
    "FERROVIÁRIA/SP", "FERROVIÁRIO/CE", "FIGUEIRENSE/SC", "FLAMENGO/RJ", "FLORESTA/CE", "FLUMINENSE/RJ",
    "FORTALEZA/CE", "GOIÁS/GO", "GRÊMIO/RS", "GUARANI/SP", "INTER LIMEIRA/SP", "INTERNACIONAL/RS", "ITABAIANA/SE",
    "ITUANO/SP", "JUVENTUDE/RS", "LONDRINA/PR", "MANAUS/AM", "MARANHÃO/MA", "MARINGÁ/PR", "MIRASSOL/SP",
    "NÁUTICO/PE", "NOVA IGUAÇU/RJ", "NOVORIZONTINO/SP", "OPERÁRIO/PR", "PALMEIRAS/SP", "PAYSANDU/PA",
    "PONTE PRETA/SP", "PORTO VELHO/RO", "REMO/PA", "RETRÔ BRASIL/PE", "SAMPAIO CORRÊA/MA", "SANTA CRUZ/PE",
    "SANTOS/SP", "SÃO BERNARDO/SP", "SÃO JOSÉ/RS", "SÃO PAULO/SP", "SOUSA/PB", "SPORT/PE", "TOCANTINÓPOLIS/TO",
    "TOMBENSE/MG", "VASCO DA GAMA/RJ", "VILA NOVA/GO", "VITÓRIA/BA", "VOLTA REDONDA/RJ", "YPIRANGA/RS",
)

/** "VASCO DA GAMA/RJ" -> ("VASCO DA GAMA", "RJ"). */
fun partesDoTime(time: String): Pair<String, String> {
    val i = time.lastIndexOf('/')
    return if (i < 0) time.trim() to "" else time.substring(0, i).trim() to time.substring(i + 1).trim()
}
