// eslint-disable-next-line @typescript-eslint/no-var-requires
const cypressTypeScriptPreprocessor = require("./cy-ts-preprocessor");

module.exports = (on, config) => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    require("@cypress/code-coverage/task")(on, config);
    
    //https://github.com/archfz/cypress-terminal-report
    // eslint-disable-next-line @typescript-eslint/no-var-requires    
    require('cypress-terminal-report/src/installLogsPrinter')(on);
    on("file:preprocessor", cypressTypeScriptPreprocessor);
    return config;
};