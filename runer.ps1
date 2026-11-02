[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Remove-Item Env:\_JAVA_OPTIONS -ErrorAction SilentlyContinue

$threads = @(1, 2, 3, 4, 6, 8, 12, 16)
$sizes = @(1500, 2500, 3500)

$csvFile = "results_java.csv"
"Task,Size,Threads,Mode,Time_sec" | Out-File -FilePath $csvFile -Encoding utf8

function Run-With-Averaging {
    param ($cmd, $taskName, $size, $t, $mode)
    
    $times = @()
    for ($i = 1; $i -le 5; $i++) {
        # Запускаем команду и переводим вывод в массив строк
        $lines = @(Invoke-Expression $cmd)
        $foundTime = $null

        foreach ($line in $lines) {
            if ($line -match "([0-9]+\.[0-9]+)\s*(?:сек|sec)") {
                $foundTime = [double]$matches[1]
                break
            }
            elseif ($line -match ":\s*([0-9]+\.[0-9]+)") {
                $foundTime = [double]$matches[1]
            }
        }

        if ($null -ne $foundTime) {
            $times += $foundTime
            Write-Host "  [$taskName N=$size T=$t] Прогон $i/5: $foundTime сек." -ForegroundColor Gray
        } else {
            # Если не распознало, выведем реальный сырой текст строки для диагностики
            $raw = ($lines -join ' ')
            Write-Host "  [$taskName N=$size T=$t] Прогон $i/5: ошибка! Вывод был: $raw" -ForegroundColor Yellow
        }
    }

    if ($times.Count -ge 3) {
        $sorted = $times | Sort-Object
        if ($sorted.Count -eq 5) {
            $avg = ($sorted[1] + $sorted[2] + $sorted[3]) / 3.0
        } else {
            $measure = $sorted | Measure-Object -Average
            $avg = $measure.Average
        }
        $avgRound = [math]::Round($avg, 4)
        Write-Host ">>> [$taskName] N=$size, T=$t, Mode=$mode -> Среднее: $avgRound сек.`n" -ForegroundColor Green
        "$taskName,$size,$t,$mode,$avgRound" | Out-File -FilePath $csvFile -Append -Encoding utf8
    }
}

Write-Host "=== 1. ЗАМЕРЫ MATMUL ===" -ForegroundColor Cyan
foreach ($n in $sizes) {
    foreach ($t in $threads) {
        $cmd = "java -Xmx4g Matmul $n $n $n $t"
        Run-With-Averaging -cmd $cmd -taskName "Matmul" -size $n -t $t -mode "none"
    }
}

Write-Host "=== 2. ЗАМЕРЫ TRIANGLE ===" -ForegroundColor Cyan
$n_triangle = 2500

foreach ($t in $threads) {
    $cmd_stat = 'cmd.exe /c "java -Xmx4g Generator ' + $n_triangle + ' ' + $n_triangle + ' | java -Xmx4g ComputeTriangle ' + $t + ' static"'
    Run-With-Averaging -cmd $cmd_stat -taskName "Triangle" -size $n_triangle -t $t -mode "static"

    $cmd_dyn = 'cmd.exe /c "java -Xmx4g Generator ' + $n_triangle + ' ' + $n_triangle + ' | java -Xmx4g ComputeTriangle ' + $t + ' dynamic 4"'
    Run-With-Averaging -cmd $cmd_dyn -taskName "Triangle" -size $n_triangle -t $t -mode "dynamic"
}

Write-Host "=== 3. ЗАМЕРЫ MATPOWER ===" -ForegroundColor Cyan
$n_pow = 1200
$p = 5
foreach ($t in $threads) {
    $cmd_pow = "java -Xmx4g Matpower $n_pow $p $t"
    Run-With-Averaging -cmd $cmd_pow -taskName "Power" -size "$n_pow^$p" -t $t -mode "barrier"
}

Write-Host "Все замеры завершены! Файл сохранен: $csvFile" -ForegroundColor Cyan
