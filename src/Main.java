import syntaxtree.*;
import visitor.*;

import java.io.*;

class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Driver <inputFile>");
            System.exit(1);
        }
        FileInputStream fis = null;
        BufferedWriter writer = null;
        for (String file : args) {
            try {
                System.out.println("---------------------------------------------------------------------------------");
                fis = new FileInputStream(file);
                MiniJavaParser parser = new MiniJavaParser(fis);
                System.err.println(file + " Program parsed successfully.");

                //------------------Semanitc Analysis
                Goal root = parser.Goal();
                SymbolTable symTable = new SymbolTable();

                PopulatorVisitor popVisitor = new PopulatorVisitor();
                root.accept(popVisitor, symTable);
                symTable.printOffsets();

                TypeCheckerVisitor checkerVisitor = new TypeCheckerVisitor();
                root.accept(checkerVisitor, symTable);
                System.out.println(file + "    Semantic Checking Successfull!!!");

                //----------------LLVM
                //open new file
                String filename = file.replace(".java", ".ll");
                writer = new BufferedWriter(new FileWriter(filename));

                LLVMvisitor llvmVisitor = new LLVMvisitor(writer);
                root.accept(llvmVisitor, symTable);


            } catch (ParseException | FileNotFoundException | PopulatorException | TypeCheckerException ex) {
                System.out.println(ex.getMessage());
            } catch (Exception ex) {
                System.out.println("Other exception: " + ex.getMessage());
            } finally {
                try {
                    if (fis != null) fis.close();
                    if (writer != null) writer.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
