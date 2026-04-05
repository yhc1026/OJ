# Static file server via PowerShell (no Python/Node)
param(
    [int]$Port = 8765
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$rootFull = [System.IO.Path]::GetFullPath($root).TrimEnd('\') + '\'

$mimes = @{
    '.html' = 'text/html; charset=utf-8'
    '.htm'  = 'text/html; charset=utf-8'
    '.css'  = 'text/css; charset=utf-8'
    '.js'   = 'application/javascript; charset=utf-8'
    '.json' = 'application/json; charset=utf-8'
    '.png'  = 'image/png'
    '.jpg'  = 'image/jpeg'
    '.jpeg' = 'image/jpeg'
    '.gif'  = 'image/gif'
    '.webp' = 'image/webp'
    '.ico'  = 'image/x-icon'
    '.svg'  = 'image/svg+xml'
    '.woff' = 'font/woff'
    '.woff2' = 'font/woff2'
    '.txt'  = 'text/plain; charset=utf-8'
    '.md'   = 'text/markdown; charset=utf-8'
}

function Get-SafeFilePath {
    param([string]$UrlPath)
    $decoded = [System.Uri]::UnescapeDataString($UrlPath)
    $rel = $decoded.TrimStart('/').Replace('\', '/')
    if ([string]::IsNullOrWhiteSpace($rel)) {
        return [System.IO.Path]::GetFullPath((Join-Path $root 'index.html'))
    }
    $candidate = [System.IO.Path]::GetFullPath((Join-Path $root $rel))
    if (-not $candidate.StartsWith($rootFull, [StringComparison]::OrdinalIgnoreCase) -and
        -not $candidate.Equals($rootFull.TrimEnd('\'), [StringComparison]::OrdinalIgnoreCase)) {
        return $null
    }
    return $candidate
}

$prefix = 'http://127.0.0.1:{0}/' -f $Port
$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add($prefix)

try {
    $listener.Start()
} catch {
    Write-Host ('[FAIL] Cannot listen on ' + $prefix + ' Port in use or no permission.')
    Write-Host $_.Exception.Message
    exit 1
}

Write-Host '========================================'
Write-Host (' DIR: ' + $root)
Write-Host (' URL: ' + $prefix)
Write-Host ' Press Ctrl+C to stop'
Write-Host '========================================'

Start-Sleep -Seconds 1
try {
    Start-Process $prefix
} catch {
    Write-Host (' Open in browser manually: ' + $prefix)
}

while ($listener.IsListening) {
    $ctx = $null
    try {
        $ctx = $listener.GetContext()
    } catch {
        break
    }
    $req = $ctx.Request
    $res = $ctx.Response
    $path = Get-SafeFilePath -UrlPath $req.Url.AbsolutePath

    try {
        if (-not $path) {
            $res.StatusCode = 403
            $res.ContentLength64 = 0
        }
        elseif (-not (Test-Path -LiteralPath $path)) {
            $res.StatusCode = 404
            $b404 = [System.Text.Encoding]::UTF8.GetBytes('404 Not Found')
            $res.ContentLength64 = $b404.Length
            $res.OutputStream.Write($b404, 0, $b404.Length)
        }
        else {
            $item = Get-Item -LiteralPath $path
            if ($item.PSIsContainer) {
                $idx = Join-Path $path 'index.html'
                if (Test-Path -LiteralPath $idx) {
                    $path = $idx
                    $item = Get-Item -LiteralPath $path
                } else {
                    $res.StatusCode = 403
                    $res.ContentLength64 = 0
                    $res.Close()
                    continue
                }
            }
            $bytes = [System.IO.File]::ReadAllBytes($path)
            $ext = [System.IO.Path]::GetExtension($path).ToLowerInvariant()
            $res.ContentType = if ($mimes.ContainsKey($ext)) { $mimes[$ext] } else { 'application/octet-stream' }
            $res.ContentLength64 = $bytes.Length
            $res.OutputStream.Write($bytes, 0, $bytes.Length)
        }
    } finally {
        $res.Close()
    }
}
