grammar SimpleLang; // grammar for the langauge

prog : dec + EOF; // program consits of a function (atleast main), EOF means can have more than one

dec
    : typed_idfr LParen vardec? RParen body // declaration of a function, it has a type, name, potential paramaters (?) and a body
;

vardec
    : typed_idfr (Comma typed_idfr)* // parameters for function, seperated by  comma if there is more than one
;

typed_idfr
    : type Idfr // an typed identifier has a type and a name (identifier) such as int x or bool y
;


type
    : IntType | BoolType | UnitType // only types we have are int, bool and unit. They are used for describing indentifiers.
;

body
    : LBrace ene+=exp (Semicolon ene+=exp)* RBrace // body of code is main part of a function inbetween the {} brackets
;

block
    : LBrace ene+=exp (Semicolon ene+=exp)* RBrace // same as a body, but used as an expression inside a body, e.g while (exp) Do block, if (exp) then {block} else {block}
;


exp // defines what an expression can be

    : Idfr Assign exp                                       #AssignExpr // assinging an expresion to an indentiier, e.g x = y = z, a = 1
    | typed_idfr Assign exp                                 #TypedAssignExpr // allows typed assingments e.g int a = 1;
    | LParen exp RParen                                     #ParenExpr // Allows multiple brackets around an exp e.g ((exp))
    | LParen exp binop exp RParen                           #BinOpExpr // binary operation between two expressions that returns int or bool, inbetween two () e.g (a + b), (5 < 6), using one of the binary operations
    | Idfr arguments                                        #InvokeExpr // (allows) / passes in list of argument expressions into the function. Similar to parameters but used when calling a function
    | block                                                 #BlockExpr // block of code
    | If exp Then block Else block                          #IfExpr // if then else structure that allows for selection, uses exp for if and blocks for the exps outcome dependant code
    | Repeat block Until exp                                #RepeatUntilExpr // repeats a block until a exp is true
    | Print exp                                             #PrintExpr // print expression for printing, e.g print x
    | While exp  Do block                                   #WhileExpr // while an exp is true it continually runs block of code
    | Space                                                 #SpaceExpr // Simple Space, doesnt have significance. e.g x * y == x*y
    | NewLine                                               #NewLineExpr // New line expression
    | Idfr                                                  #IdExpr // indentifier expresion, e.g. x, y, z
    | IntLit                                                #IntExpr // integer literal expression, e.g 0, 1, 2, -5
    | BoolLit                                               #BoolExpr // boolean literal expression, e.g true or false
;

arguments
    :LParen (args+=exp (Comma args+=exp)*)? RParen // can have mutiple or no arguments, passed into function when it is called e.g fun(int x) -> fun(5)
;

// binary operations, used between numbers or booleans in an expression to return a boolean
binop
    : Eq              #EqBinop
    | Less            #LessBinop
    | Greater         #GreaterBinop
    | LessEq          #LessEqBinop
    | GreaterEq       #GreaterEqBinop
    | Plus            #PlusBinop
    | Minus           #MinusBinop
    | Times           #TimesBinop
    | Divide          #DivideBinop
    | And             #AndBinop
    | Or              #OrBinop
    | Xor             #XorBinop
;

// define structure of language

LParen : '(' ;
Comma : ',' ;
RParen : ')' ;
LBrace : '{' ;
Semicolon : ';' ;
RBrace : '}' ;

// binary operations that return boolean types

Eq : '==' ;
Less : '<' ;
LessEq : '<=' ;
Greater : '>' ;
GreaterEq : '>=' ;
And : '&' ;
Or: '|' ;
Xor: '^' ;


// binary operations that return integer types

Plus : '+' ;
Times : '*' ;
Minus : '-' ;
Divide : '/' ;

Assign : ':=' ; // assign values, this is of unit type

// flow control words used in expressions

Print : 'print' ;
Space : 'space' ;
NewLine : 'newline' ;
If : 'if' ;
Then : 'then' ;
Else : 'else' ;
While : 'while' ;
Do : 'do' ;
Repeat: 'repeat' ;
Until: 'until' ;

IntType : 'int' ; // integers
BoolType : 'bool' ; // true or false
UnitType : 'unit' ; // defines rest of expressions e.g x := y, print x, default dummy value of skip?

BoolLit : 'true' | 'false' ; // booleans are only true or false
IntLit : '0' | ('-'? [1-9][0-9]*) ; // integers can be 0 or a negative or positive int 0 to 9999...
Idfr : [a-z][A-Za-z0-9_]* ; // identifier names are (alpha, numeric or underscores) string values of any length
WS : [ \n\r\t]+ -> skip ; // White space is skipped