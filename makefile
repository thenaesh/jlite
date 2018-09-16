all: compile run

compile:
	jflex minijava.flex
	java -jar java-cup-11b.jar -interface -parser Parser minijava.cup
	javac -cp java-cup-11b-runtime.jar:. *.java

run:
	java -cp java-cup-11b-runtime.jar:. Parser tests/sample.1.pl
	java -cp java-cup-11b-runtime.jar:. Parser tests/sample.2.pl
	java -cp java-cup-11b-runtime.jar:. Parser tests/sample.3.pl
	java -cp java-cup-11b-runtime.jar:. Parser tests/sample.4.pl
	java -cp java-cup-11b-runtime.jar:. Parser tests/sample.5.pl

clean:
	rm Lexer.java Parser.java sym.java
	rm *.class
