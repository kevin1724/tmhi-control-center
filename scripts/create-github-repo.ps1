param(
    [string]$RepoName = "tmhi-control-center",
    [ValidateSet("public", "private", "internal")]
    [string]$Visibility = "public"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git is not installed or not on PATH."
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) is not installed. Install it from https://cli.github.com/ and run: gh auth login"
}

gh auth status | Out-Null

$repoRoot = (Resolve-Path -LiteralPath ".").Path
function Invoke-SafeGit {
    git -c "safe.directory=$repoRoot" @args
    if ($LASTEXITCODE -ne 0) {
        throw "git command failed: git $args"
    }
}

if (-not (Test-Path -LiteralPath ".git")) {
    Invoke-SafeGit init -b main
}

Invoke-SafeGit add .
git -c "safe.directory=$repoRoot" diff --cached --quiet
if ($LASTEXITCODE -ne 0) {
    Invoke-SafeGit commit -m "Initial TMHI Control Center scaffold"
}

$existingOrigin = git -c "safe.directory=$repoRoot" remote get-url origin 2>$null
if ($LASTEXITCODE -eq 0 -and $existingOrigin) {
    throw "An origin remote already exists: $existingOrigin. Remove or rename it before creating the new GitHub repository."
}

$githubOwner = gh api user --jq .login
$visibilityFlag = "--$Visibility"
gh repo create $RepoName $visibilityFlag --description "Docker-hosted control center for supported T-Mobile Home Internet gateways."

Invoke-SafeGit remote add origin "https://github.com/$githubOwner/$RepoName.git"
Invoke-SafeGit push -u origin main

gh repo edit "$githubOwner/$RepoName" `
    --add-topic t-mobile `
    --add-topic tmhi `
    --add-topic home-internet `
    --add-topic control-center `
    --add-topic docker `
    --add-topic python `
    --add-topic fastapi

gh repo view "$githubOwner/$RepoName" --web
