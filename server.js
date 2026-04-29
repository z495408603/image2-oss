const express = require('express');
const path = require('node:path');
const OSS = require('ali-oss');
const {
  buildObjectName,
  buildPublicUrl,
  normalizeAliyunConfig,
  parseDataUrlImage
} = require('./src/oss-utils');

const app = express();
const port = Number(process.env.PORT || 8320);

app.use(express.json({ limit: process.env.JSON_LIMIT || '60mb' }));
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', process.env.CORS_ORIGIN || '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,POST,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type,Authorization');
  if (req.method === 'OPTIONS') {
    res.sendStatus(204);
    return;
  }
  next();
});

function createAliyunClient(config) {
  return new OSS({
    region: config.region,
    bucket: config.bucket,
    accessKeyId: config.accessKeyId,
    accessKeySecret: config.accessKeySecret,
    stsToken: config.stsToken || undefined,
    endpoint: config.endpoint || undefined,
    secure: config.secure
  });
}

function toErrorResponse(error) {
  return {
    error: error && error.message ? error.message : '未知错误'
  };
}

app.get('/health', (req, res) => {
  res.json({ ok: true, service: 'image2-oss', provider: 'aliyun' });
});

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'gpt-image.html'));
});

app.get('/gpt-image.html', (req, res) => {
  res.sendFile(path.join(__dirname, 'gpt-image.html'));
});

app.post('/api/oss/validate', (req, res) => {
  try {
    const config = normalizeAliyunConfig(req.body?.oss || req.body || {});
    res.json({
      ok: true,
      provider: config.provider,
      bucket: config.bucket,
      region: config.region,
      pathPrefix: config.pathPrefix,
      uploadMode: config.useSignedUrl ? 'private-signed-url' : 'public-url'
    });
  } catch (error) {
    res.status(400).json(toErrorResponse(error));
  }
});

app.post('/api/oss/upload', async (req, res) => {
  try {
    const config = normalizeAliyunConfig(req.body?.oss || {});
    const image = parseDataUrlImage(req.body?.image);
    const objectName = buildObjectName({
      prompt: req.body?.prompt,
      index: req.body?.index,
      contentType: image.contentType,
      pathPrefix: config.pathPrefix
    });
    const client = createAliyunClient(config);

    await client.put(objectName, image.buffer, {
      headers: {
        'Content-Type': image.contentType,
        'Cache-Control': 'public, max-age=31536000, immutable'
      }
    });

    const url = config.useSignedUrl
      ? client.signatureUrl(objectName, { expires: config.signedUrlExpires })
      : buildPublicUrl(config, objectName);

    res.json({
      ok: true,
      provider: config.provider,
      bucket: config.bucket,
      objectName,
      contentType: image.contentType,
      size: image.buffer.length,
      url
    });
  } catch (error) {
    const status = /配置缺少|必须是|不支持|为空/.test(error.message || '') ? 400 : 502;
    res.status(status).json(toErrorResponse(error));
  }
});

const server = app.listen(port, () => {
  console.log(`image2-oss server listening on http://localhost:${port}`);
});

server.on('error', error => {
  if (error.code === 'EADDRINUSE') {
    console.error(`端口 ${port} 已被占用。请先关闭旧的上传服务，或使用其他端口启动：$env:PORT=8321; npm start`);
    process.exit(1);
  }

  console.error(error);
  process.exit(1);
});
