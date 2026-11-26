/**
 * Figma REST API node + image downloader (hybrid mode).
 *
 * Usage:
 *   node fetch_figma.node.js --url <FIGMA_URL> [--out <DIR>] [--token <FIGMA_TOKEN>] [--scale <1-4>]
 *   node fetch_figma.node.js --file <FILE_KEY> --node <NODE_ID> [--out <DIR>]
 *
 * Writes figma_tree_<NODE>.json (node tree) and figma_snapshot_<NODE>.png (rendered frame)
 * into --out (default: current directory). The token comes from --token or FIGMA_TOKEN.
 */

const fs = require('fs');
const https = require('https');
const path = require('path');

function parseArgs() {
	const args = process.argv.slice(2);
	const options = {out: process.cwd(), scale: '2'};

	for (let i = 0; i < args.length; i++) {
		if (args[i] === '--url') {
			const urlStr = args[++i];

			// Supports /design/, /proto/, /file/ and /board/ URLs.
			const fileMatch = urlStr.match(/\/(file|design|proto|board)\/([a-zA-Z0-9]+)/);
			const nodeMatch = urlStr.match(/node-id=([0-9A-Za-z%:-]+)/);

			if (fileMatch) options.fileKey = fileMatch[2];

			// URLs encode node IDs as "123-456"; the API expects "123:456".
			if (nodeMatch) options.nodeId = decodeURIComponent(nodeMatch[1]).replace('-', ':');
		}
		if (args[i] === '--file') options.fileKey = args[++i];
		if (args[i] === '--node') options.nodeId = args[++i];
		if (args[i] === '--token') options.token = args[++i];
		if (args[i] === '--out') options.out = path.resolve(args[++i]);
		if (args[i] === '--scale') options.scale = args[++i];
	}

	return options;
}

function httpGet(url, headers) {
	return new Promise((resolve, reject) => {
		https.get(url, {headers}, (res) => {
			let data = '';

			if (res.statusCode < 200 || res.statusCode >= 300) {
				res.resume();

				return reject(new Error(`Status ${res.statusCode}: ${res.statusMessage}`));
			}

			res.on('data', (chunk) => {
				data += chunk;
			});
			res.on('end', () => resolve(data));
		}).on('error', reject);
	});
}

function downloadBinary(url, destPath, redirects = 0) {
	return new Promise((resolve, reject) => {
		https.get(url, (res) => {
			if ([301, 302, 303, 307, 308].includes(res.statusCode) && res.headers.location && redirects < 5) {
				res.resume();

				return resolve(downloadBinary(res.headers.location, destPath, redirects + 1));
			}

			if (res.statusCode < 200 || res.statusCode >= 300) {
				res.resume();

				return reject(new Error(`Image download failed with status ${res.statusCode}`));
			}

			const file = fs.createWriteStream(destPath);

			res.pipe(file);
			file.on('finish', () => file.close(resolve));
			file.on('error', reject);
		}).on('error', reject);
	});
}

async function fetchFigmaHybrid() {
	const options = parseArgs();
	const token = options.token || process.env.FIGMA_TOKEN;

	if (!options.fileKey || !options.nodeId) {
		console.error('Error: missing Figma file key or node ID. Pass --url with a node-id, or --file and --node.');
		process.exit(1);
	}

	if (!token) {
		console.error('Error: FIGMA_TOKEN environment variable is not set (or pass --token).');
		process.exit(1);
	}

	fs.mkdirSync(options.out, {recursive: true});

	const encodedNodeId = encodeURIComponent(options.nodeId);
	const cleanNodeId = options.nodeId.replace(/[:;]/g, '_');
	const headers = {'X-Figma-Token': token};

	try {
		console.log(`[1/2] Fetching JSON tree for node ${options.nodeId}...`);

		const jsonResponse = await httpGet(
			`https://api.figma.com/v1/files/${options.fileKey}/nodes?ids=${encodedNodeId}`,
			headers
		);
		const jsonPath = path.join(options.out, `figma_tree_${cleanNodeId}.json`);

		fs.writeFileSync(jsonPath, jsonResponse, 'utf-8');

		console.log('[2/2] Rendering PNG snapshot...');

		const imageApiResponse = JSON.parse(
			await httpGet(
				`https://api.figma.com/v1/images/${options.fileKey}?ids=${encodedNodeId}&format=png&scale=${options.scale}`,
				headers
			)
		);
		const renderUrl = imageApiResponse.images ? imageApiResponse.images[options.nodeId] : null;
		let imagePath = null;

		if (renderUrl) {
			imagePath = path.join(options.out, `figma_snapshot_${cleanNodeId}.png`);
			await downloadBinary(renderUrl, imagePath);
		}
		else {
			console.warn('Warning: Figma returned no render URL. Continuing in JSON-only mode.');
		}

		console.log('\nHybrid extraction complete.');
		console.log(`JSON_TREE=${jsonPath}`);
		if (imagePath) console.log(`PNG_SNAPSHOT=${imagePath}`);
	}
	catch (err) {
		console.error('Extraction failed:', err.message);
		process.exit(1);
	}
}

fetchFigmaHybrid();
