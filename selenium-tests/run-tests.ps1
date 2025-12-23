# Script PowerShell pour lancer les tests Selenium
# Usage: .\run-tests.ps1 [port]
# Exemple: .\run-tests.ps1 5000

param(
    [int]$Port = 5000
)

$AppUrl = "http://localhost:$Port"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tests Selenium pour BurnCare Flutter Web" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier que l'application est accessible
Write-Host "Vérification de l'accessibilité de l'application sur $AppUrl..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri $AppUrl -Method Get -TimeoutSec 5 -UseBasicParsing
    Write-Host "✓ Application accessible sur $AppUrl" -ForegroundColor Green
} catch {
    Write-Host "✗ ERREUR: L'application n'est pas accessible sur $AppUrl" -ForegroundColor Red
    Write-Host "   Assurez-vous que l'application Flutter est lancée avec:" -ForegroundColor Yellow
    Write-Host "   flutter run -d chrome --web-port=$Port" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "Voulez-vous continuer quand même? (o/N)"
    if ($continue -ne "o" -and $continue -ne "O") {
        exit 1
    }
}

Write-Host ""
Write-Host "Lancement des tests Selenium..." -ForegroundColor Yellow
Write-Host ""

# Lancer les tests Maven
mvn test "-Dapp.url=$AppUrl"

Write-Host ""
Write-Host "Tests terminés!" -ForegroundColor Cyan

