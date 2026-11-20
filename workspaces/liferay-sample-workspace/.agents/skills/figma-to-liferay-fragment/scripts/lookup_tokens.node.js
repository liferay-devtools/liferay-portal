/**
 * Search a Liferay frontend-token-definition.json for CSS variables.
 *
 * Usage:
 *   node lookup_tokens.node.js [filter...] [--file <token-definition.json>] [--hex <#RRGGBB>]
 *
 * Examples:
 *   node lookup_tokens.node.js btn primary         # tokens whose name/label/category contain all terms
 *   node lookup_tokens.node.js --hex "#0B5FFF"     # tokens whose (resolved) default equals a Figma color
 *   node lookup_tokens.node.js border-radius
 *
 * Defaults to the bundled Classic theme definition (../frontend-token-definition.json).
 * Prints one line per token: category > set | var(--css-variable) | default [= resolved] | editor type.
 */

const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
let file = path.join(__dirname, '..', 'frontend-token-definition.json');
let hex = null;
const terms = [];

for (let i = 0; i < args.length; i++) {
	if (args[i] === '--file') file = args[++i];
	else if (args[i] === '--hex') hex = args[++i].toLowerCase();
	else terms.push(args[i].toLowerCase());
}

function normalizeHex(value) {
	if (typeof value !== 'string' || !value.startsWith('#')) return null;

	let v = value.toLowerCase();

	if (v.length === 4) v = '#' + [...v.slice(1)].map((c) => c + c).join('');

	return v;
}

const definition = JSON.parse(fs.readFileSync(file, 'utf-8'));
const rows = [];
const defaults = {};

for (const category of definition.frontendTokenCategories || []) {
	for (const set of category.frontendTokenSets || []) {
		for (const token of set.frontendTokens || []) {
			const cssVariable = (token.mappings || []).find((m) => m.type === 'cssVariable');

			if (!cssVariable) continue;

			defaults[cssVariable.value] = token.defaultValue;
			rows.push({category, cssVariable: cssVariable.value, set, token});
		}
	}
}

// Follow var(--x) aliases (e.g. --btn-primary-background-color -> var(--primary) -> #0b5fff).
function resolve(value, depth = 0) {
	const match = typeof value === 'string' && value.trim().match(/^var\(--([a-z0-9-]+)\)$/i);

	if (!match || depth > 10 || !(match[1] in defaults)) return value;

	return resolve(defaults[match[1]], depth + 1);
}

let count = 0;

for (const {category, cssVariable, set, token} of rows) {
	const haystack = [category.label, category.name, set.label, set.name, token.label, token.name, cssVariable]
		.filter(Boolean)
		.join(' ')
		.toLowerCase();
	const resolved = resolve(token.defaultValue);

	if (terms.length && !terms.every((t) => haystack.includes(t))) continue;
	if (hex && normalizeHex(resolved) !== normalizeHex(hex)) continue;

	const shown = resolved === token.defaultValue ? token.defaultValue : `${token.defaultValue} = ${resolved}`;

	console.log(
		`${category.label || category.name} > ${set.label || set.name} | var(--${cssVariable}) | ${shown} | ${token.editorType || token.type}`
	);
	count++;
}

console.error(`${count} token(s) matched.`);
