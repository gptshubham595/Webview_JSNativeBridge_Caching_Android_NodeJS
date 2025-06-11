const express = require('express');
const fs = require('fs');
const path = require('path');
const app = express();
const PORT = 3000;

// app.use(cors());
app.use('/static', express.static(path.join(__dirname, 'public')));

app.get('/manifest', (req, res) => {
    const manifest = JSON.parse(fs.readFileSync('./manifest.json', 'utf8'));
    res.json(manifest);
});

app.listen(PORT, () => {
    console.log(`Backend running at http://localhost:${PORT}`);
});
