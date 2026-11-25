param(
    [long]$Trajectories = 1000000,
    [int]$Workers = [Environment]::ProcessorCount,
    [int]$Repetitions = 3,
    [long]$Seed = 42
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$classes = Join-Path $repoRoot 'target/classes'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sources = Get-ChildItem -LiteralPath (Join-Path $repoRoot 'src/main/java') -File -Filter '*.java' -Recurse |
    ForEach-Object FullName

& javac --release 17 -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& java -cp $classes br.uneb.montecarlo.BenchmarkMain $Trajectories $Workers $Repetitions $Seed
exit $LASTEXITCODE
