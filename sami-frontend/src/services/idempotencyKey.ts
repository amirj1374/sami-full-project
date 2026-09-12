let fallbackCounter = 0

function randomUuidFromValues(values: Uint8Array): string {
  values[6] = (values[6] & 0x0f) | 0x40
  values[8] = (values[8] & 0x3f) | 0x80

  const hex = Array.from(values, (value) => value.toString(16).padStart(2, '0'))
  return [
    hex.slice(0, 4).join(''),
    hex.slice(4, 6).join(''),
    hex.slice(6, 8).join(''),
    hex.slice(8, 10).join(''),
    hex.slice(10, 16).join(''),
  ].join('-')
}

function fallbackKey(): string {
  fallbackCounter += 1
  const now = Date.now().toString(36)
  const highResolution = typeof performance !== 'undefined'
    ? Math.floor(performance.now() * 1000).toString(36)
    : '0'
  return `sami-${now}-${highResolution}-${fallbackCounter.toString(36)}`
}

export function createClientIdempotencyKey(): string {
  const cryptoApi = globalThis.crypto

  if (typeof cryptoApi?.randomUUID === 'function') {
    try {
      return cryptoApi.randomUUID()
    } catch {
      // Fall through when a browser exposes randomUUID but rejects this context.
    }
  }

  if (typeof cryptoApi?.getRandomValues === 'function') {
    try {
      return randomUuidFromValues(cryptoApi.getRandomValues(new Uint8Array(16)))
    } catch {
      // Fall through to the process-local monotonic key as a last resort.
    }
  }

  return fallbackKey()
}
