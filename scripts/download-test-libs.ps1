New-Item -ItemType Directory -Force -Path lib\test | Out-Null
New-Item -ItemType Directory -Force -Path src\lib | Out-Null

$urls = @{
    "junit-platform-console-standalone-1.10.2.jar" = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"
    "mockito-core-5.11.0.jar" = "https://repo1.maven.org/maven2/org/mockito/mockito-core/5.11.0/mockito-core-5.11.0.jar"
    "byte-buddy-1.14.13.jar" = "https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.14.13/byte-buddy-1.14.13.jar"
    "byte-buddy-agent-1.14.13.jar" = "https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy-agent/1.14.13/byte-buddy-agent-1.14.13.jar"
    "objenesis-3.3.jar" = "https://repo1.maven.org/maven2/org/objenesis/objenesis/3.3/objenesis-3.3.jar"
}

$mainUrls = @{
    "slf4j-api-2.0.13.jar"        = "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar"
    "logback-classic-1.5.6.jar"  = "https://repo1.maven.org/maven2/ch/qos/logback/logback-classic/1.5.6/logback-classic-1.5.6.jar"
    "logback-core-1.5.6.jar"     = "https://repo1.maven.org/maven2/ch/qos/logback/logback-core/1.5.6/logback-core-1.5.6.jar"
    "mysql-connector-j-8.3.0.jar" = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"
}

foreach ($name in $urls.Keys) {
    Invoke-WebRequest -Uri $urls[$name] -OutFile "lib\test\$name"
}

foreach ($name in $mainUrls.Keys) {
    Invoke-WebRequest -Uri $mainUrls[$name] -OutFile "src\lib\$name"
}

Get-ChildItem lib\test
Get-ChildItem src\lib