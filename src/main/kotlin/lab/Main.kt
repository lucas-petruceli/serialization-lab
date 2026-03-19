package lab

fun main() {
    println("╔══════════════════════════════════════════════════════════════════════╗")
    println("║            Serialization Lab — Gson vs kotlinx-serialization        ║")
    println("╚══════════════════════════════════════════════════════════════════════╝")

    val labs: List<Lab> = listOf(
        Lab1,
        // Lab2,
        // Lab3,
        // Lab4,
        /* OS PRIMEIROS 4 SAO OS MAIS IMPORTANTE INICIALMENTE */
        // Lab5,
        // Lab6,
        // Lab7,
        // Lab8,
        // Lab9_5_NaoSePreocupeComIsso,
    )

    labs.forEach { it.run() }

    println()
    println("Done. ${labs.size} lab(s) executado(s).")
}
