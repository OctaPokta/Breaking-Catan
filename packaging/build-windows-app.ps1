# Builds a self-contained Windows app (bundled Java runtime, no separate Java install needed)
# for the Second-Settlement Planner desktop GUI, and zips it for distribution (e.g. as a
# GitHub Release asset).
#
# Requirements: JDK 17+ on PATH (needs jpackage, bundled with the JDK since 14).
# Run from the CatanAnalysis project root:
#   powershell -ExecutionPolicy Bypass -File packaging/build-windows-app.ps1
#
# Output: dist/output/CatanSecondSettlementPlanner-win64-v<version>.zip
#
# Note: this produces an "app-image" (a folder with a native .exe launcher + bundled JRE,
# just unzip and run) rather than a formal installer (.exe/.msi with a Setup wizard, Start
# Menu entry, uninstaller). A real installer needs the WiX Toolset (jpackage's Windows
# installer backend) and admin rights to install WiX itself - see the comment at the bottom
# of this script for that upgrade path once WiX is available.

$ErrorActionPreference = "Stop"
$AppVersion = "1.0.0"
$AppName = "Catan Second-Settlement Planner"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# Resolve the JDK's own bin/ directory (for jar/jpackage) even when only `java`/`javac` are
# on PATH but the JDK's other tools aren't - ask a running JVM for its own home directory
# rather than assuming a fixed layout.
# run through cmd.exe so its own "2>&1" merges native stderr into stdout as plain text -
# doing this directly in PowerShell 5.1 wraps each stderr line in a NativeCommandError instead
$javaHome = cmd /c "java -XshowSettings:properties -version 2>&1" | Select-String "java.home" | ForEach-Object { ($_ -split "=")[1].Trim() }
if (-not $javaHome) { throw "Could not determine java.home - is a JDK (not just a JRE) on PATH?" }
$jdkBin = Join-Path $javaHome "bin"
$jarExe = Join-Path $jdkBin "jar.exe"
$jpackageExe = Join-Path $jdkBin "jpackage.exe"
if (-not (Test-Path $jpackageExe)) { throw "jpackage not found at $jpackageExe - need a full JDK 14+, not a JRE." }

Write-Host ">>> Cleaning dist/ ..."
Remove-Item -Recurse -Force "dist" -ErrorAction SilentlyContinue

Write-Host ">>> Compiling ..."
$classesDir = "dist/staging/classes"
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
$sources = @(
    # Deliberately NOT the root .java files (Main.java/CatanAnalysisEngine.java) - those are
    # the console/CLI entry point and pull in CatanAnalysis.testing, which the GUI app doesn't
    # need at all.
    (Get-ChildItem -Path "secondsettlement" -Filter "*.java" -File),
    (Get-ChildItem -Path "secondsettlement/gui" -Filter "*.java" -File),
    (Get-ChildItem -Path "utils" -Filter "*.java" -File),
    (Get-ChildItem -Path "entities" -Filter "*.java" -File),
    (Get-ChildItem -Path "handlers" -Filter "*.java" -File)
) | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -d $classesDir $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host ">>> Packaging jar ..."
$inputDir = "dist/staging/input"
New-Item -ItemType Directory -Force -Path "$inputDir/secondsettlement" | Out-Null
Copy-Item "secondsettlement/strategies.csv" "$inputDir/secondsettlement/strategies.csv"
Push-Location $classesDir
& $jarExe --create --file "../../staging/input/CatanSecondSettlementPlanner.jar" `
    --main-class CatanAnalysis.secondsettlement.gui.CatanBoardApp .
Pop-Location
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

Write-Host ">>> Running jpackage (app-image) ..."
& $jpackageExe `
    --type app-image `
    --input $inputDir `
    --main-jar CatanSecondSettlementPlanner.jar `
    --main-class CatanAnalysis.secondsettlement.gui.CatanBoardApp `
    --name $AppName `
    --app-version $AppVersion `
    --vendor "OctaPokta" `
    --description "Interactive desktop tool for the second-settlement placement algorithm" `
    --add-modules java.desktop `
    --dest dist/output
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

Write-Host ">>> Zipping ..."
$zipPath = "dist/output/CatanSecondSettlementPlanner-win64-v$AppVersion.zip"
Compress-Archive -Path "dist/output/$AppName" -DestinationPath $zipPath -Force

Write-Host ">>> Done: $zipPath"

# ---- Upgrade path: a real installer (.exe/.msi) instead of an app-image ----
# 1. Install the WiX Toolset v3 (needs admin rights): winget install --id WiXToolset.WiXToolset -e
# 2. Re-run jpackage with --type msi (or --type exe) instead of --type app-image, adding
#    --win-shortcut --win-menu --win-dir-chooser for a normal Windows installer experience
#    (Start Menu entry, desktop shortcut, uninstaller registered in "Add or Remove Programs").
