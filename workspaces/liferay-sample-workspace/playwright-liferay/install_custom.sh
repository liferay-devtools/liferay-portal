#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

function main {
	if [ ! -f ../playwright-custom/package.json ]
	then
		return
	fi

	yarn --cwd ../playwright-custom install

	local overrides

	overrides=$(
		node --eval "
			const {liferayOverrides = {}} = require('../playwright-custom/package.json');

			for (const [name, version] of Object.entries(liferayOverrides)) {
				console.log(name + '@' + version);
			}"
	)

	if [[ -z ${overrides} ]]
	then
		return
	fi

	local override
	local override_arguments=()

	while IFS= read -r override
	do
		override_arguments+=("${override}")
	done <<< "${overrides}"

	npm install --no-package-lock --no-save "${override_arguments[@]}"
}

main