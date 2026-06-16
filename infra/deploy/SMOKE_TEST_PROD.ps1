param(
    [string]$FrontendUrl = "https://farolfinanceiro.online",
    [string]$BackendUrl = "https://farol-financeiro-msx9.onrender.com"
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Invoke-Curl {
    param(
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $output = & curl.exe @Arguments 2>&1
    $exitCode = $LASTEXITCODE

    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "curl falhou com codigo $exitCode. Saida: $output"
    }

    return [PSCustomObject]@{
        ExitCode = $exitCode
        Output = ($output -join "`n")
    }
}

function Assert-Contains {
    param(
        [string]$Text,
        [string]$Expected,
        [string]$Context
    )

    if ($Text -notmatch [regex]::Escape($Expected)) {
        throw "Falha em $Context. Valor esperado nao encontrado: $Expected`nSaida completa:`n$Text"
    }
}

function Assert-Status {
    param(
        [string]$HeadersText,
        [string[]]$ExpectedStatusLines,
        [string]$Context
    )

    foreach ($statusLine in $ExpectedStatusLines) {
        if ($HeadersText -match [regex]::Escape($statusLine)) {
            return
        }
    }

    throw "Falha em $Context. Nenhum status esperado apareceu. Esperados: $($ExpectedStatusLines -join ', ')`nSaida completa:`n$HeadersText"
}

Write-Host "Farol Financeiro - Smoke test de producao" -ForegroundColor Green
Write-Host "Frontend: $FrontendUrl"
Write-Host "Backend : $BackendUrl"

Write-Step "Validando pagina inicial do frontend"
$frontendHeaders = Invoke-Curl -Arguments @("--max-time", "30", "-sS", "-I", $FrontendUrl)
Assert-Status -HeadersText $frontendHeaders.Output -ExpectedStatusLines @("HTTP/1.1 200", "HTTP/2 200") -Context "frontend online"
Write-Host "OK - frontend respondeu 200" -ForegroundColor Green

Write-Step "Validando healthcheck do backend"
$healthHeaders = Invoke-Curl -Arguments @("--max-time", "45", "-sS", "-i", "$BackendUrl/actuator/health")
Assert-Status -HeadersText $healthHeaders.Output -ExpectedStatusLines @("HTTP/1.1 200", "HTTP/2 200") -Context "actuator health"
Assert-Contains -Text $healthHeaders.Output -Expected '{"status":"UP"}' -Context "body do healthcheck"
Write-Host "OK - backend respondeu health UP" -ForegroundColor Green

Write-Step "Validando preflight CORS do cadastro"
$registerPreflight = Invoke-Curl -Arguments @(
    "--max-time", "30",
    "-sS",
    "-i",
    "-X", "OPTIONS",
    "$BackendUrl/api/auth/register",
    "-H", "Origin: $FrontendUrl",
    "-H", "Access-Control-Request-Method: POST",
    "-H", "Access-Control-Request-Headers: content-type"
)
Assert-Status -HeadersText $registerPreflight.Output -ExpectedStatusLines @("HTTP/1.1 200", "HTTP/2 200") -Context "preflight register"
Assert-Contains -Text $registerPreflight.Output -Expected "access-control-allow-origin: $FrontendUrl" -Context "allow origin do cadastro"
Assert-Contains -Text $registerPreflight.Output -Expected "access-control-allow-credentials: true" -Context "allow credentials do cadastro"
Write-Host "OK - CORS do cadastro liberado" -ForegroundColor Green

Write-Step "Validando preflight CORS do login"
$loginPreflight = Invoke-Curl -Arguments @(
    "--max-time", "30",
    "-sS",
    "-i",
    "-X", "OPTIONS",
    "$BackendUrl/api/auth/login",
    "-H", "Origin: $FrontendUrl",
    "-H", "Access-Control-Request-Method: POST",
    "-H", "Access-Control-Request-Headers: content-type"
)
Assert-Status -HeadersText $loginPreflight.Output -ExpectedStatusLines @("HTTP/1.1 200", "HTTP/2 200") -Context "preflight login"
Assert-Contains -Text $loginPreflight.Output -Expected "access-control-allow-origin: $FrontendUrl" -Context "allow origin do login"
Assert-Contains -Text $loginPreflight.Output -Expected "access-control-allow-credentials: true" -Context "allow credentials do login"
Write-Host "OK - CORS do login liberado" -ForegroundColor Green

Write-Step "Validando sessao anonima"
$authMe = Invoke-Curl -Arguments @(
    "--max-time", "30",
    "-sS",
    "-i",
    "$BackendUrl/api/auth/me",
    "-H", "Origin: $FrontendUrl"
)
Assert-Status -HeadersText $authMe.Output -ExpectedStatusLines @("HTTP/1.1 204", "HTTP/2 204", "HTTP/1.1 401", "HTTP/2 401") -Context "auth me anonimo"
Assert-Contains -Text $authMe.Output -Expected "access-control-allow-origin: $FrontendUrl" -Context "allow origin do auth/me"
Write-Host "OK - endpoint de sessao respondeu como esperado" -ForegroundColor Green

Write-Step "Resumo final"
Write-Host "Smoke tecnico concluido com sucesso." -ForegroundColor Green
Write-Host "Fluxos ainda recomendados em navegador:"
Write-Host "1. Cadastro"
Write-Host "2. Login"
Write-Host "3. Reset de senha"
Write-Host "4. 2FA"
Write-Host "5. Criar receita e despesa"
Write-Host "6. Wishlist e notas fiscais"
Write-Host "7. Logout e F5 em /app"
