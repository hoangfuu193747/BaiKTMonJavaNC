param(
    [string]$TomcatHome = 'D:\Dowload\apache-tomcat-9.0.122',
    [ValidateRange(1024,65535)][int]$Port = 18080,
    [switch]$CreateAdmin,
    [switch]$GenerateAdminPassword,
    [switch]$CheckDatabaseOnly
)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$tomcatBase = [IO.Path]::GetFullPath((Join-Path $projectRoot 'target\auth-tomcat'))
if (-not $tomcatBase.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Tomcat base must stay inside this project.'
}
if (-not (Test-Path -LiteralPath (Join-Path $TomcatHome 'bin\bootstrap.jar'))) {
    throw 'Set -TomcatHome to your installed Tomcat 9 directory.'
}
$warPath = Join-Path $projectRoot 'target\QuanLyKyTucXa.war'
if (-not (Test-Path -LiteralPath $warPath)) {
    throw 'Run mvn package before starting Tomcat.'
}
if (-not $CheckDatabaseOnly) {
    $portProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
    try { $portProbe.Start() }
    catch { throw "Port $Port is already in use. Stop this project's Tomcat with Ctrl+C before starting it again, or use -Port with another port." }
    finally { $portProbe.Stop() }
}
if (-not $env:KTX_DB_CONFIG) {
    if (-not $env:KTX_DB_URL) {
        $env:KTX_DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=QuanLyKyTucXa;encrypt=true;trustServerCertificate=true;loginTimeout=5;'
    }
    if (-not $env:KTX_DB_USER) { $env:KTX_DB_USER = 'sa' }
}
$readPassword = $false
if (-not $env:KTX_DB_PASSWORD -and -not $env:KTX_DB_CONFIG) {
    Write-Host 'Use the SQL Server password from SSMS, not the website Admin password.'
    $dbPassword = Read-Host -AsSecureString "SQL Server password for $env:KTX_DB_USER"
    try { $env:KTX_DB_PASSWORD = ([System.Net.NetworkCredential]::new('', $dbPassword)).Password }
    finally { $dbPassword.Dispose() }
    $readPassword = $true
}
try {
    $classesPath = Join-Path $projectRoot 'target\classes'
    $jdbcJar = Join-Path $projectRoot 'target\QuanLyKyTucXa\WEB-INF\lib\mssql-jdbc-13.4.0.jre11.jar'
    if (-not (Test-Path -LiteralPath (Join-Path $classesPath 'com\qlktx\util\DatabaseCheck.class'))) {
        throw 'Run mvn package to rebuild the application before using this script.'
    }
    & java '-cp' "$classesPath;$jdbcJar" 'com.qlktx.util.DatabaseCheck'
    if ($LASTEXITCODE -ne 0) {
        throw 'Database check failed. Correct SQL credentials/configuration and run this script again.'
    }
    if ($CheckDatabaseOnly) { return }
    if ($CreateAdmin) {
        $adminArgs = @('-cp', "$classesPath;$jdbcJar", 'com.qlktx.util.AdminSetup', 'admin')
        if ($GenerateAdminPassword) { $adminArgs += '--generate' }
        & java @adminArgs
        if ($LASTEXITCODE -ne 0) { throw 'Administrator setup failed.' }
    }
    foreach ($folder in @('conf', 'logs', 'temp', 'webapps', 'work')) {
        New-Item -ItemType Directory -Path (Join-Path $tomcatBase $folder) -Force | Out-Null
    }
    Copy-Item -LiteralPath (Join-Path $TomcatHome 'conf\web.xml') -Destination (Join-Path $tomcatBase 'conf\web.xml') -Force
    Copy-Item -LiteralPath (Join-Path $TomcatHome 'conf\catalina.properties') -Destination (Join-Path $tomcatBase 'conf\catalina.properties') -Force
    Copy-Item -LiteralPath $warPath -Destination (Join-Path $tomcatBase 'webapps\QuanLyKyTucXa.war') -Force
    $serverXml = @"
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1">
  <Service name="Catalina">
    <Connector port="$Port" address="127.0.0.1" protocol="HTTP/1.1" connectionTimeout="20000" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false" />
    </Engine>
  </Service>
</Server>
"@
    [IO.File]::WriteAllText((Join-Path $tomcatBase 'conf\server.xml'), $serverXml, [Text.UTF8Encoding]::new($false))
    $bootstrap = Join-Path $TomcatHome 'bin\bootstrap.jar'
    $loggingJar = Join-Path $TomcatHome 'bin\tomcat-juli.jar'
    Copy-Item -LiteralPath (Join-Path $TomcatHome 'conf\logging.properties') -Destination (Join-Path $tomcatBase 'conf\logging.properties') -Force
    Write-Host "Open http://localhost:$Port/QuanLyKyTucXa/"
    & java "-Dcatalina.home=$TomcatHome" "-Dcatalina.base=$tomcatBase" "-Djava.io.tmpdir=$(Join-Path $tomcatBase 'temp')" "-Djava.util.logging.config.file=$(Join-Path $tomcatBase 'conf\logging.properties')" '-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager' '-Dfile.encoding=UTF-8' '-cp' "$bootstrap;$loggingJar" 'org.apache.catalina.startup.Bootstrap' 'start'
} finally {
    if ($readPassword) { Remove-Item Env:KTX_DB_PASSWORD -ErrorAction SilentlyContinue }
}
