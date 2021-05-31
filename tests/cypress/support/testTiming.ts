let commands = [];
let testAttributes;

// sends test results to the plugins process
// using cy.task https://on.cypress.io/task
const sendTestTimings = () => {
    if (!testAttributes) {
        return
    }

    const attr = testAttributes;

    testAttributes = null;

    cy.task('testTimings', attr)
};

beforeEach(sendTestTimings);

after(sendTestTimings);

Cypress.on('test:before:run', () => {
    commands.length = 0
});

Cypress.on('test:after:run', (attributes) => {
    /* eslint-disable no-console */
    console.log('Test "%s" has finished in %dms', attributes.title, attributes.duration)
    console.table(commands);
    testAttributes = {
        title: attributes.title,
        duration: attributes.duration,
        commands: Cypress._.cloneDeep(commands),
    }
});

Cypress.on('command:start', (c) => {
    console.log('command start', c.attributes.name);
    commands.push({
        name: c.attributes.name,
        started: +new Date(),
    })
});

Cypress.on('command:end', (c) => {
    console.log('command end', c.attributes.name);
    const lastCommand = commands[commands.length - 1];

    if (lastCommand.name !== c.attributes.name) {
        throw new Error('Last command is wrong')
    }

    lastCommand.endedAt = +new Date();
    lastCommand.elapsed = lastCommand.endedAt - lastCommand.started
});
