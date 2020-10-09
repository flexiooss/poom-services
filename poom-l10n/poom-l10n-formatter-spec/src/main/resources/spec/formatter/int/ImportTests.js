const test1 = require('./Test_givenFormatInt__giveDate__thenGetError')
const test2 = require('./Test_givenFormatInt__giveDateTime__thenGetError')
const test3 = require('./Test_givenFormatInt__giveFloat__thenGetError')
const test4 = require('./Test_givenFormatInt__giveInt__thenGetInt')
const test5 = require('./Test_givenFormatInt__giveString__thenGetError')
const test6 = require('./Test_givenFormatInt__giveTime__thenGetError')
const test7 = require('./Test_givenFormatIntFR__giveInt__thenGetInt')
const test8 = require('./Test_givenFormatIntIT__giveInt__thenGetInt')
const test9 = require('./Test_givenFormatIntUS__giveInt__thenGetInt')

export const testList = [
  test1, test2, test3, test4, test5, test6, test7, test8, test9
]
