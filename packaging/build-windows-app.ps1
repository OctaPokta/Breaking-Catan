# Builds a self-contained Windows app (bundled Java runtime, no separate Java install needed)
# for the Second-Settlement Planner desktop GUI, and zips/packages it for distribution (e.g. as
# a GitHub Release asset - see .github/workflows/release.yml, which calls this script with
# -Type msi on every version tag push).
#
# Requirements: JDK 17+ on PATH (needs jpackage, bundled with the JDK since 14).
# -Type exe/msi additionally requires the WiX Toolset v3 on PATH (jpackage's Windows installer
# backend) - install with: winget install --id WiXToolset.WiXToolset -e (needs admin rights),
# or on a GitHub Actions windows-latest runner: choco install wixtoolset -y (no prompt needed,
# CI runs elevated already).
#
# Run from the CatanAnalysis project root:
#   powershell -ExecutionPolicy Bypass -File packaging/build-windows-app.ps1 [-Type app-image|exe|msi] [-Version 1.0.0]
#
# Output: dist/output/CatanSecondSettlementPlanner-win64-v<version>.zip (app-image), or
#         dist/output/*.exe / *.msi (a real installer type - Setup wizard, Start Menu entry,
#         desktop shortcut, uninstaller registered in "Add or Remove Programs").

param(
    [ValidateSet("app-image", "exe", "msi")]
    [string]$Type = "app-image",
    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"
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

Write-Host ">>> Cleaning dist/staging/ ..."
# Only the staging (intermediate build) area, not dist/output - this script may be invoked more
# than once with different -Type values to produce multiple artifacts from one CI run (see
# .github/workflows/release.yml), and each invocation's output must survive the next one's run.
Remove-Item -Recurse -Force "dist/staging" -ErrorAction SilentlyContinue

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

if ($Type -eq "app-image") {
    # jpackage refuses to run if its own output directory already exists (unlike -Type exe/msi,
    # which just (re)writes a single installer file) - clear only that specific subpath so a
    # repeated local run of the same -Type doesn't collide with a previous one.
    Remove-Item -Recurse -Force "dist/output/$AppName" -ErrorAction SilentlyContinue
}

Write-Host ">>> Running jpackage (-Type $Type) ..."
$jpackageArgs = @(
    "--type", $Type,
    "--input", $inputDir,
    "--main-jar", "CatanSecondSettlementPlanner.jar",
    "--main-class", "CatanAnalysis.secondsettlement.gui.CatanBoardApp",
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", "OctaPokta",
    "--description", "Interactive desktop tool for the second-settlement placement algorithm",
    "--add-modules", "java.desktop",
    "--dest", "dist/output"
)
if ($Type -ne "app-image") {
    # a real installer, not just an unzip-and-run folder: Start Menu entry, desktop shortcut,
    # a directory-choice page, and (implicitly, via jpackage) an uninstaller registered in
    # "Add or Remove Programs"
    $jpackageArgs += @("--win-shortcut", "--win-menu", "--win-dir-chooser")
}
& $jpackageExe @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

if ($Type -eq "app-image") {
    Write-Host ">>> Zipping ..."
    $zipPath = "dist/output/CatanSecondSettlementPlanner-win64-v$AppVersion.zip"
    Compress-Archive -Path "dist/output/$AppName" -DestinationPath $zipPath -Force
    Write-Host ">>> Done: $zipPath"
} else {
    # jpackage already produced a single installer file (.exe or .msi) directly in dist/output -
    # nothing left to zip
    $installer = Get-ChildItem "dist/output" -Filter "*.$Type" | Select-Object -First 1
    Write-Host ">>> Done: $($installer.FullName)"
}
