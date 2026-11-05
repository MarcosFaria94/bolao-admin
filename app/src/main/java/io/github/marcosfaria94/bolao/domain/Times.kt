package io.github.marcosfaria94.bolao.domain

/**
 * Um Time do Coração da Timemania. [caixa]: como a Caixa escreve ("VASCO DA GAMA/RJ"; é o que
 * vai na aposta e nos comprovantes); [nome]: como aparece na tela ("Vasco da Gama"); [wiki]: o
 * artigo da Wikipédia com o brasão ("pt:Título" ou "en:Título"), buscado pelo app na primeira vez.
 */
data class TimeDoCoracao(val caixa: String, val nome: String, val wiki: String) {
    val uf: String get() = partesDoTime(caixa).second
}

/**
 * Times do Coração da Timemania (biênio de maio/2026 a maio/2028, Portaria do Ministério do
 * Esporte). A lista muda a cada dois anos: na tela de escolha dá para digitar outro time, se faltar algum.
 */
val TIMES: List<TimeDoCoracao> = listOf(
    TimeDoCoracao("ABC/RN", "ABC", "pt:ABC Futebol Clube"),
    TimeDoCoracao("ÁGUIA DE MARABÁ/PA", "Águia de Marabá", "pt:Águia de Marabá Futebol Clube"),
    TimeDoCoracao("ALTOS/PI", "Altos", "pt:Associação Atlética de Altos"),
    TimeDoCoracao("AMAZONAS/AM", "Amazonas", "pt:Amazonas Futebol Clube"),
    TimeDoCoracao("AMÉRICA/MG", "América", "pt:América Futebol Clube (Belo Horizonte)"),
    TimeDoCoracao("AMÉRICA/RN", "América", "pt:América Futebol Clube (Rio Grande do Norte)"),
    TimeDoCoracao("ANÁPOLIS/GO", "Anápolis", "pt:Anápolis Futebol Clube"),
    TimeDoCoracao("APARECIDENSE/GO", "Aparecidense", "pt:Associação Atlética Aparecidense"),
    TimeDoCoracao("ASA/AL", "ASA", "pt:Agremiação Sportiva Arapiraquense"),
    TimeDoCoracao("ATHLETIC CLUB/MG", "Athletic", "pt:Athletic Club (Minas Gerais)"),
    TimeDoCoracao("ATHLETICO/PR", "Athletico", "pt:Club Athletico Paranaense"),
    TimeDoCoracao("ATLÉTICO/GO", "Atlético", "pt:Atlético Clube Goianiense"),
    TimeDoCoracao("ATLÉTICO/MG", "Atlético", "pt:Clube Atlético Mineiro"),
    TimeDoCoracao("AVAÍ/SC", "Avaí", "pt:Avaí Futebol Clube"),
    TimeDoCoracao("BAHIA/BA", "Bahia", "pt:Esporte Clube Bahia"),
    TimeDoCoracao("BARRA/SC", "Barra", "pt:Barra Futebol Clube (Santa Catarina)"),
    TimeDoCoracao("BOTAFOGO/PB", "Botafogo", "pt:Botafogo Futebol Clube (João Pessoa)"),
    TimeDoCoracao("BOTAFOGO/RJ", "Botafogo", "pt:Botafogo de Futebol e Regatas"),
    TimeDoCoracao("BOTAFOGO/SP", "Botafogo", "pt:Botafogo Futebol Clube (Ribeirão Preto)"),
    TimeDoCoracao("BRAGANTINO/SP", "Bragantino", "pt:Red Bull Bragantino"),
    TimeDoCoracao("BRASIL/RS", "Brasil", "pt:Grêmio Esportivo Brasil"),
    TimeDoCoracao("BRASILIENSE/DF", "Brasiliense", "pt:Brasiliense Futebol Clube"),
    TimeDoCoracao("BRUSQUE/SC", "Brusque", "pt:Brusque Futebol Clube"),
    TimeDoCoracao("CASCAVEL/PR", "Cascavel", "pt:Futebol Clube Cascavel"),
    TimeDoCoracao("CAXIAS/RS", "Caxias", "pt:Sociedade Esportiva e Recreativa Caxias do Sul"),
    TimeDoCoracao("CEARÁ/CE", "Ceará", "pt:Ceará Sporting Club"),
    TimeDoCoracao("CHAPECOENSE/SC", "Chapecoense", "pt:Associação Chapecoense de Futebol"),
    TimeDoCoracao("CONFIANÇA/SE", "Confiança", "pt:Associação Desportiva Confiança"),
    TimeDoCoracao("CORINTHIANS/SP", "Corinthians", "pt:Sport Club Corinthians Paulista"),
    TimeDoCoracao("CORITIBA/PR", "Coritiba", "pt:Coritiba Foot Ball Club"),
    TimeDoCoracao("CRB/AL", "CRB", "pt:Clube de Regatas Brasil"),
    TimeDoCoracao("CRICIÚMA/SC", "Criciúma", "pt:Criciúma Esporte Clube"),
    TimeDoCoracao("CRUZEIRO/MG", "Cruzeiro", "pt:Cruzeiro Esporte Clube"),
    TimeDoCoracao("CSA/AL", "CSA", "pt:Centro Sportivo Alagoano"),
    TimeDoCoracao("CUIABÁ/MT", "Cuiabá", "pt:Cuiabá Esporte Clube"),
    TimeDoCoracao("FERROVIÁRIA/SP", "Ferroviária", "pt:Associação Ferroviária de Esportes"),
    TimeDoCoracao("FERROVIÁRIO/CE", "Ferroviário", "pt:Ferroviário Atlético Clube (Ceará)"),
    TimeDoCoracao("FIGUEIRENSE/SC", "Figueirense", "pt:Figueirense Futebol Clube"),
    TimeDoCoracao("FLAMENGO/RJ", "Flamengo", "pt:Clube de Regatas do Flamengo"),
    TimeDoCoracao("FLORESTA/CE", "Floresta", "pt:Floresta Esporte Clube"),
    TimeDoCoracao("FLUMINENSE/RJ", "Fluminense", "pt:Fluminense Football Club"),
    TimeDoCoracao("FORTALEZA/CE", "Fortaleza", "pt:Fortaleza Esporte Clube"),
    TimeDoCoracao("GOIÁS/GO", "Goiás", "pt:Goiás Esporte Clube"),
    TimeDoCoracao("GRÊMIO/RS", "Grêmio", "pt:Grêmio Foot-Ball Porto Alegrense"),
    TimeDoCoracao("GUARANI/SP", "Guarani", "pt:Guarani Futebol Clube"),
    TimeDoCoracao("INTER LIMEIRA/SP", "Inter de Limeira", "pt:Associação Atlética Internacional (Limeira)"),
    TimeDoCoracao("INTERNACIONAL/RS", "Internacional", "pt:Sport Club Internacional"),
    TimeDoCoracao("ITABAIANA/SE", "Itabaiana", "pt:Associação Olímpica de Itabaiana"),
    TimeDoCoracao("ITUANO/SP", "Ituano", "pt:Ituano Futebol Clube"),
    TimeDoCoracao("JUVENTUDE/RS", "Juventude", "pt:Esporte Clube Juventude"),
    TimeDoCoracao("LONDRINA/PR", "Londrina", "pt:Londrina Esporte Clube"),
    TimeDoCoracao("MANAUS/AM", "Manaus", "pt:Manaus Futebol Clube"),
    TimeDoCoracao("MARANHÃO/MA", "Maranhão", "pt:Maranhão Atlético Clube"),
    TimeDoCoracao("MARINGÁ/PR", "Maringá", "en:Maringá Futebol Clube"),
    TimeDoCoracao("MIRASSOL/SP", "Mirassol", "pt:Mirassol Futebol Clube"),
    TimeDoCoracao("NÁUTICO/PE", "Náutico", "pt:Clube Náutico Capibaribe"),
    TimeDoCoracao("NOVA IGUAÇU/RJ", "Nova Iguaçu", "pt:Nova Iguaçu Futebol Clube"),
    TimeDoCoracao("NOVORIZONTINO/SP", "Novorizontino", "pt:Grêmio Novorizontino"),
    TimeDoCoracao("OPERÁRIO/PR", "Operário", "pt:Operário Ferroviário Esporte Clube"),
    TimeDoCoracao("PALMEIRAS/SP", "Palmeiras", "pt:Sociedade Esportiva Palmeiras"),
    TimeDoCoracao("PAYSANDU/PA", "Paysandu", "pt:Paysandu Sport Club"),
    TimeDoCoracao("PONTE PRETA/SP", "Ponte Preta", "pt:Associação Atlética Ponte Preta"),
    TimeDoCoracao("PORTO VELHO/RO", "Porto Velho", "pt:Porto Velho Esporte Clube"),
    TimeDoCoracao("REMO/PA", "Remo", "pt:Clube do Remo"),
    TimeDoCoracao("RETRÔ BRASIL/PE", "Retrô", "pt:Retrô Futebol Clube Brasil"),
    TimeDoCoracao("SAMPAIO CORRÊA/MA", "Sampaio Corrêa", "pt:Sampaio Corrêa Futebol Clube"),
    TimeDoCoracao("SANTA CRUZ/PE", "Santa Cruz", "pt:Santa Cruz Futebol Clube"),
    TimeDoCoracao("SANTOS/SP", "Santos", "pt:Santos Futebol Clube"),
    TimeDoCoracao("SÃO BERNARDO/SP", "São Bernardo", "pt:São Bernardo Futebol Clube"),
    TimeDoCoracao("SÃO JOSÉ/RS", "São José", "pt:Esporte Clube São José"),
    TimeDoCoracao("SÃO PAULO/SP", "São Paulo", "pt:São Paulo Futebol Clube"),
    TimeDoCoracao("SOUSA/PB", "Sousa", "en:Sousa Esporte Clube"),
    TimeDoCoracao("SPORT/PE", "Sport", "pt:Sport Club do Recife"),
    TimeDoCoracao("TOCANTINÓPOLIS/TO", "Tocantinópolis", "pt:Tocantinópolis Esporte Clube"),
    TimeDoCoracao("TOMBENSE/MG", "Tombense", "pt:Tombense Futebol Clube"),
    TimeDoCoracao("VASCO DA GAMA/RJ", "Vasco da Gama", "pt:Club de Regatas Vasco da Gama"),
    TimeDoCoracao("VILA NOVA/GO", "Vila Nova", "pt:Vila Nova Futebol Clube"),
    TimeDoCoracao("VITÓRIA/BA", "Vitória", "pt:Esporte Clube Vitória"),
    TimeDoCoracao("VOLTA REDONDA/RJ", "Volta Redonda", "pt:Volta Redonda Futebol Clube"),
    TimeDoCoracao("YPIRANGA/RS", "Ypiranga", "pt:Ypiranga Futebol Clube (Erechim)"),
)

/** Os times como a Caixa escreve ("VASCO DA GAMA/RJ"). */
val TIMES_DO_CORACAO: List<String> = TIMES.map { it.caixa }

/** O time da lista com este nome da Caixa (sem diferença de acento e maiúsculas); null = digitado à mão. */
fun timeDoCoracao(caixa: String?): TimeDoCoracao? {
    if (caixa.isNullOrBlank()) return null
    val n = Conferencia.normalizar(caixa)
    return TIMES.firstOrNull { Conferencia.normalizar(it.caixa) == n }
}

/** "VASCO DA GAMA/RJ" -> ("VASCO DA GAMA", "RJ"). */
fun partesDoTime(time: String): Pair<String, String> {
    val i = time.lastIndexOf('/')
    return if (i < 0) time.trim() to "" else time.substring(0, i).trim() to time.substring(i + 1).trim()
}
