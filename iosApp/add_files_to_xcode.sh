#!/bin/bash

# Script to verify all Swift files are in place
# Run this to see what files need to be added to Xcode

echo "🔍 Checking Swift files structure..."
echo ""

check_dir() {
    local dir=$1
    if [ -d "$dir" ]; then
        echo "✅ $dir exists"
        ls -1 "$dir"/*.swift 2>/dev/null | wc -l | xargs echo "   Files:"
    else
        echo "❌ $dir missing"
    fi
}

cd "$(dirname "$0")/iosApp"

echo "Core Architecture:"
check_dir "Core"
echo ""

echo "Theme System:"
check_dir "Theme"
echo ""

echo "Components:"
check_dir "Components"
echo ""

echo "Features:"
check_dir "Features/Home"
check_dir "Features/Send"
check_dir "Features/Receive"
check_dir "Features/History"
check_dir "Features/Profile"
echo ""

echo "Utilities:"
check_dir "Utilities"
echo ""

echo "=========================================="
echo "Next steps:"
echo "1. Open Xcode: open iosApp.xcodeproj"
echo "2. Right-click 'iosApp' folder → 'Add Files to iosApp...'"
echo "3. Select each folder above and add them"
echo "4. Make sure 'Add to targets: iosApp' is checked"
echo "=========================================="
