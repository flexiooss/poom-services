grammar PropertySort;

/* Lexical rules */

COMMA : ',';
ASC : 'asc';
DESC : 'desc';

// DECIMAL, IDENTIFIER, COMMENTS, WS are set using regular expressions

DECIMAL : '-'?[0-9]+('.'[0-9]+)? ;
IDENTIFIER : [a-zA-Z_\-.0-9]+ ;
QUOTED_STRING: '\'' ('\\'. | '\'\'' | ~('\'' | '\\'))* '\'';

WS : [ \r\t\u000C\n]+ -> skip ;

/* Grammar rules */

sortCriterion
    : sortExpression EOF
    ;

sortExpression
    : sortExpression COMMA sortExpression
    | propertyExpression
    ;

propertyExpression
    : IDENTIFIER sortDirection
    | IDENTIFIER
    ;

sortDirection
    : ASC
    | DESC
    ;