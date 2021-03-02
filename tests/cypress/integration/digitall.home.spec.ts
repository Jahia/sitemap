import { digitall } from '../page-object/digitall.home.page'

describe('navigation to user', () => {
    it('navigates to the users page successfully', function () {
        digitall.goTo().editPage('Our Companies').clickOnSitemap().validateSucessMessage()
    })
})
