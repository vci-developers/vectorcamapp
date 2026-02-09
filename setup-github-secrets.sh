#!/bin/bash

# GitHub Actions Setup Helper Script
# This script helps you set up GitHub repository secrets for CI/CD

set -e

echo "🚀 VectorCam GitHub Actions Setup Helper"
echo "========================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo -e "${RED}Error: GitHub CLI (gh) is not installed.${NC}"
    echo "Please install it from: https://cli.github.com/"
    echo ""
    echo "macOS: brew install gh"
    echo "Ubuntu: sudo apt install gh"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo -e "${YELLOW}Not authenticated with GitHub CLI${NC}"
    echo "Running: gh auth login"
    gh auth login
fi

echo -e "${GREEN}✓ GitHub CLI authenticated${NC}"
echo ""

# Check for secrets.properties
if [ ! -f "secrets.properties" ]; then
    echo -e "${RED}Error: secrets.properties not found${NC}"
    echo "Please create secrets.properties with your API keys first."
    exit 1
fi

echo -e "${GREEN}✓ Found secrets.properties${NC}"
echo ""

# Read secrets.properties
echo "📖 Reading secrets from secrets.properties..."
source secrets.properties

# Function to set secret
set_secret() {
    local secret_name=$1
    local secret_value=$2
    
    if [ -z "$secret_value" ]; then
        echo -e "${YELLOW}⚠ Skipping $secret_name (empty value)${NC}"
        return
    fi
    
    echo "Setting $secret_name..."
    echo "$secret_value" | gh secret set "$secret_name"
    echo -e "${GREEN}✓ $secret_name set${NC}"
}

# Set secrets
echo ""
echo "🔐 Setting GitHub repository secrets..."
echo ""

set_secret "POSTHOG_API_KEY" "$POSTHOG_API_KEY"
set_secret "POSTHOG_HOST" "$POSTHOG_HOST"
set_secret "DEBUG_VECTORCAM_API_KEY" "$DEBUG_VECTORCAM_API_KEY"
set_secret "RELEASE_VECTORCAM_API_KEY" "$RELEASE_VECTORCAM_API_KEY"

# Firebase (optional)
echo ""
echo "🔥 Firebase App Distribution Setup (Optional)"
echo ""
read -p "Do you want to set up Firebase App Distribution? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    read -p "Enter Firebase App ID: " FIREBASE_APP_ID
    set_secret "FIREBASE_APP_ID" "$FIREBASE_APP_ID"
    
    echo ""
    read -p "Path to Firebase service account JSON file: " FIREBASE_JSON_PATH
    if [ -f "$FIREBASE_JSON_PATH" ]; then
        FIREBASE_SERVICE_ACCOUNT=$(cat "$FIREBASE_JSON_PATH")
        set_secret "FIREBASE_SERVICE_ACCOUNT" "$FIREBASE_SERVICE_ACCOUNT"
    else
        echo -e "${RED}File not found: $FIREBASE_JSON_PATH${NC}"
    fi
fi

# Keystore (optional)
echo ""
echo "🔑 Keystore Setup (Optional)"
echo ""
read -p "Do you have a keystore for signing? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    read -p "Path to keystore file (app/keystore.jks): " KEYSTORE_PATH
    KEYSTORE_PATH=${KEYSTORE_PATH:-app/keystore.jks}
    
    if [ -f "$KEYSTORE_PATH" ]; then
        KEYSTORE_BASE64=$(base64 -i "$KEYSTORE_PATH")
        set_secret "KEYSTORE_BASE64" "$KEYSTORE_BASE64"
        
        read -p "Keystore password: " -s KEYSTORE_PASSWORD
        echo
        set_secret "KEYSTORE_PASSWORD" "$KEYSTORE_PASSWORD"
        
        read -p "Key alias: " KEY_ALIAS
        set_secret "KEY_ALIAS" "$KEY_ALIAS"
        
        read -p "Key password: " -s KEY_PASSWORD
        echo
        set_secret "KEY_PASSWORD" "$KEY_PASSWORD"
    else
        echo -e "${RED}File not found: $KEYSTORE_PATH${NC}"
    fi
fi

echo ""
echo "======================================"
echo -e "${GREEN}✓ Setup complete!${NC}"
echo ""
echo "📋 Next steps:"
echo "1. Push code to trigger workflows"
echo "2. Go to Actions tab to see builds"
echo "3. Download APKs from Artifacts"
echo ""
echo "📚 Documentation:"
echo "- GITHUB_ACTIONS_SETUP.md - Full setup guide"
echo "- .github/workflows/ - Workflow files"
echo ""
echo "🎉 Happy building!"
