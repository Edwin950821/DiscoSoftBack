# Script para corregir todos los archivos del proyecto
Write-Host "Iniciando correccion de archivos..." -ForegroundColor Cyan

# 1. Eliminar archivos corruptos
Write-Host "`nEliminando archivos corruptos..." -ForegroundColor Yellow
Remove-Item "src\main\kotlin\com\kompralo\JwtService.kt" -Force -ErrorAction SilentlyContinue

# 2. Crear estructura de directorios
Write-Host "`nCreando estructura de directorios..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "src\main\kotlin\com\kompralo" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\kotlin\com\kompralo\services" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\kotlin\com\kompralo\dto" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\kotlin\com\kompralo\model" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\kotlin\com\kompralo\repository" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\kotlin\com\kompralo\config" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\kotlin\com\kompralo\controller" | Out-Null

# 3. Mostrar instrucciones
Write-Host "`nEstructura creada!" -ForegroundColor Green
Write-Host "`nAHORA DEBES COPIAR MANUALMENTE CADA ARTIFACT:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. AuthBackendApplication.kt     -> src\main\kotlin\com\kompralo\" -ForegroundColor White
Write-Host "2. JwtService.kt                 -> src\main\kotlin\com\kompralo\services\" -ForegroundColor White
Write-Host "3. User.kt                       -> src\main\kotlin\com\kompralo\model\" -ForegroundColor White
Write-Host "4. AuthDtos.kt                   -> src\main\kotlin\com\kompralo\dto\" -ForegroundColor White
Write-Host "5. UserRepository.kt             -> src\main\kotlin\com\kompralo\repository\" -ForegroundColor White
Write-Host "6. AuthService.kt                -> src\main\kotlin\com\kompralo\services\" -ForegroundColor White
Write-Host "7. CustomUserDetailsService.kt   -> src\main\kotlin\com\kompralo\services\" -ForegroundColor White
Write-Host "8. AuthController.kt             -> src\main\kotlin\com\kompralo\controller\" -ForegroundColor White
Write-Host "9. SecurityConfig.kt             -> src\main\kotlin\com\kompralo\config\" -ForegroundColor White
Write-Host "10. JwtAuthenticationFilter.kt   -> src\main\kotlin\com\kompralo\config\" -ForegroundColor White
Write-Host "11. GlobalExceptionHandler.kt    -> src\main\kotlin\com\kompralo\config\" -ForegroundColor White
Write-Host "12. build.gradle.kts             -> raiz del proyecto" -ForegroundColor White
Write-Host ""
Write-Host "TIP: Abre cada archivo en un editor y copia el contenido de los artifacts" -ForegroundColor Yellow
Write-Host ""

# 4. Verificar estructura final
Write-Host "`nEstructura actual:" -ForegroundColor Cyan
tree /F src\main\kotlin 2>$null

Write-Host "`nDespues de copiar los archivos, ejecuta:" -ForegroundColor Cyan
Write-Host "   .\gradlew clean" -ForegroundColor Green
Write-Host "   .\gradlew build" -ForegroundColor Green