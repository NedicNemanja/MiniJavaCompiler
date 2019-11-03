import syntaxtree.*;
import visitor.GJDepthFirst;

public class PopulatorVisitor extends GJDepthFirst<String, SymbolTable> {

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public String visit(MainClass n, SymbolTable symbolTable) throws Exception {
        //create and insert new class in symbol table
        ClassContents newClass = new ClassContents(n.f1.accept(this, symbolTable));

        symbolTable.insertClass(newClass);
        symbolTable.setScope(newClass);

        MethodContents mainMethod = new MethodContents("main", newClass, "void");
        symbolTable.insertMethod(mainMethod);

        n.f14.accept(this, symbolTable);
        n.f15.accept(this, symbolTable);

        symbolTable.resetClassScope();

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, SymbolTable symbolTable) throws Exception {
        ClassContents newClass = new ClassContents(n.f1.accept(this, symbolTable));
        symbolTable.insertClass(newClass);
        symbolTable.setScope(newClass);

        n.f3.accept(this, symbolTable);
        n.f4.accept(this, symbolTable);

        symbolTable.resetClassScope();

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public String visit(ClassExtendsDeclaration n, SymbolTable symbolTable) throws Exception {
        String superId = n.f3.accept(this, symbolTable);

        //check if super class exists
        ClassContents superClass = symbolTable.getClassDefinition(superId);
        if(superClass != null) {
            //if yes then create class with super and insert to symbol table
            ClassContents newClass = new ClassContents(n.f1.accept(this, symbolTable), superClass);
            symbolTable.insertClass(newClass);
            symbolTable.setScope(newClass);

            n.f5.accept(this, symbolTable);
            n.f6.accept(this, symbolTable);

            symbolTable.resetClassScope();

            return null;
        } else {
            throw new PopulatorException("Class " + n.f1.toString() + " extends " + superId + ", but " + superId + " isn't declared.");
        }

    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, SymbolTable symbolTable) throws Exception {
        symbolTable.insertVariable(n.f1.accept(this, symbolTable), n.f0.accept(this, symbolTable));
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, SymbolTable symbolTable) throws Exception {
        MethodContents newMethod = new MethodContents(n.f2.accept(this, symbolTable), symbolTable.getClassScope(),
                n.f1.accept(this, symbolTable));

        symbolTable.insertMethod(newMethod);    //just insert method to the current class scope

        symbolTable.setScope(newMethod);    //start new scope for this method

        n.f4.accept(this, symbolTable); //accept and insert parameters
        newMethod.polymorphicCheck();  //in case this method is polymorphic we need to check that it matches


        n.f7.accept(this, symbolTable); //accept and insert variables in method scope
        n.f8.accept(this, symbolTable);

        n.f10.accept(this, symbolTable);

        symbolTable.resetMethodScope(); //exit method scope

        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, SymbolTable symbolTable) throws Exception {
        symbolTable.getMethodScope().insertParameter(n.f1.accept(this, symbolTable), n.f0.accept(this, symbolTable));
        return null;
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n, SymbolTable symbolTable) {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, SymbolTable symbolTable) {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, SymbolTable symbolTable) {
        return "int";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symbolTable) {
        return n.f0.toString();
    }
}
