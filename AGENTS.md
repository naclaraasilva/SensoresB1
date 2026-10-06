# AGENTS.md — SensoresB1

Regras que **todo** código deste projeto deve seguir.

> **Nota de origem:** este documento foi escrito a partir das regras do professor e do
> estado atual do repositório. A pasta `referencia/` (projetos `appMove`,
> `sensorLuminosidade`, `tresSensores`) e o PDF `Trabalho_Sensores.pdf` **não estão
> neste repositório** — quando forem adicionados, confirme os exemplos contra este
> arquivo e ajuste o que divergir.

---

## 1. Stack e build

- **Linguagem:** Java 11 (`sourceCompatibility`/`targetCompatibility` = `VERSION_11`).
  **Nada de Kotlin** — nem arquivo `.kt`.
- **Gradle:** DSL Groovy. Usar **`build.gradle`**, **nunca `build.gradle.kts`**.
  Vale para `settings.gradle` também.
- **Versões:** centralizadas em `gradle/libs.versions.toml` (version catalog).
  Toda dependência nova entra no `.toml` e é referenciada via `libs.*` — não
  escrever coordenadas hardcoded no `build.gradle`.
- **AGP:** `9.1.0` (veja a seção 8 — o downgrade de `9.2.1` está *não commitado*).
- **SDK:** declarados em `app/build.gradle` dentro do bloco `android { }`.

### Valores de SDK — estado atual vs. alvo do trabalho

| Propriedade | Valor atual no `app/build.gradle` | Alvo do enunciado |
|---|---|---|
| `compileSdk` | `release(36) { minorApiLevel = 1 }` (API 36.1) | API 36 |
| `minSdk` | `24` | `30` |
| `targetSdk` | `36` | — |

Ao mexer no build, **altere só o que a tarefa pedir**. Não "conserte" `minSdk`/`compileSdk`
por conta própria; se a mudança for necessária, ela vem junto com a feature e é
mencionada explicitamente.

---

## 2. Proibições

**Proibido absolutamente:**

- Bibliotecas de rede: **Retrofit, Volley, OkHttp, Apache HttpClient**.
- **Kotlin** (`.kt`, plugin `kotlin-android`).
- **ViewBinding** / **DataBinding** — a UI é ligada por `findViewById`.
- **Jetpack Compose** — nenhuma `@Composable`, nenhum `build.gradle` de compose.
- Scripts Kotlin (`.kts`) no lugar dos `.gradle` Groovy.

**Única dependência externa permitida:** `MPAndroidChart` (`com.github.PhilJay`),
e somente se a tarefa pedir um gráfico. Antes de adicionar, declare no
`libs.versions.toml` e avise — não é uma lib do AndroidX.

---

## 3. Estrutura de uma Activity (padrão obrigatório)

O `MainActivity` **implementa `SensorEventListener`** e segue exatamente esta
estrutura:

```java
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager gerenciador;
    private Sensor sensorAcelerometro;   // campo separado por sensor
    private TextView txtValor;          // referência via findViewById

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtValor = findViewById(R.id.txtValor);

        gerenciador = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAcelerometro = gerenciador.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorAcelerometro != null) {                     // checar ANTES de registrar
            gerenciador.registerListener(this, sensorAcelerometro,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gerenciador.unregisterListener(this);                  // sempre desregistrar
    }

    @Override
    public void onSensorChanged(SensorEvent evento) {
        // formata e exibe
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int precisao) {
        // vazio, obrigatorio pela interface
    }
}
```

Regras que decorrem do padrão:

- `getSystemService(Context.SENSOR_SERVICE)` no `onCreate`.
- **Sempre testar `sensor != null`** antes de `registerListener` — nem todo
  aparelho tem todo sensor.
- `registerListener` só no `onResume`; `unregisterListener` no `onPause`.
- `onAccuracyChanged` é obrigatório (mesmo vazio); erro de compilação se faltar.
- Múltiplos sensors → um `Sensor` por campo, registrado/desregistrado individualmente.
- Não usar lambdas para os callbacks de sensor; estilo listener anônimo/classe.

---

## 4. Layouts

- **XML sempre**, em `app/src/main/res/layout/`.
- Raiz `ScrollView` → filho `LinearLayout` (`orientation="vertical"`) para telas
  com vários campos. O template atual usa `ConstraintLayout`; para telas de sensor
  com vários valores, siga o `ScrollView`/`LinearLayout`.
- IDs em **camelCase português**, sem prefixo de classe:
  `txtAceleracao`, `txtLuminosidade`, `btnLer`.
- **Todo texto visível vai em `res/values/strings.xml`** (`app_name`, etc.),
  nunca literal no layout. Sobretudo para textos que o usuário lê — exceto
  valores numéricos de exemplo.
- Margens/dimensões simples em `res/values/dimens.xml` quando reaproveitar.

---

## 5. HTTP na API

**O único jeito permitido de fazer requisição é o padrão do projeto `appMove`:**

- `java.net.HttpURLConnection` — nada de biblioteca de terceiros.
- `java.util.concurrent.ExecutorService` (ou `Thread`) para sair da main thread.
- `java.net.URLEncoder.encode(...)` para montar query string.
- `runOnUiThread(...)` para tocar em qualquer View.
- `try/catch` com `IOException` e mensagem de erro exibida na tela (`Toast` ou
  `TextView`).

```java
private void buscarDados(String cidade) {
    executor.execute(() -> {
        try {
            String url = "https://api.exemplo.com/dados?cidade="
                    + URLEncoder.encode(cidade, "UTF-8");
            HttpURLConnection conexao = (HttpURLConnection) new URL(url).openConnection();
            conexao.setRequestMethod("GET");
            // ... ler resposta, fechar inputStream
        } catch (IOException e) {
            runOnUiThread(() -> txtResultado.setText("Erro: " + e.getMessage()));
        }
    });
}
```

Extras:

- Declarar `<uses-permission android:name="android.permission.INTERNET" />` no
  `AndroidManifest.xml`.
- `disconnect()`/`finally` para não vazar conexão.
- Não bloquear a main thread; nunca chamar rede no `onSensorChanged` diretamente —
  o `onSensorChanged` só atualiza campos; a requisição vai para o `ExecutorService`.

---

## 6. Estilo do código

- **Comentários simples, em português.** Curto e direto:
  `// registra o sensor para ler a aceleracao`.
  Sem blocos de comentário, sem javadoc, sem "explicações" longas.
- **Nomes de variáveis, campos e IDs em português:**
  `gerenciador`, `sensorLuz`, `txtAceleracao`, `ultimoValor`, `posicaoX`.
- Métodos override da API do Android **mantêm o nome original em inglês**
  (`onSensorChanged`, `onResume`) — é o framework que nomeia.
- Indentação de **4 espaços**, sem tabs. Sem `var`. Sem streams/lambda salvo o
  necessário e permitido pelo `runOnUiThread`.
- Imports explícitos, nunca `import a.b.*`.

---

## 7. Como fazer mudanças

- **Mudanças pequenas e pontuais.** Edite o trecho necessário e pare.
- **Não reescrever arquivo inteiro** sem necessidade real. Preserve o que já está
  funcionando e o estilo existente ao redor da edição.
- Não reformatar, não reordenar imports, não "melhorar" código alheio à tarefa.
- Adicionar dependência = 2 edições: `libs.versions.toml` + `build.gradle`.
- Se algo não estiver claro, **pergunte** antes de assumir.

---

## 8. Armadilhas conhecidas deste repo

- `gradle/libs.versions.toml` tem uma alteração **não commitada** que faz downgrade
  do AGP de `9.2.1` para `9.1.0`. Não comite esse downgrade por engano ao
  mexer no arquivo; confirme com o professor o que é o correto.
- `MainActivity` atual usa `EdgeToEdge.enable(this)` + `ViewCompat.setOnApplyWindowInsetsListener(...)`
  com lambda (template do Android Studio). Ao implementar sensores, remova esse
  bloco para casar com o padrão da seção 3.
- `app/build.gradle` tem `implementation libs.activity.ktx` — é uma lib de
  **Kotlin**, mas o projeto é Java. Não a use no código; não remova sem necessidade.
- Não existe `build` de teste útil: os arquivos `ExampleUnitTest` /
  `ExampleInstrumentedTest` são o template padrão. Para checar compilação use
  `gradlew.bat assembleDebug`.
- Pastas `app/build/` e `build/` são geradas — nunca edite nada lá dentro.