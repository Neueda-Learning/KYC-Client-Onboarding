<#
.SYNOPSIS
Compiles main sources and JUnit5/Mockito tests, then runs the full test suite.

Run from the repository root:
    powershell -ExecutionPolicy Bypass -File scripts\run-tests.ps1
#>

$root = Split-Path -Parent $PSScriptRoot

Remove-Item -Recurse -Force "$root\src\out" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "$root\testout" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$root\src\out" | Out-Null
New-Item -ItemType Directory -Force -Path "$root\testout" | Out-Null

$mainFiles = Get-ChildItem -Recurse -Filter *.java "$root\src\controller", "$root\src\repository", "$root\src\service", "$root\src\util" |
    Select-Object -ExpandProperty FullName
$mainFiles += Get-ChildItem -Filter *.java "$root\src" | Select-Object -ExpandProperty FullName
javac -cp "$root\src\lib\*" -d "$root\src\out" $mainFiles
if ($LASTEXITCODE -ne 0) { throw "Main source compilation failed" }

$testFiles = Get-ChildItem -Recurse -Filter *.java "$root\test" | Select-Object -ExpandProperty FullName
javac -cp "$root\src\out;$root\src\lib\*;$root\lib\test\*" -d "$root\testout" $testFiles
if ($LASTEXITCODE -ne 0) { throw "Test compilation failed" }

$jars = (Get-ChildItem "$root\src\lib\*.jar", "$root\lib\test\*.jar" | Select-Object -ExpandProperty FullName) -join ';'
$cp = "$root\testout;$root\src\out;$jars"

# -Dnet.bytebuddy.experimental=true is required because Byte Buddy 1.14.x does not officially
# support newer JDKs (e.g. Java 25) yet; it still works for mocking plain classes like our repositories.
& java '-Dnet.bytebuddy.experimental=true' -jar "$root\lib\test\junit-platform-console-standalone-1.10.2.jar" `
    execute -cp "$cp" --scan-classpath "$root\testout" --details=tree
