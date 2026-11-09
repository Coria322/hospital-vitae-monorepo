# test-prod-suite.ps1 - Ejecucion Integral de Pruebas Bruno en Produccion
param(
    [string]$EnvName = "Production",
    [string]$BaseUrl = "https://auditoria-backend-u47a.onrender.com/api/v1",
    [string]$LogFile = "bruno_prod_results.log"
)

$ErrorActionPreference = "Continue"
$StartTime = Get-Date

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  HOSPITAL VITAE - BATERIA INTEGRAL DE PRUEBAS BRUNO (PROD)" -ForegroundColor Cyan
Write-Host "  Ambiente: $EnvName | Base URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "  Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "================================================================`n" -ForegroundColor Cyan

# 1. Obtener tokens frescos de autenticacion directa
Write-Host "[1/8] Obteniendo credenciales maestras de autenticacion..." -ForegroundColor Yellow
$tokens = @{}
$credenciales = @(
    @{ Rol = "admin"; Id = "ADM-001"; Pass = "password123" },
    @{ Rol = "medico"; Id = "MED-001"; Pass = "password123" },
    @{ Rol = "enfermera"; Id = "ENF-001"; Pass = "password123" },
    @{ Rol = "recepcion"; Id = "REC-001"; Pass = "password123" },
    @{ Rol = "laboratorio"; Id = "LAB-001"; Pass = "password123" }
)

foreach ($cred in $credenciales) {
    try {
        $body = @{ id_usuario = $cred.Id; password = $cred.Pass } | ConvertTo-Json
        $resp = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 30
        $tokens[$cred.Rol] = $resp.data.token
        Write-Host "  [OK] Token $($cred.Rol) obtenido ($($cred.Id))" -ForegroundColor Green
    } catch {
        Write-Host "  [ERROR] No se pudo obtener token para $($cred.Rol): $_" -ForegroundColor Red
        $tokens[$cred.Rol] = ""
    }
}

# 2. Actualizar archivo environments/Production.bru
$prodBruContent = @"
vars {
  base_url: $BaseUrl
  admin_token: $($tokens['admin'])
  medico_token: $($tokens['medico'])
  enfermera_token: $($tokens['enfermera'])
  recepcion_token: $($tokens['recepcion'])
  triage_id: 
  paciente_id: 
}
"@
Set-Content -Path "environments/Production.bru" -Value $prodBruContent -Encoding UTF8

# 3. Actualizar archivo environments/Production.yml (usado prioritariamente por Bruno CLI)
$prodYmlContent = @"
name: Production
variables:
  - name: base_url
    value: $BaseUrl
    enabled: true
  - name: admin_token
    value: "$($tokens['admin'])"
    enabled: true
  - name: medico_token
    value: "$($tokens['medico'])"
    enabled: true
  - name: enfermera_token
    value: "$($tokens['enfermera'])"
    enabled: true
  - name: recepcion_token
    value: "$($tokens['recepcion'])"
    enabled: true
  - name: triage_id
    value: ""
    enabled: true
  - name: paciente_id
    value: ""
    enabled: true
"@
Set-Content -Path "environments/Production.yml" -Value $prodYmlContent -Encoding UTF8
Write-Host "  [OK] environments/Production.bru y Production.yml sincronizados con tokens activos.`n" -ForegroundColor Green

# Inicializar log
$HeaderLog = @"
================================================================
  HOSPITAL VITAE - LOG DE PRUEBAS AUTOMATIZADAS BRUNO (PRODUCCION)
  Fecha: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
  Host: $BaseUrl
================================================================

"@
Set-Content -Path $LogFile -Value $HeaderLog -Encoding UTF8

# Definir las suites a ejecutar en orden logico
# Nota: 04-Logout se ejecuta al final para no revocar tokens antes de tiempo
$suites = @(
    @{ Folder = "01-Auth"; Desc = "Autenticacion y Manejo de Sesiones (MFA, JWT, Refresh)" },
    @{ Folder = "02-Usuarios"; Desc = "Gestion de Usuarios y Validacion de Cedula Profesional" },
    @{ Folder = "03-Seguridad-RBAC"; Desc = "Control de Acceso Basado en Roles (401 y 403)" },
    @{ Folder = "05-Auditoria-I"; Desc = "Bitacora Inalterable NOM-024 (AUDITORIA_I) y Bloqueo 405" },
    @{ Folder = "06-Infraestructura-y-Tiempo"; Desc = "Sincronizacion CENAM/NTP y Healthchecks de Disponibilidad" },
    @{ Folder = "07-Triage-Manchester"; Desc = "Triage Manchester, Reclasificacion Medica y Salas Urgencias" },
    @{ Folder = "08-ECE-Recetas-FHIR"; Desc = "ECE, Recetas Controladas, FHIR R4 e Interoperabilidad" },
    @{ Folder = "09-Portal-Movil-QA"; Desc = "Portal Movil Paciente, Anti-IDOR, NOM-035 y Agenda de Citas" },
    @{ Folder = "10-Sprint-Final-Release-QA"; Desc = "Quality Gate AI-06, BCP/DRP, RFCs y Evidencias Maestras" },
    @{ Folder = "04-Logout"; Desc = "Revocacion Segura de Tokens de Sesion (Logout)" }
)

$summary = @()

foreach ($s in $suites) {
    $folder = $s.Folder
    $desc = $s.Desc
    Write-Host "----------------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "Ejecutando Suite: $folder ($desc)" -ForegroundColor White
    Write-Host "----------------------------------------------------------------" -ForegroundColor Cyan
    
    Add-Content -Path $LogFile -Value "`n`n>>> SUITE: $folder ($desc)`n----------------------------------------------------------------" -Encoding UTF8
    
    $folderStart = Get-Date
    
    # Invocar Bruno CLI directamente
    $output = cmd.exe /c "bru run $folder --env $EnvName" 2>&1
    $outputStr = $output -join "`n"
    
    Add-Content -Path $LogFile -Value $outputStr -Encoding UTF8
    
    $folderDuration = [math]::Round(((Get-Date) - $folderStart).TotalSeconds, 1)
    
    # Extraer métricas de la ejecución
    $reqMatch = [regex]::Match($outputStr, "Requests\s+\|\s+(\d+)\s+\((\d+)\s+Passed\)")
    $testMatch = [regex]::Match($outputStr, "Tests\s+\|\s+(\d+)/(\d+)")
    
    $totalReq = if ($reqMatch.Success) { $reqMatch.Groups[1].Value } else { "?" }
    $passedReq = if ($reqMatch.Success) { $reqMatch.Groups[2].Value } else { "?" }
    $passedTests = if ($testMatch.Success) { "$($testMatch.Groups[1].Value)/$($testMatch.Groups[2].Value)" } else { "?" }
    
    # Status
    $passed = ($outputStr -match "✓ PASS") -and ($totalReq -eq $passedReq)
    
    if ($passed) {
        Write-Host "  STATUS: PASS | Requests: $passedReq/$totalReq | Tests: $passedTests | Tiempo: ${folderDuration}s`n" -ForegroundColor Green
    } else {
        Write-Host "  STATUS: FAIL / REVISAR | Requests: $passedReq/$totalReq | Tests: $passedTests | Tiempo: ${folderDuration}s`n" -ForegroundColor Red
    }
    
    $summary += [PSCustomObject]@{
        Suite = $folder
        Descripcion = $desc
        Status = if ($passed) { "PASS" } else { "FAIL" }
        Requests = "$passedReq/$totalReq"
        Tests = $passedTests
        Tiempo = "${folderDuration}s"
    }
}

$TotalDuration = [math]::Round(((Get-Date) - $StartTime).TotalSeconds, 1)

# Limpieza de tokens en archivos de entorno por seguridad (AGENTS.md - Cero Secretos)
$cleanBru = @"
vars {
  base_url: $BaseUrl
  admin_token: 
  medico_token: 
  enfermera_token: 
  recepcion_token: 
  triage_id: 
  paciente_id: 
}
"@
Set-Content -Path "environments/Production.bru" -Value $cleanBru -Encoding UTF8

$cleanYml = @"
name: Production
variables:
  - name: base_url
    value: $BaseUrl
    enabled: true
  - name: admin_token
    value: ""
    enabled: true
  - name: medico_token
    value: ""
    enabled: true
  - name: enfermera_token
    value: ""
    enabled: true
  - name: recepcion_token
    value: ""
    enabled: true
  - name: triage_id
    value: ""
    enabled: true
  - name: paciente_id
    value: ""
    enabled: true
"@
Set-Content -Path "environments/Production.yml" -Value $cleanYml -Encoding UTF8

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  RESUMEN FINAL DE BATERIA EN PRODUCCION" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
$summary | Format-Table -AutoSize

$SummaryLog = @"

================================================================
  RESUMEN FINAL DE EJECUCION
  Tiempo Total: ${TotalDuration}s
================================================================
$($summary | Out-String)
"@
Add-Content -Path $LogFile -Value $SummaryLog -Encoding UTF8
Write-Host "Log completo guardado en: $LogFile" -ForegroundColor Yellow
