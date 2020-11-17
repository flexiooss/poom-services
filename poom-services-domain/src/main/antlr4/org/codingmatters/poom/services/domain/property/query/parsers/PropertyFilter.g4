grammar PropertyFilter;

/* Lexical rules */

NOT : '!' ;

AND : '&&' ;
OR : '||' ;

LPAR : '(';
RPAR : ')';

TRUE  : T R U E ;
FALSE : F A L S E ;
NULL : N U L L ;

GT : '>' ;
GTE : '>=' ;
LT : '<' ;
LTE : '<=' ;
EQ : '==' ;
NEQ : '!=' ;
STARTS_WITH : S T A R T S ' ' W I T H;
ENDS_WITH : E N D S ' ' W I T H;
CONTAINS : C O N T A I N S;

/* Dates and times : watchout, order matters */
ZONED_DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9][0-9][0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[+\-][0-9][0-9]':'[0-9][0-9];
UTC_DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9][0-9][0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?'Z';
DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9][0-9][0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?;

DATE_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9];

TIME_LITERAL: [0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9][0-9][0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?;

ZONED_DATETIME_WITHOUT_SFRAC_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9][+\-][0-9][0-9]':'[0-9][0-9];
UTC_DATETIME_WITHOUT_SFRAC_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'Z';
DATETIME_WITHOUT_SFRAC_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9];
TIME_WITHOUT_SFRAC_LITERAL: [0-9][0-9]':'[0-9][0-9]':'[0-9][0-9];

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
    | TIME_WITHOUT_SFRAC_LITERAL                  #timeOperand
    | DATETIME_LITERAL              #datetimeOperand
    | DATETIME_WITHOUT_SFRAC_LITERAL              #datetimeOperand
    | UTC_DATETIME_LITERAL          #utcDatetimeOperand
    | UTC_DATETIME_WITHOUT_SFRAC_LITERAL          #utcDatetimeOperand
    | ZONED_DATETIME_LITERAL        #zonedDatetimeOperand
    | ZONED_DATETIME_WITHOUT_SFRAC_LITERAL        #zonedDatetimeOperand
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



fragment A : [aA]; // match either an 'a' or 'A'
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];