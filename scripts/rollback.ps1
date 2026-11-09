# ==============================================================================
# HOSPITAL VITAE - REVERSIÓN AUTOMÁTICA EN <= 5 MINUTOS (PROV-RNF-003)
# ==============================================================================
param(
    [string]$TargetVersion = "v1.5.0",
    [int]$Timeout = 300
)

$ErrorActionPreference = "Stop"
Write-Host ">>> Iniciando Protocolo de Reversión Inmediata (Rollback <= 5m)" -ForegroundColor Cyan
Write-Host "Versión de destino: $TargetVersion | Límite RTO: ${Timeout}s" -ForegroundColor Yellow

$exit = Start-Process -FilePath "php" -ArgumentList "artisan","deploy:rollback","--target-version=$TargetVersion","--timeout=$Timeout" -Wait -PassThru

if ($exit.ExitCode -eq 0) {
    Write-Host ">>> Reversión concluida exitosamente dentro del SLA de 5 minutos." -ForegroundColor Green
} else {
    Write-Error ">>> Falló la ejecución del rollback o se excedió el tiempo límite."
}
