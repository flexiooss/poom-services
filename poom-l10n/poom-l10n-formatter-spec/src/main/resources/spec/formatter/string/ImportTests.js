const test1 = require('./Test_givenFormatString__giveDate__thenGetError')
const test2 = require('./Test_givenFormatString__giveDateTime__thenGetError')
const test3 = require('./Test_givenFormatString__giveTime__thenGetError')
const test4 = require('./Test_givenFormatString__giveString__thenGetString')
const test5 = require('./Test_givenFormatString__giveFloat__thenGetError')

export const testList = [
  test1, test2, test3, test4, test5
]
