import { users } from '../page-object/users.page'

describe('navigation to user', () => {
    it('navigates to the users page successfully', function () {
        users.goTo()
    })
})
