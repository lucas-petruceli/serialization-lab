package lab

import com.google.gson.Gson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lab 2 — Campos ausentes no JSON (non-null)
 *
 * PROBLEMA: Quando uma chave simplesmente não existe no JSON, mas a data class
 * espera esse campo como non-null (ex: val email: String), o Gson cria o objeto
 * com null nesse campo sem lançar nenhum erro. O NPE só aparece depois, quando
 * alguém tenta usar email.length — longe do ponto onde o bug realmente aconteceu.
 * Isso torna o debug muito difícil em produção.
 *
 * Gson: cria o objeto com null no campo non-null — sem erro nenhum.
 * kotlinx: lança MissingFieldException imediatamente (fail-fast).
 */
object Lab2 : Lab {
    override val number = 2
    override val name = "Campos ausentes no JSON (non-null)"

    data class UserGson(
        val id: Int,
        val name: String,
        val email: String
    )

    @Serializable
    data class UserKotlinx(
        val id: Int,
        val name: String,
        val email: String
    )

    override fun run() {
        printHeader(this)

        val json = """{"id": 42, "name": "Lucas"}"""
        printInput(json)

        // Gson: cria objeto com email=null mesmo sendo String non-null no Kotlin
        val gsonResult = runSafe("Gson") {
            val user = Gson().fromJson(json, UserGson::class.java)
            "UserGson(id=${user.id}, name=${user.name}, email=${user.email})"
        }
        printGsonResult(gsonResult)

        // Gson: o perigo real — NPE ao acessar o campo que "não deveria" ser null
        val gsonBoom = runSafe("Gson") {
            val user = Gson().fromJson(json, UserGson::class.java)
            "email.length = ${user.email.length}"
        }
        printGsonResult("(email.length) $gsonBoom")

        // kotlinx: falha na hora da deserialização
        val kotlinxResult = runSafe("Kotlinx") {
            val user = Json.decodeFromString<UserKotlinx>(json)
            user.toString()
        }
        printKotlinxResult(kotlinxResult)

        printDiff(
            "Gson injeta null em campo non-null sem erro → NPE surpresa em runtime. " +
            "kotlinx lança MissingFieldException na deserialização (fail-fast)."
        )
    }
}
