#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

if ! command -v stripe >/dev/null 2>&1; then
  echo "Stripe CLI is not installed. Install it from https://docs.stripe.com/stripe-cli." >&2
  exit 1
fi

forward_to="${STRIPE_FORWARD_TO:-${APP_BASE_URL:-http://localhost:${SERVER_PORT:-8080}}/api/payments/webhook}"
events="${STRIPE_LISTEN_EVENTS:-checkout.session.completed}"

echo "Forwarding Stripe sandbox webhooks to ${forward_to}"
echo "Listening for events: ${events}"
echo "Copy the webhook signing secret printed by Stripe CLI into STRIPE_WEBHOOK_SECRET in .env if it changes."

exec stripe listen \
  --events "${events}" \
  --forward-to "${forward_to}" \
  "$@"
