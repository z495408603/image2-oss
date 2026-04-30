const crypto = require('node:crypto');

const SUPPORTED_CONTENT_TYPES = new Map([
  ['image/png', 'png'],
  ['image/jpeg', 'jpg'],
  ['image/webp', 'webp']
]);

function cleanString(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function toBoolean(value, fallback = false) {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase();
    if (['true', '1', 'yes', 'on'].includes(normalized)) return true;
    if (['false', '0', 'no', 'off'].includes(normalized)) return false;
  }
  return fallback;
}

function joinPrefix(prefix) {
  const cleaned = cleanString(prefix)
    .replace(/\\/g, '/')
    .replace(/^\/+|\/+$/g, '')
    .replace(/\/{2,}/g, '/');
  return cleaned ? `${cleaned}/` : '';
}

function slugifyPrompt(prompt) {
  const slug = cleanString(prompt)
    .toLowerCase()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 48);
  return slug || 'ai-image';
}

function getExtension(contentType) {
  const extension = SUPPORTED_CONTENT_TYPES.get(contentType);
  if (!extension) {
    throw new Error(`不支持的图片类型：${contentType || 'unknown'}`);
  }
  return extension;
}

function parseDataUrlImage(input) {
  const value = cleanString(input);
  const match = value.match(/^data:(image\/(?:png|jpeg|webp));base64,([a-z0-9+/=\r\n]+)$/i);
  if (!match) {
    throw new Error('图片数据必须是 PNG、JPG 或 WEBP 的 Data URL');
  }

  const contentType = match[1].toLowerCase();
  const base64 = match[2].replace(/\s/g, '');
  const buffer = Buffer.from(base64, 'base64');
  if (!buffer.length) {
    throw new Error('图片数据为空');
  }

  return { buffer, contentType, extension: getExtension(contentType) };
}

function buildObjectName({ prompt, index = 0, contentType, pathPrefix, now = new Date() }) {
  const extension = getExtension(contentType);
  const yyyy = String(now.getFullYear());
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  const stamp = now.toISOString().replace(/[-:.TZ]/g, '').slice(0, 14);
  const random = crypto.randomBytes(4).toString('hex');
  const safeIndex = Number.isFinite(Number(index)) ? Math.max(0, Number(index)) + 1 : 1;
  return `${joinPrefix(pathPrefix)}${yyyy}/${mm}/${dd}/${stamp}-${safeIndex}-${slugifyPrompt(prompt)}-${random}.${extension}`;
}

function normalizeAliyunConfig(input = {}, env = process.env) {
  const config = {
    provider: cleanString(input.provider || env.OSS_PROVIDER || 'aliyun').toLowerCase(),
    region: cleanString(input.region || env.OSS_REGION),
    bucket: cleanString(input.bucket || env.OSS_BUCKET),
    accessKeyId: cleanString(input.accessKeyId || env.OSS_ACCESS_KEY_ID),
    accessKeySecret: cleanString(input.accessKeySecret || env.OSS_ACCESS_KEY_SECRET),
    stsToken: cleanString(input.stsToken || env.OSS_STS_TOKEN),
    endpoint: cleanString(input.endpoint || env.OSS_ENDPOINT),
    pathPrefix: cleanString(input.pathPrefix || env.OSS_PATH_PREFIX || 'ai-images'),
    publicBaseUrl: cleanString(input.publicBaseUrl || env.OSS_PUBLIC_BASE_URL),
    secure: toBoolean(input.secure ?? env.OSS_SECURE, true),
    useSignedUrl: toBoolean(input.useSignedUrl ?? env.OSS_USE_SIGNED_URL, false),
    signedUrlExpires: Number(input.signedUrlExpires || env.OSS_SIGNED_URL_EXPIRES || 3600)
  };

  if (config.provider !== 'aliyun') {
    throw new Error('当前后端仅内置阿里云 OSS provider');
  }

  const missing = [];
  for (const key of ['region', 'bucket', 'accessKeyId', 'accessKeySecret']) {
    if (!config[key]) missing.push(key);
  }
  if (missing.length) {
    throw new Error(`OSS 配置缺少字段：${missing.join(', ')}`);
  }

  if (!Number.isFinite(config.signedUrlExpires) || config.signedUrlExpires <= 0) {
    config.signedUrlExpires = 3600;
  }

  return config;
}

function buildPublicUrl(config, objectName) {
  if (config.publicBaseUrl) {
    return `${config.publicBaseUrl.replace(/\/+$/g, '')}/${objectName}`;
  }

  return buildDefaultOssUrl(config, objectName);
}

function buildDefaultOssUrl(config, objectName) {
  if (config.endpoint) {
    const endpoint = config.endpoint.replace(/^https?:\/\//, '').replace(/\/+$/g, '');
    return `${config.secure ? 'https' : 'http'}://${config.bucket}.${endpoint}/${objectName}`;
  }

  return `${config.secure ? 'https' : 'http'}://${config.bucket}.${config.region}.aliyuncs.com/${objectName}`;
}

module.exports = {
  SUPPORTED_CONTENT_TYPES,
  buildDefaultOssUrl,
  buildObjectName,
  buildPublicUrl,
  normalizeAliyunConfig,
  parseDataUrlImage
};
