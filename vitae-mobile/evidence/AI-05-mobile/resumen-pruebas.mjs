// Resume las pruebas ejecutadas: unitarias (XML de Gradle) e instrumentadas (salida de `am instrument -r`).
//   ./gradlew testDebugUnitTest
//   adb shell am instrument -w -r com.example.vitae_mobile.test/androidx.test.runner.AndroidJUnitRunner > instrumentadas.txt
//   node evidence/AI-05-mobile/resumen-pruebas.mjs instrumentadas.txt > evidence/AI-05-mobile/resultados-pruebas.md
import { readFileSync, readdirSync } from "node:fs";

const quitar = (nombre) => nombre.replace("com.example.vitae_mobile.", "").trim();

const dir = "app/build/test-results/testDebugUnitTest";
const unitarias = readdirSync(dir)
  .filter((f) => f.endsWith(".xml"))
  .map((f) => {
    const m = readFileSync(`${dir}/${f}`, "utf8").match(/<testsuite name="([^"]+)" tests="(\d+)" skipped="\d+" failures="(\d+)" errors="(\d+)"/);
    return { clase: quitar(m[1]), pruebas: +m[2], fallos: +m[3] + +m[4] };
  })
  .sort((a, b) => a.clase.localeCompare(b.clase));

const salida = readFileSync(process.argv[2], "utf8");
const instrumentadas = {};
for (const [, clase, , codigo] of salida.matchAll(/INSTRUMENTATION_STATUS: class=(.+)\r?\n[\s\S]*?INSTRUMENTATION_STATUS: test=(.+)\r?\n[\s\S]*?INSTRUMENTATION_STATUS_CODE: (-?\d+)/g).map((m) => [m[0], quitar(m[1]), m[2], m[3]])) {
  if (codigo === "1") continue; // 1 = inicio de la prueba
  const c = (instrumentadas[clase] ??= { ok: 0, fallos: 0 });
  codigo === "0" ? c.ok++ : c.fallos++;
}

const suma = (xs, f) => xs.reduce((s, x) => s + f(x), 0);
console.log("# Resultados de pruebas\n\nGenerado con `resumen-pruebas.mjs` a partir de las ejecuciones reales.\n");
console.log("## Unitarias (JVM): `./gradlew testDebugUnitTest`\n\n| Clase | Pruebas | Fallos |\n|---|---|---|");
unitarias.forEach((u) => console.log(`| ${u.clase} | ${u.pruebas} | ${u.fallos} |`));
console.log(`| **Total** | **${suma(unitarias, (u) => u.pruebas)}** | **${suma(unitarias, (u) => u.fallos)}** |\n`);
console.log("## Instrumentadas (teléfono físico): `am instrument`\n\n| Clase | Pruebas OK | Fallos |\n|---|---|---|");
const filas = Object.entries(instrumentadas).sort(([a], [b]) => a.localeCompare(b));
filas.forEach(([clase, c]) => console.log(`| ${clase} | ${c.ok} | ${c.fallos} |`));
console.log(`| **Total** | **${suma(filas, ([, c]) => c.ok)}** | **${suma(filas, ([, c]) => c.fallos)}** |`);
