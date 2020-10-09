const test1 = require('./Test_givenFormatFloat__giveDate__thenGetError')
const test2 = require('./Test_givenFormatFloat__giveDateTime__thenGetError')
const test3 = require('./Test_givenFormatFloat__giveFloat__thenGetFloat')
const test4 = require('./Test_givenFormatFloat__giveInt__thenGetInt')
const test5 = require('./Test_givenFormatFloat__giveString__thenGetError')
const test6 = require('./Test_givenFormatFloat__giveTime__thenGetError')
const test7 = require('./Test_givenFormatFloatFR__giveFloat__thenGetFloat')
const test8 = require('./Test_givenFormatFloatIT__giveFloat__thenGetFloat')
const test9 = require('./Test_givenFormatFloatUS__giveFloat__thenGetFloat')

export const testList = [
  test1, test2, test3, test4, test5, test6, test7, test8, test9
]
