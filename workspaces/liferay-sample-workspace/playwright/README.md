# Playwright

Playwright tests for the Liferay Sample Workspace.

## Prerequisites

- Docker and JDK 21, if Liferay is not already running
- Node.js and Yarn

## Running the Tests

```bash
yarn install
npx playwright install chromium
npx playwright test
```

## Portal URL

The tests run against `PORTAL_URL`, which defaults to `http://localhost:8080`. To change it for yourself, set it in your shell or in `.env.local`, which Git ignores and which overrides the shell. `.env` is committed, so only put settings there that apply to everyone.

## Starting Liferay

If Liferay is not running at `PORTAL_URL` and `PORTAL_URL` is a `http://localhost:<port>` URL, the tests start the `liferay.workspace.product` release in Docker on that port and remove the containers and volumes when they finish. Liferay uses the trial license from the `liferay/dxp:latest` image.

The build tags its image as `liferay:playwright`, so the `liferay:local` image that the workspace's own stack runs is not replaced. Like any `scripts/bootstrap.sh` run, it cleans and rebuilds the workspace's `build` directory and stops a container started with `./gradlew startDockerContainer`.

## Properties and Feature Flags

`env` mirrors Liferay Home. Put the files this workspace's tests need in it, such as `portal-setup-wizard.properties` with the portal properties and feature flags they need, or OSGi configurations in `osgi/configs`. `refresh_other_workspaces.sh` leaves each workspace's `env` alone.