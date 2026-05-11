#!/bin/bash

# --- Configuration ---
SDK_DIR="$HOME/Android/Sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
JAVA_PACKAGE="openjdk-17-jdk"

echo "🚀 Starting Android SDK installation on Linux Mint..."

# 1. Update system and install Java
echo "📦 Updating system and installing $JAVA_PACKAGE..."
sudo apt update && sudo apt install -y $JAVA_PACKAGE wget unzip

# 2. Create directory structure
echo "📂 Creating SDK directory at $SDK_DIR"
mkdir -p "$SDK_DIR/cmdline-tools"

# 3. Download and Extract Command Line Tools
echo "📥 Downloading Android Command Line Tools..."
wget -q --show-progress "$CMDLINE_TOOLS_URL" -O /tmp/cmdline-tools.zip

echo "📂 Extracting..."
unzip -q /tmp/cmdline-tools.zip -d "$SDK_DIR/cmdline-tools"
# The zip extracts into a folder named 'cmdline-tools'. 
# Google requires it to be in a subfolder named 'latest' to function properly.
mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"

# 4. Set Environment Variables
echo "🔧 Configuring environment variables..."
SHELL_CONFIG="$HOME/.bashrc"
[[ $SHELL == *"zsh"* ]] && SHELL_CONFIG="$HOME/.zshrc"

cat << EOF >> "$SHELL_CONFIG"

# Android SDK
export ANDROID_HOME=$SDK_DIR
export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=\$PATH:\$ANDROID_HOME/platform-tools
export PATH=\$PATH:\$ANDROID_HOME/emulator
EOF

export ANDROID_HOME=$SDK_DIR
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# 5. Accept Licenses and install essential components
echo "📜 Accepting licenses..."
yes | sdkmanager --licenses

echo "🛠️ Installing platform-tools and build-tools..."
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "✅ Installation complete!"
echo "👉 Please run 'source $SHELL_CONFIG' or restart your terminal."
