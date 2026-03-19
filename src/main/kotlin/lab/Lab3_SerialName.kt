package lab

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lab 3 — @SerializedName vs @SerialName
 *
 * PROBLEMA: Na migração, a troca de @SerializedName por @SerialName parece mecânica,
 * mas o kotlinx é case-sensitive e não faz nenhuma conversão automática de naming
 * (snake_case → camelCase). Se o @SerialName estiver ausente ou com casing errado,
 * o campo simplesmente não é preenchido — fica com o default sem nenhum aviso.
 * Esse é um dos bugs mais comuns na migração porque "parece que funcionou".
 *
 * Gson com @SerializedName: mapeia corretamente.
 * kotlinx SEM @SerialName: campo fica com default/null sem aviso.
 * kotlinx COM @SerialName errado (casing): exceção por unknown key.
 * kotlinx COM @SerialName correto: funciona.
 */
object Lab3 : Lab {
    override val number = 3
    override val name = "@SerializedName vs @SerialName"

    // --- Gson com @SerializedName ---
    data class ProductGson(
        @SerializedName("product_name") val productName: String,
        @SerializedName("unit_price") val unitPrice: Double,
        @SerializedName("is_available") val isAvailable: Boolean
    )

    // --- kotlinx SEM @SerialName (bug silencioso) ---
    @Serializable
    data class ProductKotlinxBroken(
        val productName: String = "MISSING",
        val unitPrice: Double = -1.0,
        val isAvailable: Boolean = false
    )

    // --- kotlinx COM @SerialName mas casing errado (UPPER_CASE) ---
    @Serializable
    data class ProductKotlinxWrongCase(
        @SerialName("PRODUCT_NAME") val productName: String,
        @SerialName("UNIT_PRICE") val unitPrice: Double,
        @SerialName("IS_AVAILABLE") val isAvailable: Boolean
    )

    // --- kotlinx COM @SerialName correto ---
    @Serializable
    data class ProductKotlinxFixed(
        @SerialName("product_name") val productName: String,
        @SerialName("unit_price") val unitPrice: Double,
        @SerialName("is_available") val isAvailable: Boolean
    )

    override fun run() {
        printHeader(this)

        val json = """{"product_name": "Café", "unit_price": 9.90, "is_available": true}"""
        printInput(json)

        val lenientJson = Json { ignoreUnknownKeys = true }

        // Gson: @SerializedName mapeia snake_case → camelCase
        val gsonResult = runSafe("Gson") {
            val p = Gson().fromJson(json, ProductGson::class.java)
            p.toString()
        }
        printGsonResult(gsonResult)

        // kotlinx SEM @SerialName: campos snake_case não batem com camelCase
        // Com ignoreUnknownKeys, as chaves do JSON são ignoradas e os defaults são usados
        val kotlinxBroken = runSafe("Kotlinx(sem @SerialName)") {
            val p = lenientJson.decodeFromString<ProductKotlinxBroken>(json)
            p.toString()
        }
        printKotlinxResult("(sem @SerialName) $kotlinxBroken")

        // kotlinx COM @SerialName mas UPPER_CASE → não bate com o JSON snake_case
        val kotlinxWrongCase = runSafe("Kotlinx(@SerialName UPPER_CASE)") {
            val p = Json.decodeFromString<ProductKotlinxWrongCase>(json)
            p.toString()
        }
        printKotlinxResult("(@SerialName UPPER_CASE) $kotlinxWrongCase")

        // kotlinx COM @SerialName correto: mapeamento funciona
        val kotlinxFixed = runSafe("Kotlinx(com @SerialName)") {
            val p = Json.decodeFromString<ProductKotlinxFixed>(json)
            p.toString()
        }
        printKotlinxResult("(com @SerialName) $kotlinxFixed")

        printDiff(
            "Sem @SerialName, kotlinx não faz match de snake_case→camelCase — " +
            "campos ficam com default sem nenhum aviso. " +
            "Migração exige trocar todo @SerializedName por @SerialName."
        )
    }
}
