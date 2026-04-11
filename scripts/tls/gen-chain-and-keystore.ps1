<#
.SYNOPSIS
  3-level TLS chain (root -> intermediate -> server) + PKCS12 for Spring Boot.
  Certificate file names are project-specific.

.PARAMETER StudentTicket
  Student card number — in OU on all three certificates.
#>
param(
    [Parameter(Mandatory = $true)]
    [string] $StudentTicket,

    [string] $OutDir = "",

    [string] $KeystorePassword = "change-me-local-only"
)

$ErrorActionPreference = "Stop"
Remove-Item Env:\OPENSSL_CONF -ErrorAction SilentlyContinue

$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
if (-not $OutDir) {
    $repoRoot = Resolve-Path (Join-Path $ScriptDir "..\..")
    $OutDir = Join-Path $repoRoot "certs\generated"
}

function Find-OpenSsl {
    $cmd = Get-Command openssl -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($p in @("C:\Program Files\Git\usr\bin\openssl.exe", "C:\Program Files\OpenSSL-Win64\bin\openssl.exe")) {
        if (Test-Path $p) { return $p }
    }
    throw "OpenSSL not found. Install Git for Windows or OpenSSL."
}

$openssl = Find-OpenSsl
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Write-Utf8NoBom {
    param([string] $Path, [string] $Content)
    $enc = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($Path, $Content, $enc)
}

$work = Join-Path $env:TEMP ("ziovpo-tls-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $work | Out-Null
try {
    Copy-Item -LiteralPath (Join-Path $ScriptDir "openssl-intermediate.ext") (Join-Path $work "mid.ext")
    Copy-Item -LiteralPath (Join-Path $ScriptDir "openssl-server.ext")      (Join-Path $work "srv.ext")

    $rootKey  = Join-Path $work "ziovpo-bsuir-trust-anchor.key"
    $rootCert = Join-Path $work "ziovpo-bsuir-trust-anchor.crt"
    $midKey   = Join-Path $work "ziovpo-bsuir-policy-intermediate.key"
    $midCsr   = Join-Path $work "ziovpo-bsuir-policy-intermediate.csr"
    $midCert  = Join-Path $work "ziovpo-bsuir-policy-intermediate.crt"
    $srvKey   = Join-Path $work "ziovpo-bsuir-app-endpoint.key"
    $srvCsr   = Join-Path $work "ziovpo-bsuir-app-endpoint.csr"
    $srvCert  = Join-Path $work "ziovpo-bsuir-app-endpoint.crt"
    $chainPem = Join-Path $work "ziovpo-bsuir-chain-for-browser.pem"
    $p12work  = Join-Path $work "ziovpo-bsuir-app-endpoint.p12"

    $midExt = Join-Path $work "mid.ext"
    $srvExt = Join-Path $work "srv.ext"

    $rootCnf = Join-Path $work "root-req.cnf"
    $rootCnfText = @"
[req]
distinguished_name = dn
prompt = no
utf8 = yes
x509_extensions = v3_root

[dn]
C = BY
ST = Minsk
L = Minsk
O = ZiovpoLabCourse
CN = Ziovpo BSUIR Trust Anchor
OU = StudentTicket-$StudentTicket

[v3_root]
basicConstraints=critical,CA:TRUE
keyUsage=critical,keyCertSign,cRLSign
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer
"@
    Write-Utf8NoBom -Path $rootCnf -Content $rootCnfText

    $midReqCnf = Join-Path $work "mid-req.cnf"
    $midReqText = @"
[req]
distinguished_name = dn
prompt = no
utf8 = yes

[dn]
C = BY
ST = Minsk
L = Minsk
O = ZiovpoLabCourse
CN = Ziovpo BSUIR Policy Intermediate
OU = StudentTicket-$StudentTicket
"@
    Write-Utf8NoBom -Path $midReqCnf -Content $midReqText

    $srvReqCnf = Join-Path $work "srv-req.cnf"
    $srvReqText = @"
[req]
distinguished_name = dn
prompt = no
utf8 = yes

[dn]
C = BY
ST = Minsk
L = Minsk
O = ZiovpoLabCourse
CN = localhost
OU = StudentTicket-$StudentTicket
"@
    Write-Utf8NoBom -Path $srvReqCnf -Content $srvReqText

    Write-Host "OpenSSL: $openssl"
    Write-Host "Work:    $work"
    Write-Host "Copy to: $OutDir"

    & $openssl genrsa -out $rootKey 4096
    & $openssl req -x509 -new -nodes -key $rootKey -sha256 -days 3650 -config $rootCnf -out $rootCert

    & $openssl genrsa -out $midKey 4096
    & $openssl req -new -key $midKey -config $midReqCnf -out $midCsr
    & $openssl x509 -req -in $midCsr -CA $rootCert -CAkey $rootKey -CAcreateserial -out $midCert -days 1825 -sha256 -extfile $midExt -extensions v3_mid

    & $openssl genrsa -out $srvKey 2048
    & $openssl req -new -key $srvKey -config $srvReqCnf -out $srvCsr
    & $openssl x509 -req -in $srvCsr -CA $midCert -CAkey $midKey -CAcreateserial -out $srvCert -days 825 -sha256 -extfile $srvExt -extensions v3_server

    Get-Content $midCert, $rootCert | Set-Content -Encoding ascii $chainPem

    & $openssl pkcs12 -export -out $p12work -inkey $srvKey -in $srvCert -name "ziovpo.bsuir.app.endpoint" -certfile $chainPem -password "pass:$KeystorePassword"

    Copy-Item -Force $rootKey, $rootCert, $midKey, $midCsr, $midCert, $srvKey, $srvCsr, $srvCert, $chainPem, $p12work -Destination $OutDir
}
finally {
    Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
}

$p12 = Join-Path $OutDir "ziovpo-bsuir-app-endpoint.p12"
$p12Uri = "file:$($p12.Replace('\','/'))"
Write-Host ""
Write-Host "Done."
Write-Host "Trust anchor: $(Join-Path $OutDir 'ziovpo-bsuir-trust-anchor.crt')"
Write-Host ".env:"
Write-Host "  SSL_ENABLED=true"
Write-Host "  SSL_KEY_STORE=$p12Uri"
Write-Host "  SSL_KEY_STORE_PASSWORD=$KeystorePassword"
Write-Host "  SSL_KEY_ALIAS=ziovpo.bsuir.app.endpoint"
