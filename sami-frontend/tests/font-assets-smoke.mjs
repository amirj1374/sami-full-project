import assert from 'node:assert/strict'
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs'
import { basename, dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const assets = join(root, 'dist', 'assets')

assert.ok(existsSync(assets), 'dist/assets is missing; run npm run build first')

const cssFiles = readdirSync(assets).filter((name) => name.endsWith('.css'))
assert.ok(cssFiles.length > 0, 'production build emitted no CSS')

const fontReferences = new Set()
for (const cssFile of cssFiles) {
  const css = readFileSync(join(assets, cssFile), 'utf8')
  for (const match of css.matchAll(/url\((?:["']?)([^)"']+\.(?:woff2?|ttf))(?:["']?)\)/g)) {
    fontReferences.add(basename(match[1]))
  }
}

assert.ok(
  [...fontReferences].some((name) => name.startsWith('vazirmatn-arabic-') && name.endsWith('.woff2')),
  'built CSS does not reference the Persian/Arabic Vazirmatn WOFF2 subset',
)

for (const font of fontReferences) {
  const path = join(assets, font)
  assert.ok(existsSync(path), `font referenced by CSS is missing: ${font}`)
  assert.ok(statSync(path).size > 0, `font asset is empty: ${font}`)
}

console.log(`Verified ${fontReferences.size} built font assets: ${[...fontReferences].join(', ')}`)
