# Скрипт генерации цепочки сертификатов для каршеринга (лабораторная работа)
# Цепочка: CarsharingRootCA -> CarsharingIntermediateCA -> CarsharingServerCert (3 звена)
# ВАЖНО: Замените STUDENT_ID на ваш номер студенческого билета перед запуском!
#
# Требования: OpenSSL в PATH (Git for Windows или отдельная установка)

param(
    [Parameter(Mandatory=$false)]
    [string]$StudentId = "STUDENT_ID"
)

$ErrorActionPreference = "Stop"
# OpenSSL - use Git for Windows, override PostgreSQL's OPENSSL_CONF
$openssl = if (Test-Path "C:\Program Files\Git\usr\bin\openssl.exe") { "C:\Program Files\Git\usr\bin\openssl.exe" }
  elseif (Get-Command openssl -ErrorAction SilentlyContinue) { "openssl" }
  else { throw "OpenSSL not found. Install Git for Windows." }
$gitSslCnf = "C:\Program Files\Git\usr\ssl\openssl.cnf"
if (Test-Path $gitSslCnf) { $env:OPENSSL_CONF = $gitSslCnf }
# certs in project root (parent of demo folder)
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$certsDir = Join-Path $projectRoot "certs"
$rootDir = Join-Path $certsDir "root"
$intermediateDir = Join-Path $certsDir "intermediate"

if ($StudentId -eq "STUDENT_ID") {
    Write-Host "ERROR: Specify student ID. Example: .\demo\scripts\generate-certificates.ps1 -StudentId '12345678'" -ForegroundColor Red
    exit 1
}

Write-Host "Generating certificate chain (Student ID: $StudentId)..." -ForegroundColor Cyan

# Создаём директории
New-Item -ItemType Directory -Force -Path $rootDir | Out-Null
New-Item -ItemType Directory -Force -Path $intermediateDir | Out-Null

# 1. Root CA (CarsharingRootCA)
Write-Host "[1/3] Root CA (CarsharingRootCA)..." -ForegroundColor Green
& $openssl genrsa -out "$rootDir\carsharing-root-ca.key" 4096
& $openssl req -x509 -new -nodes -key "$rootDir\carsharing-root-ca.key" -sha256 -days 3650 `
    -out "$rootDir\carsharing-root-ca.crt" `
    -subj "/C=RU/ST=Moscow/L=Moscow/O=CarsharingLab/OU=StudentID-$StudentId/CN=CarsharingRootCA"

# 2. Intermediate CA (CarsharingIntermediateCA)
Write-Host "[2/3] Intermediate CA (CarsharingIntermediateCA)..." -ForegroundColor Green
& $openssl genrsa -out "$intermediateDir\carsharing-intermediate-ca.key" 4096
& $openssl req -new -key "$intermediateDir\carsharing-intermediate-ca.key" `
    -out "$intermediateDir\carsharing-intermediate-ca.csr" `
    -subj "/C=RU/ST=Moscow/L=Moscow/O=CarsharingLab/OU=StudentID-$StudentId/CN=CarsharingIntermediateCA"

# Создаём конфиг для Intermediate CA (basicConstraints=CA:TRUE)
$intermediateExt = @"
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:TRUE
keyUsage=critical,digitalSignature,cRLSign,keyCertSign
subjectKeyIdentifier=hash
"@
$intermediateExt | Out-File -FilePath "$intermediateDir\intermediate.ext" -Encoding ASCII

& $openssl x509 -req -in "$intermediateDir\carsharing-intermediate-ca.csr" `
    -CA "$rootDir\carsharing-root-ca.crt" -CAkey "$rootDir\carsharing-root-ca.key" `
    -CAcreateserial -out "$intermediateDir\carsharing-intermediate-ca.crt" `
    -days 1825 -sha256 -extfile "$intermediateDir\intermediate.ext"

# 3. Server Certificate (CarsharingServerCert)
Write-Host "[3/3] Server Certificate (CarsharingServerCert)..." -ForegroundColor Green
& $openssl genrsa -out "$certsDir\carsharing-server.key" 2048
& $openssl req -new -key "$certsDir\carsharing-server.key" `
    -out "$certsDir\carsharing-server.csr" `
    -subj "/C=RU/ST=Moscow/L=Moscow/O=CarsharingLab/OU=StudentID-$StudentId/CN=localhost"

# SAN для localhost и 127.0.0.1
$serverExt = @"
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:localhost,DNS:127.0.0.1,IP:127.0.0.1
subjectKeyIdentifier=hash
"@
$serverExt | Out-File -FilePath "$certsDir\server.ext" -Encoding ASCII

& $openssl x509 -req -in "$certsDir\carsharing-server.csr" `
    -CA "$intermediateDir\carsharing-intermediate-ca.crt" `
    -CAkey "$intermediateDir\carsharing-intermediate-ca.key" `
    -CAcreateserial -out "$certsDir\carsharing-server.crt" `
    -days 375 -sha256 -extfile "$certsDir\server.ext"

# 4. Собираем цепочку и создаём PKCS12 keystore для Spring Boot
Write-Host "Creating keystore (PKCS12)..." -ForegroundColor Green
$keystorePath = Join-Path $certsDir "carsharing-keystore.p12"
$keystorePassword = -join ((65..90) + (97..122) + (48..57) | Get-Random -Count 24 | ForEach-Object {[char]$_})

# Создаём полную цепочку: server + intermediate + root
Get-Content "$certsDir\carsharing-server.crt" | Out-File "$certsDir\chain.pem" -Encoding ASCII
Get-Content "$intermediateDir\carsharing-intermediate-ca.crt" | Add-Content "$certsDir\chain.pem"
Get-Content "$rootDir\carsharing-root-ca.crt" | Add-Content "$certsDir\chain.pem"

& $openssl pkcs12 -export -out $keystorePath `
    -inkey "$certsDir\carsharing-server.key" `
    -in "$certsDir\carsharing-server.crt" `
    -certfile "$certsDir\chain.pem" `
    -passout "pass:$keystorePassword" `
    -name carsharing-server

# Сохраняем пароль в файл (только для локальной разработки, НЕ коммитить!)
$keystorePassword | Out-File "$certsDir\.keystore-password" -Encoding ASCII -NoNewline
Write-Host "Password saved to certs\.keystore-password" -ForegroundColor Yellow

# Удаляем временные файлы
Remove-Item "$intermediateDir\intermediate.ext" -ErrorAction SilentlyContinue
Remove-Item "$certsDir\server.ext" -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Done. Keystore: $keystorePath" -ForegroundColor Green
Write-Host "Root CA: $rootDir\carsharing-root-ca.crt"
Write-Host "GitHub Secrets: KEYSTORE_BASE64, KEYSTORE_PASSWORD"
