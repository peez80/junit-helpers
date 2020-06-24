package de.stiffi.testing.junit.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * With this rule it's possible to easily repeat single Tests multiple times (e.g. if you find out, that every second call fails)
 * Inspired and usage: https://www.codeaffine.com/2013/04/10/running-junit-tests-repeatedly-without-loops/
 */
public class RepeatRule implements TestRule {

    @Retention( RetentionPolicy.RUNTIME )
    @Target( {
            java.lang.annotation.ElementType.METHOD
    } )
    public @interface Repeat {
        public abstract int times();
    }

    private static class RepeatStatement extends Statement {

        private final int times;
        private final Statement statement;

        private RepeatStatement( int times, Statement statement ) {
            this.times = times;
            this.statement = statement;
        }

        @Override
        public void evaluate() throws Throwable {
            for( int i = 0; i < times; i++ ) {
                System.out.println("");
                System.out.println("");
                System.out.println("============== RepeatRule: " + (i+1) + " / " + times + " ========================");
                statement.evaluate();
            }
        }


    }

    @Override
    public Statement apply(
            Statement statement, Description description )
    {
        Statement result = statement;
        Repeat repeat = description.getAnnotation( Repeat.class );
        if( repeat != null ) {
            int times = repeat.times();
            result = new RepeatStatement( times, statement );
        }
        return result;
    }
}
