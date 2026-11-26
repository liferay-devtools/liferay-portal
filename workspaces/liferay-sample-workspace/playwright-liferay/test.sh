#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

function main {
	local config=playwright.config.ts

	if [ -f ../playwright-custom/playwright.config.ts ]
	then
		config=../playwright-custom/playwright.config.ts
	fi

	NODE_PATH="${PWD}/node_modules" node_modules/.bin/playwright test --config "${config}" "${@}"
}

main "${@}"