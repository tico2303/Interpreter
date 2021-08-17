package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;
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
            if (match(VAR)) return varDeclaration();

            return statement();
        }catch (ParseError error){
            synchronize();
            return null;
        }
    }
    private Stmt statement(){
        if (match(PRINT)) return printStatement();
        return expressionStatement();
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
    private Stmt expressionStatement(){
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' after expression dude.");
        return new Stmt.Expression(expr);
    }
    private Expr expression(){
        return equality();
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
        return primary();
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
            consume(RIGHT_PAREN, "Expect ')' after expression.");
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
