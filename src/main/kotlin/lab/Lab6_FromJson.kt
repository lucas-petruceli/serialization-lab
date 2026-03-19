package lab

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Lab 6 — fromJson: tipos concretos vs problemáticos
 *
 * PROBLEMA: Nem todo fromJson migra da mesma forma. Data classes concretas são
 * substituição direta, mas o projeto pode ter padrões que complicam a migração:
 * TypeToken para genéricos (List, Map), herança simples, e classes abstratas
 * sendo passadas como tipo para o fromJson. O Gson trata alguns desses casos
 * silenciosamente (retornando LinkedTreeMap em vez do objeto esperado), enquanto
 * o kotlinx falha imediatamente. Este lab ajuda a classificar cada fromJson
 * do projeto em "migração direta" ou "precisa de atenção".
 */
object Lab6 : Lab {
    override val number = 6
    override val name = "fromJson: tipos concretos vs problemáticos"

    // ========================
    // Caso 1 — Data class concreta (seguro)
    // ========================
    data class LoginResponseGson(val name: String, val token: String)

    @Serializable
    data class LoginResponseKotlinx(val name: String, val token: String)

    // ========================
    // Caso 2 — Herança simples
    // ========================
    open class BaseResponseGson(val status: Int)
    class ErrorResponseGson(status: Int, val message: String) : BaseResponseGson(status)

    // kotlinx: herança com open class é possível mas cada nível precisa de @Serializable
    // Abordagem mais simples: data class flat com os mesmos campos
    @Serializable
    data class ErrorResponseKotlinx(val status: Int, val message: String)

    // ========================
    // Caso 3 — TypeToken com List
    // ========================
    data class ItemGson(val id: Int, val label: String)

    @Serializable
    data class ItemKotlinx(val id: Int, val label: String)

    // ========================
    // Caso 4 — Classe abstrata (problemático)
    // ========================
    abstract class BaseEventGson {
        abstract val eventType: String
    }

    data class ClickEventGson(override val eventType: String = "click", val x: Int, val y: Int) : BaseEventGson()
    data class ScrollEventGson(override val eventType: String = "scroll", val offset: Int) : BaseEventGson()

    @Serializable
    sealed class BaseEventKotlinx {
        @Serializable
        data class ClickEvent(val eventType: String = "click", val x: Int, val y: Int) : BaseEventKotlinx()

        @Serializable
        data class ScrollEvent(val eventType: String = "scroll", val offset: Int) : BaseEventKotlinx()
    }

    override fun run() {
        printHeader(this)

        val gson = Gson()
        val json = Json { ignoreUnknownKeys = true }

        // ── Caso 1: Data class concreta ──
        println("── Caso 1: Data class concreta (o caso seguro) ──")
        val loginJson = """{"name": "Lucas", "token": "abc123"}"""
        printInput(loginJson)

        val gsonLogin = runSafe("Gson") {
            val r = gson.fromJson(loginJson, LoginResponseGson::class.java)
            "LoginResponse(name=${r.name}, token=${r.token})"
        }
        printGsonResult(gsonLogin)

        val kLogin = runSafe("Kotlinx") {
            val r = json.decodeFromString<LoginResponseKotlinx>(loginJson)
            "LoginResponse(name=${r.name}, token=${r.token})"
        }
        printKotlinxResult(kLogin)
        printDiff("Comportamento idêntico. Migração direta, sem surpresas.")

        // ── Caso 2: Herança simples ──
        println("── Caso 2: Data class filha (herança simples) ──")
        val errorJson = """{"status": 404, "message": "Not Found"}"""
        printInput(errorJson)

        val gsonError = runSafe("Gson") {
            val r = gson.fromJson(errorJson, ErrorResponseGson::class.java)
            "ErrorResponse(status=${r.status}, message=${r.message})"
        }
        printGsonResult(gsonError)

        val kError = runSafe("Kotlinx") {
            val r = json.decodeFromString<ErrorResponseKotlinx>(errorJson)
            "ErrorResponse(status=${r.status}, message=${r.message})"
        }
        printKotlinxResult(kError)
        printDiff("Herança simples não é problema — tipo concreto resolve.")

        // ── Caso 3: TypeToken com List ──
        println("── Caso 3: TypeToken com List<T> (genérico) ──")
        val listJson = """[{"id": 1, "label": "A"}, {"id": 2, "label": "B"}, {"id": 3, "label": "C"}]"""
        printInput(listJson)

        val gsonList = runSafe("Gson") {
            val type = object : TypeToken<List<ItemGson>>() {}.type
            val items: List<ItemGson> = gson.fromJson(listJson, type)
            "List<Item> size=${items.size} → $items"
        }
        printGsonResult("(TypeToken) $gsonList")

        val gsonListWithout = runSafe("Gson") {
            // Sem TypeToken: Gson retorna List<LinkedTreeMap> em vez de List<Item>
            @Suppress("UNCHECKED_CAST")
            val items = gson.fromJson(listJson, List::class.java) as List<*>
            val first = items.first()
            "List<${first!!::class.simpleName}> size=${items.size} → primeiro elemento é MAP, não Item!"
        }
        printGsonResult("(sem TypeToken) $gsonListWithout")

        val kList = runSafe("Kotlinx") {
            val items = json.decodeFromString<List<ItemKotlinx>>(listJson)
            "List<Item> size=${items.size} → $items"
        }
        printKotlinxResult("(reified, sem TypeToken) $kList")
        printDiff("Gson precisa de TypeToken para genéricos. kotlinx resolve com reified inline — mais simples e type-safe.")

        // ── Caso 4: Classe abstrata ──
        println("── Caso 4: Classe abstrata como tipo (o caso problemático) ──")
        val eventJson = """{"eventType": "click", "x": 100, "y": 200}"""
        printInput(eventJson)

        val gsonAbstract = runSafe("Gson") {
            // Gson não consegue instanciar abstract class, mas se fosse uma interface
            // ou open class, retornaria LinkedTreeMap silenciosamente
            val event = gson.fromJson(eventJson, BaseEventGson::class.java)
            "type=${event::class.simpleName} → $event"
        }
        printGsonResult("(BaseEvent abstract) $gsonAbstract")

        val gsonConcrete = runSafe("Gson") {
            val event = gson.fromJson(eventJson, ClickEventGson::class.java)
            "type=${event::class.simpleName} → ClickEvent(x=${event.x}, y=${event.y})"
        }
        printGsonResult("(ClickEvent concreto) $gsonConcrete")

        val kAbstract = runSafe("Kotlinx") {
            val event = json.decodeFromString<BaseEventKotlinx>(eventJson)
            "type=${event::class.simpleName} → $event"
        }
        printKotlinxResult("(BaseEvent sealed) $kAbstract")

        val kConcrete = runSafe("Kotlinx") {
            val event = json.decodeFromString<BaseEventKotlinx.ClickEvent>(eventJson)
            "type=${event::class.simpleName} → ClickEvent(x=${event.x}, y=${event.y})"
        }
        printKotlinxResult("(ClickEvent concreto) $kConcrete")

        printDiff(
            "Gson com abstract class: exception ou LinkedTreeMap silencioso. " +
            "kotlinx com sealed class sem discriminador: exception imediata. Fail-fast."
        )

        // ── Checklist final ──
        println()
        println("╭──────────────────────────────────────────────────────────────────────────────╮")
        println("│  CHECKLIST — O que buscar no projeto antes de migrar                         │")
        println("├──────────────────────────────────────────────────────────────────────────────┤")
        println("│                                                                              │")
        println("│  REGRA RÁPIDA:                                                               │")
        println("│  Se todos os fromJson recebem data classes concretas E não existe             │")
        println("│  TypeToken, TypeAdapterFactory, RuntimeTypeAdapterFactory nem                 │")
        println("│  registerTypeAdapter no projeto → polimorfismo não é problema.               │")
        println("│  Busque por 'registerType' no projeto — pega tudo de uma vez.                │")
        println("│                                                                              │")
        println("│  O que é 'tipo concreto' vs 'não concreto'?                                  │")
        println("│  É o TIPO passado para o fromJson, não os atributos internos da classe.      │")
        println("│                                                                              │")
        println("│  NÃO concreto (problema):                                                    │")
        println("│    gson.fromJson(json, BaseEvent::class.java)   // abstract class            │")
        println("│    gson.fromJson(json, Parseable::class.java)   // interface                 │")
        println("│    gson.fromJson(json, Shape::class.java)       // sealed class              │")
        println("│                                                                              │")
        println("│  CONCRETO (seguro):                                                          │")
        println("│    gson.fromJson(json, ClickEvent::class.java)  // data class                │")
        println("│    gson.fromJson(json, LoginResponse::class.java)                            │")
        println("│                                                                              │")
        println("│  E atributo Any dentro da data class? Não é problema deste lab.              │")
        println("│  Atributo Any/Object é tratado no Lab 7 (custom serializer).                 │")
        println("│                                                                              │")
        println("├──────────────────────────────────────────────────────────────────────────────┤")
        println("│                                                                              │")
        println("│  Busca: fromJson / decodeFromString                                          │")
        println("│  [ ] Todos recebem tipo concreto (data class, não abstract/interface)?        │")
        println("│  [ ] Nenhum passa open class ou Any como tipo?                               │")
        println("│                                                                              │")
        println("│  Busca: TypeToken                                                            │")
        println("│  [ ] Nenhum TypeToken<List<T>> ou TypeToken<Map<K,V>> no projeto?             │")
        println("│  [ ] Se tem, já mapeei para decodeFromString<List<T>> com reified?            │")
        println("│                                                                              │")
        println("│  Busca: registerType (pega registerTypeAdapter e registerTypeAdapterFactory)  │")
        println("│  [ ] Nenhum registerTypeAdapter ou registerTypeAdapterFactory no projeto?     │")
        println("│  [ ] Se tem, cada um tem equivalente KSerializer planejado?                   │")
        println("│                                                                              │")
        println("│  Busca: sealed class / abstract class / interface usadas em JSON              │")
        println("│  [ ] Alguma sealed/abstract/interface é passada para fromJson?                │")
        println("│  [ ] Se sim, preciso de @SerialName + discriminador no kotlinx?               │")
        println("│                                                                              │")
        println("│  Busca: GsonBuilder                                                          │")
        println("│  [ ] Identifiquei todas as configurações do GsonBuilder?                      │")
        println("│  [ ] setDateFormat → mapeei para KSerializer de Date customizado?             │")
        println("│  [ ] serializeNulls → mapeei para explicitNulls = true no Json {}?            │")
        println("│  [ ] setFieldNamingPolicy → mapeei para @SerialName nos campos?               │")
        println("│                                                                              │")
        println("├──────────────────────────────────────────────────────────────────────────────┤")
        println("│  TUDO marcado → migração segura, Lab5 polimorfismo pode ser pulado           │")
        println("│  Algum NÃO marcado → resolver antes de migrar, ver Lab5 polimorfismo         │")
        println("╰──────────────────────────────────────────────────────────────────────────────╯")
    }
}
