const assert = require('node:assert/strict');
const test = require('node:test');
const {
  buildDefaultOssUrl,
  buildObjectName,
  buildPublicUrl,
  normalizeAliyunConfig,
  parseDataUrlImage
} = require('../src/oss-utils');

test('normalizeAliyunConfig reads explicit config and validates required fields', () => {
  const config = normalizeAliyunConfig({
    region: 'oss-cn-hangzhou',
    bucket: 'demo-bucket',
    accessKeyId: 'id',
    accessKeySecret: 'secret',
    pathPrefix: ' image/output ',
    useSignedUrl: 'true'
  }, {});

  assert.equal(config.provider, 'aliyun');
  assert.equal(config.region, 'oss-cn-hangzhou');
  assert.equal(config.bucket, 'demo-bucket');
  assert.equal(config.pathPrefix, 'image/output');
  assert.equal(config.useSignedUrl, true);
});

test('normalizeAliyunConfig reports missing fields', () => {
  assert.throws(
    () => normalizeAliyunConfig({ bucket: 'demo' }, {}),
    /region, accessKeyId, accessKeySecret/
  );
});

test('parseDataUrlImage parses supported image data urls', () => {
  const parsed = parseDataUrlImage('data:image/png;base64,aGVsbG8=');
  assert.equal(parsed.contentType, 'image/png');
  assert.equal(parsed.extension, 'png');
  assert.equal(parsed.buffer.toString('utf8'), 'hello');
});

test('buildObjectName uses prefix, date folders, prompt slug, and extension', () => {
  const objectName = buildObjectName({
    prompt: '赛博 城市 / Cyber City',
    index: 1,
    contentType: 'image/jpeg',
    pathPrefix: '/ai//images/',
    now: new Date('2026-04-29T08:00:00.000Z')
  });

  assert.match(objectName, /^ai\/images\/2026\/04\/29\/20260429080000-2-赛博-城市-cyber-city-[a-f0-9]{8}\.jpg$/);
});

test('buildPublicUrl uses custom public base url when provided', () => {
  const url = buildPublicUrl({
    publicBaseUrl: 'https://cdn.example.com/assets/',
    bucket: 'bucket',
    region: 'oss-cn-hangzhou',
    secure: true
  }, 'ai/a.png');

  assert.equal(url, 'https://cdn.example.com/assets/ai/a.png');
});

test('buildDefaultOssUrl uses aliyun bucket endpoint when no custom domain is needed', () => {
  const url = buildDefaultOssUrl({
    bucket: 'gpt-image2-shadow',
    region: 'oss-cn-beijing',
    secure: false
  }, 'ai-images/a.png');

  assert.equal(url, 'http://gpt-image2-shadow.oss-cn-beijing.aliyuncs.com/ai-images/a.png');
});
