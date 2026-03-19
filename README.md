# Migração Gson → kotlinx-serialization

---

## O que é serialização

Quando um objeto existe dentro do app, ele vive na memória RAM como uma estrutura de ponteiros e referências — um formato que só o processo atual entende. Para enviar esse objeto pela rede, salvá-lo em disco ou passá-lo para outro processo, é preciso transformá-lo em uma sequência linear de bytes. Esse processo se chama **serialização**. O caminho inverso — reconstruir o objeto a partir dos bytes — é a **deserialização**.

Todo app Android que se comunica com uma API faz isso o tempo todo, geralmente usando JSON como formato intermediário.

---

## Gson

O Gson é uma biblioteca do Google criada em 2008, quando o mundo ainda era Java puro. Ele funciona por reflection em runtime: quando você pede para deserializar um JSON, o Gson examina os campos da classe em tempo de execução, um por um, e tenta casar cada chave do JSON com um campo da classe.

Isso tem um custo: é lento, consome memória, e o compilador não consegue verificar se algo vai dar errado antes de rodar o app. Erros aparecem em produção, frequentemente sem lançar exceção — o objeto é criado com campos nulos ou zerados sem nenhum aviso.

A vantagem dessa abordagem é a flexibilidade: por não precisar de anotações nem de geração de código, o Gson consegue serializar qualquer classe em runtime sem nenhuma preparação prévia. Isso é útil em cenários onde o schema do JSON não é conhecido em tempo de compilação — estruturas completamente dinâmicas, plugins carregados em runtime, ou sistemas legados onde modificar as classes não é uma opção.

## kotlinx-serialization

O kotlinx-serialization usa uma abordagem diferente: um plugin de compilador. Quando você anota uma classe com @Serializable, o compilador gera automaticamente o código de serialização para aquela classe em tempo de compilação. Não há reflection em runtime.

O resultado é que o compilador conhece os tipos, respeita o sistema de null safety do Kotlin, e erros de configuração aparecem antes de o app chegar ao usuário. A performance também é significativamente melhor.

A desvantagem dessa abordagem é justamente a rigidez que a torna segura: toda classe que precisa ser serializada deve ser anotada e conhecida em tempo de compilação. Estruturas verdadeiramente dinâmicas exigem o uso de JsonElement — um tipo genérico da própria lib que representa JSON bruto — o que adiciona complexidade onde o Gson resolveria automaticamente. Quanto ao tamanho do app, o código gerado pelo compilador para cada classe é pequeno — na ordem de poucos KB por classe — e o impacto no APK final é desprezível na prática.

---

## Índice dos Labs

Para facilitar o entendimento das diferenças de comportamento entre Gson e kotlinx-serialization, preparamos uma série de labs — pequenos scripts executáveis que demonstram lado a lado como cada lib se comporta nos cenários mais comuns (e nos mais traiçoeiros) da migração. Cada lab mostra o input JSON, o resultado do Gson, o resultado do kotlinx, e explica a diferença.

| Lab | Tema | Problema |
|-----|------|----------|
| 1 | Campos com valor default | Gson ignora defaults do Kotlin e injeta null/zero da JVM em campos non-null quando o JSON traz `null` explícito |
| 2 | Campos ausentes no JSON (non-null) | Gson cria objetos com `null` em campos non-null sem erro quando a chave não existe no JSON — NPE surpresa em runtime |
| 3 | `@SerializedName` vs `@SerialName` | kotlinx não faz conversão automática de snake_case para camelCase — campos ficam com default sem aviso se o `@SerialName` estiver ausente ou com casing errado |
| 4 | `@Transient`: JVM vs kotlinx | `@Transient` da JVM não tem efeito no kotlinx-serialization — campos sensíveis podem vazar no JSON se não usar `@kotlinx.serialization.Transient` |
| 5 | ignoreUnknownKeys | Gson ignora campos extras no JSON silenciosamente. kotlinx rejeita por padrão — precisa de `ignoreUnknownKeys = true` para aceitar |
| 6 | fromJson: tipos concretos vs problemáticos | Nem todo `fromJson` migra igual — tipos concretos são diretos, mas `TypeToken`, herança e classes abstratas exigem atenção especial |
| 7 | Custom serializer | Tipos que nem Gson nem kotlinx sabem serializar por padrão (ex: `Date`) — Gson usa `TypeAdapter`, kotlinx usa `KSerializer` |
| 8 | Migração gradual (coexistência) | Como fazer Gson e kotlinx conviverem no mesmo build durante a migração incremental |
| 9_6 | Polimorfismo (NaoSePreocupeComIsso) | Gson não suporta polimorfismo nativamente — precisa de TypeAdapterFactory manual. kotlinx resolve com sealed class + `@SerialName` |

---

## O que vocês irão avaliar

Estamos migrando as data classes do projeto de Gson para kotlinx-serialization. A tarefa de vocês é localizar e migrar os **modelos mais simples** — data classes concretas sem hierarquia de herança complexa, sem campos de tipo genérico como `Any`, e sem uso de `TypeToken`.

Para cada classe migrada, o trabalho é:

1. Adicionar `@Serializable` na classe
2. Trocar `@SerializedName("campo_api")` por `@SerialName("campo_api")`
3. Verificar campos non-null que podem vir ausentes da API — se puderem, tornar nullable ou adicionar valor default

---

## Pontos que não devem ser tocados por enquanto

Durante a análise vocês podem encontrar os padrões abaixo. **Não migrem esses casos** — apenas documentem onde estão e avisem. Eles têm implicações que serão avaliadas separadamente.

### Moshi

Verifiquem se o Moshi está sendo usado em algum módulo do projeto — procurem por `import com.squareup.moshi` em qualquer arquivo. Se encontrarem, reportem em quais módulos ele aparece e não toquem em nada relacionado.

Prestem atenção especial em classes que usam `@Json`, `@JsonClass` ou `@Raw` do Moshi — essas anotações têm comportamentos específicos que precisam ser avaliados com cuidado antes de qualquer migração.

### Annotations do Gson que indicam complexidade

Além do `@SerializedName` — que é seguro migrar — o Gson tem outras anotações que sinalizam comportamentos especiais. Se encontrarem qualquer uma das abaixo, **documentem e não toquem**:

- **`@Expose`** — controla quais campos participam da serialização, exige `GsonBuilder` configurado
- **`@JsonAdapter`** — indica que aquela classe ou campo tem um serializador customizado registrado
- **`@Since`** e **`@Until`** — usados para versionamento de campos por versão de API
- Qualquer annotation de pacote `com.google.gson.annotations` que **não** seja `@SerializedName`

### Outros padrões para documentar e não migrar

- Classes anotadas com `@Parcelize` que possuem campos do tipo `Map` ou `Any`
- Leitura e escrita de objetos via `SharedPreferences` ou `DataStore`
- `TypeConverter` no Room que usa Gson internamente
- Parse manual do `errorBody` no tratamento de erros do Retrofit — procurem por `errorBody()` no projeto e registrem os arquivos encontrados
- Qualquer classe marcada com `abstract` ou `interface` sendo deserializada diretamente
- Atenção os Labs apartir do 6, são casos especificos e não queremos lidar com eles por hora.

> Se encontrarem qualquer um desses casos, registrem o **arquivo**, a **linha** e o **padrão encontrado**. Isso vai alimentar a próxima fase da migração.

---

## Requisitos

- **JDK 21** ou superior
- **Gradle 8.5** (já incluído via wrapper, não precisa instalar)

Para verificar o JDK:

```bash
java -version
```

---

## Como rodar

Dentro da pasta `serialization-lab`, execute:

```bash
./gradlew run
```

### Progredindo pelos labs

O projeto roda a partir do arquivo `src/main/kotlin/lab/Main.kt`. Nele, todos os labs estão listados mas comentados. A ideia é ir descomentando **um lab por vez**, rodar, entender o output, e só depois seguir para o próximo.

```kotlin
val labs: List<Lab> = listOf(
    Lab1,        // ← descomente este primeiro
    // Lab2,
    // Lab3,
    // Lab4,
    // Lab5,
    // Lab6,
    // Lab7,
    // Lab8,
)
```

1. Descomente o `Lab1`
2. Rode `./gradlew run`
3. Leia o output — cada lab mostra o input JSON, o resultado do Gson, o resultado do kotlinx, e a diferença entre eles
4. Quando entender o comportamento, descomente o próximo lab
5. Repita até o Lab8

Você pode deixar vários labs descomentados ao mesmo tempo para rodar todos de uma vez, ou focar em um só por vez — fica a seu critério.
