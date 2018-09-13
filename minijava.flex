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
BooleanLiteral = true|false

new_line = \r|\n|\r\n;
white_space = {new_line} | [ \t\f]
Identifier = [a-z][A-Za-z0-9_]*
ClassName = [A-Z][A-Za-z0-9_]*


%state STRING
%state CLASS

%%

<YYINITIAL>{
    /* literals */
    {IntLiteral}      { return symbol("Intconst",INTCONST, new Integer(Integer.parseInt(yytext()))); }

    /* separators */
    "("               { return symbol("(",LPAREN); }
    ")"               { return symbol(")",RPAREN); }
    "+"               { return symbol("+",PLUS); }
    "-"               { return symbol("+",MINUS); }
    "*"               { return symbol("+",TIMES); }
    "/"               { return symbol("+",DIV); }

    {white_space}     { /* ignore */ }

    "\""              { string.setLength(0); yybegin(STRING); }
    "class "          { yybegin(CLASS); }
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

<CLASS> {
  {ClassName}                      { className = yytext(); }
  "{"                              { if (className != null) return symbol("ClassBegin", CLASS_BEGIN, className);
                                     else error("Class declaration not followed by a class name"); }
  "}"                              { yybegin(YYINITIAL);
                                     String className_ = className;
                                     return symbol("ClassEnd", CLASS_END, className_); }
  {white_space}                    {}
}

/* error fallback */
[^]              {  /* throw new Error("Illegal character <"+ yytext()+">");*/
		                error("Illegal character <"+ yytext()+">");
                 }
