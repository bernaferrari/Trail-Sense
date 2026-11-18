import initSqlite from "@sqlite.org/sqlite-wasm";

const databases = new Map();
const statements = new Map();
let nextId = 1;

// This VFS uses OPFS without requiring cross-origin isolation on a static host.
const storage = initSqlite().then(async sqlite => ({
    sqlite,
    pool: await sqlite.installOpfsSAHPoolVfs({
        name: "trail-sense",
        directory: ".trail-sense-sqlite",
        preserveOnInitFailure: true
    })
}));
storage.catch(() => {});

self.onmessage = async ({ data: { id, data: request } }) => {
    try {
        const { sqlite, pool } = await storage;
        const data = execute(sqlite, pool, request);
        if (request.cmd !== "close") self.postMessage({ id, data });
    } catch (error) {
        if (request.cmd !== "close") self.postMessage({ id, error: String(error.message ?? error) });
    }
};

function get(handles, id) {
    const handle = handles.get(id);
    if (!handle) throw new Error("SQLite handle is closed");
    return handle;
}

// Commands follow the AndroidX WebWorkerSQLiteDriver protocol.
function execute(sqlite, pool, request) {
    switch (request.cmd) {
        case "open": {
            const databaseId = nextId++;
            const fileName = request.fileName.startsWith("/") ? request.fileName : "/" + request.fileName;
            databases.set(databaseId, new pool.OpfsSAHPoolDb(fileName));
            return { databaseId };
        }
        case "prepare": {
            const statement = get(databases, request.databaseId).prepare(request.sql);
            const statementId = nextId++;
            statements.set(statementId, statement);
            return {
                statementId,
                parameterCount: sqlite.capi.sqlite3_bind_parameter_count(statement),
                columnNames: Array.from({ length: statement.columnCount }, (_, index) =>
                    sqlite.capi.sqlite3_column_name(statement, index))
            };
        }
        case "step": {
            const statement = get(statements, request.statementId);
            statement.reset();
            statement.clearBindings();
            if (request.bindings.length) statement.bind(request.bindings);
            const rows = [];
            const columnTypes = [];
            while (statement.step()) {
                if (!rows.length) {
                    for (let index = 0; index < statement.columnCount; index++) {
                        columnTypes.push(sqlite.capi.sqlite3_column_type(statement, index));
                    }
                }
                rows.push(statement.get([]));
            }
            return { rows, columnTypes };
        }
        case "close":
            if (request.statementId !== undefined) {
                get(statements, request.statementId).finalize();
                statements.delete(request.statementId);
            }
            if (request.databaseId !== undefined) {
                get(databases, request.databaseId).close();
                databases.delete(request.databaseId);
            }
            return;
        default:
            throw new Error("Unknown SQLite request");
    }
}
