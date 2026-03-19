package lab

import com.google.gson.Gson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lab 5 — ignoreUnknownKeys
 *
 * PROBLEMA: APIs evoluem — campos novos aparecem no JSON o tempo todo.
 * O Gson ignora campos extras silenciosamente, então o projeto nunca sentiu isso.
 * Ao migrar para kotlinx, o comportamento default é rejeitar qualquer chave
 * desconhecida com exceção. Se a API adicionar um campo novo amanhã, o app
 * quebra sem que nenhum código tenha mudado. É preciso configurar
 * ignoreUnknownKeys = true, mas surge a dúvida: onde configurar isso quando
 * não posso alterar o Json global?
 *
 * Gson: ignora campos extras silenciosamente.
 * kotlinx default: lança exceção para chaves desconhecidas.
 * kotlinx com ignoreUnknownKeys = true: ignora extras, funciona.
 */
object Lab5 : Lab {
    override val number = 5
    override val name = "ignoreUnknownKeys"

    data class ProfileGson(
        val name: String,
        val age: Int
    )

    @Serializable
    data class ProfileKotlinx(
        val name: String,
        val age: Int
    )

    override fun run() {
        printHeader(this)

        val json = """{"name": "Lucas", "age": 28, "country": "BR", "premium": true}"""
        printInput(json)
        println("Classe espera apenas: name, age")
        println("JSON tem extras: country, premium")
        println()

        // Gson: ignora campos extras silenciosamente
        val gsonResult = runSafe("Gson") {
            val p = Gson().fromJson(json, ProfileGson::class.java)
            "Profile(name=${p.name}, age=${p.age}) ← country e premium ignorados sem aviso"
        }
        printGsonResult(gsonResult)

        // kotlinx strict (default): rejeita campos desconhecidos
        val kotlinxStrict = runSafe("Kotlinx(strict)") {
            val p = Json.decodeFromString<ProfileKotlinx>(json)
            p.toString()
        }
        printKotlinxResult("(default) $kotlinxStrict")

        // kotlinx com ignoreUnknownKeys = true
        val kotlinxIgnore = runSafe("Kotlinx(ignoreUnknownKeys)") {
            val lenient = Json { ignoreUnknownKeys = true }
            val p = lenient.decodeFromString<ProfileKotlinx>(json)
            "Profile(name=${p.name}, age=${p.age}) ← extras ignorados"
        }
        printKotlinxResult("(ignoreUnknownKeys=true) $kotlinxIgnore")

        println()
        printDiff(
            "Gson ignora campos extras sem aviso (pode mascarar erro de mapeamento). " +
            "kotlinx por padrão rejeita — mais seguro. Use ignoreUnknownKeys=true para migrar."
        )

        // ── Discussão: como lidar sem alterar o Json global ──
        println("┌──────────────────────────────────────────────────────────────────────┐")
        println("│  E se eu não posso alterar o Json global do projeto?                 │")
        println("├──────────────────────────────────────────────────────────────────────┤")
        println("│                                                                      │")
        println("│  Opção 1 — Json local por caso de uso:                               │")
        println("│    val apiJson = Json { ignoreUnknownKeys = true }                   │")
        println("│    val strictJson = Json { /* default, sem ignore */ }               │")
        println("│    Cada módulo/feature usa a instância adequada.                     │")
        println("│                                                                      │")
        println("│  Opção 2 — Json { } herda de outro:                                  │")
        println("│    val base = Json { ignoreUnknownKeys = true }                      │")
        println("│    val custom = Json(base) { encodeDefaults = true }                 │")
        println("│    Compõe configurações sem duplicar.                                │")
        println("│                                                                      │")
        println("│  Opção 3 — Wrapper/extension function:                               │")
        println("│    fun <T> decodeFromApi(json: String): T                            │")
        println("│    Centraliza a instância Json usada para APIs externas.             │")
        println("│                                                                      │")
        println("│  Recomendação:                                                       │")
        println("│  Na migração, comece com ignoreUnknownKeys = true no global.         │")
        println("│  Depois, quando estável, avalie remover para ter validação strict.   │")
        println("│  Campos extras ignorados podem esconder bugs de mapeamento.          │")
        println("└──────────────────────────────────────────────────────────────────────┘")
    }
}
