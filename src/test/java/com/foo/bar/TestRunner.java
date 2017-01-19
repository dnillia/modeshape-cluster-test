package com.foo.bar;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

/**
 * The command-line test runner.
 * 
 * @author Illia Khokholkov
 *
 */
public class TestRunner {

    public static void main(String[] args) throws Exception {
        JUnitCore core = new JUnitCore();
        core.addListener(new TextListener(System.out));
        core.run(TestSuite.class);
    }
}
