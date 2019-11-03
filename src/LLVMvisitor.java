import syntaxtree.*;
import visitor.GJDepthFirst;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

class LLVMvisitor extends GJDepthFirst<VisitorReturn, SymbolTable> {

    private BufferedWriter out;
    private int tabs = 0;

    private int currentTemp = 0;
    private int currentLabel = 0;

    private List<String[]> parameterBuffer = new ArrayList<>(); //alloca's for paramter lists are stored here

    //whenever we visit an ExpressionList of a MethodCall we create a list of the expressions for that ExpressionList,
    //but we need to save each list to a stack, because a methodCall might have other methodCalls in its ExpressionList
    private Stack<List<VisitorReturn>> methodCallStack = new Stack<>();

    LLVMvisitor(BufferedWriter out) throws IntermediateException {
        this.out = out;
    }

    /**
     * generates instructions
     */
    private void emit(String instr) throws IntermediateException {
        try {
            out.write("\t".repeat(tabs) + instr + "\n");
        } catch (IOException ex) {
            throw new IntermediateException("Failed to emit \"" + instr + "\"");
        }
    }

    private void emitNoNewline(String instr) throws IntermediateException {
        try {
            out.write("\t".repeat(tabs) + instr);
        } catch (IOException ex) {
            throw new IntermediateException("Failed to emit \"" + instr + "\"");
        }
    }

    /**
     * emit the string as is, without indentation or newline
     *
     * @param str
     */
    private void emitPlain(String str) {
        try {
            out.write(str);
        } catch (IOException ex) {
            throw new IntermediateException("Failed to emitPlain \"" + str + "\"");
        }
    }

    /**
     * Emit alloca and store line of parameter lists
     */
    private void emitParameterBuffer() {
        for (String[] strArr : parameterBuffer) {
            emit(strArr[0]);
            emit(strArr[1]);
        }
    }

    /**
     * generate a unique label name
     */
    private String newLabel() {
        return "lbl" + (currentLabel++);
    }

    /**
     * generate a unique temporary name
     */
    private String newTemp() {
        return "%_" + (currentTemp++);
    }

    /**
     * Entering new scope. Reset temp and label counters and also increace tab indentation.
     */
    private void newScope() {
        this.currentTemp = 0;
        this.currentLabel = 0;
        tabs++;
    }

    private void emitVirtualTables(SymbolTable symbolTable) {
        boolean mainFlag = true;    //main class has special treatment

        for (Map.Entry<String, ClassContents> entry : symbolTable.getClassMap().entrySet()) { //for every class, emit a virtual table
            ClassContents clazz = entry.getValue();
            String virtualTable = "@." + clazz.getType();
            if (mainFlag) {
                virtualTable += "_vtable = global [0 x i8*] []";
                mainFlag = false;
            } else {
                int numMethods = clazz.getNumOfMethods();
                String vTableFunctions = "";
                for (MethodContents method : clazz.getAllMethods()) { //for every method of class emit return type and parameters
                    if (!vTableFunctions.isEmpty()) {
                        //if this is not the first paramter then add a comma
                        vTableFunctions += ",\n\t\t";
                    }
                    //add return type
                    vTableFunctions += "i8* bitcast (" + minijavaToLLVMtype(method.getReturnType() + " (i8*");
                    //add argument types
                    for (String paramType : method.getParameterList()) {
                        vTableFunctions += "," + minijavaToLLVMtype(paramType);
                    }
                    //add method name
                    vTableFunctions += ")* @" + method.getName() + " to i8*)";
                }

                virtualTable += "_vtable = global [" + numMethods + " x i8*] [" + vTableFunctions + "]";

            }
            emit(virtualTable);
        }
    }

    /*********************************       VISITOR METHODS   ********************************************************/
    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public VisitorReturn visit(Goal n, SymbolTable symbolTable) throws Exception {

        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        return null;
    }

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
    public VisitorReturn visit(MainClass n, SymbolTable symbolTable) throws Exception {
        emitVirtualTables(symbolTable);
        emit("declare i8* @calloc(i32, i32)\n" +
                "declare i32 @printf(i8*, ...)\n" +
                "declare void @exit(i32)\n" +
                "\n" +
                "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                "define void @print_int(i32 %i) {\n" +
                "    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "    ret void\n" +
                "}\n" +
                "\n" +
                "define void @throw_oob() {\n" +
                "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str)\n" +
                "    call void @exit(i32 1)\n" +
                "    ret void\n" +
                "}\n");
        //emit main method
        emit("define i32 @main() {");
        newScope();
        n.f14.accept(this, symbolTable);
        n.f15.accept(this, symbolTable);
        emit("ret i32 0");
        tabs--;
        emit("}");
        return null;
    }

    /**
     * f0 -> ClassDeclaration()
     * | ClassExtendsDeclaration()
     *
     * @return same
     */
    public VisitorReturn visit(TypeDeclaration n, SymbolTable symbolTable) throws Exception {
        return n.f0.accept(this, symbolTable);
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public VisitorReturn visit(ClassDeclaration n, SymbolTable symbolTable) throws Exception {
        //get class from symbol table
        ClassContents clazz = symbolTable.getClassDefinition(n.f1.accept(this, symbolTable).id);
        if (clazz == null) {
            throw new IntermediateException(n.f1.toString() + " is not a defined Class in the symbol table.", n.f0.beginLine);
        }
        symbolTable.setScope(clazz);    //enter scope of class

        n.f3.accept(this, symbolTable);
        n.f4.accept(this, symbolTable); //check stuff in class

        symbolTable.resetClassScope();  //exit scope of class

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
    public VisitorReturn visit(ClassExtendsDeclaration n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        n.f3.accept(this, symbolTable);
        n.f4.accept(this, symbolTable);
        n.f5.accept(this, symbolTable);
        n.f6.accept(this, symbolTable);
        n.f7.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public VisitorReturn visit(VarDeclaration n, SymbolTable symbolTable) throws Exception {
        String type = n.f0.accept(this, symbolTable).type;
        emit("%" + n.f1.accept(this, symbolTable).id + " = alloca " + minijavaToLLVMtype(type));
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
    public VisitorReturn visit(MethodDeclaration n, SymbolTable symbolTable) throws Exception {
        MethodContents method = symbolTable.getMethodFromScope(n.f2.accept(this, symbolTable).id);
        String rtype = n.f1.accept(this, symbolTable).type;
        emitNoNewline("define " + minijavaToLLVMtype(rtype) + " @" + method.getName() + "(i8* %this");

        symbolTable.setScope(method);   //enter method scope

        //emit parameter list
        n.f4.accept(this, symbolTable);
        emitPlain(") {\n");
        newScope(); //reset counters, increase indentation
        emitParameterBuffer();

        //emit local variable declarations
        n.f7.accept(this, symbolTable);

        //emit statements
        n.f8.accept(this, symbolTable);
        //emit return
        n.f10.accept(this, symbolTable);

        tabs--;
        emit("}");
        symbolTable.resetMethodScope();     //exit method scope
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public VisitorReturn visit(FormalParameterList n, SymbolTable symbolTable) throws Exception {
        emitPlain(", ");
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public VisitorReturn visit(FormalParameter n, SymbolTable symbolTable) throws Exception {
        String type = minijavaToLLVMtype(n.f0.accept(this, symbolTable).type);
        String id = n.f1.accept(this, symbolTable).id;
        //emit to parameter list
        emitPlain(type + " %." + id);
        //create and store alloca-store block for this param, to be emitted after param list is done
        String[] strArray = {"%" + id + " = alloca " + type, "store " + type + " %." + id + ", " + type + "* %" + id};
        parameterBuffer.add(strArray);
        return null;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public VisitorReturn visit(FormalParameterTail n, SymbolTable symbolTable) throws Exception {
        return n.f0.accept(this, symbolTable);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public VisitorReturn visit(FormalParameterTerm n, SymbolTable symbolTable) throws Exception {
        emitPlain(", ");
        return n.f1.accept(this, symbolTable);
    }

    /**
     * f0 -> ArrayType()
     * | BooleanType()
     * | IntegerType()
     * | Identifier()
     */
    public VisitorReturn visit(Type n, SymbolTable symbolTable) throws Exception {
        return n.f0.accept(this, symbolTable);
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public VisitorReturn visit(ArrayType n, SymbolTable symbolTable) throws Exception {
        return new VisitorReturn("int[]", null);
    }

    /**
     * f0 -> "boolean"
     */
    public VisitorReturn visit(BooleanType n, SymbolTable symbolTable) throws Exception {
        return new VisitorReturn("boolean", null);
    }

    /**
     * f0 -> "int"
     */
    public VisitorReturn visit(IntegerType n, SymbolTable symbolTable) throws Exception {
        return new VisitorReturn("int", null);
    }

    /**
     * f0 -> Block()
     * | AssignmentStatement()
     * | ArrayAssignmentStatement()
     * | IfStatement()
     * | WhileStatement()
     * | PrintStatement()
     */
    public VisitorReturn visit(Statement n, SymbolTable symbolTable) throws Exception {
        return n.f0.accept(this, symbolTable);  //todo statements
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public VisitorReturn visit(Block n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public VisitorReturn visit(AssignmentStatement n, SymbolTable symbolTable) throws Exception {
        String identifier = n.f0.accept(this, symbolTable).id;
        String identifierType = minijavaToLLVMtype(symbolTable.getIdTypeFromScope(identifier));

        VisitorReturn expr = n.f2.accept(this, symbolTable);

        if (symbolTable.getMethodScope().getVariableType(identifier) != null) {
            //if its  a local method variable
            emit("store " + identifierType + " " + expr.id + ", " + identifierType + "* %" + identifier);
        } else {
            //offset = offset+8 because at 0 there is the v_table pointer (ptr size is 8)
            int offset = symbolTable.getClassScope().getFieldOffset(identifier) + 8;
            String temp = newTemp();
            String bitcastTemp = newTemp();
            emit(temp + " = getelementptr i8, i8* %this, i32 " + offset);
            emit(bitcastTemp + " = bitcast i8* " + temp + " to " + identifierType + "*");
            emit("store " + identifierType + " " + expr.id + ", " + identifierType + "* " + bitcastTemp);
        }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public VisitorReturn visit(ArrayAssignmentStatement n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        n.f3.accept(this, symbolTable);
        n.f4.accept(this, symbolTable);
        n.f5.accept(this, symbolTable);
        n.f6.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public VisitorReturn visit(IfStatement n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        n.f3.accept(this, symbolTable);
        n.f4.accept(this, symbolTable);
        n.f5.accept(this, symbolTable);
        n.f6.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public VisitorReturn visit(WhileStatement n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        n.f3.accept(this, symbolTable);
        n.f4.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public VisitorReturn visit(PrintStatement n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        n.f3.accept(this, symbolTable);
        n.f4.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> AndExpression()
     * | CompareExpression()
     * | PlusExpression()
     * | MinusExpression()
     * | TimesExpression()
     * | ArrayLookup()
     * | ArrayLength()
     * | MessageSend()
     * | Clause()
     */
    public VisitorReturn visit(Expression n, SymbolTable symbolTable) throws Exception {
        return n.f0.accept(this, symbolTable);  //todo all expressions
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public VisitorReturn visit(AndExpression n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public VisitorReturn visit(CompareExpression n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public VisitorReturn visit(PlusExpression n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public VisitorReturn visit(MinusExpression n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public VisitorReturn visit(TimesExpression n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public VisitorReturn visit(ArrayLookup n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        n.f3.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public VisitorReturn visit(ArrayLength n, SymbolTable symbolTable) throws Exception {
        String _ret = null;
        n.f0.accept(this, symbolTable);
        n.f1.accept(this, symbolTable);
        n.f2.accept(this, symbolTable);
        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public VisitorReturn visit(MessageSend n, SymbolTable symbolTable) throws Exception {
        //get class
        VisitorReturn classReg = n.f0.accept(this, symbolTable);
        String methodId = n.f2.accept(this, symbolTable).id;
        //get method position
        ClassContents clazz = symbolTable.getClassDefinition(classReg.id);
        String methodRetType = minijavaToLLVMtype(clazz.getMethod(methodId).getReturnType());
        int methodIndex = clazz.getMethodOffset(methodId) / 8;
        emit("; " + classReg.id + "." + methodId + " : " + methodIndex);
        //cast and load object
        String bitcastTemp = newTemp();
        String loadTemp = newTemp();
        emit(bitcastTemp + " bitcast " + classReg.toString() + " to i8***");
        emit(loadTemp + " = load i8**, i8*** " + bitcastTemp);
        //get objects method from vtable
        String methPtrTemp = newTemp();
        String methTemp = newTemp();
        emit(methPtrTemp + " = getelementptr i8*, i8** " + loadTemp + ", i32 " + methodIndex);
        emit(methTemp + " = load i8*, i8** " + methPtrTemp);
        //visit ExpressionList and get argumentList to methodCallStack
        n.f4.accept(this, symbolTable);
        //pop from stack and read arguments
        String argumentTypes = "i8*";
        String argumentList = classReg.toString();  //%this
        for (VisitorReturn v : methodCallStack.pop()) {
            argumentTypes += ", " + v.type;
            argumentList += ", " + v.type + " " + v.id;
        }
        //bitcast methTemp to method argument types
        String bitcastTemp2 = newTemp();
        emit(bitcastTemp2 + " bitcast i8* " + methTemp + " to (" + argumentTypes + ")*");
        //call method
        String returnTemp = newTemp();
        emit(returnTemp + " = call " + methodRetType + " " + bitcastTemp2 + "(" + argumentList + ")");
        return new VisitorReturn(methodRetType, returnTemp);
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     *
     * @return always null
     */
    public VisitorReturn visit(ExpressionList n, SymbolTable symbolTable) throws Exception {
        //new list of VisitorReturns, add it to the stack
        methodCallStack.push(new LinkedList<VisitorReturn>());
        //add expression to list
        methodCallStack.peek().add(n.f0.accept(this, symbolTable));
        //visit other expressions of the list
        return n.f1.accept(this, symbolTable);
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     *
     * @return always null
     */
    public VisitorReturn visit(ExpressionTail n, SymbolTable symbolTable) throws Exception {
        return n.f0.accept(this, symbolTable);
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     *
     * @return null
     */
    public VisitorReturn visit(ExpressionTerm n, SymbolTable symbolTable) throws Exception {
        methodCallStack.peek().add(n.f1.accept(this, symbolTable));
        return null;
    }

    /**
     * f0 -> NotExpression()
     * | PrimaryExpression()
     *
     * @return same
     */
    public VisitorReturn visit(Clause n, SymbolTable symbolTable) throws Exception {
        return n.f0.accept(this, symbolTable);
    }

    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()   //special case, uses type resolution
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     *
     * @return same
     */
    public VisitorReturn visit(PrimaryExpression n, SymbolTable symbolTable) throws Exception {
        VisitorReturn ret = n.f0.accept(this, symbolTable);
        if (ret.type == null) {
            throw new IntermediateException("Unrecognized Primary Expression. Should have been an identifier, but the identifier wasn't found neither as a local method variable nor as a class field (inherited or not)");
        }
        if (ret.id == null) {
            //if its a literal
            return ret; //todo: what happens for literals that return no id?
        }
        return ret;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     *
     * @return literal
     */
    public VisitorReturn visit(IntegerLiteral n, SymbolTable symbolTable) throws Exception {
        return new VisitorReturn("i32", null, n.f0.toString());
    }

    /**
     * f0 -> "true"
     *
     * @return literal
     */
    public VisitorReturn visit(TrueLiteral n, SymbolTable symbolTable) throws Exception {
        return new VisitorReturn("i1", null, "1");
    }

    /**
     * f0 -> "false"
     *
     * @return literal
     */
    public VisitorReturn visit(FalseLiteral n, SymbolTable symbolTable) throws Exception {
        return new VisitorReturn("i1", null, "0");
    }

    /**
     * f0 -> <IDENTIFIER>
     *
     * @return the register there you loaded the variable's value, if its not a variable but just a name then return the id (type=null)
     */
    public VisitorReturn visit(Identifier n, SymbolTable symbolTable) throws Exception {
        String id = n.f0.toString();
        //find the type of the identifier
        String idType = symbolTable.getMethodScope().getVariableType(id);
        if (idType != null) {
            //if its a local method variable
            String type = minijavaToLLVMtype(idType);
            //load identifier to new register and return that register
            String loadTemp = newTemp();
            emit(loadTemp + " = load " + type + ", " + type + "* %" + id);
            return new VisitorReturn(type, id);
        }
        idType = symbolTable.getClassScope().getInheritedField(id);
        if (idType != null) {
            //if its a class field
            String type = minijavaToLLVMtype(idType);
            //get offset
            int offset = symbolTable.getClassScope().getFieldOffset(id) + 8;
            //load identifier to new register and return that register
            String temp = newTemp();
            String bitcastTemp = newTemp();
            String loadTemp = newTemp();
            emit(temp + " = getelementptr i8, i8* %this, i32 " + offset);
            emit(bitcastTemp + " = bitcast i8* " + temp + " to " + type + "*");
            emit(loadTemp + " = load " + type + ", " + type + "* " + bitcastTemp);
            return new VisitorReturn(type, loadTemp);
        }
        //Seems like this identifier doesnt refer to any existing variable or field
        return new VisitorReturn(null, id);
    }

    /**
     * f0 -> "this"
     *
     * @return "%this (literal value)
     */
    public VisitorReturn visit(ThisExpression n, SymbolTable symbolTable) throws Exception {
        return new VisitorReturn("i8*", null, "%this");
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     *
     * @return the ptr to arrayallocation
     */
    public VisitorReturn visit(ArrayAllocationExpression n, SymbolTable symbolTable) throws Exception {
        VisitorReturn arraySize = n.f3.accept(this, symbolTable);
        String comparisonTemp = newTemp();
        String successLabel = newLabel();
        String failLabel = newLabel();
        String arraySizeTemp = newTemp();
        String callocTemp = newTemp();
        String bitcastTemp = newTemp();

        //if arraySize < 0 throw_oob
        emit(comparisonTemp + " = icmp slt " + arraySize.toString() + ", 0");
        emit("br i1 " + comparisonTemp + ", label %" + failLabel + ", label %" + successLabel);
        emit(failLabel + ":");
        emit("call void @throw_oob()");
        //if arraySize > 0
        emit("br label %" + successLabel);
        emit(successLabel + ":");
        emit(arraySizeTemp + " = add" + arraySize.toString() + ", 1");
        emit(callocTemp + " = call i8* @calloc(i32 4, i32 " + arraySizeTemp + ")");
        emit(bitcastTemp + " = bitcast i8* " + callocTemp + " to i32*");
        emit("store i32 " + arraySize.id + ", i32* " + bitcastTemp);

        return new VisitorReturn("i32*", bitcastTemp, null);
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     *
     * @return the register where the vtable ptr is stored now
     */
    public VisitorReturn visit(AllocationExpression n, SymbolTable symbolTable) throws Exception {
        VisitorReturn ret = n.f1.accept(this, symbolTable);
        //get class and its offset
        ClassContents clazz = symbolTable.getClassMap().get(ret.id);
        int offset = clazz.getFieldOffset() + 8;
        //allocate memory to get vtable
        String callocTemp = newTemp();
        String bitcastTemp = newTemp();
        emit(callocTemp + " = call i8* @calloc(i32 1, i32 " + offset + ")");
        emit(bitcastTemp + " = bitcast i8* " + callocTemp + " to i8***");
        //get the number of methods this method has, including the methods it inherits
        String vtableType = "[" + clazz.getNumOfMethods() + " x i8*]";
        //get vtable and store it
        String vtableTemp = newTemp();
        emit(vtableTemp + " = getelementptr " + vtableType + ", " + vtableType + "* @." + clazz.getType() + "_vtable, i32 0, i32 0");
        emit("store i8** " + vtableTemp + ", i8*** " + bitcastTemp);

        return new VisitorReturn("i8*", callocTemp);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     *
     * @return the tempRegister that has the not-value
     */
    public VisitorReturn visit(NotExpression n, SymbolTable symbolTable) throws Exception {
        VisitorReturn clause = n.f1.accept(this, symbolTable);
        String tempReg = newTemp();
        emit(tempReg + " = xor i1 1, " + clause.id);
        return new VisitorReturn("i1", tempReg);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     *
     * @return the same expression
     */
    public VisitorReturn visit(BracketExpression n, SymbolTable symbolTable) throws Exception {
        return n.f1.accept(this, symbolTable);
    }


    /**************utility*************************************/
    private static String minijavaToLLVMtype(String minijavaType) {
        switch (minijavaType) {
            case "boolean":
                return "i1";
            case "int":
                return "i32";
            case "int[]":
                return "i32*";
        }
        return "i8*";   //identifier
    }
}


class VisitorReturn {
    public String type;
    public String id;
    public String literalValue;

    VisitorReturn(String type, String id) {
        this.type = type;
        this.id = id;
        this.literalValue = null;
    }

    VisitorReturn(String type, String id, String literalValue) {
        this.type = type;
        this.id = id;
        this.literalValue = literalValue;
    }

    public String asString() {
        return type + " " + id;
    }
}