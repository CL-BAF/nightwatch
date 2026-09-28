# From the project root. Downloads only the official Fabric example's Gradle wrapper.
$ErrorActionPreference = 'Stop'
$archive = Join-Path $env:TEMP 'fabric-example-mod-26.3.zip'
$unpacked = Join-Path $env:TEMP 'nightwatch-fabric-template'
Invoke-WebRequest 'https://github.com/FabricMC/fabric-example-mod/archive/refs/heads/26.3.zip' -OutFile $archive
if (Test-Path $unpacked) { Remove-Item $unpacked -Recurse -Force }
Expand-Archive $archive -DestinationPath $unpacked
$template = Join-Path $unpacked 'fabric-example-mod-26.3'
Copy-Item (Join-Path $template 'gradlew.bat') . -Force
Copy-Item (Join-Path $template 'gradlew') . -Force
New-Item -ItemType Directory -Path 'gradle' -Force | Out-Null
Copy-Item (Join-Path $template 'gradle/wrapper') 'gradle/' -Recurse -Force
Write-Host 'Gradle wrapper installed. Check java -version (Java 25), then run .\gradlew.bat build'
