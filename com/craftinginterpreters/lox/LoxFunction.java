package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    LoxFunction(Stmt.Function declaration, Environment closure){
        this.closure = closure;
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments){
        //a new env is created when the func is called
        Environment environment = new Environment(closure);
        for (int i=0; i < declaration.params.size(); i++){
            // the functions parameter (in the definition) are bound to the arguments
            // passed in when the func is called
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        try{
            interpreter.executeBlock(declaration.body, environment);
        }
        // unwinds callstack to callee scope and returns value if 'return' stmt is found
        catch(Return returnValue){
            return returnValue.value;
        }
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
