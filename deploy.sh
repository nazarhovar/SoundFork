#!/usr/bin/env bash
set -euo pipefail

# ─── SoundFork Deployment Script ───────────────────────────
# Prerequisites: Docker, Docker Compose, git
#
# Usage:
#   chmod +x deploy.sh
#   ./deploy.sh
#
# Then set up HTTPS:
#   sudo apt install certbot python3-certbot-nginx
#   sudo certbot --nginx -d your-domain.com
# ───────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "🚀 SoundFork Deployment"
echo "────────────────────────────"

# 1. Copy .env if missing
if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "⚠️  Created .env from .env.example — edit it before deploying!"
        echo "   nano .env"
        exit 1
    else
        echo "❌ No .env or .env.example found"
        exit 1
    fi
fi

# 2. Pull latest code
if git rev-parse --git-dir > /dev/null 2>&1; then
    echo "📦 Pulling latest changes..."
    git pull
fi

# 3. Build and start
echo "🐳 Building and starting containers..."
docker compose up -d --build

# 4. Wait for health
echo "⏳ Waiting for services to start..."
sleep 15

# 5. Check health
echo "🏥 Checking health..."
if curl -sf http://localhost:8080 > /dev/null 2>&1; then
    echo "✅ SoundFork is running at http://localhost:8080"
else
    echo "⚠️  SoundFork may still be starting — check with: docker compose logs soundfork"
fi

echo "📋 Useful commands:"
echo "   docker compose logs -f soundfork    # Follow app logs"
echo "   docker compose ps                   # Check all containers"
echo "   docker compose down                 # Stop everything"
