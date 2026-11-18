import { createRequire } from "node:module";

const require = createRequire(process.env.TRAIL_SENSE_WEB_TEST_PACKAGE);
const { chromium } = require("playwright");
const browser = await chromium.launch({
    executablePath: process.env.CHROME_BIN,
    args: ["--enable-unsafe-swiftshader"]
});
try {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.on("pageerror", error => console.error(error));
    await page.goto(process.argv[2]);
    await page.waitForSelector('body[aria-busy="false"]', { state: "attached", timeout: 60_000 });
    await page.waitForFunction(() => navigator.serviceWorker.controller !== null);
    await context.setOffline(true);
    await page.reload();
    await page.waitForSelector('body[aria-busy="false"]', { state: "attached", timeout: 60_000 });
} finally {
    await browser.close();
}
