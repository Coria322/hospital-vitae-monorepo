// Calcula la cobertura de líneas (TASK-M-019, MC-10 >= 80%) a partir del informe JaCoCo de AGP.
//   ./gradlew createDebugUnitTestCoverageReport && node evidence/AI-05-mobile/cobertura.mjs
// Sale con código 1 si ViewModels o lógica de datos bajan de 80%.
import { readFileSync } from "node:fs";

const informe = "app/build/reports/coverage/test/debug/report.xml";
const xml = readFileSync(informe, "utf8");

const archivos = [...xml.matchAll(/<package name="([^"]+)">([\s\S]*?)<\/package>/g)].flatMap(([, paquete, cuerpo]) =>
  [...cuerpo.matchAll(/<sourcefile name="([^"]+)">([\s\S]*?)<\/sourcefile>/g)].map(([, nombre, c]) => {
    const l = c.match(/<counter type="LINE" missed="(\d+)" covered="(\d+)"\/>/);
    return { ruta: `${paquete.replace("com/example/vitae_mobile", "")}/${nombre}`.replace(/^\//, ""), perdidas: l ? +l[1] : 0, cubiertas: l ? +l[2] : 0 };
  }),
);

// Sin clases que dependen del runtime de Android (se prueban en el teléfono), cableado ni dobles de prueba de debug.
const androidODeCableado = /KeystoreTokenStore|EntornoDispositivo|AvisoPrevio\.kt|Fabrica|ApiConfig|SimulacionRoot/;
const esMock = /Mock/;
const grupos = {
  "ViewModels": (a) => /ViewModel\.kt$/.test(a.ruta),
  "Lógica de datos de producción (data/**, sin Android, cableado ni mocks)": (a) =>
    a.ruta.startsWith("data/") && !androidODeCableado.test(a.ruta) && !esMock.test(a.ruta),
  "Dobles de prueba de debug (Mock*, informativo)": (a) => esMock.test(a.ruta),
  "Todo el código (informativo)": () => true,
};

let falla = false;
console.log(`| Grupo | Líneas cubiertas | % |\n|---|---|---|`);
for (const [nombre, filtro] of Object.entries(grupos)) {
  const sel = archivos.filter(filtro);
  const c = sel.reduce((s, a) => s + a.cubiertas, 0);
  const t = c + sel.reduce((s, a) => s + a.perdidas, 0);
  const pct = t ? (100 * c) / t : 0;
  console.log(`| ${nombre} | ${c} / ${t} | ${pct.toFixed(1)}% |`);
  if (/^(ViewModels|Lógica)/.test(nombre) && pct < 80) falla = true;
}
if (process.argv.includes("--detalle")) {
  console.log("\nArchivos de la lógica de datos por debajo de 80%:");
  for (const a of archivos.filter(grupos["Lógica de datos de producción (data/**, sin Android, cableado ni mocks)"])) {
    const t = a.cubiertas + a.perdidas;
    if (t && a.cubiertas / t < 0.8) console.log(`  ${a.ruta}: ${a.cubiertas}/${t}`);
  }
}
process.exit(falla ? 1 : 0);
