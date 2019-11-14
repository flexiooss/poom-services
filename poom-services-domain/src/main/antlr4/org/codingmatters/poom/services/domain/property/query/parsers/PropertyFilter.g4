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
LTE : '<=>' ;
EQ : '==' ;
STARTS_WITH : 'starts with';
ENDS_WITH : 'ends with';
CONTAINS : 'contains';

// DECIMAL, IDENTIFIER, COMMENTS, WS are set using regular expressions

DECIMAL : '-'?[0-9]+('.'[0-9]+)? ;
IDENTIFIER : [a-zA-Z_]+[a-zA-Z_\-.0-9]* ;
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
    ;

operator
    : GT
    | GTE
    | LT
    | LTE
    | EQ
    | STARTS_WITH
    | ENDS_WITH
    | CONTAINS
    ;
