import syntaxtree.*;
import visitor.GJDepthFirst;

import java.util.EmptyStackException;
import java.util.Stack;

/**
 * Visit tree nodes and perform type checking for node that its necessary.
 */
public class TypeCheckerVisitor extends GJDepthFirst<String, SymbolTable> {

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
        ClassContents clazz = symbolTable.getClassDefinition(n.f1.accept(this, symbolTable));
        if (clazz == null) {
            throw new TypeCheckerException(n.f1.toString() + " is not a defined Class in the symbol table.", n.f0.beginLine);
        }
        symbolTable.setScope(clazz);    //enter score of main class

        MethodContents method = clazz.getMethod("main");
        if (method == null) {
            throw new TypeCheckerException("Main Class " + n.f1.toString() + " has no main method", n.f0.beginLine);
        }
        symbolTable.setScope(method);   //enter scope of main method

        n.f11.accept(this, symbolTable);
        n.f14.accept(this, symbolTable);    //check other stuff
        n.f15.accept(this, symbolTable);

        symbolTable.resetMethodScope(); //exit scope of method
        symbolTable.resetClassScope();  //exit scope of class
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
        //get class from symbol table
        ClassContents clazz = symbolTable.getClassDefinition(n.f1.accept(this, symbolTable));
        if (clazz == null) {
            throw new TypeCheckerException(n.f1.toString() + " is not a defined Class in the symbol table.", n.f0.beginLine);
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
    public String visit(ClassExtendsDeclaration n, SymbolTable symbolTable) throws Exception {
        //check that class is defined in symbol table
        ClassContents clazz = symbolTable.getClassDefinition(n.f1.accept(this, symbolTable));
        if (clazz == null) {
            throw new TypeCheckerException("Extended class " + n.f1.toString() + " is not a defined Class in the symbol table.", n.f0.beginLine);
        }
        //check that superClass is defined symbol table
        ClassContents superClass = symbolTable.getClassDefinition(n.f3.accept(this, symbolTable));
        if (superClass == null) {
            throw new TypeCheckerException("can't extend undefined Class " + n.f3.toString(), n.f0.beginLine);
        }

        symbolTable.setScope(clazz);    //enter scope of class

        n.f5.accept(this, symbolTable); //check stuff in class
        n.f6.accept(this, symbolTable); //check stuff in class

        symbolTable.resetClassScope();  //exit scope of class

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
        //check method return type
        try {
            n.f1.accept(this, symbolTable);
        } catch (Exception ex) {
            throw new TypeCheckerException("Return type of" + n.f2.toString() + " is not a primtiive type or a defined Class.", n.f0.beginLine);
        }

        //get method definition from current class scope
        MethodContents method = symbolTable.getMethodFromScope(n.f2.accept(this, symbolTable));
        if (method == null) {
            throw new TypeCheckerException("No such method " + symbolTable.getClassScope().getType() + "." + n.f2.toString(), n.f0.beginLine);
        }

        symbolTable.setScope(method);   //enter method scope

        n.f4.accept(this, symbolTable);
        n.f7.accept(this, symbolTable); //check stuff
        n.f8.accept(this, symbolTable);

        //check if the return type matches
        String returnType = n.f10.accept(this, symbolTable);
        String expectedType = n.f1.accept(this, symbolTable);
        if (!returnType.equals(expectedType)) {
            throw new TypeCheckerException("Return type does not match for " + method.getName() + ". Expected " + expectedType + " but got " + returnType, n.f11.beginLine);
        }

        symbolTable.resetMethodScope();     //exit method scope

        return returnType;
    }

    /**
     * f0 -> ArrayType()
     * | BooleanType()
     * | IntegerType()
     * | Identifier()
     */
    public String visit(Type n, SymbolTable symbolTable) throws Exception {
        //check that the type is valid (either primary or a defined class in symbol table)
        String type = n.f0.accept(this, symbolTable);
        if (!symbolTable.isValidType(type)) {
            throw new PopulatorException("Invalid type " + type);
        }
        return type;
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(ArrayType n, SymbolTable argu) throws Exception {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
     */
    public String visit(BooleanType n, SymbolTable argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "int"
     */
    public String visit(IntegerType n, SymbolTable argu) throws Exception {
        return "int";
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, SymbolTable symbolTable) throws Exception {
        //find leftType in this scope
        String leftId = n.f0.accept(this, symbolTable);
        String leftType = symbolTable.getIdTypeFromScope(leftId);
        if (leftType == null) {
            throw new TypeCheckerException("No such variable " + leftId + " of type " + leftType + " in scope ", n.f1.beginLine);
        }

        String rightType = n.f2.accept(this, symbolTable);
        //check if the types are same
        if (!rightType.equals(leftType)) {
            //check if rightType is subtype of leftType
            ClassContents rightClass = symbolTable.getClassDefinition(rightType);
            if (rightClass != null) {
                if (rightClass.isOfSuperType(leftType)) {
                    return null;    //its sub type, so all good
                }
            }
            throw new TypeCheckerException("Right and Left values don't match. " + leftId + "( of type " + leftType + ") = " + "( of type " + rightType + ")", n.f1.beginLine);
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
    public String visit(ArrayAssignmentStatement n, SymbolTable symbolTable) throws Exception {
        //check that the array exists in scope
        String leftType = symbolTable.getIdTypeFromScope(n.f0.accept(this, symbolTable));
        if (!"int[]".equals(leftType)) {
            throw new TypeCheckerException("No such array in scope " + n.f0.toString(), n.f1.beginLine);
        }

        //check that array index is int
        String exprType = n.f2.accept(this, symbolTable);
        if (!"int".equals(exprType)) {
            throw new TypeCheckerException("Array allocation expression not an int, but " + exprType, n.f3.beginLine);
        }

        //check that right and left types are the same array
        String rightType = n.f5.accept(this, symbolTable);
        if (!leftType.equals(rightType + "[]")) {
            throw new TypeCheckerException("Right and Left values don't match. " + n.f0.toString() + "( element of " + leftType + ") = " + "( of type " + rightType + ")", n.f4.beginLine);
        }

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
    public String visit(IfStatement n, SymbolTable symbolTable) throws Exception {
        //check that condition is boolean
        String condType = n.f2.accept(this, symbolTable);
        if (!"boolean".equals(condType)) {
            throw new TypeCheckerException("If statement condition must be a boolean and not " + condType, n.f0.beginLine);
        }

        n.f4.accept(this, symbolTable);
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
    public String visit(WhileStatement n, SymbolTable symbolTable) throws Exception {
        //check that condition is boolean
        String condType = n.f2.accept(this, symbolTable);
        if (!"boolean".equals(condType)) {
            throw new TypeCheckerException("If statement condition must be a boolean and not " + condType, n.f0.beginLine);
        }

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
    public String visit(PrintStatement n, SymbolTable symbolTable) throws Exception {
        String exprType = n.f2.accept(this, symbolTable);
        if ("int".equals(exprType) || "boolean".equals(exprType)) {
            return null;
        }
        throw new TypeCheckerException("Print statement only accepts int or boolean. Found:" + exprType, n.f0.beginLine);
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
    public String visit(Expression n, SymbolTable symbolTable) throws Exception {
        String expr = n.f0.accept(this, symbolTable);
        if (symbolTable.isValidType(expr)) {    //if expression is primtive or Class type
            return expr;
        } else {                                //else it is an identifier
            String exprType = symbolTable.getIdTypeFromScope(expr);     //get type of identifier in this scope
            if (exprType != null) {
                return exprType;    //return identifier type
            } else {
                throw new TypeCheckerException("Couldn't resolve type of expression: " + expr, -1);
            }
        }
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, SymbolTable symbolTable) throws Exception {
        String type1 = n.f0.accept(this, symbolTable);
        String type2 = n.f2.accept(this, symbolTable);
        if ("boolean".equals(type1) && "boolean".equals(type2)) {
            return "boolean";
        }
        throw new TypeCheckerException("And Expression cannot have clause different than boolean. Found " + type1 + " && " + type2, n.f1.beginLine);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, SymbolTable symbolTable) throws Exception {
        String type1 = n.f0.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(type1)) {
            //if its not a primtive type, then lets see if its an identifier
            type1 = symbolTable.getIdTypeFromScope(type1);
        }

        String type2 = n.f2.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(type2)) {
            //if its not a primtive type, then lets see if its an identifier
            type2 = symbolTable.getIdTypeFromScope(type2);
        }

        if (type1 != null && type2 != null) {
            if ("int".equals(type1) && "int".equals(type2)) {
                return "boolean";
            }
        }
        throw new TypeCheckerException("Comparison can only be between int types. Found " + type1 + " < " + type2, n.f1.beginLine);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, SymbolTable symbolTable) throws Exception {
        String type1 = n.f0.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(type1)) {
            //if its not a primtive type, then lets see if its an identifier
            type1 = symbolTable.getIdTypeFromScope(type1);
        }

        String type2 = n.f2.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(type2)) {
            //if its not a primtive type, then lets see if its an identifier
            type2 = symbolTable.getIdTypeFromScope(type2);
        }

        if (type1 != null && type2 != null) {
            if ("int".equals(type1) && "int".equals(type2)) {
                return "int";
            }
        }
        throw new TypeCheckerException("Addition can only be between int types. Found " + type1 + " + " + type2, n.f1.beginLine);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, SymbolTable symbolTable) throws Exception {
        String type1 = n.f0.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(type1)) {
            //if its not a primtive type, then lets see if its an identifier
            type1 = symbolTable.getIdTypeFromScope(type1);
        }

        String type2 = n.f2.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(type2)) {
            //if its not a primtive type, then lets see if its an identifier
            type2 = symbolTable.getIdTypeFromScope(type2);
        }

        if (type1 != null && type2 != null) {
            if ("int".equals(type1) && "int".equals(type2)) {
                return "int";
            }
        }
        throw new TypeCheckerException("Subtraction can only be between int types. Found " + type1 + " - " + type2, n.f1.beginLine);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, SymbolTable symbolTable) throws Exception {
        String type1 = n.f0.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(type1)) {
            //if its not a primtive type, then lets see if its an identifier
            type1 = symbolTable.getIdTypeFromScope(type1);
        }

        String type2 = n.f2.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(type2)) {
            //if its not a primtive type, then lets see if its an identifier
            type2 = symbolTable.getIdTypeFromScope(type2);
        }

        if (type1 != null && type2 != null) {
            if ("int".equals(type1) && "int".equals(type2)) {
                return "int";
            }
        }
        throw new TypeCheckerException("Multiplication can only be between int types. Found " + type1 + " + " + type2, n.f1.beginLine);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, SymbolTable symbolTable) throws Exception {
        //check that the array exists in this scope
        String arrayId = n.f0.accept(this, symbolTable);
        String arrayType = symbolTable.getIdTypeFromScope(arrayId);

        if (!"int[]".equals(arrayType)) {
            throw new TypeCheckerException("No such array in scope " + arrayId, n.f1.beginLine);
        }

        //check that array index is int
        String indexType = n.f2.accept(this, symbolTable);
        if (!symbolTable.isPrimitive(indexType)) {
            //if its an identifier get its type
            indexType = symbolTable.getIdTypeFromScope(indexType);
        }

        if (!"int".equals(indexType)) {
            throw new TypeCheckerException("Array allocation expression not an int, but " + indexType, n.f3.beginLine);
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, SymbolTable symbolTable) throws Exception {
        //check that the array exists in this scope
        String arrayId = n.f0.accept(this, symbolTable);
        String arrayType = symbolTable.getIdTypeFromScope(arrayId);

        if (!"int[]".equals(arrayType)) {
            throw new TypeCheckerException("No such array in scope " + arrayId, n.f1.beginLine);
        }

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, SymbolTable symbolTable) throws Exception {
        //check that the PrimaryExpression exists in current scope
        String className = n.f0.accept(this, symbolTable);
        String classType = symbolTable.getIdTypeFromScope(className);
        if (classType == null) {
            //seems like the PrimaryException returned a type, no worries keep going
            classType = className;
        }
        //check that PrimaryExpression is a class
        ClassContents clazz = symbolTable.getClassDefinition(classType);
        if (clazz == null) {
            throw new TypeCheckerException("Not a Class " + className + " (in MessageSend)", n.f1.beginLine);
        }

        //check that this class has such method
        String methodName = n.f2.accept(this, symbolTable);
        MethodContents method = clazz.getInheritedMethod(methodName);
        if (method == null) {
            throw new TypeCheckerException("No such method " + clazz.getType() + "." + methodName, n.f3.beginLine);
        }

        MethodCallStack.push(new MethodCall(method));
        n.f4.accept(this, symbolTable);     //check method call argument list
        MethodCallStack.pop();

        return method.getReturnType();
    }

    /**
     * Stack of MethodCalls. Used to type check call list arguments.
     * push to stack when calling a function, pop when exiting.
     * Use getNextParameterType to peek at top of stack and get the type of the next parameter type.
     */
    static class MethodCallStack {

        private static Stack<MethodCall> methodStack = new Stack<>();

        static void push(MethodCall methodCall) {
            methodStack.push(methodCall);
        }

        static void pop() {
            methodStack.pop();
        }

        static MethodContents peekMethod() throws Exception {
            try {
                return methodStack.peek().getMethod();
            } catch (EmptyStackException ex) {
                throw new Exception("MethodCall peek failed. Stack is empty.");
            }
        }

        static MethodCall peek() throws Exception {
            try {
                return methodStack.peek();
            } catch (EmptyStackException ex) {
                throw new Exception("MethodCall peek failed. Stack is empty.");
            }
        }

        static String getNextParameterType() throws Exception {
            try {
                return methodStack.peek().getNextParameterType();
            } catch (EmptyStackException ex) {
                throw new Exception("MethodCall peek failed. Stack is empty.");
            }
        }

    }

    /**
     * Element of MethodCallStack. Holds value about the method and the next parameter to be checked in argument list.
     */
    public class MethodCall {
        private MethodContents method;
        private int nextParamIndex;

        MethodCall(MethodContents method) {
            this.method = method;
            this.nextParamIndex = 0;
        }

        MethodContents getMethod() {
            return method;
        }

        String getNextParameterType() throws Exception {
            if (method.getParameterList().size() <= nextParamIndex) {
                throw new Exception(method.getName() + "'s parameter list is shorter that the call arg list.");
            }
            return method.getParameterList().get(nextParamIndex++);
        }

        int getNumOfArguments() {
            return nextParamIndex;
        }
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, SymbolTable symbolTable) throws Exception {
        String callListType = n.f0.accept(this, symbolTable);
        String expectedParamType;
        try {
            expectedParamType = MethodCallStack.getNextParameterType();
        } catch (Exception ex) {
            throw new TypeCheckerException(ex.getMessage(), -1);
        }

        //check if call arg and param are the same type
        if (!expectedParamType.equals(callListType)) {
            //if not check if maybe param type is super type of call type
            ClassContents callClassType = symbolTable.getClassDefinition(callListType);
            if (callClassType != null) {
                if (!callClassType.isOfSuperType(expectedParamType)) {
                    throw new TypeCheckerException(symbolTable.getMethodScope().getName() + "()'s call doesn't match the parameter list. Expected " + expectedParamType + " but got " + callListType, -1);
                }
            } else {
                throw new TypeCheckerException(symbolTable.getMethodScope().getName() + "()'s call doesn't match the parameter list. Expected " + expectedParamType + " but got " + callListType, -1);
            }
        }


        n.f1.accept(this, symbolTable);

        //when you are done with checking the types of the arguments, check that the ExpressionList.size() is the same as method paramList.size()
        MethodContents method = MethodCallStack.peekMethod();
        int paramListSize = method.getParameterList().size();
        int argListSize = MethodCallStack.peek().getNumOfArguments();
        if (paramListSize != argListSize) {
            throw new TypeCheckerException(method.getName() + " call list(" + argListSize + ") differs in legth from defined parameter list(" + paramListSize + ").", -1);
        }
        return null;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, SymbolTable symbolTable) throws Exception {
        String callListType = n.f1.accept(this, symbolTable);
        String expectedParamType;
        try {
            expectedParamType = MethodCallStack.getNextParameterType();
        } catch (Exception ex) {
            throw new TypeCheckerException(ex.getMessage(), n.f0.beginLine);
        }


        //check if call arg and param are the same type
        if (!expectedParamType.equals(callListType)) {
            //if not check if maybe param type is super type of call type
            ClassContents callClassType = symbolTable.getClassDefinition(callListType);
            if (callClassType != null) {
                if (!callClassType.isOfSuperType(expectedParamType)) {
                    throw new TypeCheckerException(symbolTable.getMethodScope().getName() + "()'s call doesn't match the parameter list. Expected " + expectedParamType + " but got " + callListType, -1);
                }
            } else {
                throw new TypeCheckerException(symbolTable.getMethodScope().getName() + "()'s call doesn't match the parameter list. Expected " + expectedParamType + " but got " + callListType, -1);
            }
        }

        return null;
    }

    /**
     * f0 -> NotExpression()
     * | PrimaryExpression()
     */
    public String visit(Clause n, SymbolTable symbolTable) throws Exception {
        String clause = n.f0.accept(this, symbolTable);
        if (symbolTable.isValidType(clause)) {
            return clause;
        } else if (clause.endsWith("()")) {       //if expression end with "()" then its an AllocationExpression
            return clause;
        } else {                                //else it is an identifier
            String exprType = symbolTable.getIdTypeFromScope(clause);     //if expression is identifier
            if (exprType != null) {
                return exprType;    //return identifier type
            } else {
                throw new TypeCheckerException("Couldn't resolve type of primary expression: " + clause, -1);
            }
        }
    }

    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     */
    public String visit(PrimaryExpression n, SymbolTable symbolTable) throws Exception {
        String expr = n.f0.accept(this, symbolTable);
        if (symbolTable.isValidType(expr)) {    //if expression is primtive or Class type
            return expr;
        } else if (expr.endsWith("()")) {       //if expression end with "()" then its an AllocationExpression
            return expr;
        } else {                                //else it is an identifier
            String exprType = symbolTable.getIdTypeFromScope(expr);     //if expression is identifier
            if (exprType != null) {
                return expr;    //return identifier type
            } else {
                throw new TypeCheckerException("Couldn't resolve type of primary expression: " + expr, -1);
            }
        }
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, SymbolTable argu) {
        return "int";
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, SymbolTable argu) {
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, SymbolTable argu) {
        return "boolean";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, SymbolTable symbolTable) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, SymbolTable symbolTable) throws Exception {
        if (symbolTable.getClassScope() == null) {
            throw new TypeCheckerException("Undeclared reference to \"this\". Not in a class scope.", n.f0.beginLine);
        }
        return symbolTable.getClassScope().getType(); //this is the same type as the current class
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n, SymbolTable symbolTable) throws Exception {
        //check that expression is int
        String exprType = n.f3.accept(this, symbolTable);
        if ("int".equals(exprType)) {
            return "int[]";
        }
        throw new TypeCheckerException("Array allocation expression not an int, but " + exprType, n.f0.beginLine);
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, SymbolTable symbolTable) throws Exception {
        //check that class is defined in symbol table
        String classType = n.f1.accept(this, symbolTable);
        ClassContents clazz = symbolTable.getClassDefinition(classType);
        if (clazz == null) {
            throw new TypeCheckerException("Tried to instantiate undeclared Class " + classType, n.f0.beginLine);
        }
        return clazz.getType() + "()"; //same type as f1
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, SymbolTable symbolTable) throws Exception {
        //check that not expression is boolean
        String clause = n.f1.accept(this, symbolTable);
        if ("boolean".equals(clause)) {
            return clause;
        }
        //maybe its an identifier?
        String clauseType = symbolTable.getIdTypeFromScope(clause);
        if (clauseType != null) {
            if ("boolean".equals(clauseType)) {
                return clause;
            }
        }
        throw new TypeCheckerException("NotExpression had a non-boolean clause. Found: " + clause, n.f0.beginLine);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, SymbolTable argu) throws Exception {
        return n.f1.accept(this, argu); //same type as f1
    }

}
