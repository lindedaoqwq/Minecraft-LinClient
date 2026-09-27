#!/usr/bin/env bash
#
# One-shot publish script for LinClient.
#
# This script creates the GitHub repository (if missing) and pushes the current
# branch + a tag so the GitHub Actions workflow builds the jar and publishes a Release.
#
# USAGE:
#   export GH_TOKEN="github_pat_xxx"   # your Personal Access Token (contents:write)
#   ./push.sh
#
# It does NOT hard-code any secret; the token is read from the GH_TOKEN env var.
set -euo pipefail

TOKEN="${GH_TOKEN:?Please set GH_TOKEN to your GitHub PAT (e.g. export GH_TOKEN=github_pat_xxx)}"
REPO="lindedaoqwq/Minecraft-LinClient"
API="https://api.github.com"
REMOTE="https://${TOKEN}@github.com/${REPO}.git"

echo ">> Ensuring repository ${REPO} exists ..."
curl -sS -o /dev/null -w "   create repo -> HTTP %{http_code}\n" \
  -X POST -H "Authorization: Bearer ${TOKEN}" -H "Accept: application/vnd.github+json" \
  -d '{"name":"Minecraft-LinClient","description":"Practical non-cheating Minecraft client mod (Forge 1.20.1)","homepage":"https://github.com/lindedaoqwq/Minecraft-LinClient/releases/latest","private":false,"auto_init":false}' \
  "${API}/user/repos" || echo "   (repo may already exist - continuing)"

echo ">> Configuring remote ..."
git remote remove origin 2>/dev/null || true
git remote add origin "${REMOTE}"

echo ">> Pushing branch ..."
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
git push -u origin "${BRANCH}:main"

echo ">> Tagging v1.0.0 and pushing tag (triggers Release) ..."
git tag -f v1.0.0
git push origin v1.0.0

echo ""
echo "Done. Watch the build + Release here:"
echo "  https://github.com/${REPO}/releases/latest"
echo "  https://github.com/${REPO}/actions"
