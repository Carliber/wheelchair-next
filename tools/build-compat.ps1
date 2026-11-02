# Rebuild libs/wheelchair-compat.jar from src/compat/java (ported InfiniStack, MIT).
# Files whose imports are still missing from compat-libs are EXCLUDED (recorded in
# ref/compat-fail-list.txt) — drop the missing jars into compat-libs/ and rerun.
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$cache = "$env:USERPROFILE\.gradle\caches"
$fg = "$cache\minecraft\net\minecraftforge\forge\1.7.10-10.13.4.1614-1.7.10\forgeBin-1.7.10-10.13.4.1614-1.7.10.jar"
$libs = @(
  "$fg",
  "$cache\modules-2\files-2.1\org.lwjgl.lwjgl\lwjgl\2.9.1\f58c5aabcef0e41718a564be9f8e412fff8db847\lwjgl-2.9.1.jar",
  "$cache\modules-2\files-2.1\com.google.code.findbugs\jsr305\2.0.1\516c03b21d50a644d538de0f0369c620989cd8f0\jsr305-2.0.1.jar",
  "$cache\modules-2\files-2.1\org.apache.logging.log4j\log4j-api\2.0-beta9\1dd66e68cccd907880229f9e2de1314bd13ff785\log4j-api-2.0-beta9.jar",
  "$cache\modules-2\files-2.1\io.netty\netty-all\4.0.10.Final\9e50bd52ffe257a0e2cd8d971688d6ce7d174325\netty-all-4.0.10.Final.jar",
  "$root\libs\unimixins-all-1.7.10-0.2.1.jar"
) + (Get-ChildItem "$root\compat-libs\*.jar" | ForEach-Object { $_.FullName })
$cp = ($libs -join ';')
$out = "$root\build\compat-classes"
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Path $out | Out-Null
$all = Get-ChildItem "$root\src\compat\java" -Recurse -Filter *.java | ForEach-Object { $_.FullName }

function TryCompile($srcs) {
  $list = "$env:TEMP\compat-srcs.txt"
  $srcs | Set-Content -Encoding ascii $list
  & javac "-J-Duser.language=en" -proc:none -source 1.8 -target 1.8 -encoding UTF-8 -nowarn -d $out -cp $cp "@$list" 2> "$env:TEMP\compat-err.txt"
  return $LASTEXITCODE -eq 0
}

$excluded = @()
$active = $all
for ($round = 0; $round -lt 15 -and $active.Count -gt 0; $round++) {
  if (TryCompile $active) { break }
  $err = Get-Content "$env:TEMP\compat-err.txt" -Encoding UTF8
  $missing = @{}
  foreach ($line in $err) {
    if ($line -match '^([^:]+\.java):\d+: error: package ([\w.]+) does not exist') {
      $f = $Matches[1]; $pkg = $Matches[2]
      if (-not $missing.ContainsKey($pkg)) { $missing[$pkg] = @() }
      if ($missing[$pkg] -notcontains $f) { $missing[$pkg] += $f }
    }
  }
  if ($missing.Count -eq 0) {
    Write-Host "unresolvable errors (not missing packages) — showing first lines:"
    $err | Select-Object -First 12 | Write-Host
    exit 1
  }
  $drop = @()
  foreach ($pkg in $missing.Keys) { $drop += $missing[$pkg] }
  $drop = $drop | Select-Object -Unique
  Write-Host ("round {0}: {1} pkgs missing ({2}...) -> excluding {3} files" -f $round, $missing.Count, (($missing.Keys | Select-Object -First 3) -join ','), $drop.Count)
  $excluded += $drop
  $active = $active | Where-Object { $drop -notcontains $_ }
  if ($drop.Count -eq 0) { Write-Host 'stuck'; exit 1 }
}
if (-not (TryCompile $active)) {
  Write-Host 'FINAL COMPILE STILL FAILING:'
  Get-Content "$env:TEMP\compat-err.txt" -Encoding Default | Select-Object -First 12 | Write-Host
  exit 1
}
$excluded = $excluded | Select-Object -Unique
$excluded | ForEach-Object { $_.Replace("$root\", '') } | Set-Content "$root\ref\compat-fail-list.txt"
Push-Location $out
& jar cf "$root\libs\wheelchair-compat.jar" .
Pop-Location
Write-Host ("OK: {0} compiled, {1} excluded (ref/compat-fail-list.txt)" -f $active.Count, $excluded.Count)
