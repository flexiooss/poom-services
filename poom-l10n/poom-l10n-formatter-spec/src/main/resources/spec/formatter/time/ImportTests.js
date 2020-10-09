const test1 = require('./Test_givenFormatTime__giveDate__thenGetError')
const test2 = require('./Test_givenFormatTime__giveDateTime__thenGetError')
const test3 = require('./Test_givenFormatTime__giveFloat__thenGetError')
const test4 = require('./Test_givenFormatTime__giveInt__thenGetError')
const test5 = require('./Test_givenFormatTime__giveString__thenGetError')
const test6 = require('./Test_givenFormatTime__giveTime__thenGetError')
const test7 = require('./Test_givenFormatTimeEN__giveTime__thenGetError')
const test8 = require('./Test_givenFormatTimeFR__giveTime__thenGetError')

export const testList = [
  test1, test2, test3, test4, test5, test6, test7, test8
]
