#!/usr/bin/env bash

cd "$(dirname "${BASH_SOURCE[0]}")"

source ../scripts/_common.sh

function main {
	if [[ -n $(docker ps --all --filter name=^database$ --filter name=^liferay$ --quiet) ]]
	then
		_die "Liferay is not running, and an existing \"database\" or \"liferay\" container prevents the tests from starting their own. Set \"PORTAL_URL\" to that Liferay, or remove the containers with \"docker rm --force database liferay\"."
	fi

	export COMPOSE_PROJECT_NAME=liferay-playwright-stack
	export LIFERAY_COMPOSE_OVERRIDES=playwright
	export LIFERAY_IMAGE=liferay:playwright

	trap "docker_compose down --volumes" EXIT

	mkdir -p env

	../scripts/bootstrap.sh

	sleep infinity
}

main "${@}"