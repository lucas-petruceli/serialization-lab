package lab

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lab 8 — Migração gradual (coexistência)
 *
 * PROBLEMA: Em projetos grandes, migrar todas as data classes de Gson para kotlinx
 * de uma vez é inviável — o risco de quebra é alto e o PR seria imenso.
 * A migração precisa ser incremental: algumas classes já migradas convivem com
 * outras que ainda usam Gson no mesmo build, às vezes no mesmo módulo.
 * Isso gera dúvidas práticas: como organizar o código para que as duas libs
 * coexistam sem conflito? Como evitar que alguém use o serializer errado?
 * E como garantir que o JSON produzido por uma lib é consumível pela outra?
 *
 * Este lab simula essa coexistência e mostra um padrão de wrapper para
 * encapsular a decisão de qual serializer usar.
 */
object Lab8 : Lab {
    override val number = 8
    override val name = "Migração gradual (coexistência)"

    // ========================
    // Classe ainda no Gson (não migrada)
    // ========================
    data class AddressGson(
        @SerializedName("street_name") val streetName: String,
        @SerializedName("zip_code") val zipCode: String,
        val city: String
    )

    // ========================
    // Classe já migrada para kotlinx
    // ========================
    @Serializable
    data class UserKotlinx(
        val id: Int,
        val name: String,
        @SerialName("email_address") val emailAddress: String
    )

    // ========================
    // Wrapper que encapsula a coexistência
    // ========================
    class JsonSerializer {
        @PublishedApi internal val gson = Gson()
        @PublishedApi internal val kotlinxJson = Json { ignoreUnknownKeys = true }

        // Deserialização — Gson (classes ainda não migradas)
        inline fun <reified T> fromGson(json: String, clazz: Class<T>): T {
            return gson.fromJson(json, clazz)
        }

        // Deserialização — kotlinx (classes já migradas)
        inline fun <reified T : Any> fromKotlinx(json: String): T {
            @Suppress("UNCHECKED_CAST")
            return when (T::class) {
                UserKotlinx::class -> kotlinxJson.decodeFromString<UserKotlinx>(json) as T
                else -> throw IllegalArgumentException("Tipo ${T::class.simpleName} não registrado no kotlinx")
            }
        }

        // Serialização — Gson
        fun toGson(obj: Any): String {
            return gson.toJson(obj)
        }

        // Serialização — kotlinx
        fun toKotlinx(obj: UserKotlinx): String {
            return kotlinxJson.encodeToString(obj)
        }
    }

    override fun run() {
        printHeader(this)

        val serializer = JsonSerializer()

        // ── Cenário 1: Classe Gson (AddressGson) — ainda não migrada ──
        println("── Cenário 1: Classe ainda no Gson (AddressGson) ──")
        val addressJson = """{"street_name": "Rua das Flores", "zip_code": "01234-567", "city": "São Paulo"}"""
        printInput(addressJson)

        val address = runSafe("Gson") {
            val a = serializer.fromGson(addressJson, AddressGson::class.java)
            "Address(streetName=${a.streetName}, zipCode=${a.zipCode}, city=${a.city})"
        }
        printGsonResult(address)

        val addressEncode = runSafe("Gson") {
            val a = serializer.fromGson(addressJson, AddressGson::class.java)
            serializer.toGson(a)
        }
        printGsonResult("encode → $addressEncode")

        // ── Cenário 2: Classe kotlinx (UserKotlinx) — já migrada ──
        println()
        println("── Cenário 2: Classe já migrada (UserKotlinx) ──")
        val userJson = """{"id": 1, "name": "Lucas", "email_address": "lucas@email.com"}"""
        printInput(userJson)

        val user = runSafe("Kotlinx") {
            val u = serializer.fromKotlinx<UserKotlinx>(userJson)
            "User(id=${u.id}, name=${u.name}, emailAddress=${u.emailAddress})"
        }
        printKotlinxResult(user)

        val userEncode = runSafe("Kotlinx") {
            val u = serializer.fromKotlinx<UserKotlinx>(userJson)
            serializer.toKotlinx(u)
        }
        printKotlinxResult("encode → $userEncode")

        // ── Cenário 3: Interoperabilidade — JSON produzido por um, consumido pelo outro ──
        println()
        println("── Cenário 3: Interoperabilidade entre Gson e kotlinx ──")

        // kotlinx produz JSON, Gson consome (simulando módulo legado recebendo dado de módulo migrado)
        val kotlinxProduced = runSafe("Kotlinx→Gson") {
            val u = UserKotlinx(id = 2, name = "Ana", emailAddress = "ana@email.com")
            val json = serializer.toKotlinx(u)
            "kotlinx produziu: $json"
        }
        println(kotlinxProduced)

        val gsonConsumes = runSafe("Kotlinx→Gson") {
            val json = Json { ignoreUnknownKeys = true }.encodeToString(
                UserKotlinx(id = 2, name = "Ana", emailAddress = "ana@email.com")
            )
            // Módulo legado consome com Gson usando uma data class equivalente
            data class UserLegacy(
                val id: Int,
                val name: String,
                @SerializedName("email_address") val emailAddress: String
            )
            val u = Gson().fromJson(json, UserLegacy::class.java)
            "Gson consumiu: UserLegacy(id=${u.id}, name=${u.name}, emailAddress=${u.emailAddress})"
        }
        println(gsonConsumes)

        println()
        printDiff(
            "Gson e kotlinx produzem JSON compatível — a coexistência funciona. " +
            "O wrapper centraliza a decisão de qual serializer usar por classe."
        )

        // ── Guia de migração gradual ──
        println("┌──────────────────────────────────────────────────────────────────────┐")
        println("│  Guia de migração gradual                                            │")
        println("├──────────────────────────────────────────────────────────────────────┤")
        println("│                                                                      │")
        println("│  Passo 1 — Setup                                                     │")
        println("│    Adicione kotlinx-serialization no build.gradle sem remover Gson.   │")
        println("│    As duas libs coexistem sem conflito de dependências.               │")
        println("│                                                                      │")
        println("│  Passo 2 — Migrar classes simples primeiro                            │")
        println("│    Data classes concretas, sem TypeAdapter, sem herança.               │")
        println("│    Adicione @Serializable e troque @SerializedName por @SerialName.   │")
        println("│    Use o checklist do Lab 5 para identificar quais são seguras.       │")
        println("│                                                                      │")
        println("│  Passo 3 — Trocar os call sites gradualmente                          │")
        println("│    Substitua gson.fromJson() por Json.decodeFromString() nos pontos   │")
        println("│    que usam as classes já migradas. Um PR por módulo/feature.          │")
        println("│                                                                      │")
        println("│  Passo 4 — Migrar casos complexos                                    │")
        println("│    TypeAdapter → KSerializer (Lab 7)                                  │")
        println("│    TypeToken → reified inline (Lab 5, caso 3)                         │")
        println("│    Polimorfismo → sealed class + @SerialName (Lab 5*)                 │")
        println("│                                                                      │")
        println("│  Passo 5 — Remover Gson                                               │")
        println("│    Só quando não houver mais nenhum import com.google.gson.            │")
        println("│    Busque no projeto inteiro antes de remover a dependência.           │")
        println("│                                                                      │")
        println("└──────────────────────────────────────────────────────────────────────┘")
    }
}
