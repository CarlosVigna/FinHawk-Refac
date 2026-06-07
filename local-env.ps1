$env:JAVA_HOME="C:\Users\jose.garcia\.jdks\ms-17.0.19"
$env:Path="$env:JAVA_HOME\bin;$env:Path"

$env:DATABASE_URL="jdbc:postgresql://ep-patient-cake-ahsqct6g-pooler.c-3.us-east-1.aws.neon.tech:5432/neondb?sslmode=require"
$env:DATABASE_USERNAME="neondb_owner"
$env:DATABASE_PASSWORD="npg_kjTQd0CtAv7s"

$env:JWT_SECRET="my-secret-key"

Write-Host ""
Write-Host "================================="
Write-Host " FinHawk Environment Loaded"
Write-Host "================================="
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "DATABASE: Neon Connected"
Write-Host ""