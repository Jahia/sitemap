export const jahiaProcessingConfig = {
    url: Cypress.env('JAHIA_PROCESSING_URL'),
    username: 'root',
    password: Cypress.env('SUPER_USER_PASSWORD'),
}
export const jahiaBrowsingConfig = {
    url: Cypress.env('JAHIA_URL'),
    username: 'root',
    password: Cypress.env('ADMIN_PASSWORD'),
}
