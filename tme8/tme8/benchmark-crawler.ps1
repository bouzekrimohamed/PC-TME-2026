$ErrorActionPreference = "Stop"

$cases = @(
    @{ depth = 0; workers = 1 },
    @{ depth = 1; workers = 1 },
    @{ depth = 1; workers = 2 },
    @{ depth = 1; workers = 4 },
    @{ depth = 2; workers = 4 }
)

Write-Host "depth,workers,elapsed_ms,status"

foreach ($c in $cases) {
    $depth = $c.depth
    $workers = $c.workers

    $outFile = [System.IO.Path]::GetTempFileName()
    try {
        $p = Start-Process java -ArgumentList "-cp", "src", "pc.crawler.WebCrawlerParallel", "$depth", "$workers" `
            -NoNewWindow -PassThru -RedirectStandardOutput $outFile -RedirectStandardError $outFile

        $ok = $p.WaitForExit(180000)
        if (-not $ok) {
            try { Stop-Process -Id $p.Id -Force } catch {}
            Write-Host "$depth,$workers,,timeout"
            continue
        }

        $content = Get-Content $outFile -Raw
        $m = [regex]::Match($content, "elapsed=(\d+)\s+ms")
        if ($m.Success) {
            Write-Host "$depth,$workers,$($m.Groups[1].Value),ok"
        } else {
            Write-Host "$depth,$workers,,no_elapsed_found"
        }
    }
    finally {
        Remove-Item $outFile -ErrorAction SilentlyContinue
    }
}
