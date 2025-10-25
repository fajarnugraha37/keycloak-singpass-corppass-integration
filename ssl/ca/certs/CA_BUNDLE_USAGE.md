# 📋 CA Bundle Usage Examples

## cURL
`ash
curl --cacert ca/certs/ca-bundle.crt https://example.com
`

## Node.js
`javascript
process.env.SSL_CA = 'ca/certs/ca-bundle.crt';
// or
const https = require('https');
const fs = require('fs');
const ca = fs.readFileSync('ca/certs/ca-bundle.crt');
`

## Python requests
`python
import requests
response = requests.get('https://example.com', verify='ca/certs/ca-bundle.crt')
`

## OpenSSL verify
`ash
openssl verify -CAfile ca/certs/ca-bundle.crt certificate.crt
`

## Git (for HTTPS repositories)
`ash
git config --global http.sslcainfo ca/certs/ca-bundle.crt
`

## Environment Variable
`ash
export SSL_CERT_FILE=ca/certs/ca-bundle.crt
export REQUESTS_CA_BUNDLE=ca/certs/ca-bundle.crt
export CURL_CA_BUNDLE=ca/certs/ca-bundle.crt
`
