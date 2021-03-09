const jestConfig = require('@jahia/test-framework').jestConfig;

jestConfig['testPathIgnorePatterns'].push('<rootDir>/tests');

module.exports = jestConfig;
