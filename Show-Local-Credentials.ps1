param(
    [ValidateSet('postgres', 'app', 'web')]
    [string]$Name
)

if (-not $Name) {
    Write-Output 'Choose -Name postgres, app, or web to view one account.'
    return
}

$store = Join-Path $PSScriptRoot '.local-secrets\credentials.dpapi'
if (-not (Test-Path -LiteralPath $store)) {
    throw 'Local credential file was not found.'
}

try {
    Add-Type -AssemblyName System.Security
    $encrypted = [System.IO.File]::ReadAllBytes($store)
    $plain = [System.Security.Cryptography.ProtectedData]::Unprotect(
        $encrypted,
        $null,
        [System.Security.Cryptography.DataProtectionScope]::CurrentUser
    )
    $records = [System.Text.Encoding]::UTF8.GetString($plain) | ConvertFrom-Json
    $record = $records.$Name
    Write-Output "Entry: $($record.label)"
    Write-Output "Username: $($record.username)"
    Write-Output "Password: $($record.password)"
} catch [System.Security.Cryptography.CryptographicException] {
    throw 'Cannot decrypt with this Windows account.'
} finally {
    if ($plain) {
        [System.Array]::Clear($plain, 0, $plain.Length)
    }
}
