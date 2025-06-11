const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const resourceDir = path.join(__dirname, 'public');
const manifest = [];

function getHash(buffer) {
    return crypto.createHash('md5').update(buffer).digest('hex');
}

function scanDir(dir, base = '') {
    const items = fs.readdirSync(dir);
    items.forEach(item => {
        const fullPath = path.join(dir, item);
        const relPath = path.join(base, item);
        const stat = fs.statSync(fullPath);

        if (stat.isDirectory()) {
            scanDir(fullPath, relPath);
        } else {
            const data = fs.readFileSync(fullPath);
            manifest.push({
                path: `static/${relPath.replace(/\\/g, '/')}`,
                hash: getHash(data)
            });
        }
    });
}

scanDir(resourceDir);
fs.writeFileSync('manifest.json', JSON.stringify(manifest, null, 2));
console.log('Manifest generated!');
