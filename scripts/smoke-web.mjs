import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(process.env.TRAIL_SENSE_WEB_TEST_PACKAGE);
const { chromium } = require("playwright");
const url = process.argv[2];
const browser = await chromium.launch({
    executablePath: process.env.CHROME_BIN,
    args: ["--enable-unsafe-swiftshader"]
});

async function waitForApp(page) {
    await page.waitForSelector('body[aria-busy="false"]', { state: "attached", timeout: 60_000 });
}

async function noteContents(page, workerUrl, seed) {
    return page.evaluate(async ({ workerUrl, seed }) => {
        const worker = new Worker(workerUrl);
        let nextId = 1;
        const request = (data) => new Promise((resolve, reject) => {
            const id = nextId++;
            const timeout = setTimeout(() => reject(new Error("No SQLite response for " + data.cmd)), 10_000);
            worker.onmessage = ({ data: response }) => {
                if (response.id !== id) return;
                clearTimeout(timeout);
                if (response.error) reject(new Error(response.error));
                else resolve(response.data);
            };
            worker.postMessage({ id, data });
        });

        try {
            const { databaseId } = await request({ cmd: "open", fileName: "smoke.db" });
            const execute = async (sql, bindings = []) => {
                const { statementId } = await request({ cmd: "prepare", databaseId, sql });
                const result = await request({ cmd: "step", statementId, bindings });
                worker.postMessage({ data: { cmd: "close", statementId } });
                return result;
            };
            if (seed) {
                await execute("CREATE TABLE notes (contents TEXT)");
                await execute("INSERT INTO notes VALUES (?)", ["Made it"]);
            }
            const { rows } = await execute("SELECT contents FROM notes");
            return rows;
        } finally {
            worker.terminate();
        }
    }, { workerUrl, seed });
}

try {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.on("pageerror", error => console.error(error));
    await page.goto(url);
    await waitForApp(page);
    const [sqliteWorker] = page.workers();
    assert.ok(sqliteWorker, "The app did not create its SQLite worker");
    const workerUrl = sqliteWorker.url();
    await page.waitForFunction(() => navigator.serviceWorker.controller !== null);
    await context.setOffline(true);
    await page.reload();
    await waitForApp(page);

    // An isolated browser context can reopen the same bundled worker without locking the app database.
    const fixture = await browser.newPage();
    const fixtureUrl = new URL("storage-smoke", url).href;
    await fixture.route(fixtureUrl, route => route.fulfill({ contentType: "text/html", body: "<title>SQLite smoke test</title>" }));
    await fixture.goto(fixtureUrl);
    assert.deepEqual(await noteContents(fixture, workerUrl, true), [["Made it"]]);
    await fixture.reload();
    assert.deepEqual(await noteContents(fixture, workerUrl, false), [["Made it"]]);
} finally {
    await browser.close();
}
