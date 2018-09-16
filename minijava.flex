import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;

%%
%public
%class Lexer
%cup
%implements sym
%char
%line
%column

%{
    StringBuffer string = new StringBuffer();
    String className = null;
    public Lexer(java.io.Reader in, ComplexSymbolFactory sf){
    	this(in);
	    symbolFactory = sf;
    }
    ComplexSymbolFactory symbolFactory;

  private Symbol symbol(String name, int sym) {
       return symbolFactory.newSymbol(name, sym, new Location(yyline+1,yycolumn+1,yychar), new Location(yyline+1,yycolumn+yylength(),yychar+yylength()));
  }

  private Symbol symbol(String name, int sym, Object val) {
      Location left = new Location(yyline+1,yycolumn+1,yychar);
      Location right= new Location(yyline+1,yycolumn+yylength(), yychar+yylength());
      return symbolFactory.newSymbol(name, sym, left, right,val);
  }

  private Symbol symbol(String name, int sym, Object val,int buflength) {
      Location left = new Location(yyline+1,yycolumn+yylength()-buflength,yychar+yylength()-buflength);
      Location right= new Location(yyline+1,yycolumn+yylength(), yychar+yylength());
      return symbolFactory.newSymbol(name, sym, left, right,val);
  }

  private void error(String message) {
    System.out.println("Error at line "+(yyline+1)+", column "+(yycolumn+1)+" : "+message);
  }
%}

%eofval{
     return symbolFactory.newSymbol("EOF", EOF, new Location(yyline+1,yycolumn+1,yychar), new Location(yyline+1,yycolumn+1,yychar+1));
%eofval}

IntLiteral = 0 | [1-9][0-9]*

new_line = \r|\n|\r\n;
white_space = {new_line} | [ \t\f]
IdName = [a-z][A-Za-z0-9_]*
ClassName = [A-Z][A-Za-z0-9_]*


%state STRING

%%

<YYINITIAL>{
    /* keywords */
    "class"           { return symbol("class", CLASS); }
    "while"           { return symbol("while", WHILE); }
    "readln"          { return symbol("readln", READLN); }
    "println"         { return symbol("println", PRINTLN); }
    "if"              { return symbol("if", IF); }
    "else"            { return symbol("else", ELSE); }
    "this"            { return symbol("this", THIS); }
    "new"             { return symbol("new", NEW); }
    "null"            { return symbol("null", NULL); }
    "Void main"       { return symbol("mainfunc", MAINFUNC); }
    "return"          { return symbol("return", RETURN); }

    /* literals */
    {IntLiteral}      { return symbol("IntConst",INTCONST, new Integer(Integer.parseInt(yytext()))); }
    "true"            { return symbol("true", BOOLCONST, true); }
    "false"           { return symbol("false", BOOLCONST, false); }

    /* operators */
    "+"               { return symbol("+",PLUS); }
    "-"               { return symbol("+",MINUS); }
    "*"               { return symbol("+",TIMES); }
    "/"               { return symbol("+",DIV); }
    "<"               { return symbol("<",LT); }
    ">"               { return symbol(">",GT); }
    "<="              { return symbol("<=",LEQ); }
    ">="              { return symbol(">=",GEQ); }
    "=="              { return symbol("==",EQ); }
    "!="              { return symbol("!=",NEQ); }
    "||"              { return symbol("||",OR); }
    "&&"              { return symbol("&&",AND); }

    /* blocks and statements */
    "("               { return symbol("(",LPAREN); }
    ")"               { return symbol(")",RPAREN); }
    "{"               { return symbol("{", LBLOCK); }
    "}"               { return symbol("}", RBLOCK); }
    ";"               { return symbol(";", ENDSTMT); }
    ","               { return symbol(",", COMMA); }
    "."               { return symbol(",", DOT); }

    /* identifiers */
    {IdName}          { String name = yytext(); return symbol("<id>"+name, IDNAME, name); }
    {ClassName}       { String name = yytext(); return symbol("<class>"+name, CLASSNAME, name); }

    {white_space}     { /* ignore */ }

    "\""              { string.setLength(0); yybegin(STRING); }
}

<STRING> {
    \"                             { yybegin(YYINITIAL);
                                     return symbol("String", STRING_LITERAL, string.toString()); }
    [^\n\r\"\\]+                   { string.append( yytext() ); }
    \\t                            { string.append('\t'); }
    \\n                            { string.append('\n'); }

    \\r                            { string.append('\r'); }
    \\\"                           { string.append('\"'); }
    \\                             { string.append('\\'); }
}

/* error fallback */
[^]              {  /* throw new Error("Illegal character <"+ yytext()+">");*/
		                error("Illegal character <"+ yytext()+">");
                 }
