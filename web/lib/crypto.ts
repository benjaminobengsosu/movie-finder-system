import crypto from 'crypto';

// Secret application-level pepper identical to the Android application
export const AUTH_PASSWORD_PEPPER = "02456504a869a6e52b03d281897c926ddb2bfc1f88f5b1f678b064c49858989b";

const PBKDF2_ITERATIONS = 10000;
const KEY_LENGTH_BYTES = 32; // 256 bits

/**
 * Generates a cryptographically random 16-byte salt as a 32-character hex string.
 */
export function generateSalt(): string {
  return crypto.randomBytes(16).toString('hex');
}

/**
 * Hashes a password using per-user salt and shared application pepper via PBKDF2 HMAC-SHA256.
 */
export function hashPassword(password: string, saltHex: string, pepper: string = AUTH_PASSWORD_PEPPER): string {
  const pepperedPassword = `${password}:${pepper}`;
  const saltBuffer = Buffer.from(saltHex, 'hex');
  const derivedKey = crypto.pbkdf2Sync(
    pepperedPassword,
    saltBuffer,
    PBKDF2_ITERATIONS,
    KEY_LENGTH_BYTES,
    'sha256'
  );
  return derivedKey.toString('hex');
}

/**
 * Verifies an entered password against stored salt and hash using constant-time comparison.
 */
export function verifyPassword(
  enteredPass: string,
  saltHex: string,
  storedHash: string,
  pepper: string = AUTH_PASSWORD_PEPPER
): boolean {
  try {
    const computed = hashPassword(enteredPass, saltHex, pepper);
    return crypto.timingSafeEqual(Buffer.from(computed, 'hex'), Buffer.from(storedHash, 'hex'));
  } catch {
    return false;
  }
}

/**
 * Generates 5 emergency recovery backup codes (RECV-XXXX-XXXX).
 */
export function generateRecoveryCodes(): string[] {
  const alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
  const codes: string[] = [];
  for (let i = 0; i < 5; i++) {
    let part1 = '';
    let part2 = '';
    const bytes = crypto.randomBytes(8);
    for (let j = 0; j < 4; j++) part1 += alphabet[bytes[j] % alphabet.length];
    for (let j = 4; j < 8; j++) part2 += alphabet[bytes[j] % alphabet.length];
    codes.push(`RECV-${part1}-${part2}`);
  }
  return codes;
}
