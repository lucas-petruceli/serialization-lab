package lab

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Lab 6 — Polimorfismo
 *
 * Sealed class Shape com Circle e Rect.
 * Gson puro: não sabe deserializar para o subtipo correto.
 * Gson com RuntimeTypeAdapterFactory (manual): funciona.
 * kotlinx com @Serializable sealed class: funciona nativamente com discriminador "type".
 */
object Lab9_6_NaoSePreocupeComIsso : Lab {
    override val number = 99
    override val name = "Polimorfismo (sealed class)"

    // ========================
    // Gson models (sem @Serializable)
    // ========================
    abstract class ShapeGson {
        abstract val type: String
    }

    data class CircleGson(val radius: Double) : ShapeGson() {
        override val type = "circle"
    }

    data class RectGson(val width: Double, val height: Double) : ShapeGson() {
        override val type = "rect"
    }

    // RuntimeTypeAdapterFactory simplificado para o lab
    class ShapeAdapterFactory : TypeAdapterFactory {
        override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (!ShapeGson::class.java.isAssignableFrom(type.rawType)) return null

            val circleDelegate = gson.getDelegateAdapter(this, TypeToken.get(CircleGson::class.java))
            val rectDelegate = gson.getDelegateAdapter(this, TypeToken.get(RectGson::class.java))

            @Suppress("UNCHECKED_CAST")
            return object : TypeAdapter<T>() {
                override fun write(out: JsonWriter, value: T) {
                    when (value) {
                        is CircleGson -> circleDelegate.write(out, value)
                        is RectGson -> rectDelegate.write(out, value)
                        else -> throw IllegalArgumentException("Unknown shape")
                    }
                }

                override fun read(reader: JsonReader): T {
                    val jsonElement = com.google.gson.JsonParser.parseReader(reader).asJsonObject
                    val result = when (jsonElement.get("type")?.asString) {
                        "circle" -> circleDelegate.fromJsonTree(jsonElement)
                        "rect" -> rectDelegate.fromJsonTree(jsonElement)
                        else -> throw IllegalArgumentException("Unknown type: ${jsonElement.get("type")}")
                    }
                    @Suppress("UNCHECKED_CAST")
                    return result as T
                }
            } as TypeAdapter<T>
        }
    }

    // ========================
    // kotlinx models
    // ========================
    @Serializable
    sealed class ShapeKotlinx {
        @Serializable
        @SerialName("circle")
        data class Circle(val radius: Double) : ShapeKotlinx()

        @Serializable
        @SerialName("rect")
        data class Rect(val width: Double, val height: Double) : ShapeKotlinx()
    }

    override fun run() {
        printHeader(this)

        val circleJson = """{"type": "circle", "radius": 5.0}"""
        val rectJson = """{"type": "rect", "width": 10.0, "height": 3.0}"""

        // --- Gson puro: deserializa como base class, perde o subtipo ---
        println("── Gson puro (sem adapter) ──")
        printInput(circleJson)
        val gsonPure = runSafe("Gson") {
            val shape = Gson().fromJson(circleJson, ShapeGson::class.java)
            "type=${shape::class.simpleName} → Gson não sabe instanciar classe abstrata"
        }
        printGsonResult(gsonPure)
        println()

        // --- Gson com TypeAdapterFactory ---
        println("── Gson com TypeAdapterFactory ──")
        val gsonConfigured = GsonBuilder()
            .registerTypeAdapterFactory(ShapeAdapterFactory())
            .create()

        printInput(circleJson)
        val gsonCircle = runSafe("Gson(adapter)") {
            val shape = gsonConfigured.fromJson(circleJson, ShapeGson::class.java)
            "${shape::class.simpleName} → $shape"
        }
        printGsonResult(gsonCircle)

        printInput(rectJson)
        val gsonRect = runSafe("Gson(adapter)") {
            val shape = gsonConfigured.fromJson(rectJson, ShapeGson::class.java)
            "${shape::class.simpleName} → $shape"
        }
        printGsonResult(gsonRect)
        println()

        // --- kotlinx: sealed class com @SerialName funciona nativamente ---
        println("── kotlinx-serialization (sealed class nativo) ──")
        val kotlinxJson = Json { ignoreUnknownKeys = true }

        printInput(circleJson)
        val kCircle = runSafe("Kotlinx") {
            val shape = kotlinxJson.decodeFromString<ShapeKotlinx>(circleJson)
            "${shape::class.simpleName} → $shape"
        }
        printKotlinxResult(kCircle)

        printInput(rectJson)
        val kRect = runSafe("Kotlinx") {
            val shape = kotlinxJson.decodeFromString<ShapeKotlinx>(rectJson)
            "${shape::class.simpleName} → $shape"
        }
        printKotlinxResult(kRect)

        // Bônus: kotlinx serializa incluindo o discriminador automaticamente
        println()
        println("── kotlinx: serialização (objeto → JSON) ──")
        val encoded = runSafe("Kotlinx") {
            val shape: ShapeKotlinx = ShapeKotlinx.Circle(radius = 7.5)
            kotlinxJson.encodeToString(shape)
        }
        printKotlinxResult("encodeToString(Circle) → $encoded")

        printDiff(
            "Gson não suporta polimorfismo nativamente — precisa de TypeAdapterFactory manual (boilerplate pesado). " +
            "kotlinx suporta sealed class nativamente com discriminador 'type' via @SerialName."
        )
    }
}
