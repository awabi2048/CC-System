param([string]$Spec = "../.docs/planning/active-review/cc_content_unified_complete_implementation_spec_2026-07-21.md")
$ErrorActionPreference = 'Stop'
$lines = Get-Content -Encoding utf8 -LiteralPath $Spec
function Between($a,$b) { $i=[Array]::IndexOf($lines,$a); $j=[Array]::IndexOf($lines,$b); $lines[($i+1)..($j-1)] }
function Rows($section,$n) { $section | ? {$_ -match '^\| `'} | % {$p=$_.Trim('|').Split('|')|%{$_.Trim()}; if($p.Count-ge $n){,$p}} }
function T($s) { ([regex]::Match($s,'`([^`]+)`')).Groups[1].Value }

$names=[ordered]@{}
$descriptions=[ordered]@{}
foreach($row in Rows (Between '# 21. 中間材料完全定義' '# 22. 完成料理完全定義') 2){$names[(T $row[0]).Replace('cooking.','')]=$row[1]}
foreach($heading in @('## 22.2 BASIC','## 22.3 INTERMEDIATE','## 22.4 ADVANCED','## 22.5 TOP')){
  $next=@{'## 22.2 BASIC'='## 22.3 INTERMEDIATE';'## 22.3 INTERMEDIATE'='## 22.4 ADVANCED';'## 22.4 ADVANCED'='## 22.5 TOP';'## 22.5 TOP'='## 22.6 説明文'}[$heading]
  foreach($row in Rows (Between $heading $next) 2){$names[(T $row[0])]=$row[1]}
}
foreach($row in Rows (Between '## 22.6 説明文' '# 23. 旧非酒類11件の移行') 2){$descriptions[(T $row[0])]=$row[1]}
foreach($row in Rows (Between '# 24. かまど・燻製器完全定義' '# 25. 火力不一致の共通失敗成果物') 10){$names[(T $row[0])]=$row[1];$descriptions[(T $row[0])]=$row[9]}

$fixed=[ordered]@{knife='包丁';frying_pan='フライパン';burnt_solid_food='焦げた料理';burnt_bowl_food='焦げた汁物';burnt_bottle_liquid='焦げた飲み物';undercooked_solid_food='生焼けの料理';undercooked_bowl_food='加熱不足の汁物';underprepared_bottle_liquid='抽出不足の飲み物'}
foreach($p in $fixed.GetEnumerator()){$names[$p.Key]=$p.Value}
$c=[Text.StringBuilder]::new()
[void]$c.AppendLine('cooking:')
[void]$c.AppendLine('  ui:')
[void]$c.AppendLine('    title:')
[void]$c.AppendLine('      cutting: "まな板"')
[void]$c.AppendLine('      pan: "フライパン調理"')
[void]$c.AppendLine('      cauldron: "大釜調理"')
foreach($line in @('    close: "閉じる"','    info: "料理情報"','    start: "開始"','    cancel: "取消"','    state: "状態"','    heat: "火力"','    liquid_result: "液体成果物"')){[void]$c.AppendLine($line)}
[void]$c.AppendLine('  process:')
[void]$c.AppendLine('    started: "§a調理を開始しました。"')
[void]$c.AppendLine('    completed: "§a処理が完了しました。"')
[void]$c.AppendLine('    cancelled: "§e処理を取り消しました。"')
[void]$c.AppendLine('  error:')
foreach($line in @('    no_heat: "§c有効な火力がありません。"','    no_pan: "§c開始時のフライパンを持ってください。"','    in_use: "§cこの設備は使用中です。"','    recipe_not_found: "§c一致するレシピがありません。"','    ambiguous_recipe: "§c候補を一意に決定できません。"','    tier_locked: "§cこのレシピは未解放です。"','    container_required: "§c正しい空容器が必要です。"')){[void]$c.AppendLine($line)}
[void]$c.AppendLine('  recipe:')
foreach($p in $names.GetEnumerator()) {[void]$c.AppendLine("    $($p.Key): `"$($p.Value)`"")}
[void]$c.AppendLine('  catalog:')
[void]$c.AppendLine('    title: "料理図鑑 {page}/{pages}"')
[void]$c.AppendLine('custom_items:')
[void]$c.AppendLine('  cooking:')
foreach($p in $names.GetEnumerator()) {
  [void]$c.AppendLine("    $($p.Key):")
  [void]$c.AppendLine("      name: `"$($p.Value)`"")
  $d=if($descriptions[$p.Key]){$descriptions[$p.Key]}else{'料理や醸造に使用する加工品。'}
  [void]$c.AppendLine("      description: `"$d`"")
}

$brewNames=[ordered]@{}
foreach($row in Rows (Between '# 34. 完成酒名・モデル' '# 35. 醸造PotionEffect') 2){$brewNames[(T $row[0])]=$row[1]}
$prepByOutput=[ordered]@{}
foreach($row in Rows (Between '# 33. 醸造family完全定義' '# 34. 完成酒名・モデル') 3){$prepByOutput[(T $row[1])]=(T $row[2])}
$b=[Text.StringBuilder]::new()
[void]$b.AppendLine('brewery:')
[void]$b.AppendLine('  preparation:')
foreach($p in $prepByOutput.GetEnumerator()) {[void]$b.AppendLine("    $($p.Key):");[void]$b.AppendLine("      name: `"$($brewNames[$p.Value])の原液`"")}
[void]$b.AppendLine('  recipe:')
foreach($p in $brewNames.GetEnumerator()) {
 [void]$b.AppendLine("    $($p.Key):");[void]$b.AppendLine("      name: `"$($p.Value)`"")
 [void]$b.AppendLine('      final:')
 foreach($tier in @('low','standard','high')){[void]$b.AppendLine("        ${tier}:");[void]$b.AppendLine("          name: `"$($p.Value)`"");[void]$b.AppendLine('          description: "発酵、蒸留、熟成の必要工程を経て完成した醸造品。"')}
}

$jaCooking='src/main/resources/lang/ja_jp/content/cooking_generated.yml'; $jaBrew='src/main/resources/lang/ja_jp/content/brewery_generated.yml'
[IO.File]::WriteAllText($jaCooking,$c.ToString(),[Text.UTF8Encoding]::new($false));[IO.File]::WriteAllText($jaBrew,$b.ToString(),[Text.UTF8Encoding]::new($false))
$englishCooking=$c.ToString(); foreach($p in $names.GetEnumerator()){$englishCooking=$englishCooking.Replace($p.Value,$p.Key.Replace('_',' '))}
$englishBrew=$b.ToString(); foreach($p in $brewNames.GetEnumerator()){$englishBrew=$englishBrew.Replace($p.Value,$p.Key.Replace('_',' '))}
[IO.File]::WriteAllText('src/main/resources/lang/en_us/content/cooking_generated.yml',$englishCooking,[Text.UTF8Encoding]::new($false));[IO.File]::WriteAllText('src/main/resources/lang/en_us/content/brewery_generated.yml',$englishBrew,[Text.UTF8Encoding]::new($false))
