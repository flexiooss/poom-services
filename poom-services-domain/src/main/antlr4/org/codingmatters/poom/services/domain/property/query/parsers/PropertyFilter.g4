grammar PropertyFilter;

/* Lexical rules */

NOT : '!' ;

AND : '&&' ;
OR : '||' ;

LPAR : '(';
RPAR : ')';

TRUE  : 'true' ;
FALSE : 'false' ;
NULL : 'null' ;

GT : '>' ;
GTE : '>=' ;
LT : '<' ;
LTE : '<=' ;
EQ : '==' ;
NEQ : '!=' ;
STARTS_WITH : 'starts with';
ENDS_WITH : 'ends with';
CONTAINS : 'contains';

/* Dates and times : watchout, order matters */
ZONED_DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9][0-9][+\-][0-9][0-9]':'[0-9][0-9];
UTC_DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9][0-9]'Z';
DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9][0-9];

DATE_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9];

TIME_LITERAL: [0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9][0-9][0-9]?[0-9]?[0-9]?;

DECIMAL : '-'?[0-9]+('.'[0-9]+)? ;
IDENTIFIER : [a-zA-Z_\-.0-9]+ ;
QUOTED_STRING: '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';

WS : [ \r\t\u000C\n]+ -> skip ;

/* Grammar rules */

criterion
    : expression EOF
    ;

expression
    : LPAR expression RPAR                      #parenthesized
    | NOT expression                            #negation
    | left=expression AND right=expression      #and
    | left=expression OR right=expression       #or
    | IDENTIFIER operator operand               #comparison
    ;

operand
    : IDENTIFIER                    #propertyOperand
    | DECIMAL                       #decimalOperand
    | QUOTED_STRING                 #stringOperand
    | TRUE                          #trueOperand
    | FALSE                         #falseOperand
    | NULL                          #nullOperand
    | TIME_LITERAL                  #timeOperand
    | UTC_TIME_LITERAL                  #timeOperand
    | ZONED_TIME_LITERAL                  #timeOperand
    | DATETIME_LITERAL              #datetimeOperand
    | UTC_DATETIME_LITERAL          #utcDatetimeOperand
    | ZONED_DATETIME_LITERAL        #zonedDatetimeOperand
    | DATE_LITERAL                  #dateOperand
    ;

operator
    : GT
    | GTE
    | LT
    | LTE
    | EQ
    | NEQ
    | STARTS_WITH
    | ENDS_WITH
    | CONTAINS
    ;
