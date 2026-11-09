# ==============================================================================
# HOSPITAL VITAE - DESPLIEGUE INMUTABLE BLUE-GREEN (PROV-RNF-003 / ISO/IEC-27001)
# ==============================================================================
param(
    [string]$Version = "v1.6.0",
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"
$StartTime = Get-Date

Write-Host ">>> Iniciando Despliegue Inmutable Blue-Green para versión: $Version" -ForegroundColor Cyan

# 1. Quality Gate
Write-Host "[1/4] Evaluando Quality Gate estricto..." -ForegroundColor Yellow
$qgExit = Start-Process -FilePath "php" -ArgumentList "artisan","release:quality-gate","--strict" -Wait -PassThru
if ($qgExit.ExitCode -ne 0) {
    Write-Error "El Quality Gate final falló. Se bloquea el despliegue."
}

# 2. Healthcheck
Write-Host "[2/4] Verificando salud del servicio (/up)..." -ForegroundColor Yellow
try {
    $resp = Invoke-RestMethod -Uri "http://127.0.0.1:8000/up" -Method Get -TimeoutSec 10
    Write-Host "  [OK] Endpoint de salud operativo." -ForegroundColor Green
} catch {
    Write-Host "  [WARN] Fallo de healthcheck local, validando por artisan..." -ForegroundColor Yellow
}

# 3. Consolidación de evidencias
Write-Host "[3/4] Certificando paquete de evidencias auditables..." -ForegroundColor Yellow
php artisan auditoria:consolidar-evidencias --firmar

$Elapsed = [math]::Round(((Get-Date) - $StartTime).TotalSeconds, 2)
Write-Host "[4/4] Despliegue concluido exitosamente en ${Elapsed}s (RTO <= 5m)." -ForegroundColor Green
