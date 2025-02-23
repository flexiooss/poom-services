grammar PropertyFilter;

/* Lexical rules */

NOT : '!' ;

AND : '&&' ;
OR : '||' ;

LPAR : '(';
RPAR : ')';

COMMA : ',';

TRUE  : T R U E ;
FALSE : F A L S E ;
NULL : N U L L ;

IS_EMPTY : I S ' ' E M P T Y;
IS_NOT_EMPTY : I S ' ' N O T ' ' E M P T Y;

GT : '>' ;
GTE : '>=' ;
LT : '<' ;
LTE : '<=' ;
EQ : '==' ;
REQ : '=~' ;
NEQ : '!=' ;
STARTS_WITH : S T A R T S ' ' W I T H;
ENDS_WITH : E N D S ' ' W I T H;
CONTAINS : C O N T A I N S;
IN : I N;
ANY: A N Y;
STARTS_WITH_ANY : S T A R T S ' ' W I T H' ' A N Y;
ENDS_WITH_ANY : E N D S ' ' W I T H ' ' A N Y;
CONTAINS_ANY : C O N T A I N S ' ' A N Y;
CONTAINS_ALL : C O N T A I N S ' ' A L L;

/* Dates and times : watchout, order matters */
ZONED_DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[+\-][0-9][0-9]':'[0-9][0-9];
UTC_DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?'Z';
DATETIME_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?;

DATE_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9];

TIME_LITERAL: [0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'.'[0-9][0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?[0-9]?;

ZONED_DATETIME_WITHOUT_SFRAC_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9][+\-][0-9][0-9]':'[0-9][0-9];
UTC_DATETIME_WITHOUT_SFRAC_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9]'Z';
DATETIME_WITHOUT_SFRAC_LITERAL: [0-9][0-9][0-9][0-9]'-'[0-9][0-9]'-'[0-9][0-9]'T'[0-9][0-9]':'[0-9][0-9]':'[0-9][0-9];
TIME_WITHOUT_SFRAC_LITERAL: [0-9][0-9]':'[0-9][0-9]':'[0-9][0-9];

DECIMAL : '-'?[0-9]+('.'[0-9]+)? ;
IDENTIFIER : [a-zA-Z_\-.0-9]+ ;
QUOTED_STRING: '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';

PATTERN: '/' ~('/')* '/' I?;
PATTERN_OPT: 'i';

WS : [ \r\t\u000C\n]+ -> skip ;

/* Grammar rules */

criterion
    : expression EOF
    ;

expression
    : LPAR expression RPAR                                  #parenthesized
    | NOT expression                                        #negation
    | left=expression AND right=expression                  #and
    | left=expression OR right=expression                   #or
    | IDENTIFIER IS_EMPTY                                   #isEmpty
    | IDENTIFIER IS_NOT_EMPTY                               #isNotEmpty
    | IDENTIFIER REQ PATTERN                                #isMatchingPattern
    | IDENTIFIER operator operand                           #comparison
    | IDENTIFIER IN LPAR operand_list RPAR                  #in
    | IDENTIFIER IN LPAR RPAR                               #inEmpty
    | IDENTIFIER ANY IN LPAR operand_list RPAR              #anyIn
    | IDENTIFIER ANY IN LPAR RPAR                           #anyInEmpty
    | IDENTIFIER STARTS_WITH_ANY LPAR operand_list RPAR     #startsWithAny
    | IDENTIFIER STARTS_WITH_ANY LPAR RPAR                  #startsWithAnyEmpty
    | IDENTIFIER ENDS_WITH_ANY LPAR operand_list RPAR       #endsWithAny
    | IDENTIFIER ENDS_WITH_ANY LPAR RPAR                    #endsWithAnyEmpty
    | IDENTIFIER CONTAINS_ANY LPAR operand_list RPAR        #containsAny
    | IDENTIFIER CONTAINS_ANY LPAR RPAR                     #containsAnyEmpty
    | IDENTIFIER CONTAINS_ALL LPAR operand_list RPAR        #containsAll
    | IDENTIFIER CONTAINS_ALL LPAR RPAR                     #containsAllEmpty
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

operand_list
    : operand
    | operand_list COMMA operand
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