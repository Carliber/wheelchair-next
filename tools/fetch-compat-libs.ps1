param([string]$Out = "$PSScriptRoot\..\compat-libs")
$ErrorActionPreference = 'Continue'
$env:HTTP_PROXY = $null; $env:HTTPS_PROXY = $null
$mods = @(
  @{r='GTNewHorizons/AFSU';                   t='1.3.1-GTNH'},
  @{r='GTNewHorizons/CarpentersBlocks';       t='3.7.0-GTNH'},
  @{r='GTNewHorizons/CookingForBlockheads';   t='1.4.4-GTNH'},
  @{r='GTNewHorizons/AsieLib';                t='0.7.0'},
  @{r='GTNewHorizons/ArchitectureCraft';      t='1.11.6'},
  @{r='CleanroomMC/BogoSorter';               t=''},
  @{r='WayOfTime/BloodArsenal';               t='1.4.10'},
  @{r='tuhljin/Automagy';                     t='0.28.2'}
)
$failed = @()
foreach ($m in $mods) {
  if (-not $m.r) { continue }
  $tag = if ($m.t) { $m.t } else { (gh release list -R $m.r --limit 1 | Select-Object -First 1) -split "`t" | Select-Object -First 1 }
  if (-not $tag) { $failed += "$($m.r) (no release)"; continue }
  Write-Host ">> $($m.r) @$tag"
  gh release download $tag -R $m.r --pattern '*.jar' --dir $Out --clobber 2>&1 | Out-Null
  if ($LASTEXITCODE -ne 0) { $failed += "$($m.r)@$tag" }
}
Write-Host '--- failed ---'
$failed | ForEach-Object { Write-Host $_ }
