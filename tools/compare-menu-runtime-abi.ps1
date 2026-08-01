param(
    [Parameter(Mandatory = $true)][string]$OldClasses,
    [Parameter(Mandatory = $true)][string]$NewClasses
)
$classes = @(
    'com.awabi2048.ccsystem.api.gui.MenuInteraction$Action',
    'com.awabi2048.ccsystem.api.gui.MenuInteraction$Capability',
    'com.awabi2048.ccsystem.api.gui.MenuInteraction$Branches',
    'com.awabi2048.ccsystem.api.gui.MenuInteraction$ClickBranches',
    'com.awabi2048.ccsystem.api.gui.MenuActionBranch'
)
$failed = $false
foreach ($class in $classes) {
    $old = & javap -classpath $OldClasses -public -s $class | Where-Object { $_ -match '^  public|^    descriptor:' }
    $new = & javap -classpath $NewClasses -public -s $class | Where-Object { $_ -match '^  public|^    descriptor:' }
    $missing = Compare-Object $old $new | Where-Object SideIndicator -eq '<='
    if ($missing) {
        $failed = $true
        Write-Error "Missing public ABI in $class`n$($missing.InputObject -join "`n")"
    }
}
if ($failed) { exit 1 }
