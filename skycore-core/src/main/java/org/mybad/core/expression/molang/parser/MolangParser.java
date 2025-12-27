package org.mybad.core.expression.molang.parser;

import org.mybad.core.expression.molang.ast.*;
import org.mybad.core.expression.molang.ast.math.MathExpression;
import org.mybad.core.expression.molang.ast.operator.AdditionExpression;
import org.mybad.core.expression.molang.ast.operator.DivisionExpression;
import org.mybad.core.expression.molang.ast.operator.MultiplicationExpression;
import org.mybad.core.expression.molang.ast.operator.SubtractionExpression;
import org.mybad.core.expression.molang.reference.ReferenceType;
import org.mybad.core.expression.molang.exception.MolangLexException;
import org.mybad.core.expression.molang.exception.MolangParseException;
import org.mybad.core.expression.molang.lexer.LexedMolang;
import org.mybad.core.expression.molang.lexer.MolangLexer;
import org.mybad.core.expression.molang.lexer.MolangTokenInstance;
import org.mybad.core.expression.molang.lexer.MolangTokenType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mybad.core.expression.molang.lexer.MolangTokenType.*;

/**
 * Recursive descent parser for the Molang language. Visit {@link MolangParser#parse(String)} to get started.
 */
public class MolangParser {

    public static final int FLAG_NONE = 0x00000000;
    public static final int FLAG_SIMPLIFY = 0x00000001;
    public static final int FLAG_CACHE = 0x00000100;

    private static final Map<String, MolangExpression> AST_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, MolangExpression> SIMPLIFIED_AST_CACHE = new ConcurrentHashMap<>();

    private final LexedMolang input;
    private int cursor = 0;

    private MolangParser(LexedMolang input) {
        this.input = input;
    }

    /**
     * Parses the {@link String} {@code input} into a simplified {@link MolangExpression} with simplification ({@link MolangParser#FLAG_SIMPLIFY}) and expression caching ({@link MolangParser#FLAG_CACHE}) enabled.
     *
     * <p>
     * If the input contains invalid tokens or non-Molang syntax, a {@link MolangLexException} or {@link MolangParseException} will be thrown.
     *
     * @param input the input to parse into a {@link MolangExpression}
     * @return the parsed and simplified {@link MolangExpression}
     * @throws MolangLexException   if an invalid token was found in the {@code input}
     * @throws MolangParseException if an expression could not be parsed from the tokenized {@code input}
     */
    @ApiStatus.AvailableSince("1.0.0")
    public static MolangExpression parse(String input) throws MolangLexException, MolangParseException {
        return parse(input, FLAG_CACHE | FLAG_SIMPLIFY);
    }

    /**
     * Parses the {@link String} {@code input} into a simplified {@link MolangExpression}. {@code flags} can be used to control
     * properties about how the expression is calculated:
     *
     * <ul>
     *     <li>{@link MolangParser#FLAG_NONE} for no special options</li>
     *     <li>{@link MolangParser#FLAG_SIMPLIFY} to simplify the returned {@link MolangExpression}</li>
     *     <li>{@link MolangParser#FLAG_CACHE} to check cache for (or parse & cache) the expression input</li>
     * </ul>
     *
     * <p>
     * If the input contains invalid tokens or non-Molang syntax, a {@link MolangLexException} or {@link MolangParseException} will be thrown.
     *
     * @param input the input to parse into a {@link MolangExpression}
     * @param flags bitwise flags for control over parsing and caching behavior
     * @return the parsed and simplified {@link MolangExpression}
     * @throws MolangLexException   if an invalid token was found in the {@code input}
     * @throws MolangParseException if an expression could not be parsed from the tokenized {@code input}
     */
    @ApiStatus.AvailableSince("1.0.0")
    public static MolangExpression parse(String input, int flags) throws MolangLexException, MolangParseException {
        boolean simplify = (flags & FLAG_SIMPLIFY) == FLAG_SIMPLIFY;
        boolean cache = (flags & FLAG_CACHE) == FLAG_CACHE;

        // If the caller does not want to use cache, parse and return early:
        if(!cache) {
            return parse(MolangLexer.lex(input), simplify);
        }

        // Check if we can avoid lexing & parsing by falling back to the cache.
        // Pick cache based on whether simplify is set - we don't want non-simplified AST getting
        // returned to users asking for simplified cache.
        Map<String, MolangExpression> cacheMap = simplify ? SIMPLIFIED_AST_CACHE : AST_CACHE;
        @Nullable MolangExpression cached = cacheMap.get(input);
        if(cached == null) {
            cached = parse(MolangLexer.lex(input), true);
            cacheMap.put(input, cached);
        }

        return cached;
    }

    /**
     * Parse the {@link LexedMolang} into a {@link MolangExpression}.
     *
     * @param input the {@link LexedMolang} input to parse, obtained from {@link MolangLexer}
     * @param simplify whether the returned {@link MolangExpression} should be simplified for faster evaluation
     * @return a new {@link MolangExpression} parsed from the {@link LexedMolang}
     * @throws MolangParseException if a grammar syntax issue occurs while parsing
     */
    @ApiStatus.AvailableSince("1.0.0")
    public static MolangExpression parse(LexedMolang input, boolean simplify) throws MolangParseException {
        MolangExpression expression = new MolangParser(input).expression();
        if(simplify) {
            expression = expression.simplify();
        }

        return expression;
    }

    private MolangExpression expressionArgument(boolean required) throws MolangParseException {
        MolangExpression expression = expression();

        if(!match(COMMA)) {
            if(required) {
                throw new MolangParseException("Expected to find a comma after argument input!");
            }
        }

        return expression;
    }

    private MolangExpression expression() throws MolangParseException {
        return ternary();
    }

    private MolangExpression ternary() throws MolangParseException {
        MolangExpression left = or();

        // Left = the condition for the ternary operator.
        while (match(QUESTION)) {
            MolangExpression trueBranch = or();

            if(!match(COLON)) {
                throw new MolangParseException("Expected to find ':' after the true branch in conditional ternary expression!");
            }

            MolangExpression falseBranch = or();
            left = new TernaryExpression(left, trueBranch, falseBranch);
        }

        return left;
    }

    private MolangExpression or() throws MolangParseException {
        MolangExpression left = and();
        while (match(DOUBLE_PIPE)) {
            MolangTokenInstance operator = previous();
            left = new BinaryExpression(left, and(), operator.type());
        }

        return left;
    }

    private MolangExpression and() throws MolangParseException {
        MolangExpression left = comparison();

        while (match(DOUBLE_AMPERSAND)) {
            MolangTokenInstance operator = previous();

            // Read the right-hand of the && expression.
            try {
                MolangExpression right = comparison();
                left = new BinaryExpression(left, right, operator.type());
            } catch (MolangParseException parseException) {
                throw new MolangParseException("Expected to find an expression after '&&' (AND) operator. Did you forget the second value?");
            }
        }

        return left;
    }

    private MolangExpression comparison() throws MolangParseException {
        MolangExpression left = term();

        while (match(LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL)) {
            MolangTokenInstance operator = previous();
            left = new BinaryExpression(left, term(), operator.type());
        }

        return left;
    }

    private MolangExpression term() throws MolangParseException {
        MolangExpression left = factor();

        // +
        while (match(PLUS)) {
            left = new AdditionExpression(left, factor());
        }

        // -
        while (match(MINUS)) {
            left = new SubtractionExpression(left, factor());
        }

        return left;
    }

    private MolangExpression factor() throws MolangParseException {
        MolangExpression left = unary();

        // *
        while (match(STAR)) {
            left = new MultiplicationExpression(left, unary());
        }

        // /
        while (match(SLASH)) {
            left = new DivisionExpression(left, unary());
        }

        return left;
    }

    private MolangExpression unary() throws MolangParseException {
        if(match(MINUS, BANG)) {
            MolangTokenInstance operator = previous();
            MolangExpression right = parenthesis();
            return new UnaryExpression(right, operator.type());
        }

        return parenthesis();
    }

    private MolangExpression parenthesis() throws MolangParseException {
        if(match(LEFT_PAREN)) {
            MolangExpression interior = expression();
            if(!match(RIGHT_PAREN)) {
                throw new MolangParseException("Expected to find closing ')' to end opening '('!");
            }

            return interior;
        }

        return unit();
    }

    private MolangExpression unit() throws MolangParseException {
        if(match(NUMBER)) {
            return new ConstantExpression((float) previous().value());
        }

        if(match(IDENTIFIER)) {

            // variable.<x>
            MolangTokenInstance token = previous();
            Object value = token.value();
            if(value instanceof String) {
                String string = (String) value;
                if(!match(DOT)) {
                    throw new MolangParseException(String.format("Expected to find reference . after '%s'!", string));
                }

                switch (string) {
                    case "query":
                    case "q":
                        return reference(ReferenceType.QUERY);
                    case "variable":
                    case "v":
                        return reference(ReferenceType.VARIABLE);
                    case "math":
                    case "m":
                        return math();
                    default:
                        throw new IllegalStateException("Unexpected value: " + string);
                }
            }
        }

        throw new MolangParseException("Failed to parse next token: " + (cursor == 0 ? peek() : previous()));
    }

    private MolangExpression math() throws MolangParseException {
        if(match(IDENTIFIER)) {
            String function = previous().lexeme();

            // only math.pi doesn't have a method call
            if(!function.equals("pi")) {
                if(!match(LEFT_PAREN)) {
                    throw new MolangParseException("Expected to find opening parenthesis '(' when starting math call!");
                }
            }

            MolangExpression mathExpression;
            switch(function) {
                case "abs":
                    mathExpression = new MathExpression.Abs(expression());
                    break;
                case "acos":
                    mathExpression = new MathExpression.Acos(expression());
                    break;
                case "asin":
                    mathExpression = new MathExpression.Asin(expression());
                    break;
                case "atan":
                    mathExpression = new MathExpression.Atan(expression());
                    break;
                case "atan2":
                    mathExpression = new MathExpression.Atan2(expressionArgument(true), expression());
                    break;
                case "ceil":
                    mathExpression = new MathExpression.Ceil(expression());
                    break;
                case "clamp":
                    mathExpression = new MathExpression.Clamp(expressionArgument(true), expressionArgument(true), expression());
                    break;
                case "cos":
                    mathExpression = new MathExpression.Cos(expression());
                    break;
                case "die_roll":
                    mathExpression = new MathExpression.DieRoll(expressionArgument(true), expressionArgument(true), expression());
                    break;
                case "die_roll_integer":
                    mathExpression = new MathExpression.DieRollInteger(expressionArgument(true), expressionArgument(true), expression());
                    break;
                case "exp":
                    mathExpression = new MathExpression.Exp(expression());
                    break;
                case "floor":
                    mathExpression = new MathExpression.Floor(expression());
                    break;
                case "hermite_blend":
                    mathExpression = new MathExpression.HermiteBlend(expression());
                    break;
                case "lerp":
                    mathExpression = new MathExpression.Lerp(expressionArgument(true), expressionArgument(true), expression());
                    break;
                // case "lerprotate":
                //     mathExpression = new MathExpression.LerpRotate(expression());
                //     break;
                case "ln":
                    mathExpression = new MathExpression.Ln(expression());
                    break;
                case "max":
                    mathExpression = new MathExpression.Max(expressionArgument(true), expression());
                    break;
                case "min":
                    mathExpression = new MathExpression.Min(expressionArgument(true), expression());
                    break;
                case "min_angle":
                    mathExpression = new MathExpression.MinAngle(expression());
                    break;
                case "mod":
                    mathExpression = new MathExpression.Mod(expressionArgument(true), expression());
                    break;
                case "pi":
                    mathExpression = new MathExpression.Pi();
                    break;
                case "pow":
                    mathExpression = new MathExpression.Pow(expressionArgument(true), expression());
                    break;
                case "random":
                    mathExpression = new MathExpression.Random(expressionArgument(true), expression());
                    break;
                case "random_integer":
                    mathExpression = new MathExpression.RandomInteger(expressionArgument(true), expression());
                    break;
                case "round":
                    mathExpression = new MathExpression.Round(expression());
                    break;
                case "sin":
                    mathExpression = new MathExpression.Sin(expression());
                    break;
                case "sqrt":
                    mathExpression = new MathExpression.Sqrt(expression());
                    break;
                case "trunc":
                    mathExpression = new MathExpression.Trunc(expression());
                    break;
                default:
                    throw new IllegalStateException("Unexpected math function: " + function);
            }

            if(!function.equals("pi")) {
                if(!match(RIGHT_PAREN)) {
                    throw new MolangParseException("Expected to find closing parenthesis ')' when ending math call!");
                }
            }

            return mathExpression;
        }

        throw new MolangParseException("Expected to find math function name after 'math.'");
    }

    private MolangExpression reference(ReferenceType type) throws MolangParseException {
        if(match(IDENTIFIER)) {
            return new ReferenceExpression(type, previous().lexeme());
        }

        throw new MolangParseException("Expected to find name after .");
    }

    private MolangTokenInstance peek() {
        return input.tokens().get(cursor);
    }

    private MolangTokenInstance previous() {
        return input.tokens().get(cursor - 1);
    }

    /**
     * Returns {@code true} if the next available {@link MolangTokenInstance} is one of the specified {@link MolangTokenType}, otherwise {@code false} if it does not match.
     *
     * <p>
     * If this parser is at the end of its token input, {@code false} is returned.
     *
     * @param type any {@link MolangTokenType} to try to match the next {@link MolangTokenInstance} against
     * @return {@code true} if the next token instance matches the given type(s), otherwise {@code false}
     */
    private boolean match(MolangTokenType... type) {
        if(cursor >= input.tokens().size()) {
            return false;
        }

        MolangTokenType next = input.tokens().get(cursor).type();
        for (MolangTokenType check : type) {
            if(next == check) {
                cursor++;
                return true;
            }
        }

        return false;
    }
}
