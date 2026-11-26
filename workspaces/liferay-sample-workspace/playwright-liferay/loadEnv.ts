/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import * as dotenv from 'dotenv';
import {join} from 'path';

dotenv.config({path: join(__dirname, '..', 'playwright-custom', '.env.local')});
