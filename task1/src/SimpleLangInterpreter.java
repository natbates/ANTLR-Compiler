import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class SimpleLangInterpreter extends AbstractParseTreeVisitor<Integer> implements SimpleLangVisitor<Integer> {

    /**
     * It serves as an interpreter for the SimpleLang language,
     * providing functionality to visit and interpret the parse tree nodes of SimpleLang expressions,
     * statements, and other constructs.
    */

    private final Map<String, SimpleLangParser.DecContext> global_funcs = new HashMap<>(); // Map of all global functions. The key is the function name and the value is the functions declaration
    private final Stack<Map<String, Integer>> frames = new Stack<>(); // Stack of all the frames used to visit all the different tokens

    public Integer visitProgram(SimpleLangParser.ProgContext ctx, String[] args) // program context is a parse tree is passed in along with any arguments that are being passed into the main function
    {
        for (int i = 0; i < ctx.dec().size(); ++i) {
            // Populates global functions map with all the functions, using there names as key and the dec instances as values
            SimpleLangParser.DecContext dec = ctx.dec(i); // gets a function
            SimpleLangParser.Typed_idfrContext typedIdfr = dec.typed_idfr(); // gets functions typed Idfr (type + name)
            global_funcs.put(typedIdfr.Idfr().getText(), dec); // adds the function to the global func

        }

        SimpleLangParser.DecContext main = global_funcs.get("main"); // gets the main function from global functions
        List<SimpleLangParser.Typed_idfrContext> paramList = (main.vardec() != null) ? main.vardec().typed_idfr() : null; // need to check that param list isn't null when fetching it from main function


        //System.out.println("Amount of functions: " + global_funcs.size()); // Used for Debugging

        Map<String, Integer> newFrame = new HashMap<>();

        // Only the main function takes arguments passed in from this function (String[] args). if any, there is also always a main function
        // goes through main functions arguments, checks if is boolean or an integer and adds them to stack frame. Args can only be BoolLit or IntLit.

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("true")) {
                assert paramList != null;
                newFrame.put(paramList.get(i).Idfr().getText(), 1); // adds true boolean arguments to the frame in form of a 1 for true
            } else if (args[i].equals("false")) {
                assert paramList != null;
                newFrame.put(paramList.get(i).Idfr().getText(), 0); // adds false boolean arguments to the frame in form of a 0 for false
            } else {
                assert paramList != null;
                newFrame.put(paramList.get(i).Idfr().getText(), Integer.parseInt(args[i])); // adds integer arguments to the frame
            }
        }

        frames.push(newFrame); // Adds new frame full of arguments onto the stack

        Integer returnValue = visit(main); // Visit main function first as all other relevant functions will be inside here, so they will be visited from this

        frames.pop(); // Pop the frame after visiting the main function

       //System.out.println("Final Return Value: " + returnValue); // Used for Debugging
        return returnValue; // returns main return value back to the task 1 file

    }

    @Override public Integer visitProg(SimpleLangParser.ProgContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitDec(SimpleLangParser.DecContext ctx) // Called when a function in declared
    {
        // Language stores parameters in vardec to make it look better on tree and increase functionality
        List<SimpleLangParser.Typed_idfrContext> paramList = (ctx.vardec() != null) ? ctx.vardec().typed_idfr() : null; // need to check that param list is not null when fetching it from main function

        //System.out.println("Visiting Function " + (ctx.typed_idfr().Idfr().getText())); // Functions name for Debugging
        //System.out.println("Has This Many Parameters " + (paramList != null ? paramList.size() : 0)); // How many parameters for Debugging

        if ((paramList != null ? paramList.size() : 0) > 0){

            visit(ctx.vardec());
        }

        // Visit the body of the function
        //System.out.println("Visiting Body of dec: " + ctx.typed_idfr().Idfr().getText());
        return visit(ctx.body());
    }

    @Override
    public Integer visitVardec(SimpleLangParser.VardecContext ctx) {

        // Visits all the parameters when declaring a function

        for (SimpleLangParser.Typed_idfrContext param : ctx.typed_idfr()) { // goes through every parameter
            //String parameterName = param.Idfr().getText(); // Gets parameters name
            //String parameterType = param.type().getText(); // Gets parameter type: int or bool ( or unit )
            //System.out.println("Parameter: " + parameterName + ", Type: " + parameterType); // Used for Debugging

            // Process each parameter in the list
            visit(param.Idfr());

        }
        return null;
    }

    @Override public Integer visitInvokeExpr(SimpleLangParser.InvokeExprContext ctx)
    {
        // deals with when a function is called e.g fun(5, 4), it has arguments and connects to the body of the functions declaration which is stored in global functions

        // Must deal now with multiple arguments for a function instead of just one, can get args easily from context but not parameters (must fetch from global functions)
        String functionName = ctx.Idfr().getText(); // gets function name from context
        SimpleLangParser.DecContext dec = global_funcs.get(functionName); // gets function declaration from name using the global functions map
        //System.out.println("Invoke Function : " + functionName); // Used for Debugging

        Map<String, Integer> newFrame = new HashMap<>();

        if (dec.vardec() != null) { // checks it has parameters, can be any function including main

            List<SimpleLangParser.Typed_idfrContext> paramList = dec.vardec().typed_idfr(); // Gets list of parameters

            for (int i = 0; i < paramList.size(); i++) { // loops through all the parameters in the function dec, assume they cant be left empty and type is correct
                SimpleLangParser.Typed_idfrContext param = paramList.get(i); // gets parameter, these are stored in the paramList which is defined when defining what a function (dec) is in the grammar language
                SimpleLangParser.ExpContext arg = ctx.arguments().args.get(i); // gets the arguments from the context

                // Evaluate the argument and add it to the new frame under the name of the parameter, so fun(int x) - > fun(5) -> x = 5
                newFrame.put(param.Idfr().getText(), visit(arg));
            }
        }
        frames.push(newFrame); // pushes frame onto stack

        Integer result = visit(dec.body()); // visits body of the function that has been called, so fun() will go an visit the function int fun() 's body

        frames.pop(); // Pop the frame after visiting the invoked function

        return result;
    }

    @Override public Integer visitTyped_idfr(SimpleLangParser.Typed_idfrContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitType(SimpleLangParser.TypeContext ctx)
    {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitBody(SimpleLangParser.BodyContext ctx) {

        // Visits body of a function
        Integer returnValue = null;
        for (int i = 0; i < ctx.ene.size(); ++i) {
            SimpleLangParser.ExpContext exp = ctx.ene.get(i); // ene is the collection of expressions inside a body or block
            //System.out.println("visiting All Expressions in " + exp.getText()); // Used for Debugging
            returnValue = visit(exp); // updates return value with each expression
        }
        return returnValue; // last return value is the result of all the expressions sequential evaluation

    }

    @Override public Integer visitBlock(SimpleLangParser.BlockContext ctx)
    {
        // Visits block inside the body of a function, does the same thing as visitBody but different structural contexts within the program
        Integer returnValue = null;
        for (int i = 0; i < ctx.ene.size(); ++i) {
            SimpleLangParser.ExpContext exp = ctx.ene.get(i);
            returnValue = visit(exp);
        }
        return returnValue;
    }

    @Override public Integer visitAssignExpr(SimpleLangParser.AssignExprContext ctx)
    {
        // Assigns a value to an identifier e.g a = 5, there is no type checking here as we assume it's not needed

        //System.out.println("Type Assignment visited"); // Used for Debugging
        String variableName = ctx.Idfr().getText(); // gets variable name from idfr

        // Evaluate the expression on the right-hand side
        Integer expressionValue = visit(ctx.exp());

        // Assign the result to the variable in the current frame
        frames.peek().put(variableName, expressionValue);
        return null;

    }

    @Override
    public Integer visitTypedAssignExpr(SimpleLangParser.TypedAssignExprContext ctx) {

        // Similar to AssignExpr but for typed identifiers instead. e.g. int x = 1; Once again no type checking

        //System.out.println("Type Assignment visited"); // Used for Debugging
        String variableName = ctx.typed_idfr().Idfr().getText(); // gets variable name, now it's a typed_idfr we get the idfr from

        // Evaluate the expression on the right-hand side
        Integer expressionValue = visit(ctx.exp());

        // Assign the result to the variable in the current frame
        frames.peek().put(variableName, expressionValue);
        return null;
    }

    @Override
    public Integer visitParenExpr(SimpleLangParser.ParenExprContext ctx) {
        return null;
    }

    @Override public Integer visitBinOpExpr(SimpleLangParser.BinOpExprContext ctx) {

        // Called when binary operation is used between two expressions, no type checking but assume only int +/-* another int, and bool ><= (etc) bool
        // System.out.println("Visited BinOp Expression"); // Used for Debugging

        SimpleLangParser.ExpContext operand1 = ctx.exp(0); // gets operand name e.g x, y, from the first part of the binop expression (exp binop exp)
        Integer oprnd1 = visit(operand1);
        //System.out.println("Operand 1: " + operand1.getText() + " Result: " + oprnd1); // Used for Debugging

        //System.out.println("Binary Operator: " + ctx.binop().getText()); // Used for Debugging

        SimpleLangParser.ExpContext operand2 = ctx.exp(1); // gets operand name e.g x, y, from the second part of the binop expression (exp binop exp)
        Integer oprnd2 = visit(operand2);
        //System.out.println("Operand 2: " + operand2.getText() + " Result: " + oprnd2); // Used for Debugging

        // Check if either operand is null - shouldn't happen as code is not meant to have user errors in it
        if (oprnd1 == null || oprnd2 == null) {
            return 0;
        }

        switch (((TerminalNode) (ctx.binop().getChild(0))).getSymbol().getType()) { // Handles logic for return values based on the operand values and the binop used

            case SimpleLangParser.Eq: // Equals / ==

                return ((Objects.equals(oprnd1, oprnd2)) ? 1 : 0);


            case SimpleLangParser.Less:// Less Than / <

                return ((oprnd1 < oprnd2) ? 1 : 0);



            case SimpleLangParser.Greater: // Greater Than / >
                return ((oprnd1 > oprnd2) ? 1 : 0);


            case SimpleLangParser.GreaterEq: // Greater Than or Equal too / >=
                return ((oprnd1 >= oprnd2) ? 1 : 0);


            case SimpleLangParser.LessEq: // Less Than or Equal too / <=
                return ((oprnd1 <= oprnd2) ? 1 : 0);


            // Assume no type checking needed and both operands are of type bool when user calls AND, OR, XOR. Boolean values are 1 for true and 0 for false.
            case SimpleLangParser.And: // And / &&

                return (oprnd1 != 0 && oprnd2 != 0) ? 1 : 0;


            case SimpleLangParser.Or: // Or / ||
                return (oprnd1 != 0 || oprnd2 != 0) ? 1 : 0;


            case SimpleLangParser.Xor: // Xor / ^^
                return ((oprnd1 != 0) ^ (oprnd2 != 0)) ? 1 : 0;


            case SimpleLangParser.Plus: // Add / +

                return oprnd1 + oprnd2;


            case SimpleLangParser.Minus: // Subtract / -

                return oprnd1 - oprnd2;


            case SimpleLangParser.Times: // Multiply / *

                return oprnd1 * oprnd2;


            case SimpleLangParser.Divide: // Divide / /

                return oprnd1 / oprnd2;


            default: {
                throw new RuntimeException("Shouldn't be here - wrong binary operator.");
            }

        }
    }

    @Override public Integer visitBlockExpr(SimpleLangParser.BlockExprContext ctx) { // Visits Block Expression, goes to visitBlock
        //System.out.println("Visited Block Expression"); // Used for Debugging
        return visit(ctx.block());
    }

    @Override public Integer visitIfExpr(SimpleLangParser.IfExprContext ctx) // Used for if then else expressions. Assume if there is an If there must be a then and a else each with a block of code (can be empty or skip)
    {
        //System.out.println("Visited If Expression"); // Used for Debugging

        SimpleLangParser.ExpContext cond = ctx.exp();
        //System.out.println("If Condition Exp: " + ctx.exp().getText()); // Used for Debugging
        Integer condValue = visit(cond); // visits condition of if and returns value based on its expression being true or false
        //System.out.println("Condition Value of IF (1 = then, 0 = else): " + condValue); // Used for Debugging
        if (condValue > 0) { // means condition of if exp was 1 meaning true, so it should go to the then block

            SimpleLangParser.BlockContext thenBlock = ctx.block(0); // fetches then block
            //System.out.println("Then Block"); // Used for Debugging
            return visit(thenBlock); // Visits Then Block

        } else { // means condition of if exp was 0 meaning false, so it should go to the else block

            SimpleLangParser.BlockContext elseBlock = ctx.block(1); // fetches else block as It's after then block
            //System.out.println("Else Block"); // Used for Debugging
            return visit(elseBlock); // Visits Else Block

        }
    }

    @Override
    public Integer visitRepeatUntilExpr(SimpleLangParser.RepeatUntilExprContext ctx) { // repeat block until expression is true
        do {
            // Execute the code block inside the repeat until loop
            visit(ctx.block());

            // Repeat until the condition is true
        } while ( visit(ctx.exp()) == 0); // While the return value of the expression is equal to 0 which means false, keep repeating the do block
        return null;
    }


    @Override public Integer visitPrintExpr(SimpleLangParser.PrintExprContext ctx) { // print statement of type unit

        //System.out.println("PRINT Visited Print Expression"); // Used for Debugging
        SimpleLangParser.ExpContext exp = ctx.exp();

        if (((TerminalNode) exp.getChild(0)).getSymbol().getType() == SimpleLangParser.Space) {

            System.out.print(" "); // If the exp symbol is of type space then it prints a space

        } else if (((TerminalNode) exp.getChild(0)).getSymbol().getType() == SimpleLangParser.NewLine) {

            System.out.println(); // If the exp symbol is of type newLine then it prints a new line

        } else { // otherwise it prints out the value of the expression e.g 8

            System.out.print(visit(exp));
        }
        return null;
    }

    @Override
    public Integer visitWhileExpr(SimpleLangParser.WhileExprContext ctx) { // Simple while loop that visits the block while the value of expression is 1 (true)
        //System.out.println("Visited While Expression"); // Used for Debugging
        while (visit(ctx.exp()) > 0) {
            visit(ctx.block());
        }
        return null;
    }

    @Override public Integer visitIdExpr(SimpleLangParser.IdExprContext ctx) {

        // Returns value of identifier by looking it up in the frame and returning the value e.g  a = 5, visitIdExpr( a ctx ) returns 5
        return frames.peek().get(ctx.Idfr().getText());
    }

    @Override public Integer visitIntExpr(SimpleLangParser.IntExprContext ctx)
    {
        // Returns integer from string text to be used in operations
        return Integer.parseInt(ctx.IntLit().getText());

    }

    @Override
    public Integer visitBoolExpr(SimpleLangParser.BoolExprContext ctx) {

        String boolValue = ctx.getText(); // Get the boolean literal as a string
        // Convert the boolean literal to an integer (1 for true, 0 for false), used for binop calculations and loops etc
        return boolValue.equals("true") ? 1 : 0;
    }

    @Override public Integer visitSpaceExpr(SimpleLangParser.SpaceExprContext ctx) {
        return null;
    }

    @Override
    public Integer visitNewLineExpr(SimpleLangParser.NewLineExprContext ctx) {
        return null;
    }

    @Override
    public Integer visitArguments(SimpleLangParser.ArgumentsContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitEqBinop(SimpleLangParser.EqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitLessBinop(SimpleLangParser.LessBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitGreaterBinop(SimpleLangParser.GreaterBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitLessEqBinop(SimpleLangParser.LessEqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitGreaterEqBinop(SimpleLangParser.GreaterEqBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override public Integer visitPlusBinop(SimpleLangParser.PlusBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitMinusBinop(SimpleLangParser.MinusBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }
    @Override public Integer visitTimesBinop(SimpleLangParser.TimesBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitDivideBinop(SimpleLangParser.DivideBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitAndBinop(SimpleLangParser.AndBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitOrBinop(SimpleLangParser.OrBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

    @Override
    public Integer visitXorBinop(SimpleLangParser.XorBinopContext ctx) {
        throw new RuntimeException("Should not be here!");
    }

}
