#!/usr/bin/env node
/*
 * Ruin resource-pack handshake checker (Mineflayer, headless).
 *
 * Mineflayer CANNOT render textures — this does NOT verify the Catalyst/glyphs
 * look right. It verifies the SERVER-PUSH HANDSHAKE that has been failing:
 *   1. what resource-pack URL + SHA-1 your server advertises,
 *   2. whether that URL actually downloads,
 *   3. whether the downloaded file's real SHA-1 matches the advertised one.
 * A mismatch (or a non-downloadable URL) is exactly what makes the vanilla
 * client show "Failed to download resource pack".
 *
 * Usage:
 *   node rp-check.js --host <ip> [--port 25565] [--user Probe] [--version 1.21.4] [--auth offline|microsoft]
 *
 * Notes:
 *   - Default auth is "offline" (works on cracked / online-mode=false servers).
 *     For an online-mode server use --auth microsoft (a browser code login prompt appears).
 *   - Run it from a machine that can actually reach your server + the pack URL
 *     (e.g. your own PC), so it sees the same network path the real client does.
 */

const mineflayer = require('mineflayer')
const crypto = require('crypto')
const https = require('https')
const http = require('http')

function arg(name, def) {
  const i = process.argv.indexOf('--' + name)
  return i !== -1 && process.argv[i + 1] ? process.argv[i + 1] : def
}

const host = arg('host')
const port = parseInt(arg('port', '25565'), 10)
const username = arg('user', 'RPProbe')
const version = arg('version', false) // let mineflayer auto-detect if omitted
const auth = arg('auth', 'offline')

if (!host) {
  console.error('ERROR: --host <ip> is required.\n' +
    'Example: node rp-check.js --host play.example.net --port 25565 --version 1.21.4')
  process.exit(1)
}

console.log(`[connect] ${host}:${port}  user=${username}  version=${version || 'auto'}  auth=${auth}`)

const bot = mineflayer.createBot({
  host, port, username, auth,
  version: version || undefined,
  // Do NOT auto-accept; we want to observe the offer, verify it ourselves.
  acceptResourcePack: false,
})

let handled = false

function download(url) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith('https') ? https : http
    const req = lib.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return resolve(download(res.headers.location)) // follow redirect
      }
      if (res.statusCode !== 200) {
        return reject(new Error(`HTTP ${res.statusCode} ${res.statusMessage}`))
      }
      const chunks = []
      res.on('data', (c) => chunks.push(c))
      res.on('end', () => resolve(Buffer.concat(chunks)))
    })
    req.on('error', reject)
    req.setTimeout(15000, () => req.destroy(new Error('timeout after 15s')))
  })
}

async function inspect(url, hash) {
  if (handled) return
  handled = true
  console.log('\n=== resource pack advertised by server ===')
  console.log('  url  :', url)
  console.log('  hash :', hash || '(none sent)')
  if (!url) {
    console.log('  VERDICT: server sent no URL — nothing for the client to download.')
    return finish()
  }
  try {
    const buf = await download(url)
    const real = crypto.createHash('sha1').update(buf).digest('hex')
    console.log('\n=== download result ===')
    console.log('  bytes        :', buf.length)
    console.log('  real sha1    :', real)
    const looksZip = buf.length >= 2 && buf[0] === 0x50 && buf[1] === 0x4b // "PK"
    console.log('  is a zip?    :', looksZip ? 'yes (PK header)' : 'NO — not a zip (likely an HTML page, e.g. a share/preview link)')
    if (hash && hash.length) {
      const match = real.toLowerCase() === hash.toLowerCase()
      console.log('\n  VERDICT:', match
        ? 'HASH MATCHES — the client would accept this pack.'
        : 'HASH MISMATCH — the client REJECTS this. Set resource-pack-sha1 to the "real sha1" above.')
    } else {
      console.log('\n  VERDICT: URL downloads a', looksZip ? 'valid zip' : 'NON-zip',
        '— server sent no hash to compare. (A missing/short hash can also cause rejection on some versions.)')
    }
  } catch (e) {
    console.log('\n=== download FAILED ===')
    console.log('  error:', e.message)
    console.log('  VERDICT: the client would show "Failed to download resource pack".')
    console.log('  -> URL is unreachable / not a direct download / blocked from this network.')
  }
  finish()
}

function finish() {
  setTimeout(() => { try { bot.quit() } catch {} process.exit(0) }, 500)
}

// High-level event (fires for both legacy play-state and 1.20.2+ config-state packs).
bot.on('resourcePack', (url, hash) => inspect(url, hash))

// Raw fallback: catch any packet whose name mentions resource_pack, across states.
bot._client.on('packet', (data, meta) => {
  if (!meta || !meta.name || !/resource_pack/i.test(meta.name)) return
  const url = data.url || data.location
  const hash = data.hash || ''
  if (url) inspect(url, hash)
})

bot.on('kicked', (reason) => { console.log('[kicked]', typeof reason === 'string' ? reason : JSON.stringify(reason)); process.exit(0) })
bot.on('error', (err) => { console.log('[error]', err.message) })
bot.on('end', (r) => { if (!handled) console.log('[end] disconnected before any resource pack was offered. reason:', r) })

// Safety: if no pack is offered within 20s, say so and exit.
setTimeout(() => {
  if (!handled) {
    console.log('\nNo resource-pack offer received within 20s.')
    console.log('-> Check server.properties has a resource-pack= URL set (and require-resource-pack / resource-pack-required).')
    try { bot.quit() } catch {}
    process.exit(0)
  }
}, 20000)
