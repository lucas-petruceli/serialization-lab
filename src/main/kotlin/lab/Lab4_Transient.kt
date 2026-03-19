package lab

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lab 4 — @Transient: JVM vs kotlinx
 *
 * PROBLEMA: No Gson, @Transient da JVM exclui campos da serialização — é assim
 * que muitos projetos escondem tokens, senhas e dados sensíveis do JSON.
 * Ao migrar para kotlinx, esse @Transient simplesmente não tem efeito.
 * Se o desenvolvedor não trocar por @kotlinx.serialization.Transient,
 * o campo sensível aparece no JSON serializado — um vazamento silencioso
 * que não gera erro nenhum e só é descoberto quando já é tarde.
 *
 * Gson: @Transient da JVM exclui o campo.
 * kotlinx sem Transient (encodeDefaults=true): campo aparece no JSON (vazamento).
 * kotlinx com @kotlinx.serialization.Transient: campo excluído corretamente.
 */
object Lab4 : Lab {
    override val number = 4
    override val name = "@Transient: JVM vs kotlinx"

    // --- Gson: usa @Transient da JVM para excluir campo ---
    data class SessionGson(
        val userId: Int,
        val username: String,
        @Transient val token: String = "secret-token-abc123"
    )

    // --- kotlinx: classe SEM nenhum Transient (todos os campos serializados) ---
    @Serializable
    data class SessionKotlinxNoTransient(
        val userId: Int,
        val username: String,
        val token: String = "secret-token-abc123"
    )

    // --- kotlinx: com @kotlinx.serialization.Transient (correto) ---
    @Serializable
    data class SessionKotlinxFixed(
        val userId: Int,
        val username: String,
        @kotlinx.serialization.Transient val token: String = "secret-token-abc123"
    )

    // --- Gson com ExclusionStrategy (outra forma comum de excluir) ---
    // annotation class GsonExclude

    // data class SessionGsonStrategy(
    //     val userId: Int,
    //     val username: String,
    //     @GsonExclude val token: String = "secret-token-abc123"
    // )

    override fun run() {
        printHeader(this)

        println("Objetivo: serializar Session excluindo o campo 'token' (dado sensível)")
        println()

        // Gson com @Transient JVM: exclui o campo
        val gsonTransient = runSafe("Gson") {
            val obj = SessionGson(userId = 1, username = "lucas")
            Gson().toJson(obj)
        }
        printGsonResult("(@Transient JVM) → $gsonTransient")

        // // Gson com ExclusionStrategy: também exclui
        // val gsonStrategy = runSafe("Gson") {
        //     val gson = GsonBuilder().setExclusionStrategies(object : ExclusionStrategy {
        //         override fun shouldSkipField(f: FieldAttributes) = f.getAnnotation(GsonExclude::class.java) != null
        //         override fun shouldSkipClass(clazz: Class<*>?) = false
        //     }).create()
        //     val obj = SessionGsonStrategy(userId = 1, username = "lucas")
        //     gson.toJson(obj)
        // }
        // printGsonResult("(ExclusionStrategy) → $gsonStrategy")

        val fullJson = Json { encodeDefaults = true }

        // kotlinx SEM Transient + encodeDefaults: token APARECE no JSON (vazamento!)
        val kotlinxNoTransient = runSafe("Kotlinx(sem Transient)") {
            val obj = SessionKotlinxNoTransient(userId = 1, username = "lucas")
            fullJson.encodeToString(obj)
        }
        printKotlinxResult("(sem Transient, encodeDefaults=true) → $kotlinxNoTransient")

        // kotlinx COM @kotlinx.serialization.Transient: campo excluído mesmo com encodeDefaults
        val kotlinxFixed = runSafe("Kotlinx(kotlinx @Transient)") {
            val obj = SessionKotlinxFixed(userId = 1, username = "lucas")
            fullJson.encodeToString(obj)
        }
        printKotlinxResult("(kotlinx @Transient, encodeDefaults=true) → $kotlinxFixed")

        printDiff(
            "Na migração, @Transient da JVM e ExclusionStrategy do Gson não têm efeito no kotlinx. " +
            "Se esquecer de adicionar @kotlinx.serialization.Transient, campos sensíveis vazam no JSON."
        )
    }
}
