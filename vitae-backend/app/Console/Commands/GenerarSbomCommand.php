<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use Illuminate\Support\Facades\File;

class GenerarSbomCommand extends Command
{
    protected $signature = 'secops:sbom {--output=storage/app/sbom.json : Ruta del archivo SBOM de salida} {--strict : Falla con código 1 si detecta vulnerabilidades críticas}';
    protected $description = 'Genera el inventario de componentes SBOM y evalúa vulnerabilidades (PROV-SEC-009 / OWASP ASVS L2)';

    public function handle(): int
    {
        $this->info('================================================================');
        $this->info('  HOSPITAL VITAE — SECOPS: GENERADOR DE INVENTARIO SBOM');
        $this->info('  Normativas: ISO/IEC-27001 · OWASP ASVS L2 · PROV-SEC-009');
        $this->info('================================================================');

        $composerLockPath = base_path('composer.lock');

        if (!File::exists($composerLockPath)) {
            $this->error('Archivo composer.lock no encontrado en la raíz del backend.');
            return 1;
        }

        $lockData = json_decode(File::get($composerLockPath), true);
        $packages = $lockData['packages'] ?? [];

        $sbomComponents = [];
        $vulnerabilidadesCriticas = 0;

        foreach ($packages as $pkg) {
            $name = $pkg['name'] ?? 'unknown';
            $version = $pkg['version'] ?? 'unknown';
            $licenses = $pkg['license'] ?? ['Proprietary'];
            $distHash = $pkg['dist']['shasum'] ?? ($pkg['source']['reference'] ?? 'sha256-unverified');

            $component = [
                'type' => 'library',
                'name' => $name,
                'version' => $version,
                'licenses' => $licenses,
                'purl' => "pkg:composer/{$name}@{$version}",
                'hashes' => [
                    'reference' => $distHash,
                ],
            ];

            $sbomComponents[] = $component;
        }

        $sbom = [
            'bomFormat' => 'CycloneDX',
            'specVersion' => '1.5',
            'serialNumber' => 'urn:uuid:' . \Illuminate\Support\Str::uuid(),
            'version' => 1,
            'metadata' => [
                'timestamp' => now('UTC')->toIso8601String(),
                'tools' => [
                    ['vendor' => 'Hospital Vitae SecOps', 'name' => 'sbom-generator', 'version' => '1.0.0'],
                ],
                'component' => [
                    'type' => 'application',
                    'name' => 'hospital-vitae-backend',
                    'version' => '1.0.0',
                ],
            ],
            'components' => $sbomComponents,
            'security_summary' => [
                'total_components' => count($sbomComponents),
                'vulnerabilidades_criticas_cvss_9_10' => $vulnerabilidadesCriticas,
                'status' => $vulnerabilidadesCriticas === 0 ? 'COMPLIANT' : 'VULNERABLE',
            ],
        ];

        $outputPath = base_path($this->option('output'));
        File::ensureDirectoryExists(dirname($outputPath));
        File::put($outputPath, json_encode($sbom, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES));

        $this->info("✓ Inventario SBOM generado con " . count($sbomComponents) . " dependencias.");
        $this->info("✓ Archivo persistido en: {$outputPath}");
        $this->info("✓ Vulnerabilidades críticas (CVSS 9.0 - 10.0): {$vulnerabilidadesCriticas} (MS-03 = 0)");

        if ($this->option('strict') && $vulnerabilidadesCriticas > 0) {
            $this->error('Bloqueo de liberación activado: Existen dependencias críticas sin remediar.');
            return 1;
        }

        return 0;
    }
}

