package lab

interface Lab {
    val number: Int
    val name: String
    fun run()
}

fun printHeader(lab: Lab) {
    println()
    println("=".repeat(70))
    println("[LAB ${lab.number} — ${lab.name}]")
    println("=".repeat(70))
}

fun printInput(json: String) {
    println("Input JSON: $json")
}

fun printGsonResult(result: String) {
    println("Gson result: $result")
}

fun printKotlinxResult(result: String) {
    println("Kotlinx result: $result")
}

fun printDiff(diff: String) {
    println("Diferença: $diff")
    println()
}

fun runSafe(label: String, block: () -> String): String {
    return try {
        block()
    } catch (e: Exception) {
        "$label EXCEPTION → ${e::class.simpleName}: ${e.message}"
    }
}
