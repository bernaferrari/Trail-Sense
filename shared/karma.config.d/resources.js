const resources = "kotlin/composeResources";
config.files.push({ pattern: resources + "/**/*", included: false, served: true, watched: false });
config.proxies["/composeResources/"] = "/base/" + resources + "/";
config.browserNoActivityTimeout = 120_000;
config.browserDisconnectTimeout = 10_000;
