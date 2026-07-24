param(
    [string]$DockerHubUsername = "",
    [string]$Repository = "kevin1724/tmhi-control-center"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) is not installed. Install it from https://cli.github.com/ and run: gh auth login"
}

if (-not $DockerHubUsername) {
    $DockerHubUsername = Read-Host "Docker Hub username"
}

if (-not $DockerHubUsername) {
    throw "Docker Hub username is required."
}

$secureToken = Read-Host "Docker Hub access token" -AsSecureString
$tokenPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken)
try {
    $dockerHubToken = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($tokenPointer)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($tokenPointer)
}

if (-not $dockerHubToken) {
    throw "Docker Hub token is required."
}

gh variable set DOCKERHUB_USERNAME --repo $Repository --body $DockerHubUsername
gh secret set DOCKERHUB_TOKEN --repo $Repository --body $dockerHubToken

Write-Host "Configured GitHub Actions Docker Hub variable and secret for $Repository."
