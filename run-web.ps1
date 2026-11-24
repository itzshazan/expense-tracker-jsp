param(
    [ValidateRange(1024, 65535)][int]$Port = 8080,
    [string]$TomcatHome,
    [string]$EnvFile,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
if ($EnvFile) {
    $settingsFile = Get-Item -LiteralPath $EnvFile
    if ($settingsFile.PSIsContainer) {
        throw '-EnvFile must point to a file.'
    }
    $env:EXPENSE_ENV_FILE = $settingsFile.FullName
}
Set-Location -LiteralPath $root
$portCheck = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
try { $portCheck.Start() }
catch { throw "Port $Port is already in use or unavailable. Choose another port with -Port." }
finally { $portCheck.Stop() }
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:\Program Files\Java\latest\bin\java.exe')) {
    $env:JAVA_HOME = 'C:\Program Files\Java\latest'
}
if (-not $SkipBuild) {
    & (Join-Path $root 'mvnw.cmd') -B -ntp -DskipTests package
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$war = Join-Path $root 'target\expense-tracker.war'
if (-not (Test-Path -LiteralPath $war -PathType Leaf)) {
    throw 'Build target\expense-tracker.war first, or omit -SkipBuild.'
}

$runtime = Join-Path $root '.runtime'
New-Item -Force -ItemType Directory -Path $runtime | Out-Null
if (-not $TomcatHome) {
    $version = '10.1.62'
    $dist = "apache-tomcat-$version"
    $TomcatHome = Join-Path $runtime $dist
    if (-not (Test-Path -LiteralPath (Join-Path $TomcatHome 'bin\catalina.bat'))) {
        $archive = Join-Path $runtime "$dist.zip"
        $uri = "https://dlcdn.apache.org/tomcat/tomcat-10/v$version/bin/$dist.zip"
        Write-Host "Downloading Apache Tomcat $version"
        try {
            Invoke-WebRequest -Uri $uri -OutFile $archive -TimeoutSec 60 -UseBasicParsing
        } catch {
            $uri = "https://archive.apache.org/dist/tomcat/tomcat-10/v$version/bin/$dist.zip"
            Invoke-WebRequest -Uri $uri -OutFile $archive -TimeoutSec 60 -UseBasicParsing
        }
        $verification = Invoke-WebRequest -Uri "$uri.sha512" -TimeoutSec 30 -UseBasicParsing
        $checksum = [string]$verification.Content
        $expected = [regex]::Match($checksum.Trim(), '^[a-fA-F0-9]{128}').Value
        if (-not $expected -or (Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash -ne $expected) {
            throw 'Tomcat download failed SHA-512 verification.'
        }
        Expand-Archive -LiteralPath $archive -DestinationPath $runtime -Force
    }
}
$TomcatHome = (Resolve-Path -LiteralPath $TomcatHome).Path
$launcher = Join-Path $TomcatHome 'bin\catalina.bat'
if (-not (Test-Path -LiteralPath $launcher -PathType Leaf)) {
    throw '-TomcatHome must point to an Apache Tomcat 10.1 installation.'
}

$base = Join-Path $runtime "server-$Port"
foreach ($folder in 'conf', 'logs', 'temp', 'webapps', 'work') {
    New-Item -Force -ItemType Directory -Path (Join-Path $base $folder) | Out-Null
}
Copy-Item -Path (Join-Path $TomcatHome 'conf\*') -Destination (Join-Path $base 'conf') -Recurse -Force
$config = @"
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Service name="Catalina">
    <Connector port="$Port" address="127.0.0.1" protocol="HTTP/1.1"
               connectionTimeout="20000" maxSwallowSize="22020096" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="false" autoDeploy="false" />
    </Engine>
  </Service>
</Server>
"@
Set-Content -LiteralPath (Join-Path $base 'conf\server.xml') -Value $config -Encoding utf8
Copy-Item -LiteralPath $war -Destination (Join-Path $base 'webapps\ROOT.war') -Force
$env:CATALINA_HOME = $TomcatHome
$env:CATALINA_BASE = $base
$env:CATALINA_OPTS = ("$env:CATALINA_OPTS -Djava.awt.headless=true").Trim()
if (-not $env:EXPENSE_WEB_URL) {
    $env:EXPENSE_WEB_URL = "http://127.0.0.1:$Port"
}
Write-Host "Expense Tracker JSP   http://127.0.0.1:$Port/"
& $launcher run
exit $LASTEXITCODE
