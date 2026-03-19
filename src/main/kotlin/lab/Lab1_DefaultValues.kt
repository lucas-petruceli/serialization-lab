package lab

import com.google.gson.Gson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lab 1 — Campos com valor default
 *
 * PROBLEMA: Quando o JSON traz null explícito para campos que têm default no Kotlin,
 * o Gson ignora os defaults e injeta null/zero da JVM — criando bugs silenciosos.
 * Um campo String non-null com default "abc" recebe null sem nenhum erro.
 * O desenvolvedor acha que o default protege, mas o Gson bypassa o construtor Kotlin.
 *
 * Gson: mantém defaults de Int/Boolean (construtor Kotlin é chamado), mas injeta null em String non-null.
 * kotlinx strict: rejeita null em non-null (exceção).
 * kotlinx com coerceInputValues=true: substitui null pelo default do Kotlin.
 */
object Lab1 : Lab {
    override val number = 1
    override val name = "Campos com valor default"

    // Gson version (sem anotações kotlinx)
    data class ConfigGson(
        val retryCount: Int = 3,
        val label: String = "default-label",
        val enabled: Boolean = true
    )

    @Serializable
    data class ConfigKotlinx(
        val retryCount: Int = 3,
        val label: String = "default-label",
        val enabled: Boolean = true
    )

    // Nullable SEM default — campo obrigatório no JSON
    @Serializable
    data class ProfileSemDefault(
        val name: String,
        val nickname: String?       // nullable mas SEM = null
    )

    // Nullable COM default — campo opcional no JSON
    @Serializable
    data class ProfileComDefault(
        val name: String,
        val nickname: String? = null // nullable COM = null
    )

    override fun run() {
        printHeader(this)

        val json = """{"retryCount": null, "label": null, "enabled": null}"""
        printInput(json)

        // Gson: primitivos (Int, Boolean) mantêm defaults pois null não os sobrescreve
        // mas String non-null recebe null silenciosamente → bug latente
        val gsonResult = runSafe("Gson") {
            val config = Gson().fromJson(json, ConfigGson::class.java)
            "ConfigGson(retryCount=${config.retryCount}, label=${config.label}, enabled=${config.enabled})"
        }
        printGsonResult(gsonResult)

        // kotlinx SEM coerceInputValues: null em non-null field → exceção
        val kotlinxStrict = runSafe("Kotlinx(strict)") {
            val config = Json.decodeFromString<ConfigKotlinx>(json)
            config.toString()
        }
        printKotlinxResult("(strict)  $kotlinxStrict")

        // kotlinx COM coerceInputValues: null em non-null field → usa default do Kotlin
        val kotlinxCoerce = runSafe("Kotlinx(coerce)") {
            val lenientJson = Json { coerceInputValues = true }
            val config = lenientJson.decodeFromString<ConfigKotlinx>(json)
            config.toString()
        }
        printKotlinxResult("(coerce)  $kotlinxCoerce")

        printDiff(
            "Gson mantém defaults de primitivos mas injeta null em String non-null (bug silencioso). " +
            "kotlinx strict rejeita null em non-null. " +
            "kotlinx com coerceInputValues=true usa os defaults do construtor Kotlin."
        )

        // ── Nullable: com e sem default ──
        println()
        println("── Nullable: String? vs String? = null ──")
        val jsonSemNickname = """{"name": "Lucas"}"""
        printInput(jsonSemNickname)
        println("Campo 'nickname' é String? — nullable. Mas tem = null?")
        println()

        // String? SEM = null → kotlinx exige a chave no JSON
        val semDefault = runSafe("Kotlinx(String? sem default)") {
            val p = Json.decodeFromString<ProfileSemDefault>(jsonSemNickname)
            p.toString()
        }
        printKotlinxResult("(String? sem default)  $semDefault")

        // String? COM = null → kotlinx aceita ausência da chave
        val comDefault = runSafe("Kotlinx(String? = null)") {
            val p = Json.decodeFromString<ProfileComDefault>(jsonSemNickname)
            p.toString()
        }
        printKotlinxResult("(String? = null)       $comDefault")

        println()
        println("┌──────────────────────────────────────────────────────────────────────┐")
        println("│  ATENÇÃO: val x: String?  vs  val x: String? = null                  │")
        println("├──────────────────────────────────────────────────────────────────────┤")
        println("│                                                                      │")
        println("│  val nickname: String?                                                │")
        println("│    → Campo é nullable, MAS obrigatório no JSON.                       │")
        println("│    → Se a chave não vier, kotlinx lança MissingFieldException.        │")
        println("│                                                                      │")
        println("│  val nickname: String? = null                                         │")
        println("│    → Campo é nullable E opcional no JSON.                              │")
        println("│    → Se a chave não vier, kotlinx usa null como default.              │")
        println("│                                                                      │")
        println("│  REGRA: Ao migrar, se a API pode omitir o campo, use sempre           │")
        println("│  String? = null (com o = null explícito). Sem o default, o kotlinx    │")
        println("│  trata o campo como obrigatório mesmo sendo nullable.                 │")
        println("└──────────────────────────────────────────────────────────────────────┘")
    }
}
