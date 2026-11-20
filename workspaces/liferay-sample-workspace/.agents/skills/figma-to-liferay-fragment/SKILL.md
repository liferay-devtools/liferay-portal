---

name: figma-to-liferay-fragment
description: Convert Figma designs into Liferay DXP page fragments (index.html, styles.css, main.js, configuration.json, fragment.json) or whole fragment collections, from a Figma design/prototype URL (REST API hybrid mode: JSON tree + rendered PNG), an exported Figma JSON file, or a screenshot/mockup image. Use when the user asks to turn a Figma frame, page, component, prototype link, or design screenshot into Liferay fragments.

---

# Figma to Liferay DXP Fragment Generator

Converts Figma designs into Liferay DXP page fragments that follow Clay / Bootstrap 4 conventions and the Liferay fragment specification.

The Figma specific workflow lives here. For the general fragment rules, such as file layout in a site initializer, valid configuration field types, FreeMarker, deploying, and the live fragment API, **also load `scaffold-fragment`**, as the workspace preflight rule requires. Where the two disagree, `scaffold-fragment` holds the verified behavior.

`<skill-dir>` below is the folder holding this file, such as `.agents/skills/figma-to-liferay-fragment` or the agent specific copy of it (`.claude/skills/figma-to-liferay-fragment`). Run the scripts from the workspace root with that path.

## When to Invoke

- "Convert this Figma URL / frame / prototype into a Liferay fragment"
- "Turn this design screenshot into a fragment collection"
- "Split this Figma page into sections and build fragments for each"

## Before Starting: Ask Two Things

Ask both questions together in one prompt, through the agent's structured question tool when it has one, unless the user already answered them:

1. **Which theme the site uses.** Options: Classic (the default theme shipped with Liferay DXP, recommended) or a custom theme. This decides which CSS variables are valid (see "Style Book and Theme Tokens"). Dialect is no longer included in DXP releases; if a site still runs it, treat it as a custom theme.

1. **Where the output should go.** Options:
	- **Standalone zip collection** (default, the presales flow): `./src/liferay-fragments/<collection-key>/` in the workspace, zipped and imported through **Site Administration → Design → Fragments → Import**.
	- **Site initializer tree**: `client-extensions/<site-initializer>/site-initializer/fragments/group/<collection-key>/fragments/<fragment-name>/`, following `scaffold-fragment`.
	- **Straight into the running portal**: through the MCP `headless-admin-fragment-v1.0` tool set (`postSiteFragmentSet`, then `postSiteFragmentSetFragment`). See `skills/mcp-server/SKILL.md`. A **Design Library** target does not accept these writes; see `scaffold-fragment` → "Design Libraries Are Read Only Over Headless".

## Workflow

### 1. Get the Design

**Preferred: hybrid mode from a Figma URL** (`/design/`, `/proto/`, `/file/`, `/board/` with a `node-id`). Write the output to a temporary folder outside the workspace, such as the agent's scratch directory, not the workspace root:

```bash
node <skill-dir>/scripts/fetch_figma.node.js \
	--url "<FIGMA_URL>" \
	--out "<temp-dir>/figma"
```

The script needs `FIGMA_TOKEN` in the environment (see `README.md`). It prints `JSON_TREE=<path>` and `PNG_SNAPSHOT=<path>`:

- `figma_tree_<node>.json` holds exact text, hex colors, font families, sizes, weights, spacing, and the node hierarchy. It can run to megabytes, so **do not load it whole into context**. Extract what you need with a short `node` or `python` script, for example walking `nodes[<id>].document` and printing each `TEXT` node's `characters` and `style`, and each frame's `name`, `layoutMode`, `itemSpacing`, `padding*`, and `fills`.
- `figma_snapshot_<node>.png` is the rendered frame. **Look at it** with the agent's image viewing capability, such as a file read tool that renders images. Use it for visual arrangement, overlays, multicolor text spans, and responsive decisions.

Combine the two: take copy and exact values from the JSON, and layout from the PNG.

**When the node is a flattened picture, find the layered original.** UI kits often link a preview: a single `RECTANGLE` with an `IMAGE` fill and no children, so the JSON holds no text, colors, or spacing. Do not read values off the picture yet. List the file's pages and top level frames (`GET https://api.figma.com/v1/files/<file-key>?depth=2` with the `X-Figma-Token` header), look for the same section in a layered page (a landing page frame, a component set), and fetch that node instead. Verified on a community UI kit: the linked hero was a locked preview image, while the kit's free landing page held the same hero, navbar, and footer fully layered. Export icons with `GET /v1/images/<file-key>?ids=<ids>&format=svg` and photos with `format=jpg&scale=2`.

**Fallback 1: a screenshot or mockup.** When there is no token or URL, the user can paste an image or give a path. Look at the image directly. Text and values must then be read off the image, so say they are approximate.

**Fallback 2: a local exported JSON.** Process it like the hybrid JSON tree, without the PNG.

If the fetch fails with `403`, the token is missing or expired. With `404`, the file key or node ID is wrong, or the token cannot access the file. Report the error to the user; do not guess the design.

### 2. Single Fragment or Collection

- **One component or frame**: one fragment folder.
- **A full page or several sections**: identify the top level sections (Header, Hero, Features, CTA, Footer, and so on), give each its own fragment folder, and add one `collection.json` at the collection root.

Skip Header and Footer when the target site already provides them through its master page, but say you skipped them.

### 3. Map the Design to HTML

- Auto layout and grids become Flexbox or the Bootstrap grid (`row`, `col-*`, `d-flex`).
- Use semantic elements: `section`, `h1` to `h3`, `p`, `ul`, `a`, `button`.
- Make the content an author would change editable, using the **attribute syntax only**. Never use the legacy `<lfr-editable>` tag.

| Type | Use For | Example |
| --- | --- | --- |
| `text` | Plain headings, badges, labels | `<span data-lfr-editable-id="badge" data-lfr-editable-type="text">Limited Offer</span>` |
| `rich-text` | Paragraphs, titles with inline `<span>` highlights | `<p data-lfr-editable-id="intro" data-lfr-editable-type="rich-text">…</p>` |
| `html` | Raw HTML blocks | `<div data-lfr-editable-id="raw" data-lfr-editable-type="html">…</div>` |
| `image` | On the `<img>` itself | `<img data-lfr-editable-id="hero-img" data-lfr-editable-type="image" src="…" alt="…" />` |
| `link` | On the `<a>` itself | `<a class="btn btn-primary" data-lfr-editable-id="cta" data-lfr-editable-type="link" href="#">Explore</a>` |
| `date-time` | Dates with a picker | `<time datetime="2026-05-15" data-lfr-editable-id="date" data-lfr-editable-type="date-time">May 15, 2026</time>` |
| `action` | Object or workflow action buttons | `<button data-lfr-editable-id="submit" data-lfr-editable-type="action">Submit</button>` |

**Put the editable attribute on the heading element itself**, for example `<h1 data-lfr-editable-id="title" data-lfr-editable-type="rich-text">`. Do not wrap an `<h1>` inside an editable `<div>`. A page level override replaces the editable region's inner markup, so a wrapped heading tag is lost (see `manage-pages`, "A Literal Override Replaces the Editable's Inner Markup").

**Plan for reuse with different content.** Text that is not editable is fixed in the HTML, and the only way to change it per page is editing that page's copy of the fragment code. Make every visible string a marketer might want to change editable.

### 4. Generate the Files

For each fragment:

- **`index.html`**: semantic markup, editable attributes, Clay utility classes (`mb-3`, `btn`, `text-primary`), and FreeMarker only when configuration drives the markup. Wrap everything in one root element carrying a unique fragment class, such as `<section class="acme-hero-v1">`.
- **`styles.css`**: scope every selector under that root class so nothing leaks globally. Use theme CSS variables, not literals (see below). Responsive rules go in media queries.
- **`main.js`**: only if the design needs behavior. Use the `fragmentElement` global to scope DOM queries, and never `document.querySelector` for the fragment's own nodes.
- **`configuration.json`**: options a site builder should change without code, such as style variant, alignment, or show/hide toggles:

	```json
	{
		"fieldSets": [
			{
				"fields": [
					{
						"dataType": "string",
						"defaultValue": "light",
						"label": "Style Variant",
						"name": "styleVariant",
						"type": "select",
						"typeOptions": {
							"validValues": [
								{"label": "Light", "value": "light"},
								{"label": "Dark", "value": "dark"}
							]
						}
					}
				],
				"label": "General Configuration"
			}
		]
	}
	```

- **`fragment.json`**: the path keys must match the file names you actually wrote:

	```json
	{
		"configurationPath": "configuration.json",
		"cssPath": "styles.css",
		"htmlPath": "index.html",
		"jsPath": "main.js",
		"name": "Acme Hero V1",
		"type": "component"
	}
	```

- **`collection.json`** (collections only): `{"name": "<Collection Name>", "description": "<one line>"}`.

### 5. Validate

Run a short script over the output:

- Every `configuration.json`, `fragment.json`, and `collection.json` parses as JSON.
- Every `data-lfr-editable-id` is unique within its fragment, and IDs are stable and descriptive (`hero-title`, not `text1`).
- No `"dataType": "boolean"` anywhere (see below).
- No `gap` on any `.row` (see below).
- Every file named in `fragment.json` exists.
- Fragment folder names are unique keys (see "Unique Keys").

### 6. Package and Deliver

- **Zip collection**: zip the `<collection-key>/` folder, keeping `collection.json` at the zip root's collection folder, and tell the user the import path in the UI.
- **Site initializer**: follow `scaffold-fragment` → "Deploy".
- **Live portal through MCP**: create the set, then create each fragment with `html`, `css`, `js`, and `configuration`. Read back with `getSiteFragmentSetFragmentsPage` to confirm.

Then show the user the generated sections next to the Figma snapshot, and list any value you approximated.

## Liferay Technical Constraints

These come from failures already hit in practice. Follow them strictly.

### Style Book and Theme Tokens

- **Never hardcode** hex colors, font stacks, or border radii in `styles.css`. Map each Figma value to a theme CSS variable.
- **For Classic**, use `frontend-token-definition.json` in this folder, which is the Classic theme token definition shipped with Liferay DXP (274 tokens, about 80 KB). **Query it with the lookup script instead of reading it**:

	```bash
	node <skill-dir>/scripts/lookup_tokens.node.js btn primary
	node <skill-dir>/scripts/lookup_tokens.node.js --hex "#0B5FFF"
	node <skill-dir>/scripts/lookup_tokens.node.js border-radius
	```

	Each line prints `category > set | var(--name) | default [= resolved] | editor`. Many Classic tokens default to another variable (`--btn-primary-background-color` is `var(--primary)`). The script follows those references, so `--hex` also finds alias tokens; prefer the most specific token for the element, such as `--btn-primary-*` for a primary button rather than `--primary`. Use only variable names the script prints; never invent one.
- **Classic token names are Bootstrap style, not Dialect's `--color-*` names.** `--color-brand-primary`, `--color-action-primary-default`, `--color-action-neutral-default`, and `--border-radius-xs` **do not exist in Classic**, and a missing variable fails silently: the property just falls back to inherited or initial values. Common Classic mappings:

	| Figma value | Classic variable |
	| --- | --- |
	| Page background / body text | `var(--body-bg)`, `var(--body-color)` |
	| Brand and theme colors | `var(--brand-color-1)` to `var(--brand-color-4)`, `var(--primary)`, `var(--secondary)`, `var(--success)`, `var(--info)`, `var(--warning)`, `var(--danger)`, `var(--light)`, `var(--dark)` |
	| Shades | Lighter: `var(--primary-l0)` to `var(--primary-l3)`. Darker: `var(--primary-d1)`, `var(--primary-d2)`. Same pattern per theme color. Palette colors (`--blue`, `--orange`, ...) go `-l1` to `-l5` and `-d1` to `-d4`. |
	| Neutrals | `var(--white)`, `var(--black)`, `var(--gray-100)` to `var(--gray-900)`, `var(--gray-dark)`, `var(--text-muted)` |
	| Buttons | `var(--btn-primary-background-color)`, `-border-color`, `-color`, and their `-hover-*` variants; same for `--btn-secondary-*`, `--btn-outline-primary-*`, `--btn-outline-secondary-*`, `--btn-link-*` |
	| Corners | `var(--border-radius)`, `var(--border-radius-sm)`, `var(--border-radius-lg)`, `var(--border-radius-circle)`, `var(--rounded-pill)` |
	| Shadows | `var(--box-shadow-sm)`, `var(--box-shadow)`, `var(--box-shadow-lg)` |
	| Fonts | `var(--font-family-base)`, `var(--font-family-sans-serif)`, `var(--font-family-monospace)`, `var(--font-size-base)`, `var(--font-size-sm)`, `var(--font-size-lg)`, `var(--font-weight-light)` to `var(--font-weight-bolder)` (including `--font-weight-semi-bold`) |
	| Headings and display text | `var(--h1-font-size)` to `var(--h6-font-size)`, `var(--display1-size)` to `var(--display4-size)`, `var(--display1-weight)` to `var(--display4-weight)`, `var(--display-line-height)`, `var(--lead-font-size)` |
	| Spacing | `var(--spacer-0)` to `var(--spacer-10)` (0 to 10rem), matching Bootstrap `m-*` / `p-*` utilities `0`–`10` |
	| Container widths | `var(--container-max-sm)` to `var(--container-max-xl)` |

- **Brand colors from the design go in the Style Book, not the CSS.** Classic's defaults are Liferay blue (`--primary` / `--brand-color-1` = `#0b5fff`). When the Figma brand color differs, still reference `var(--primary)` (or the matching `--btn-*` token) in `styles.css`, and tell the user which Style Book values to set so the theme supplies the brand color. Do not hardcode the hex to make it look right.
- **For a custom theme**, tell the user the bundled file is Classic's and ask for their theme's `frontend-token-definition.json`. It ships inside the theme's WAR (for example under `WEB-INF/`), or in the theme source next to `liferay-plugin-package.properties`. Pass it with `--file <path>`, or replace the bundled copy. Until you have it, map only to Classic / Bootstrap variables, and flag every value that does not map.
- Where no token matches a Figma value, keep a literal only as a fallback inside `var()`, such as `var(--primary, #F7A01C)`, and list it in your summary.

### The CSS Gap Trap on Bootstrap Rows

Never put `gap` on a `.row` holding percentage columns (`.col-md-4` is exactly `33.3333%`). The gap pushes the total past 100%, and the last column wraps onto a new line. Let the grid gutters handle horizontal spacing, and add `mb-4 mb-md-0` on the columns for stacked mobile spacing. `gap` is fine on a plain `d-flex` container.

### Checkbox Fields Take No `dataType`

Never write `"dataType": "boolean"`. `dataType` is a closed enum without `boolean`, and the import fails or rejects the whole collection. A checkbox needs only `name`, `label`, `type`, and `defaultValue`. When printing a checkbox value in FreeMarker, use `?c`: `${(configuration.showBadge!true)?c}` (see `scaffold-fragment`).

### `fragment.json` Type Is `component`

Use `"type": "component"`. A `"type": "section"` fragment imports and renders, but it makes the headless fragment listing return `400` for every fragment in its set (see `scaffold-fragment` → "The Live Fragment API").

### Fidelity First

Replicate the Figma layout as designed: columns, rows, spacing, images, and copy. Do not add creative extras, such as text overlays on gallery images, unless the user asks. When the design is ambiguous, say what you chose.

### Unique Keys

The fragment folder name is the fragment's key, and keys must be unique. When producing a new iteration of existing fragments, suffix both the folder and the display name (`acme-hero-v6`, `Acme Hero V6`) to avoid import collisions.

## Success Signal

- Every fragment folder has the files its `fragment.json` names, and the validation script reports no errors.
- The collection imports without errors (for MCP delivery, the read back lists every fragment).
- Placed on a test page, each fragment renders like its Figma snapshot, and every editable region is highlighted in the page editor.