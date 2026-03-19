package lab

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lab 7 — Custom serializer
 *
 * PROBLEMA: Alguns tipos não são serializáveis por padrão — nem no Gson, nem no
 * kotlinx. O caso mais comum é java.util.Date, mas vale para qualquer tipo que
 * não tem mapeamento direto para JSON (UUID, Instant, enums complexos, etc).
 * No Gson, isso é resolvido com TypeAdapter registrado no GsonBuilder — uma
 * configuração global que, se esquecida, causa falha em runtime. No kotlinx,
 * o KSerializer pode ser vinculado diretamente no campo com @Serializable(with=...),
 * e se não estiver presente, o erro é de compilação, não de runtime.
 *
 * Gson: TypeAdapter registrado globalmente no GsonBuilder.
 * kotlinx: KSerializer vinculado no campo ou via SerializersModule.
 */
object Lab7 : Lab {
    override val number = 7
    override val name = "Custom serializer"

    private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    // ========================
    // Gson: TypeAdapter para Date
    // ========================
    class DateTypeAdapter : TypeAdapter<Date>() {
        private val formatter = SimpleDateFormat(DATE_FORMAT, Locale.US)

        override fun write(out: JsonWriter, value: Date?) {
            if (value == null) out.nullValue()
            else out.value(formatter.format(value))
        }

        override fun read(reader: JsonReader): Date? {
            val str = reader.nextString()
            return formatter.parse(str)
        }
    }

    data class EventGson(
        val name: String,
        val createdAt: Date
    )

    // ========================
    // kotlinx: KSerializer para Date
    // ========================
    object DateSerializer : KSerializer<Date> {
        private val formatter = SimpleDateFormat(DATE_FORMAT, Locale.US)

        override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Date) {
            encoder.encodeString(formatter.format(value))
        }

        override fun deserialize(decoder: Decoder): Date {
            return formatter.parse(decoder.decodeString())!!
        }
    }

    @Serializable
    data class EventKotlinx(
        val name: String,
        @Serializable(with = DateSerializer::class)
        val createdAt: Date
    )

    override fun run() {
        printHeader(this)

        val jsonInput = """{"name": "deploy", "createdAt": "2026-03-18T14:30:00"}"""
        printInput(jsonInput)
        println()

        // ── Gson sem TypeAdapter: Date não deserializa ──
        println("── Gson sem TypeAdapter ──")
        val gsonDefault = runSafe("Gson") {
            val event = Gson().fromJson(jsonInput, EventGson::class.java)
            "Event(name=${event.name}, createdAt=${event.createdAt})"
        }
        printGsonResult(gsonDefault)

        // ── Gson com TypeAdapter: funciona ──
        println()
        println("── Gson com TypeAdapter ──")
        val gsonConfigured = GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .create()

        val gsonWithAdapter = runSafe("Gson(TypeAdapter)") {
            val event = gsonConfigured.fromJson(jsonInput, EventGson::class.java)
            "Event(name=${event.name}, createdAt=${event.createdAt})"
        }
        printGsonResult(gsonWithAdapter)

        // Gson: serializa de volta
        val gsonEncode = runSafe("Gson(TypeAdapter)") {
            val event = gsonConfigured.fromJson(jsonInput, EventGson::class.java)
            gsonConfigured.toJson(event)
        }
        printGsonResult("encode → $gsonEncode")

        // ── kotlinx com KSerializer ──
        println()
        println("── kotlinx com KSerializer ──")
        val kDecode = runSafe("Kotlinx") {
            val event = Json.decodeFromString<EventKotlinx>(jsonInput)
            "Event(name=${event.name}, createdAt=${event.createdAt})"
        }
        printKotlinxResult(kDecode)

        val kEncode = runSafe("Kotlinx") {
            val event = Json.decodeFromString<EventKotlinx>(jsonInput)
            Json.encodeToString(EventKotlinx.serializer(), event)
        }
        printKotlinxResult("encode → $kEncode")

        println()
        printDiff(
            "Gson precisa de TypeAdapter registrado no GsonBuilder (configuração global). " +
            "kotlinx usa @Serializable(with=...) direto no campo (configuração local, por classe)."
        )

        // ── Comparação lado a lado ──
        println("┌──────────────────────────────────────────────────────────────────────┐")
        println("│  Comparação: TypeAdapter (Gson) vs KSerializer (kotlinx)             │")
        println("├──────────────────────────────────────────────────────────────────────┤")
        println("│                                                                      │")
        println("│  Gson TypeAdapter:                                                   │")
        println("│    - Registrado globalmente no GsonBuilder                           │")
        println("│    - Implementa write(JsonWriter) e read(JsonReader)                 │")
        println("│    - Não tem validação em tempo de compilação                        │")
        println("│    - Se esquecer de registrar → falha silenciosa ou exceção          │")
        println("│                                                                      │")
        println("│  kotlinx KSerializer:                                                │")
        println("│    - Vinculado diretamente ao campo com @Serializable(with=...)      │")
        println("│    - Ou registrado via SerializersModule para uso global             │")
        println("│    - Implementa serialize(Encoder) e deserialize(Decoder)            │")
        println("│    - Erro de compilação se o tipo não tiver serializer               │")
        println("│                                                                      │")
        println("│  Migração:                                                           │")
        println("│    1. Busque registerTypeAdapter no GsonBuilder do projeto            │")
        println("│    2. Para cada TypeAdapter, crie um KSerializer equivalente          │")
        println("│    3. Use @Serializable(with=...) no campo ou SerializersModule      │")
        println("└──────────────────────────────────────────────────────────────────────┘")
    }
}
