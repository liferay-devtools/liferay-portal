# Figma to Liferay Fragment Skill

An agent skill for Liferay Workspace. The agent loads `SKILL.md` when you ask it to turn a Figma design into Liferay fragments, or you can invoke it by name (`figma-to-liferay-fragment`) where your agent supports explicit skill invocation, such as `/figma-to-liferay-fragment` in Claude Code.

## Contents

```
figma-to-liferay-fragment/
├── SKILL.md                          # Instructions the agent follows
├── README.md                         # This file
├── frontend-token-definition.json    # Classic theme tokens, shipped with Liferay DXP (replace for custom themes)
└── scripts/
    ├── fetch_figma.node.js           # Downloads node JSON + rendered PNG from the Figma API
    └── lookup_tokens.node.js         # Searches the token file by name or hex value, resolving var() aliases
```

## Setup: Figma Token

Hybrid mode needs a Figma personal access token with read only *File content* scope:

1. In Figma, open **Main menu → Help and account → Account settings → Security → Personal access tokens**.

1. Generate a token and copy it.

1. Store it as `FIGMA_TOKEN`:
	- PowerShell: `[System.Environment]::SetEnvironmentVariable('FIGMA_TOKEN', '<token>', 'User')`
	- macOS / Linux: add `export FIGMA_TOKEN="<token>"` to `~/.zshrc` or `~/.bashrc`

1. Restart your agent session so it picks up the variable.

Without a token, paste a screenshot of the design into the chat instead.

## Example Prompts

- `Create a Liferay fragment collection from this Figma page, one fragment per section: https://www.figma.com/proto/<file>/<name>?node-id=17098-114086`
- `Generate a single fragment from this Figma component: https://www.figma.com/design/<file>/<name>?node-id=101-202`
- `Turn this mockup into a responsive Liferay fragment` (with an image attached)

A full page request, where the skill works together with `scaffold-fragment`, `manage-pages`, and `theme-and-design`:

```text
I want to create a new page in the site "<Site Name>" with title "Home". This will be the home page
of the site and it should have this Hero designed in Figma:
https://www.figma.com/design/<file>/<name>?node-id=<node-id> . If the needed fragments (header,
footer, hero banner, etc.) do not exist yet, create them in a new fragment set in a Design Library
called "<Design Library Name>" (with Group Id <group-id>) as well as the needed Style Book (if you
can't create the Style Book via API let me know which values I should enter manually). If there is
no Master Page Template, create one with the header and the footer.
```

The agent first asks which theme the site uses and where the fragments should go: a zip for manual import, the site initializer tree, or straight into the running portal through MCP.

## Changes From the Earlier Dialect Version

This skill replaces an earlier Gemini only version that targeted the Dialect theme.

- Works with any agent: script paths are relative to the skill folder, and fetched Figma files go to a temporary folder instead of the workspace root.
- `fetch_figma.node.js` adds `--out` and `--scale`, follows redirects on the PNG download, and fails clearly on HTTP errors.
- New `lookup_tokens.node.js`, so the token file is searched rather than read whole. It resolves `var()` aliases, so a Figma hex also finds tokens like `--btn-primary-background-color`.
- Screenshots are viewed directly by the agent; there is no `--image` flag.
- Adds output options (zip, site initializer, live MCP), a validation step, and links to the workspace's `scaffold-fragment`, `manage-pages`, and `mcp-server` skills for verified Liferay behavior.
- The bundled `frontend-token-definition.json` is the **Classic** theme's (shipped with Liferay DXP), replacing the Dialect file; Dialect is no longer included in DXP releases. `SKILL.md` maps Figma values to Classic's Bootstrap-style variables (`--primary`, `--btn-primary-*`, `--spacer-*`, `--border-radius`, ...) instead of Dialect's `--color-*` names.