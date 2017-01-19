package com.foo.bar;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * The suite of tests to run.
 * 
 * @author Illia Khokholkov
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ RowLockContentionTest.class })
public class TestSuite {}
