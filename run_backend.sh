#!/bin/bash

# CallShield Local Development Script
# Starts Supabase services and serves Edge Functions for local development.
#
# Usage: ./run_backend.sh [options] [env-file]
# Default env file: supabase/.env
#
# Examples:
#   ./run_backend.sh                      # Start with supabase/.env, preserve DB
#   ./run_backend.sh supabase/.env.dev    # Start with a different env file
#   ./run_backend.sh --restart            # Restart services + apply pending migrations
#   ./run_backend.sh --reset              # Start fresh (destroys all local data)
#   ./run_backend.sh --reset --restart    # Full clean restart
#   ./run_backend.sh --help               # Show this usage

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SUPABASE_DIR="$SCRIPT_DIR/supabase"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

show_usage() {
    echo -e "${BLUE}CallShield Local Development Server${NC}"
    echo ""
    echo "Usage: $0 [options] [env-file]"
    echo ""
    echo "Options:"
    echo "  --reset     Destroy and recreate the local database"
    echo "  --restart   Stop services, start fresh, and apply pending migrations"
    echo "  --help      Show this message"
    echo ""
    echo "Parameters:"
    echo "  env-file    Path to env file (default: supabase/.env)"
    echo ""
    echo "Examples:"
    echo "  $0                           # Quick start, preserve DB"
    echo "  $0 --restart                 # Restart + apply migrations, preserve DB"
    echo "  $0 --reset                   # Clean DB reset"
    echo "  $0 supabase/.env.dev         # Use alternate env file"
    echo ""
    echo -e "${BLUE}Edge Functions served:${NC}"
    echo "  • reputation        POST /reputation"
    echo "  • report            POST /report"
    echo "  • correct           POST /correct"
    echo "  • seed-db-manifest  GET  /seed-db-manifest"
    echo "  • verify-subscription POST /verify-subscription"
}

# ── Parse arguments ────────────────────────────────────────────────────────────
RESET_DB=false
RESTART_SERVICES=false
ENV_FILE_PARAM=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --reset)    RESET_DB=true;          shift ;;
        --restart)  RESTART_SERVICES=true;  shift ;;
        --help)     show_usage;             exit 0 ;;
        -*)
            echo -e "${RED}❌ Unknown option: $1${NC}"
            show_usage
            exit 1
            ;;
        *)
            ENV_FILE_PARAM="$1"
            shift
            ;;
    esac
done

# Resolve env file path
if [[ -z "$ENV_FILE_PARAM" ]]; then
    ENV_FILE="$SUPABASE_DIR/.env"
else
    if [[ "$ENV_FILE_PARAM" = /* ]]; then
        ENV_FILE="$ENV_FILE_PARAM"
    else
        ENV_FILE="$SCRIPT_DIR/$ENV_FILE_PARAM"
    fi
fi

# ── Validate prerequisites ─────────────────────────────────────────────────────
if [ ! -f "$SUPABASE_DIR/config.toml" ]; then
    echo -e "${RED}❌ supabase/config.toml not found. Run from the project root.${NC}"
    exit 1
fi

if ! command -v supabase &>/dev/null; then
    echo -e "${RED}❌ Supabase CLI not found. Install it: https://supabase.com/docs/guides/cli${NC}"
    exit 1
fi

if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}❌ Environment file not found: $ENV_FILE${NC}"
    echo ""
    echo "Copy the example and fill in your values:"
    echo "  cp supabase/.env.example supabase/.env"
    echo "  # then edit supabase/.env"
    exit 1
fi

# ── Print startup summary ──────────────────────────────────────────────────────
echo -e "${BLUE}🛡️  CallShield Local Development${NC}"
echo -e "  Env file : ${GREEN}$ENV_FILE${NC}"

if [ "$RESET_DB" = true ]; then
    echo -e "  Database : ${YELLOW}RESET (existing data will be destroyed)${NC}"
else
    echo -e "  Database : ${GREEN}PRESERVE${NC}"
fi

if [ "$RESTART_SERVICES" = true ]; then
    echo -e "  Services : ${BLUE}RESTART + apply pending migrations${NC}"
elif [ "$RESET_DB" = true ]; then
    echo -e "  Services : ${YELLOW}RESTART (required for DB reset)${NC}"
else
    echo -e "  Services : ${GREEN}QUICK START (reuse existing if running)${NC}"
fi
echo ""

# ── Load env secrets ───────────────────────────────────────────────────────────
echo -e "${BLUE}🔐 Loading secrets from env file...${NC}"

load_env_var() {
    local key="$1"
    local val
    val=$(grep "^${key}=" "$ENV_FILE" 2>/dev/null | tail -1 | cut -d'=' -f2-)
    if [[ -n "$val" ]]; then
        export "${key}=${val}"
        echo -e "  ${GREEN}✓${NC} $key"
    else
        echo -e "  ${YELLOW}⚠${NC}  $key not set in env file"
    fi
}

load_env_var "GOOGLE_SERVICE_ACCOUNT_KEY"

# ── Stop services if needed ────────────────────────────────────────────────────
if [ "$RESET_DB" = true ] || [ "$RESTART_SERVICES" = true ]; then
    echo -e "\n${BLUE}🛑 Stopping existing Supabase services...${NC}"
    (cd "$SUPABASE_DIR" && supabase stop 2>/dev/null) || true
fi

# ── Start Supabase ─────────────────────────────────────────────────────────────
echo -e "\n${BLUE}🚀 Starting Supabase services...${NC}"
if ! (cd "$SUPABASE_DIR" && supabase start); then
    echo -e "${RED}❌ Failed to start Supabase services${NC}"
    exit 1
fi

# ── DB reset or migrations ─────────────────────────────────────────────────────
if [ "$RESET_DB" = true ]; then
    echo -e "\n${YELLOW}🔄 Resetting database and applying all migrations...${NC}"
    if ! (cd "$SUPABASE_DIR" && supabase db reset); then
        echo -e "${RED}❌ Database reset failed${NC}"
        exit 1
    fi
    echo -e "${GREEN}✅ Database reset complete${NC}"
elif [ "$RESTART_SERVICES" = true ]; then
    echo -e "\n${BLUE}📦 Applying pending migrations...${NC}"
    if (cd "$SUPABASE_DIR" && supabase migration up); then
        echo -e "${GREEN}✅ Migrations applied${NC}"
    else
        echo -e "${YELLOW}⚠️  No pending migrations (or check output above)${NC}"
    fi
fi

# ── Status summary ─────────────────────────────────────────────────────────────
echo -e "\n${BLUE}🔍 Service status:${NC}"
(cd "$SUPABASE_DIR" && supabase status)

echo -e ""
echo -e "${GREEN}✅ Supabase services running${NC}"
echo -e ""
echo -e "${YELLOW}📡 Local endpoints:${NC}"
echo -e "  API:         ${GREEN}http://127.0.0.1:43210${NC}"
echo -e "  Studio:      ${GREEN}http://127.0.0.1:43212${NC}"
echo -e "  Database:    ${GREEN}postgresql://postgres:postgres@127.0.0.1:43211/postgres${NC}"
echo -e "  Inbucket:    ${GREEN}http://127.0.0.1:43213${NC}"
echo -e ""
echo -e "${YELLOW}📱 Android .env values (for testing against local backend):${NC}"
echo -e "  BASE_URL=http://10.0.2.2:43210/functions/v1"
echo -e "  (Use 10.0.2.2 from the Android emulator to reach host localhost)"
echo -e ""

# ── Serve Edge Functions ───────────────────────────────────────────────────────
echo -e "${BLUE}⚡ Starting Edge Functions server...${NC}"
echo -e "${BLUE}   Serving: reputation, report, correct, seed-db-manifest, verify-subscription${NC}"
echo -e "${YELLOW}   Press Ctrl+C to stop${NC}"
echo -e ""

cd "$SUPABASE_DIR" && supabase functions serve --env-file "$ENV_FILE"
