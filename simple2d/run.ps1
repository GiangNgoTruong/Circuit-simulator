$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if (Test-Path -LiteralPath 'C:\Program Files\Java\jdk-21.0.12.1\bin\javac.exe') {
    $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME\bin\javac.exe")) {
    throw 'Set JAVA_HOME to a JDK 21+ installation.'
}
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($mavenCommand) { $mavenPath = $mavenCommand.Source }
elseif (Test-Path -LiteralPath 'E:\apache-maven-3.10.0\bin\mvn.cmd') { $mavenPath = 'E:\apache-maven-3.10.0\bin\mvn.cmd' }
else { throw 'Maven was not found. Add its bin folder to PATH.' }
$repositoryPath = Join-Path $projectRoot 'target\m2'
& $mavenPath -B "-Dmaven.repo.local=$repositoryPath" compile
if ($LASTEXITCODE -ne 0) { throw 'Maven compile failed.' }
$javafxJars = @('javafx-base', 'javafx-graphics', 'javafx-controls') | ForEach-Object {
    $jar = Join-Path $repositoryPath "org\openjfx\$_\21.0.2\$_-21.0.2-win.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw "JavaFX runtime missing: $jar" }
    $jar
}
& "$env:JAVA_HOME\bin\java.exe" "-Djavafx.cachedir=$projectRoot/target/fx-cache" --module-path ($javafxJars -join ';') --add-modules javafx.controls -cp "$projectRoot/target/classes" circuitsim.extension.simple2d.SimpleCircuitApp
exit $LASTEXITCODE
