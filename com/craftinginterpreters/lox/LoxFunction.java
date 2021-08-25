package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;

    LoxFunction(Stmt.Function declaration){
        this.declaration = declaration;
    }

    @Override
    Object call(Interpreter interpreter, List<Object> arguments){
        //a new env is created when the func is called
        Environment environment = new Environment(interpreter.globals);
        for (int i=0; i < declaration.params.size(); i++){
            // the functions parameter (in the definition) are bound to the arguments
            // passed in when the func is called
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        interpreter.executeBlock(declaration.body, environment);
        return null;
    }
    @Override
    public int arity(){
        return declaration.params.size();
    }
    @Override
    public String toString(){
        return "<fn " + declaration.name.lexeme + ">";
    }
}