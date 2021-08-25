package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import static com.craftinginterpreters.lox.TokenType.*;
/*
Below is the expression Grammer

expression   → equality ;
equality     → comparison ( ( "!=" | "==" ) comparison )* ;
comparison   → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term         → factor ( ( "-" | "+" ) factor )* ;
factor       → unary ( ( "/" | "*" ) unary )* ;
unary        → ( "!" | "-" ) unary
               | primary ;
primary      → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;
-------------------------------------------------------------


Expression Grammer translated to Code: 

Grammar notation	Code representation
Terminal	        Code to match and consume a token
Nonterminal	        Call to that rule’s function
|	                if or switch statement
* or +	            while or for loop
?	                if statement
-------------------------------------------------------------


Statement Grammer:

program        → statement* EOF ;

statement      → exprStmt
               | printStmt ;

exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
---------------------------------------------------------------------

Variable grammer
program        → declaration* EOF ;

declaration    → varDecl
               | statement ;

statement      → exprStmt
               | printStmt ;
------------
Assignment Syntax
expression     → assignment ;
assignment     → IDENTIFIER "=" assignment
               | equality ;
-----------------
if Statement grammer
statement      → exprStmt
               | ifStmt
               | printStmt
               | block ;

ifStmt         → "if" "(" expression ")" statement
               ( "else" statement )? ;

-------
OR and AND grammer
expression     → assignment ;
assignment     → IDENTIFIER "=" assignment
               | logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
----------------
While loop grammer
statement      → exprStmt
               | ifStmt
               | printStmt
               | whileStmt
               | block ;

whileStmt      → "while" "(" expression ")" statement ;
---------------------

For-loop grammer
statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | whileStmt
               | block ;

forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                 expression? ";"
                 expression? ")" statement ;
----
Function grammer

declaration    → funDecl
               | varDecl
               | statement ;

funDecl        → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
*/


class Parser {
    private static class ParseError extends RuntimeException{}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }
    List<Stmt> parse(){
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()){
            statements.add(declaration());
        }
        return statements;
    }
    private Stmt declaration(){
        try{
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();

            return statement();
        }catch (ParseError error){
            synchronize();
            return null;
        }
    }
    private Stmt statement(){
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(IF)) return ifStatement();
        if (match(FOR)) return forStatement();
        return expressionStatement();
    }
    private Stmt forStatement(){
        //initializer
        consume(LEFT_PAREN, "Expected '(' after for dude.");
        Stmt initializer;
        if (match(SEMICOLON)){
            initializer = null;
        } else if (match(VAR)){
            initializer = varDeclaration();
        }else{
            initializer = expressionStatement();
        }
        //condition
        Expr condition = null;
        if (!check(SEMICOLON)){
            condition = expression();
        }
        consume(SEMICOLON, "Expected ';' after loop condition broh.");

        //increment
        Expr increment = null;
        if (!check(RIGHT_PAREN)){
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expected ')' after for clause.");
        //body statement
        Stmt body = statement();
        if (increment != null){
            body = new Stmt.Block(
                Arrays.asList(body, new Stmt.Expression(increment))
            );
        }
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);
        if (initializer != null){
            body = new Stmt.Block(Arrays.asList(initializer,body));
        }
        return body;
        
    }
    private Stmt ifStatement(){
        consume(LEFT_PAREN, "Expected '(' after 'if' my-dude.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after 'if' my-dude.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)){
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);

    }
    private Stmt printStatement(){
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value dude.");
        return new Stmt.Print(value);
    }
    private Stmt varDeclaration(){
        Token name = consume(IDENTIFIER, "Excepted variable name dude.");

        Expr initializer = null;
        if (match(EQUAL)){
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name,initializer);
    }
    private Stmt whileStatement(){
        consume(LEFT_PAREN, "Expected '(' after 'while' dude.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after condtion broh.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }
    private Stmt expressionStatement(){
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' after expression dude.");
        return new Stmt.Expression(expr);
    }
    private Stmt.Function function(String kind){
        Token name = consume(IDENTIFIER, "Expected " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)){
            do{
                if (parameters.size() >= 255){
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expected parameter name."));
            }while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expected '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }
    private List<Stmt> block(){
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()){
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Dude, totally Expected a '}' after block.");
        return statements;
    }
    private Expr expression(){
        return assignment();
    }
    private Expr assignment(){
        Expr expr = or();

        if (match(EQUAL)){
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable){
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "My-Dude, Invalid assignment target.");
        }
        return expr;
    }
    private Expr or(){
        Expr expr = and();
        while(match(OR)){
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr and(){
        Expr expr = equality();

        while (match(AND)){
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr equality(){
        Expr expr = comparision();

        while (match(BANG_EQUAL, EQUAL_EQUAL)){
            Token operator = previous();
            Expr right = comparision();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    private Expr comparision(){
        Expr expr = term();

        while(match(GREATER,GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    //Addition/Subtraction
    private Expr term(){
        Expr expr = factor();

        while (match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    //Multiplication/Division
    private Expr factor(){
        Expr expr = unary();

        while (match(SLASH,STAR)){
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    private Expr unary(){
        if (match(BANG, MINUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }
    private Expr finishCall(Expr callee){
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)){
            do {
                if (arguments.size() >= 255){
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            }while(match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN, "Expected ')' after argument.");
        return new Expr.Call(callee, paren, arguments);
    }
    private Expr call(){
        Expr expr = primary();

        while (true){
            if (match(LEFT_PAREN)){
                expr = finishCall(expr);
            }else{
                break;
            }
        }
        return expr;
    }
    private Expr primary(){
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }
        if (match(IDENTIFIER)){
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)){
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression, my-dude.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }
    private boolean match(TokenType... types){
        for (TokenType type : types){
            if (check(type)){
                advance();
                return true;
            }
        }
        return false;
    }
    private Token consume(TokenType type, String message){
        if (check(type)) return advance();
        System.out.println("Consume: " + peek());
        throw error(peek(), message);
    }
    private ParseError error(Token token, String message){
        Lox.error(token, message);
        return new ParseError();
    }
    private void synchronize(){
        advance();

        while (!isAtEnd()){
            //if end of statement stop
            if (previous().type == SEMICOLON) return;
            //Discards tokens until we reach statement boundary
            switch (peek().type){
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
    private boolean check(TokenType type){
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    private Token advance() {
        if(!isAtEnd()) current++;
        return previous();
    }
    private boolean isAtEnd(){
        return peek().type == EOF;
    }
    private Token peek(){
        return tokens.get(current);
    }
    private Token previous(){
        return tokens.get(current -1);
    }
}
