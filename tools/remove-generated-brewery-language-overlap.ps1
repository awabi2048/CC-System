param(
    [string]$LanguageRoot = "src/main/resources/lang"
)

$ErrorActionPreference = 'Stop'
$utf8 = [Text.UTF8Encoding]::new($false)

foreach ($locale in @('ja_jp', 'en_us')) {
    $path = Join-Path $LanguageRoot "$locale/content/brewery.yml"
    $text = [IO.File]::ReadAllText((Join-Path (Get-Location) $path), [Text.Encoding]::UTF8)
    $updated = [regex]::Replace(
        $text,
        '(?ms)^  garden:\r?\n.*?(?=^  item:)',
        ''
    )
    if ($updated -eq $text) {
        throw "garden/recipe section was not found: $path"
    }
    [IO.File]::WriteAllText((Join-Path (Get-Location) $path), $updated, $utf8)
}
