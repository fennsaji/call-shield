#!/bin/bash

# CallGuard Android Local Development Script
# Builds the debug APK with env-injected BuildConfig fields, installs it on a
# connected device/emulator, and launches the app.
#
# Usage: ./run_android.sh [env-file] [device-id]
# Default env file: android/local.properties
#
# Examples:
#   ./run_android.sh                              # Uses android/local.properties, auto-selects device
#   ./run_android.sh android/local.properties.dev # Uses alternate env file
#   ./run_android.sh android/local.properties emulator-5554  # Target specific device

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_DIR="$SCRIPT_DIR/android"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ── Arguments ──────────────────────────────────────────────────────────────────
ENV_FILE_ARG="${1:-}"
DEVICE_ID_ARG="${2:-}"

if [[ -z "$ENV_FILE_ARG" ]]; then
    ENV_FILE="$ANDROID_DIR/local.properties"
elif [[ "$ENV_FILE_ARG" = /* ]]; then
    ENV_FILE="$ENV_FILE_ARG"
else
    ENV_FILE="$SCRIPT_DIR/$ENV_FILE_ARG"
fi

echo -e "${BLUE}🛡️  CallGuard Android — Local Development Build${NC}"
echo -e "${BLUE}📄 Env file: ${ENV_FILE}${NC}"

# ── Validate env file ──────────────────────────────────────────────────────────
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}❌ Environment file not found: $ENV_FILE${NC}"
    echo ""
    echo "Copy the example and fill in your values:"
    echo "  cp android/local.properties.example android/local.properties"
    echo "  # then edit android/local.properties"
    exit 1
fi

# ── Load env vars (key=value format, skip comments and sdk.dir) ────────────────
load_prop() {
    local key="$1"
    grep "^${key}=" "$ENV_FILE" 2>/dev/null | tail -1 | cut -d'=' -f2- | tr -d '\r'
}

SUPABASE_URL=$(load_prop "SUPABASE_URL")
SUPABASE_ANON_KEY=$(load_prop "SUPABASE_ANON_KEY")
HMAC_SALT=$(load_prop "HMAC_SALT")

# ── Validate required vars ─────────────────────────────────────────────────────
MISSING_VARS=""
[ -z "$SUPABASE_URL" ]      && MISSING_VARS="${MISSING_VARS}  - SUPABASE_URL\n"
[ -z "$SUPABASE_ANON_KEY" ] && MISSING_VARS="${MISSING_VARS}  - SUPABASE_ANON_KEY\n"

if [ -n "$MISSING_VARS" ]; then
    echo -e "${RED}❌ Missing required properties in ${ENV_FILE}:${NC}"
    echo -e "$MISSING_VARS"
    echo "Required: SUPABASE_URL, SUPABASE_ANON_KEY"
    exit 1
fi

echo -e "${GREEN}✅ Properties loaded${NC}"
echo -e "  SUPABASE_URL:      ${SUPABASE_URL}"
echo -e "  SUPABASE_ANON_KEY: ${SUPABASE_ANON_KEY:0:20}..."
if [ -n "$HMAC_SALT" ]; then
    echo -e "  HMAC_SALT:         ${HMAC_SALT}"
else
    echo -e "  HMAC_SALT:         ${YELLOW}(using default: callguard-v1-salt-2024)${NC}"
fi

# ── Detect Android SDK ─────────────────────────────────────────────────────────
SDK_DIR=$(load_prop "sdk.dir")

if [ -z "$ANDROID_HOME" ]; then
    if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR" ]; then
        export ANDROID_HOME="$SDK_DIR"
        echo -e "\n${GREEN}✅ Android SDK from local.properties: ${ANDROID_HOME}${NC}"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        echo -e "\n${YELLOW}⚙️  Auto-detected Android SDK: ${ANDROID_HOME}${NC}"
    elif [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
        echo -e "\n${YELLOW}⚙️  Auto-detected Android SDK: ${ANDROID_HOME}${NC}"
    fi
fi

if [ -n "$ANDROID_HOME" ]; then
    export PATH="$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$PATH"
fi

# Ensure JAVA_HOME is set — required by gradlew
# Always prefer Android Studio's bundled JDK to avoid version mismatches
AS_JDK="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
HOMEBREW_JDK17="/opt/homebrew/opt/openjdk@17"
SYSTEM_JDK="/Library/Java/JavaVirtualMachines"
if [ -d "$AS_JDK" ]; then
    export JAVA_HOME="$AS_JDK"
elif [ -d "$HOMEBREW_JDK17" ]; then
    export JAVA_HOME="$HOMEBREW_JDK17"
elif ls "$SYSTEM_JDK" 2>/dev/null | grep -q "17\|21"; then
    export JAVA_HOME="$SYSTEM_JDK/$(ls "$SYSTEM_JDK" | grep "17\|21" | head -1)/Contents/Home"
elif [ -z "$JAVA_HOME" ]; then
    echo -e "${RED}❌ Java not found. Install via: brew install openjdk${NC}"
    exit 1
fi
if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
    echo -e "${GREEN}✅ JAVA_HOME: ${JAVA_HOME}${NC}"
fi

if ! command -v adb &>/dev/null; then
    echo -e "${RED}❌ adb not found. Ensure Android SDK platform-tools are installed.${NC}"
    echo "  Set ANDROID_HOME or add sdk.dir to ${ENV_FILE}"
    exit 1
fi

echo -e "${GREEN}✅ Android SDK: ${ANDROID_HOME}${NC}"

# ── Helpers ────────────────────────────────────────────────────────────────────
is_emulator() { [[ "$1" == emulator-* ]] && return 0 || return 1; }

device_name() {
    local id="$1"
    if is_emulator "$id"; then
        adb -s "$id" emu avd name 2>/dev/null | head -1 | tr -d '\r' || echo "Android Emulator"
    else
        adb -s "$id" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "Physical Device"
    fi
}

# ── Device detection ───────────────────────────────────────────────────────────
echo -e "\n${BLUE}🔍 Detecting Android devices...${NC}"

ADB_DEVICES=$(adb devices 2>/dev/null | grep -v "List of devices" | grep "device$" | awk '{print $1}')

if [ -z "$ADB_DEVICES" ]; then
    echo -e "${YELLOW}📱 No connected devices found. Checking available emulators...${NC}"

    if ! command -v emulator &>/dev/null; then
        echo -e "${RED}❌ No devices connected and emulator command not found.${NC}"
        echo "Connect a device or install Android SDK emulator support."
        exit 1
    fi

    AVAILABLE_EMULATORS=$(emulator -list-avds 2>/dev/null)
    if [ -z "$AVAILABLE_EMULATORS" ]; then
        echo -e "${RED}❌ No connected devices and no AVDs found.${NC}"
        echo "Create an emulator in Android Studio or via avdmanager."
        exit 1
    fi

    echo -e "${CYAN}Available emulators:${NC}"
    idx=1
    declare -a EMULATOR_ARRAY
    while IFS= read -r avd; do
        echo -e "  ${GREEN}${idx})${NC} $avd"
        EMULATOR_ARRAY[$idx]="$avd"
        ((idx++))
    done <<< "$AVAILABLE_EMULATORS"

    echo ""
    read -p "Select emulator to start (1-$((idx-1))): " selection
    if [ -z "$selection" ] || [ "$selection" -lt 1 ] || [ "$selection" -ge "$idx" ]; then
        echo -e "${RED}❌ Invalid selection${NC}"; exit 1
    fi

    echo -e "${YELLOW}📱 Starting emulator: ${EMULATOR_ARRAY[$selection]}${NC}"
    emulator -avd "${EMULATOR_ARRAY[$selection]}" -no-snapshot-load >/dev/null 2>&1 &

    echo -e "${BLUE}⏳ Waiting for emulator to boot...${NC}"
    adb wait-for-device
    while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
        sleep 2
    done
    echo -e "${GREEN}✅ Emulator booted${NC}"

    DEVICE_ID=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}' | head -1)
else
    DEVICE_COUNT=$(echo "$ADB_DEVICES" | wc -l | tr -d ' ')

    if [ -n "$DEVICE_ID_ARG" ]; then
        if echo "$ADB_DEVICES" | grep -q "^${DEVICE_ID_ARG}$"; then
            DEVICE_ID="$DEVICE_ID_ARG"
            echo -e "${GREEN}✅ Using specified device: ${DEVICE_ID}${NC}"
        else
            echo -e "${RED}❌ Device '$DEVICE_ID_ARG' not found.${NC}"
            echo "Connected devices:"; echo "$ADB_DEVICES"; exit 1
        fi
    elif [ "$DEVICE_COUNT" -eq 1 ]; then
        DEVICE_ID=$(echo "$ADB_DEVICES" | head -1)
        NAME=$(device_name "$DEVICE_ID")
        TYPE=$(is_emulator "$DEVICE_ID" && echo "Emulator" || echo "Physical Device")
        echo -e "${GREEN}✅ Auto-selected: ${NAME} (${DEVICE_ID}) — ${TYPE}${NC}"
    else
        echo -e "${CYAN}📱 Multiple devices found. Select one:${NC}"
        echo ""
        idx=1
        declare -a DEVICE_ID_ARRAY
        while IFS= read -r dev; do
            NAME=$(device_name "$dev")
            TYPE=$(is_emulator "$dev" && echo "Emulator" || echo "Physical Device")
            echo -e "  ${GREEN}${idx})${NC} ${NAME}"
            echo -e "     ${BLUE}ID:${NC}   ${dev}"
            echo -e "     ${BLUE}Type:${NC} ${TYPE}"
            echo ""
            DEVICE_ID_ARRAY[$idx]="$dev"
            ((idx++))
        done <<< "$ADB_DEVICES"

        read -p "Select device (1-$((idx-1))): " selection
        if [ -z "$selection" ] || [ "$selection" -lt 1 ] || [ "$selection" -ge "$idx" ]; then
            echo -e "${RED}❌ Invalid selection${NC}"; exit 1
        fi

        DEVICE_ID="${DEVICE_ID_ARRAY[$selection]}"
        NAME=$(device_name "$DEVICE_ID")
        TYPE=$(is_emulator "$DEVICE_ID" && echo "Emulator" || echo "Physical Device")
        echo -e "${GREEN}✅ Selected: ${NAME} (${DEVICE_ID}) — ${TYPE}${NC}"
    fi
fi

# Ensure emulator is fully booted before building
if is_emulator "$DEVICE_ID"; then
    echo -e "${BLUE}⏳ Ensuring emulator is fully booted...${NC}"
    while [ "$(adb -s "$DEVICE_ID" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
        sleep 2
    done
fi

echo -e "${GREEN}✅ Device ready: ${DEVICE_ID}${NC}"

# ── Supabase connectivity check ────────────────────────────────────────────────
CHECK_URL="$SUPABASE_URL"
if [[ "$SUPABASE_URL" == *"10.0.2.2"* ]]; then
    CHECK_URL="${SUPABASE_URL//10.0.2.2/127.0.0.1}"
    echo -e "\n${BLUE}🔗 Checking Supabase (${SUPABASE_URL} → testing via 127.0.0.1)...${NC}"
else
    echo -e "\n${BLUE}🔗 Checking Supabase: ${SUPABASE_URL}${NC}"
fi

if ! curl -s --connect-timeout 5 --max-time 10 "${CHECK_URL}/rest/v1/" >/dev/null 2>&1; then
    echo -e "${RED}❌ Supabase is not accessible at ${SUPABASE_URL}${NC}"
    echo "  • For local backend: run ./run_backend.sh first"
    echo "  • For emulator targeting host: use SUPABASE_URL=http://10.0.2.2:54321"
    echo "  • For remote Supabase: verify SUPABASE_URL in ${ENV_FILE}"
    exit 1
fi
echo -e "${GREEN}✅ Supabase accessible${NC}"

# ── Build & install ────────────────────────────────────────────────────────────
echo -e "\n${BLUE}🔨 Building debug APK...${NC}"

# Compose Gradle -P flags
GRADLE_PROPS="-PSUPABASE_URL=${SUPABASE_URL} -PSUPABASE_ANON_KEY=${SUPABASE_ANON_KEY}"
if [ -n "$HMAC_SALT" ]; then
    GRADLE_PROPS="$GRADLE_PROPS -PHMAC_SALT=${HMAC_SALT}"
fi

cd "$ANDROID_DIR"
./gradlew installDebug $GRADLE_PROPS

echo -e "${GREEN}✅ APK built and installed${NC}"

# ── Launch app ─────────────────────────────────────────────────────────────────
APP_ID="com.fenn.callguard.debug"
MAIN_ACTIVITY="com.fenn.callguard.MainActivity"

echo -e "\n${BLUE}🚀 Launching ${APP_ID}...${NC}"
adb -s "$DEVICE_ID" shell am start -n "${APP_ID}/${MAIN_ACTIVITY}"

echo -e ""
echo -e "${GREEN}✅ CallGuard launched on device${NC}"
echo -e ""
echo -e "${YELLOW}📱 Device:${NC} $(device_name "$DEVICE_ID") (${DEVICE_ID})"
echo -e "${YELLOW}📦 App ID:${NC} ${APP_ID}"
echo -e ""
echo -e "${BLUE}Useful adb commands:${NC}"
echo -e "  ${GREEN}adb -s $DEVICE_ID logcat -s CallGuard${NC}   # Stream app logs"
echo -e "  ${GREEN}adb -s $DEVICE_ID shell am force-stop $APP_ID${NC}  # Kill app"
echo -e "  ${GREEN}adb -s $DEVICE_ID uninstall $APP_ID${NC}     # Uninstall"
echo -e ""
echo -e "${YELLOW}💡 To rebuild and reinstall: re-run this script.${NC}"
