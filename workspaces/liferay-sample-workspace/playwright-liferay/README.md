# Playwright

`playwright-liferay` is managed and provided by Liferay. Do not edit it.

`playwright-custom` holds this workspace's tests and customizations. Create it when needed.

## Running the Tests

Run every command from `playwright-liferay`:

```bash
yarn install
npx playwright install chromium
yarn test
```

- **Reports:** Reports are written to `playwright-liferay/playwright-report` (HTML) and `playwright-liferay/test-results/TEST-playwright.xml` (JUnit).
- **Startup:** The tests start up an environment with Docker Compose when it is not already running and will shut it down when the tests finish. If an environment is already up when the tests are invoked, it will be used instead. Run `yarn start` and `yarn stop` to keep one up between runs.
- **Type Checking:** Run `yarn typecheck`.

## Writing Tests

Tests can be placed anywhere inside `playwright-custom`. Import what Liferay provides from `playwright-liferay/core`:

- `fixtures/backendPageTest` provides `backendPage`, a second page signed in as the administrator.
- `fixtures/dataApiHelpersTest` provides `apiHelpers`, which calls the headless APIs and cleans up data after each test.
- `fixtures/featureFlagsTest` sets feature flags for the tests.
- `fixtures/loginTest` signs in before each test.
- `utils` holds helpers that wait for alerts, page loads, and elements to appear or disappear.

Combine the fixtures with `mergeTests`, as in this test in `playwright-custom/tests`:

```typescript
import {mergeTests} from '@playwright/test';

import {dataApiHelpersTest} from '../../playwright-liferay/core/fixtures/dataApiHelpersTest';
import {loginTest} from '../../playwright-liferay/core/fixtures/loginTest';

const test = mergeTests(loginTest(), dataApiHelpersTest());
```

## Customizing

Each of these is optional and lives in `playwright-custom`:

- `.env.local` sets `PORTAL_URL` (defaults to `http://localhost:8080`) and `LIFERAY_USER_PASSWORD` (defaults to `test`). Do not put one in `playwright-liferay`, since it would take precedence.
- `env` is mounted into the portal as `/mnt/liferay/files`, for files such as `portal-ext.properties`.
- `package.json` adds dependencies, which `yarn install` installs. Do not add `@playwright/test` to its dependencies, since `playwright-liferay` provides it. To change the version of a package that `playwright-liferay` provides, list it under `liferayOverrides`:

	```json
	{
		"liferayOverrides": {
			"@playwright/test": "1.56.1"
		},
		"private": true
	}
	```

	After changing the Playwright version, run `npx playwright install chromium` again. After removing an override, run `yarn install --check-files` to restore the version `playwright-liferay` provides.

- `playwright.config.ts` extends `playwright-liferay/playwright.config.ts`:

	```typescript
	import {defineConfig} from '@playwright/test';

	import liferayConfig from '../playwright-liferay/playwright.config';

	export default defineConfig({...liferayConfig, timeout: 120 * 1000});
	```

- `tsconfig.json` extends `playwright-liferay/tsconfig.json` and replaces it for `yarn typecheck`:

	```json
	{
		"extends": "../playwright-liferay/tsconfig.json"
	}
	```