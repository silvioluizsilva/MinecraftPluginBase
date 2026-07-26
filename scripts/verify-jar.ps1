param(
    [Parameter(Mandatory = $false)]
    [string]$JarPath = "target/pluginbase-0.0.1.jar"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$resolved = (Resolve-Path -LiteralPath $JarPath).Path
$archive = [System.IO.Compression.ZipFile]::OpenRead($resolved)
try {
    $names = @($archive.Entries | ForEach-Object FullName)
    $required = @(
        "br/net/silvioluizsilva/pluginbase/api/PluginBaseApi.class",
        "br/net/silvioluizsilva/pluginbase/api/DatabaseHealth.class",
        "br/net/silvioluizsilva/pluginbase/libs/mysql/cj/jdbc/Driver.class",
        "META-INF/services/java.sql.Driver",
        "plugin.yml"
    )
    foreach ($entry in $required) {
        if ($entry -notin $names) {
            throw "Entrada obrigatória ausente no JAR: $entry"
        }
    }
    if ($names | Where-Object { $_ -like "org/slf4j/*" }) {
        throw "O JAR não deve empacotar SLF4J."
    }
    if ($names | Where-Object { $_ -like "com/google/protobuf/*" }) {
        throw "Protobuf não relocada foi encontrada."
    }
    $serviceEntry = $archive.GetEntry("META-INF/services/java.sql.Driver")
    $reader = [System.IO.StreamReader]::new($serviceEntry.Open())
    try {
        $driver = $reader.ReadToEnd().Trim()
    } finally {
        $reader.Dispose()
    }
    if ($driver -ne "br.net.silvioluizsilva.pluginbase.libs.mysql.cj.jdbc.Driver") {
        throw "Descritor JDBC inválido: $driver"
    }
    Write-Output "JAR validado: $resolved"
} finally {
    $archive.Dispose()
}
