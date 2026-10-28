package io.github.marcosfaria94.bolao.domain.indices

import kotlin.math.abs
import kotlin.math.max

/**
 * Auditoria das regras dos índices: depois de cada sorteio, refaz o rateio do concurso com as
 * regras do parametros_k.json e compara com o que a Caixa pagou em cada faixa.
 *
 * Regras (as mesmas do cálculo do k1): prêmio bruto = arrecadação × frac_premio × (1 − frac_rgp);
 * os prêmios fixos saem primeiro; o resto (a base) vai para as faixas de rateio, cada uma com a
 * sua fração. A faixa que não teve ganhador no concurso anterior passa o pote para a mesma faixa
 * (herança, quando o arquivo diz) e, na +Milionária, a faixa sem ganhador desce para a de baixo
 * (cascata). O prêmio principal recebe ainda o acumulado do concurso anterior. Cada ganhador
 * recebe pote ÷ ganhadores; acima do limite de isenção, a Caixa informa o valor já sem o IR.
 *
 * Se a Caixa mudar a fração de uma faixa, a divisão entre as faixas ou um prêmio fixo, a conta
 * deixa de bater (a diferença normal é de 1 ou 2 centavos, do arredondamento).
 */
object AuditoriaRegras {

    /** Uma faixa conferida: o que as regras dão e o que a Caixa pagou, por ganhador. */
    data class Faixa(
        val faixa: Int,
        val nome: String,
        val ganhadores: Int,
        val esperado: Double,
        val pago: Double,
    ) {
        val confere: Boolean get() = abs(pago - esperado) <= tolerancia(esperado)
    }

    data class Resultado(val numero: Int, val faixas: List<Faixa>) {
        val confere: Boolean get() = faixas.all { it.confere }
        val divergentes: List<Faixa> get() = faixas.filter { !it.confere }
    }

    /** Arredondamento da Caixa: até 5 centavos ou 0,05% do valor (o que for maior). */
    fun tolerancia(valor: Double): Double = max(0.05, abs(valor) * 5e-4)

    /**
     * Confere o rateio do concurso [u] (o último apurado) com as regras; [a] é o concurso anterior
     * (para a herança e o acumulado do prêmio principal). null = não dá para conferir este concurso:
     * concurso especial (outras regras), rateio ainda incompleto na Caixa ou nenhuma faixa com ganhador.
     */
    fun auditar(p: ParametrosK, chave: String, u: RegistroK, a: RegistroK?): Resultado? {
        val m = p.modalidades[chave] ?: return null
        if (u.indicadorConcursoEspecial == 2 || u.valorArrecadado <= 0) return null
        // A Caixa publica primeiro os ganhadores e só depois os valores: espera ficar completo.
        if (m.faixas.any { it.faixa !in u.ganhadores }) return null
        if (m.faixas.any { (u.ganhadores[it.faixa] ?: 0) > 0 && (u.premios[it.faixa] ?: 0.0) <= 0.0 }) return null
        val ir = p.irAliquota
        val limite = p.limiteIsencao
        val anterior = a?.takeIf { it.numero == u.numero - 1 }

        var fixos = 0.0
        for (f in m.faixas) if (f.fixo) fixos += (u.ganhadores[f.faixa] ?: 0) * f.valor
        val base = u.valorArrecadado * m.fracPremio * (1 - m.fracRgp) - fixos
        val heranca = anterior?.let { IndiceK.herancaFaixas(m, it) }.orEmpty()

        // Pote bruto de cada faixa de rateio, com a herança e a cascata.
        val potes = LinkedHashMap<Int, Double>()
        for (f in m.faixas) {
            if (!f.rateio) continue
            potes[f.faixa] = f.fracaoNo(u.numero) * base + (heranca[f.faixa] ?: 0.0)
        }
        var desce = 0.0
        for (fx in m.ordemCascata) {
            val pote = (potes[fx] ?: continue) + desce
            potes[fx] = pote
            desce = if ((u.ganhadores[fx] ?: 0) == 0) pote else 0.0
        }

        val faixas = buildList {
            for (f in m.faixas) {
                val ganhadores = u.ganhadores[f.faixa] ?: 0
                if (ganhadores <= 0) continue
                val pago = u.premios[f.faixa] ?: continue
                val esperado = if (f.fixo) {
                    IndiceK.liquido(f.valor, limite, ir)
                } else {
                    var pote = potes.getValue(f.faixa)
                    if (f.principal) {
                        // O principal só com o anterior conhecido e fora do concurso que recebe a
                        // reserva dos finais 0 e 5 (aí entram valores que a API não detalha).
                        val ant = anterior ?: continue
                        if (ant.numeroConcursoFinal05 != 0 && ant.numeroConcursoFinal05 == u.numero) continue
                        pote += IndiceK.acumuladoPrincipal(ant, u.numero) / (1 - ir)
                    }
                    IndiceK.liquido(pote / ganhadores, limite, ir)
                }
                add(Faixa(f.faixa, f.nome, ganhadores, esperado, pago))
            }
        }
        return if (faixas.isEmpty()) null else Resultado(u.numero, faixas)
    }
}
