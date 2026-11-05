package io.github.marcosfaria94.bolao.net

import org.junit.Assert.assertEquals
import org.junit.Test

class BrasoesTest {
    /** A resposta da Wikipédia: o título pedido segue a normalização e o redirecionamento até a página com a miniatura. */
    @Test
    fun leAsMiniaturasDaWikipedia() {
        val json = """
        {"batchcomplete":true,"query":{
          "normalized":[{"fromencoded":false,"from":"clube do Remo","to":"Clube do Remo"}],
          "redirects":[{"from":"América Futebol Clube (Minas Gerais)","to":"América Futebol Clube (Belo Horizonte)"}],
          "pages":[
            {"pageid":1,"ns":0,"title":"Clube do Remo","thumbnail":{"source":"https://thumb.wikimedia.org/remo.png","width":120,"height":120}},
            {"pageid":2,"ns":0,"title":"América Futebol Clube (Belo Horizonte)","thumbnail":{"source":"https://thumb.wikimedia.org/america.png","width":120,"height":120}},
            {"pageid":3,"ns":0,"title":"Sousa Esporte Clube"},
            {"ns":0,"title":"Time Que Não Existe","missing":true}
          ]}}
        """.trimIndent()
        val r = Brasoes.miniaturas(
            json,
            listOf("clube do Remo", "América Futebol Clube (Minas Gerais)", "Sousa Esporte Clube", "Time Que Não Existe"),
        )
        assertEquals(
            mapOf(
                "clube do Remo" to "https://thumb.wikimedia.org/remo.png",
                "América Futebol Clube (Minas Gerais)" to "https://thumb.wikimedia.org/america.png",
            ),
            r,
        )
        assertEquals(emptyMap<String, String>(), Brasoes.miniaturas("{}", listOf("x")))
    }
}
